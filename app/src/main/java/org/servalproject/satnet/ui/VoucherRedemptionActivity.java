/**
 * Copyright (C) 2025 SATNET AFRICA
 *
 * This file is part of SATNET AFRICA (http://satnetafrica.org)
 *
 * SATNET AFRICA is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.servalproject.satnet.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.servalproject.R;
import org.servalproject.bitcoin.BitcoinWallet;
import org.servalproject.bitcoin.security.WalletEncryption;
import org.servalproject.permissions.RuntimePermissionGate;
import org.servalproject.satnet.SatnetRoleConflictPolicy;
import org.servalproject.satnet.SatnetRoleManager;
import org.servalproject.satnet.SatnetRuntimeConfig;
import org.servalproject.satnet.SatnetStartupGate;
import org.servalproject.satnet.WalletSessionStore;
import org.servalproject.satnet.SatnetAuthorizationEngine;
import org.servalproject.satnet.verifier.SettlementVerifier;
import org.servalproject.voucher.BitcoinVoucher;
import org.servalproject.voucher.VoucherLedger;
import org.servalproject.voucher.VoucherParticipantSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Voucher Redemption Screen
 *
 * Features:
 * - Scan QR code for voucher
 * - Manual code entry
 * - Verify voucher validity
 * - Redeem to wallet
 * - Offline capability
 */
public class VoucherRedemptionActivity extends AppCompatActivity {
    private static final String TAG = "VoucherRedemption";
    private static final int CAMERA_PERMISSION_REQUEST = 1;
    private static final String[] CAMERA_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private Button scanButton;
    private Button manualEntryButton;
    private Button redeemButton;
    private LinearLayout manualEntryLayout;
    private EditText voucherCodeInput;
    private TextView stageBadgeText;
    private TextView runtimeStatusText;
    private TextView voucherDetailsText;
    private LinearLayout trustBadgeRow;
    private TextView manifestBadgeText;
    private TextView ledgerBadgeText;
    private TextView rotationBadgeText;
    private LinearLayout auditHistorySection;
    private TextView auditHistoryText;
    private LinearLayout merchantBadgeRow;
    private TextView merchantBadgeText;
    private TextView merchantStatusBadgeText;

