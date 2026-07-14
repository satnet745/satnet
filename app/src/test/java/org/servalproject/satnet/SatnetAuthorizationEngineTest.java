/**
 * Copyright (C) 2025 SATNET AFRICA
 *
 * This file is part of SATNET AFRICA (http://satnetafrica.org)
 *
 * SATNET AFRICA is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */

package org.servalproject.satnet;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.satnet.ui.SatnetRuntimeTestHelper;
import org.servalproject.voucher.VoucherLedger;
import org.servalproject.voucher.VoucherParticipantSnapshot;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SatnetAuthorizationEngineTest {

    private Context context;
    private SatnetRoleManager roleManager;
    private VoucherLedger voucherLedger;

    @Before
    public void setUp() {
        ServalBatPhoneApplication app = SatnetRuntimeTestHelper.prepareApp();
        context = SatnetRuntimeTestHelper.resetAppData(app);
        roleManager = new SatnetRoleManager(context);
        voucherLedger = new VoucherLedger(context);
    }

    @Test
    public void verifierCanInspectVoucherFromDifferentAgent() {
        // Setup: Agent issues voucher
        roleManager.registerAsAgent("Alice Agent", "Kampala");
        String agentSubjectId = roleManager.getRoleSubjectId(SatnetRoleManager.ROLE_AGENT);
        String agentRootId = roleManager.getParticipantRootSubjectId();

        // Setup: Different verifier — clear prefs first so a distinct root identity is generated
        context.getSharedPreferences("satnet_roles", android.content.Context.MODE_PRIVATE)
                .edit().clear().commit();
        SatnetRoleManager verifierRoleManager = new SatnetRoleManager(context);
        verifierRoleManager.registerAsVerifier();

        // Create participant snapshot for voucher issued by agent
        VoucherParticipantSnapshot participantSnapshot = new VoucherParticipantSnapshot(
                "test_voucher_123",
                "agent_001",
                agentSubjectId,
                agentRootId,
                null, null, null, null, null,
                null, null, null, null,
                VoucherLedger.RISK_STATE_NONE, 0, null,
                null, null, 0L,
                VoucherLedger.DISPUTE_STATUS_NONE,
                null, 0, 1);

        // Act: Verifier inspects voucher
        SatnetAuthorizationEngine.AuthorizationDecision decision = SatnetAuthorizationEngine.authorize(
                verifierRoleManager,
                SatnetRoleConflictPolicy.ACTION_INSPECT_VOUCHER,
                SatnetRoleManager.ROLE_VERIFIER,
                participantSnapshot,
                "Test inspection");

        // Assert: Should be allowed
        assertTrue(decision.allowed);
        assertNotNull(decision.roleAuthorization);
        assertNotNull(decision.conflictCheck);
    }

    @Test
    public void verifierCannotInspectOwnIssuedVoucher() {
        // Setup: Same person as agent and verifier
        roleManager.registerAsAgent("Alice Agent", "Kampala");
        roleManager.registerAsVerifier();

        String agentSubjectId = roleManager.getRoleSubjectId(SatnetRoleManager.ROLE_AGENT);
        String rootId = roleManager.getParticipantRootSubjectId();

        // Create participant snapshot where agent and verifier are same
        VoucherParticipantSnapshot participantSnapshot = new VoucherParticipantSnapshot(
                "test_voucher_456",
                "agent_001",
                agentSubjectId,
                rootId,
                null, null, null, null, null,
                null, null, null, null,
                VoucherLedger.RISK_STATE_NONE, 0, null,
                null, null, 0L,
                VoucherLedger.DISPUTE_STATUS_NONE,
                null, 0, 1);

        // Act: Same person tries to inspect (conflict of interest)
        SatnetAuthorizationEngine.AuthorizationDecision decision = SatnetAuthorizationEngine.authorize(
                roleManager,
                SatnetRoleConflictPolicy.ACTION_INSPECT_VOUCHER,
                SatnetRoleManager.ROLE_VERIFIER,
                participantSnapshot,
                "Self-inspection attempt");

        // Assert: Should be denied due to conflict policy
        assertFalse(decision.allowed);
        assertNotNull(decision.conflictCheck);
        assertFalse(decision.conflictCheck.allowed);
    }

    @Test
    public void verifierWithoutRoleCapabilityCannotInspect() {
        // Setup: No verifier role registered
        roleManager.registerAsUser();

        VoucherParticipantSnapshot participantSnapshot = new VoucherParticipantSnapshot(
                "test_voucher_789",
                "agent_001",
                null, null, null, null, null, null, null,
                null, null, null, null,
                VoucherLedger.RISK_STATE_NONE, 0, null,
                null, null, 0L,
                VoucherLedger.DISPUTE_STATUS_NONE,
                null, 0, 1);

        // Act: User without verifier role tries to inspect
        SatnetAuthorizationEngine.AuthorizationDecision decision = SatnetAuthorizationEngine.authorize(
                roleManager,
                SatnetRoleConflictPolicy.ACTION_INSPECT_VOUCHER,
                SatnetRoleManager.ROLE_VERIFIER,
                participantSnapshot,
                "Unauthorized verifier attempt");

        // Assert: Should be denied at role authorization level
        assertFalse(decision.allowed);
        assertNotNull(decision.roleAuthorization);
        assertFalse(decision.roleAuthorization.allowed);
    }

    @Test
    public void userCanRedeemVoucherNotLinkedToThemselves() {
        // Setup: Agent issues, different user redeems
        roleManager.registerAsUser();

        VoucherParticipantSnapshot participantSnapshot = new VoucherParticipantSnapshot(
                "test_voucher_redeem",
                "agent_001",
                "different_agent_subject",
                "different_agent_root",
                null, null, null, null, null,
                null, null, null, null,
                VoucherLedger.RISK_STATE_NONE, 0, null,
                null, null, 0L,
                VoucherLedger.DISPUTE_STATUS_NONE,
                null, 0, 1);

        // Act: User redeems voucher
        SatnetAuthorizationEngine.AuthorizationDecision decision = SatnetAuthorizationEngine.authorize(
                roleManager,
                SatnetRoleConflictPolicy.ACTION_REDEEM_VOUCHER,
                SatnetRoleManager.ROLE_USER,
                participantSnapshot,
                "Redemption attempt");

        // Assert: Should be allowed
        assertTrue(decision.allowed);
    }

    @Test
    public void authorizationDecisionIncludesTimestamp() {
        roleManager.registerAsVerifier();

        VoucherParticipantSnapshot participantSnapshot = new VoucherParticipantSnapshot(
                "test_voucher_ts",
                "agent_001",
                "different_agent",
                "different_root",
                null, null, null, null, null,
                null, null, null, null,
                VoucherLedger.RISK_STATE_NONE, 0, null,
                null, null, 0L,
                VoucherLedger.DISPUTE_STATUS_NONE,
                null, 0, 1);

        // Act
        long beforeDecision = System.currentTimeMillis();
        SatnetAuthorizationEngine.AuthorizationDecision decision = SatnetAuthorizationEngine.authorize(
                roleManager,
                SatnetRoleConflictPolicy.ACTION_INSPECT_VOUCHER,
                SatnetRoleManager.ROLE_VERIFIER,
                participantSnapshot,
                "Timestamp test");
        long afterDecision = System.currentTimeMillis();

        // Assert: Decision has timestamp within expected range
        assertNotNull(decision);
        assertTrue(decision.decidedAt >= beforeDecision);
        assertTrue(decision.decidedAt <= afterDecision);
    }
}