    private VoucherLedger voucherLedger;
    private SatnetRoleManager roleManager;
    private BitcoinWallet wallet;
    private BitcoinVoucher currentVoucher;
    private boolean redeemBlockedByPolicy = false;
    private String redeemBlockedMessage;
    private boolean pendingScanAfterPermission = false;
    private String walletId;
    private String walletSessionToken;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private boolean busy = false;
    private final ActivityResultLauncher<Intent> qrScannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK) {
                    Toast.makeText(this, R.string.satnet_voucher_scan_cancelled, Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent data = result.getData();
                if (data == null) {
                    Toast.makeText(this, R.string.satnet_voucher_scan_cancelled, Toast.LENGTH_SHORT).show();
                    return;
                }
                String qrPayload = data.getStringExtra(QRScannerActivity.EXTRA_QR_RESULT);
                processQRScanResult(qrPayload);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            SatnetUiSupport.applySecureWindow(this);
            setContentView(R.layout.activity_voucher_redemption);

            voucherLedger = new VoucherLedger(this);
            roleManager = new SatnetRoleManager(this);
            if (!roleManager.hasCapability(SatnetRoleManager.CAP_VOUCHER_REDEEM)) {
                Toast.makeText(this, R.string.satnet_voucher_unavailable_for_role, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            walletId = getIntent().getStringExtra(BitcoinWalletActivity.EXTRA_WALLET_ID);
            if (walletId == null || walletId.trim().isEmpty()) {
                walletId = BitcoinWalletActivity.DEFAULT_WALLET_ID;
            }
            walletSessionToken = getIntent().getStringExtra(WalletSessionStore.EXTRA_SESSION_TOKEN);
            wallet = new BitcoinWallet(this, walletId);

            // Bind UI elements
            SatnetUiSupport.requireView(this, R.id.qr_preview, android.widget.FrameLayout.class, "qr_preview");
            scanButton = SatnetUiSupport.requireView(this, R.id.scan_button, Button.class, "scan_button");
            manualEntryButton = SatnetUiSupport.requireView(this, R.id.manual_entry_button, Button.class, "manual_entry_button");
            redeemButton = SatnetUiSupport.requireView(this, R.id.redeem_button, Button.class, "redeem_button");
            manualEntryLayout = SatnetUiSupport.requireView(this, R.id.manual_entry_layout, LinearLayout.class, "manual_entry_layout");
            voucherCodeInput = SatnetUiSupport.requireView(this, R.id.voucher_code_input, EditText.class, "voucher_code_input");
            stageBadgeText = SatnetUiSupport.requireView(this, R.id.voucher_stage_badge_text, TextView.class, "voucher_stage_badge_text");
            runtimeStatusText = SatnetUiSupport.requireView(this, R.id.voucher_runtime_status_text, TextView.class, "voucher_runtime_status_text");
            voucherDetailsText = SatnetUiSupport.requireView(this, R.id.voucher_details_text, TextView.class, "voucher_details_text");
            trustBadgeRow = SatnetUiSupport.requireView(this, R.id.voucher_trust_badge_row, LinearLayout.class, "voucher_trust_badge_row");
            manifestBadgeText = SatnetUiSupport.requireView(this, R.id.voucher_manifest_badge, TextView.class, "voucher_manifest_badge");
            ledgerBadgeText = SatnetUiSupport.requireView(this, R.id.voucher_ledger_badge, TextView.class, "voucher_ledger_badge");
            rotationBadgeText = SatnetUiSupport.requireView(this, R.id.voucher_rotation_badge, TextView.class, "voucher_rotation_badge");
            auditHistorySection = SatnetUiSupport.requireView(this, R.id.voucher_audit_history_section, LinearLayout.class, "voucher_audit_history_section");
            auditHistoryText = SatnetUiSupport.requireView(this, R.id.voucher_audit_history_text, TextView.class, "voucher_audit_history_text");
            merchantBadgeRow = SatnetUiSupport.requireView(this, R.id.voucher_merchant_badge_row, LinearLayout.class, "voucher_merchant_badge_row");
            merchantBadgeText = SatnetUiSupport.requireView(this, R.id.voucher_merchant_badge, TextView.class, "voucher_merchant_badge");
            merchantStatusBadgeText = SatnetUiSupport.requireView(this, R.id.voucher_merchant_status_badge, TextView.class, "voucher_merchant_status_badge");

            // Click listeners
            scanButton.setOnClickListener(v -> startQRScanner());
            manualEntryButton.setOnClickListener(v -> showManualEntry());
            redeemButton.setOnClickListener(v -> redeemVoucher());

            // Initially disable redeem button
            redeemButton.setEnabled(false);

            refreshRuntimeStatus();
            setBusy(false, null);
        } catch (Throwable t) {
            SatnetUiSupport.failInitialization(this, TAG, t, getString(R.string.satnet_voucher_init_failed));
        }
    }

    private boolean checkCameraPermission() {
        return RuntimePermissionGate.ensurePermissions(this, CAMERA_PERMISSIONS, CAMERA_PERMISSION_REQUEST);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startQRScanner() {
        SatnetStartupGate.Status runtimeStatus = refreshRuntimeStatus();
        if (!runtimeStatus.canUseRoleTools()) {
            Toast.makeText(this, runtimeStatus.getBlockingMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        if (busy) {
            Toast.makeText(this, R.string.satnet_voucher_busy, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!checkCameraPermission()) {
            pendingScanAfterPermission = true;
            Toast.makeText(this, R.string.satnet_voucher_camera_required, Toast.LENGTH_SHORT).show();
            updateScanButtonState();
            return;
        }

        Toast.makeText(this, R.string.satnet_voucher_scanner_opening, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, QRScannerActivity.class);
        qrScannerLauncher.launch(intent);
    }

    private void updateScanButtonState() {
        SatnetStartupGate.Status runtimeStatus = refreshRuntimeStatus();
        boolean granted = hasCameraPermission();
        boolean enabled = !busy && runtimeStatus.canUseRoleTools();
        scanButton.setEnabled(enabled);
        scanButton.setAlpha(enabled ? 1.0f : 0.5f);
        scanButton.setText(!runtimeStatus.canUseRoleTools()
                ? getString(R.string.satnet_voucher_scan_button_warming)
                : (busy
                ? getString(R.string.satnet_voucher_scan_button_busy)
                : (granted
                ? getString(R.string.satnet_voucher_scan_button)
                : getString(R.string.satnet_voucher_scan_button_grant))));
    }

    private void showManualEntry() {
        SatnetStartupGate.Status runtimeStatus = refreshRuntimeStatus();
        if (!runtimeStatus.canUseRoleTools()) {
            Toast.makeText(this, runtimeStatus.getBlockingMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        if (busy) {
            Toast.makeText(this, R.string.satnet_voucher_busy, Toast.LENGTH_SHORT).show();
            return;
        }
        // Show dialog for manual numeric code entry
        // Format: XXXX-XXXX-XXXX-XXXX

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.satnet_voucher_manual_dialog_title);

        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint(R.string.satnet_voucher_manual_dialog_hint);
        builder.setView(input);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String code = input.getText().toString().trim();
            parseManualVoucher(code);
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void parseManualVoucher(String code) {
        try {
            String normalized = code == null ? "" : code.trim();
            if (normalized.isEmpty()) {
                Toast.makeText(this, R.string.satnet_voucher_enter_code, Toast.LENGTH_SHORT).show();
                return;
            }

            if (voucherCodeInput != null) {
                voucherCodeInput.setText(normalized);
            }
            if (normalized.startsWith("satnet_voucher|")) {
                processQRScanResult(normalized);
                return;
            }
            manualEntryLayout.setVisibility(android.view.View.VISIBLE);
            voucherDetailsText.setText(R.string.satnet_voucher_numeric_code_unavailable);
            redeemButton.setEnabled(false);
            currentVoucher = null;
        } catch (Exception e) {
            Toast.makeText(this, R.string.satnet_voucher_invalid_code, Toast.LENGTH_SHORT).show();
        }
    }

    private void processQRScanResult(String qrPayload) {
        SatnetStartupGate.Status runtimeStatus = refreshRuntimeStatus();
        if (!runtimeStatus.canUseRoleTools()) {
            Toast.makeText(this, runtimeStatus.getBlockingMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        setBusy(true, getString(R.string.satnet_voucher_processing));
        backgroundExecutor.execute(() -> {
            try {
                if (qrPayload == null || qrPayload.trim().isEmpty()) {
                    throw new IllegalArgumentException(getString(R.string.satnet_voucher_empty_payload));
                }

                BitcoinVoucher parsedVoucher = BitcoinVoucher.parseQRPayload(qrPayload);
                BitcoinVoucher.ValidationResult validation = parsedVoucher.validate();

                if (!validation.isValid) {
                    runOnUiThread(() -> {
                        if (isFinishing()) {
                            return;
                        }
                        clearDisplayedVoucher();
                        setBusy(false, null);
                        Toast.makeText(this, getString(R.string.satnet_voucher_invalid, validation.message), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                if (voucherLedger.isVoucherRedeemed(parsedVoucher.getVoucherId())) {
                    runOnUiThread(() -> {
                        if (isFinishing()) {
                            return;
                        }
                        clearDisplayedVoucher();
                        setBusy(false, null);
                        Toast.makeText(this, R.string.satnet_voucher_already_redeemed, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                SettlementVerifier.WorkflowMetadataCheck metadataCheck =
                        SettlementVerifier.inspectVoucherMetadata(parsedVoucher, voucherLedger);
                if (!metadataCheck.isValid) {
                    runOnUiThread(() -> {
                        if (isFinishing()) {
                            return;
                        }
                        clearDisplayedVoucher();
                        setBusy(false, null);
                        Toast.makeText(this,
                                getString(R.string.satnet_voucher_metadata_invalid, metadataCheck.message),
                                Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                VoucherParticipantSnapshot participantSnapshot =
                        voucherLedger.getVoucherParticipantSnapshot(parsedVoucher.getVoucherId());
                SatnetRoleConflictPolicy.ConflictCheck conflictCheck = SatnetRoleConflictPolicy.authorizeAction(
                        roleManager == null ? null : roleManager.getParticipantSubjectId(),
                        roleManager == null ? null : roleManager.getParticipantRootSubjectId(),
                        SatnetRoleConflictPolicy.ACTION_REDEEM_VOUCHER,
                        roleManager == null ? SatnetRoleManager.ROLE_USER : roleManager.getActiveRole(),
                        participantSnapshot);

                runOnUiThread(() -> {
                    if (isFinishing()) {
                        return;
                    }
                    currentVoucher = parsedVoucher;
                    redeemBlockedByPolicy = !conflictCheck.allowed;
                    redeemBlockedMessage = conflictCheck.allowed ? null : conflictCheck.message;
                    VoucherLedger.VerifierAuditSnapshot auditSnapshot =
                            voucherLedger.getVerifierAuditSnapshot(parsedVoucher.getVoucherId());
                    java.util.List<VoucherLedger.VerifierAuditRecord> auditHistory =
                            voucherLedger.listVerifierAuditRecords(parsedVoucher.getVoucherId());
                    String metadataSummary = metadataCheck.summary;
                    if (!conflictCheck.allowed && conflictCheck.message != null && !conflictCheck.message.trim().isEmpty()) {
                        metadataSummary = (metadataSummary == null || metadataSummary.trim().isEmpty())
                                ? conflictCheck.message
                                : metadataSummary + "\n\n" + conflictCheck.message;
                    }
                    displayVoucherDetails(parsedVoucher, metadataSummary);
                    applyTrustBadges(metadataCheck, auditSnapshot);
                    applyMerchantBadges(participantSnapshot);
                    renderAuditHistory(auditHistory);
                    redeemButton.setEnabled(!redeemBlockedByPolicy);
                    setBusy(false, null);
                    Toast.makeText(this,
                            conflictCheck.allowed
                                    ? getString(R.string.satnet_voucher_ready)
                                    : getString(R.string.satnet_voucher_policy_blocked, conflictCheck.message),
                            Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing()) {
                        return;
                    }
                    clearDisplayedVoucher();
                    setBusy(false, null);
                    Toast.makeText(this, getString(R.string.satnet_voucher_processing_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void displayVoucherDetails(BitcoinVoucher voucher, String metadataSummary) {
        manualEntryLayout.setVisibility(android.view.View.VISIBLE);
        if (voucherCodeInput != null) {
            voucherCodeInput.setText(voucher.getNumericCode());
        }
        if (voucherDetailsText != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            String direction = voucher.getDirection() == BitcoinVoucher.DIRECTION_SELL
                    ? getString(R.string.satnet_voucher_direction_sell)
                    : getString(R.string.satnet_voucher_direction_buy);
            String resolvedMetadataSummary = metadataSummary == null || metadataSummary.trim().isEmpty()
                    ? SettlementVerifier.buildVoucherMetadataSummary(voucher)
                    : metadataSummary;
            voucherDetailsText.setText(getString(
                    R.string.satnet_voucher_details_format,
                    voucher.getVoucherId(),
                    voucher.getAgentId(),
                    voucher.getDenomination(),
                    direction,
                    format.format(new Date(voucher.getExpiryTime())),
                    resolvedMetadataSummary));
        }
    }

    private void redeemVoucher() {
        SatnetStartupGate.Status runtimeStatus = refreshRuntimeStatus();
        if (!runtimeStatus.canUseRoleTools()) {
            Toast.makeText(this, runtimeStatus.getBlockingMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        if (busy) {
            Toast.makeText(this, R.string.satnet_voucher_busy, Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentVoucher == null) {
            Toast.makeText(this, R.string.satnet_voucher_no_selection, Toast.LENGTH_SHORT).show();
            return;
        }
        if (redeemBlockedByPolicy) {
            Toast.makeText(this,
                    getString(R.string.satnet_voucher_policy_blocked,
                            redeemBlockedMessage == null ? getString(R.string.satnet_voucher_no_selection) : redeemBlockedMessage),
                    Toast.LENGTH_LONG).show();
            return;
        }

         final BitcoinVoucher voucherToRedeem = currentVoucher;
         setBusy(true, getString(R.string.satnet_voucher_redeeming));
         backgroundExecutor.execute(() -> {
             try {
                 // Use authorization engine for final redemption check
                 SatnetAuthorizationEngine.AuthorizationDecision authDecision = SatnetAuthorizationEngine.authorize(
                         roleManager,
                         SatnetRoleConflictPolicy.ACTION_REDEEM_VOUCHER,
                         roleManager == null ? SatnetRoleManager.ROLE_USER : roleManager.getActiveRole(),
                         voucherLedger.getVoucherParticipantSnapshot(voucherToRedeem.getVoucherId()),
                         "Final redemption check");

                 if (!authDecision.allowed) {
                     throw new IllegalStateException(authDecision.message);
                 }
                 String walletAddress = getWalletAddressForRedemption();
                 voucherToRedeem.redeem(walletAddress);
                 voucherLedger.recordRedemption(voucherToRedeem,
                         walletAddress,
                         null,
                         roleManager == null ? null : roleManager.getParticipantSubjectId(),
                         roleManager == null ? null : roleManager.getParticipantRootSubjectId());
                 runOnUiThread(() -> {
                     if (isFinishing()) {
                         return;
                     }
                     clearDisplayedVoucher();
                     setBusy(false, null);
                     Toast.makeText(this, R.string.satnet_voucher_success, Toast.LENGTH_LONG).show();
                 });
             } catch (Exception e) {
                 runOnUiThread(() -> {
                     if (isFinishing()) {
                         return;
                     }
                     setBusy(false, null);
                     if (isWalletUnlockRequired(e.getMessage())) {
                         redirectToUnlockFlow();
                         return;
                     }
                     Toast.makeText(this, getString(R.string.satnet_voucher_redeem_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                 });
             }
         });
    }

    private String getWalletAddressForRedemption() throws Exception {
        if (wallet == null) {
            throw new IllegalStateException(getString(R.string.satnet_voucher_wallet_unavailable));
        }
        if (!wallet.hasStoredSeed()) {
            throw new IllegalStateException(getString(R.string.satnet_voucher_wallet_missing));
        }
        if (!wallet.isInitialized()) {
            char[] walletPin = refreshWalletSessionToken();
            if (walletPin == null || walletPin.length == 0) {
                throw new IllegalStateException(getString(R.string.satnet_voucher_wallet_locked));
            }
            try {
                wallet.loadEncryptedSeed(walletPin);
            } finally {
                WalletEncryption.clearChars(walletPin);
            }
        }
        if (!wallet.isInitialized()) {
            throw new IllegalStateException(getString(R.string.satnet_voucher_wallet_open_failed));
        }
        String walletAddress = wallet.getDerivedAddress(0);
        if (walletAddress == null || walletAddress.trim().isEmpty()) {
            throw new IllegalStateException(getString(R.string.satnet_voucher_wallet_no_address));
        }
        return walletAddress;
    }

    private void clearDisplayedVoucher() {
        currentVoucher = null;
        redeemBlockedByPolicy = false;
        redeemBlockedMessage = null;
        redeemButton.setEnabled(false);
        if (voucherCodeInput != null) {
            voucherCodeInput.setText("");
        }
        if (voucherDetailsText != null) {
            voucherDetailsText.setText(R.string.satnet_voucher_details_placeholder);
        }
        hideTrustBadges();
        hideMerchantBadges();
        if (manualEntryLayout != null) {
            manualEntryLayout.setVisibility(android.view.View.GONE);
        }
    }

    private void applyTrustBadges(SettlementVerifier.WorkflowMetadataCheck metadataCheck,
            VoucherLedger.VerifierAuditSnapshot auditSnapshot) {
        if (trustBadgeRow == null || manifestBadgeText == null || ledgerBadgeText == null || rotationBadgeText == null) {
            return;
        }
        SettlementVerifier.TrustBadgeState badgeState =
                SettlementVerifier.buildTrustBadgeState(metadataCheck, auditSnapshot);
        trustBadgeRow.setVisibility(android.view.View.VISIBLE);
        bindBadge(manifestBadgeText, badgeState.manifestBadgeText, badgeState.manifestPositive);
        bindBadge(ledgerBadgeText, badgeState.ledgerBadgeText, badgeState.ledgerPositive);
        bindBadge(rotationBadgeText, badgeState.rotationBadgeText, badgeState.rotationDetected);
    }

    private void hideTrustBadges() {
        if (trustBadgeRow != null) {
            trustBadgeRow.setVisibility(android.view.View.GONE);
        }
        if (auditHistorySection != null) {
            auditHistorySection.setVisibility(android.view.View.GONE);
        }
    }

    private void applyMerchantBadges(VoucherParticipantSnapshot participantSnapshot) {
        if (merchantBadgeRow == null || merchantBadgeText == null || merchantStatusBadgeText == null) {
            return;
        }
        if (participantSnapshot == null) {
            merchantBadgeRow.setVisibility(android.view.View.GONE);
            return;
        }

        // Check if there's merchant settlement context
        VoucherLedger.MerchantSettlementContext merchantContext = participantSnapshot.getMerchantSettlementContext();
        if (merchantContext == null) {
            merchantBadgeRow.setVisibility(android.view.View.GONE);
            return;
        }

        merchantBadgeRow.setVisibility(android.view.View.VISIBLE);

        // Show merchant identifier (truncated for privacy)
        String merchantId = merchantContext.merchantSubjectId;
        if (merchantId != null && merchantId.length() > 8) {
            merchantId = merchantId.substring(0, 8) + "...";
        }
        merchantBadgeText.setText(getString(R.string.satnet_voucher_merchant_badge, merchantId));

        // Show settlement status
        String statusText;
        boolean isPositive = false;
        switch (merchantContext.settlementStatus) {
            case "MERCHANT_SETTLEMENT_ACCEPTED":
                statusText = getString(R.string.satnet_voucher_merchant_status_accepted);
                isPositive = true;
                break;
            case "MERCHANT_SETTLEMENT_PENDING":
                statusText = getString(R.string.satnet_voucher_merchant_status_pending);
                isPositive = false;
                break;
            case "MERCHANT_SETTLEMENT_COMPLETED":
                statusText = getString(R.string.satnet_voucher_merchant_status_completed);
                isPositive = true;
                break;
            default:
                statusText = getString(R.string.satnet_voucher_merchant_status_unknown);
                isPositive = false;
                break;
        }
        merchantStatusBadgeText.setText(statusText);
        merchantStatusBadgeText.setTextColor(ContextCompat.getColor(this,
                isPositive ? R.color.satnet_accent : R.color.satnet_alert));
    }

    private void hideMerchantBadges() {
        if (merchantBadgeRow != null) {
            merchantBadgeRow.setVisibility(android.view.View.GONE);
        }
    }

    private void renderAuditHistory(java.util.List<VoucherLedger.VerifierAuditRecord> auditHistory) {
        if (auditHistorySection == null || auditHistoryText == null) {
            return;
        }
        auditHistorySection.setVisibility(android.view.View.VISIBLE);
        auditHistoryText.setText(auditHistory == null || auditHistory.isEmpty()
                ? getString(R.string.satnet_voucher_audit_history_placeholder)
                : SettlementVerifier.buildAuditHistorySummary(auditHistory, 3));
    }

    private void bindBadge(TextView badgeView, String label, boolean positiveState) {
        if (badgeView == null) {
            return;
        }
        badgeView.setText(label);
        badgeView.setTextColor(ContextCompat.getColor(this,
                positiveState ? R.color.satnet_accent : R.color.satnet_alert));
    }

    private void setBusy(boolean busyState, String message) {
        busy = busyState;
        SatnetStartupGate.Status runtimeStatus = refreshRuntimeStatus();
        boolean interactionReady = runtimeStatus.canUseRoleTools();
        if (busyState && voucherDetailsText != null && message != null) {
            voucherDetailsText.setText(message);
            if (manualEntryLayout != null) {
                manualEntryLayout.setVisibility(android.view.View.VISIBLE);
            }
        }
        if (manualEntryButton != null) {
            manualEntryButton.setEnabled(!busyState && interactionReady);
        }
        if (voucherCodeInput != null) {
            voucherCodeInput.setEnabled(!busyState && interactionReady);
        }
        if (redeemButton != null) {
            redeemButton.setEnabled(!busyState && interactionReady && currentVoucher != null && !redeemBlockedByPolicy);
        }
        updateScanButtonState();
    }

    private SatnetStartupGate.Status refreshRuntimeStatus() {
        SatnetStartupGate.Status runtimeStatus = SatnetStartupGate.evaluate(this);
        if (stageBadgeText != null) {
            stageBadgeText.setText(SatnetRuntimeConfig.getWalletSummary());
        }
        if (runtimeStatusText != null) {
            runtimeStatusText.setText(getString(
                    R.string.satnet_voucher_runtime_summary,
                    SatnetRuntimeConfig.getRoleSummary(roleManager != null
                            ? roleManager.getActiveRole()
                            : SatnetRoleManager.ROLE_USER),
                    runtimeStatus.getLocalFirstMessage(getString(R.string.satnet_voucher_capability_label))));
        }
        return runtimeStatus;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                Toast.makeText(this, R.string.satnet_voucher_camera_required, Toast.LENGTH_SHORT).show();
                pendingScanAfterPermission = false;
            } else if (pendingScanAfterPermission) {
                pendingScanAfterPermission = false;
                startQRScanner();
            }
            updateScanButtonState();
        }
    }

    @Override
    protected void onDestroy() {
        backgroundExecutor.shutdownNow();
        super.onDestroy();
        if (wallet != null) {
            wallet.clearSensitiveMemory();
        }
    }

    private char[] refreshWalletSessionToken() {
        WalletSessionStore.SessionAccess sessionAccess = WalletSessionStore.refreshSession(walletSessionToken);
        if (sessionAccess == null) {
            return null;
        }
        try {
            walletSessionToken = sessionAccess.token;
            return sessionAccess.consumePinChars();
        } finally {
            sessionAccess.close();
        }
    }

    private boolean isWalletUnlockRequired(String message) {
        return getString(R.string.satnet_voucher_wallet_locked).equals(message);
    }

    private void redirectToUnlockFlow() {
        Toast.makeText(this, R.string.satnet_voucher_wallet_locked, Toast.LENGTH_LONG).show();
        Intent unlockIntent = new Intent(this, BitcoinWalletSetupActivity.class);
        unlockIntent.putExtra(BitcoinWalletActivity.EXTRA_WALLET_ID, walletId);
        unlockIntent.putExtra(BitcoinWalletSetupActivity.EXTRA_UNLOCK_ONLY, true);
        startActivity(unlockIntent);
        finish();
    }
}
