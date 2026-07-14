package org.servalproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.servalproject.servaldna.keyring.KeyringIdentity;

/**
 * Shared phone-number / DID support for onboarding and SATNET wallet gating.
 */
public final class IdentityPhoneNumberSupport {
    private static final String TAG = "IdentityPhone";
    private static final String PREFS_NAME = "satnet_identity";
    private static final String KEY_CONFIGURED_DID = "configured_did";
    private static final int MIN_DID_DIGITS = 5;
    /** Must match serval-dna's DID_MAXSIZE (31). */
    private static final int MAX_DID_LENGTH = 31;

    private IdentityPhoneNumberSupport() {
    }

    public static String normalizeDid(String rawDid) {
        if (rawDid == null || rawDid.trim().isEmpty()) {
            return "";
        }
        // Only keep ASCII digits (0-9).
        // Character.isDigit() also accepts Unicode digit characters (e.g. Arabic-Indic),
        // which serval-dna's C-layer str_is_did() rejects because it uses the C isdigit()
        // function that only recognises ASCII digits.
        StringBuilder digitsOnly = new StringBuilder(rawDid.length());
        for (int i = 0; i < rawDid.length(); i++) {
            char c = rawDid.charAt(i);
            if (c >= '0' && c <= '9') {
                digitsOnly.append(c);
            }
        }
        String result = digitsOnly.toString();
        // Truncate to the server's DID_MAXSIZE limit to avoid an "Invalid DID" 400 response.
        if (result.length() > MAX_DID_LENGTH) {
            result = result.substring(0, MAX_DID_LENGTH);
        }
        return result;
    }

    public static boolean isConfiguredDid(String did) {
        return normalizeDid(did).length() >= MIN_DID_DIGITS;
    }

    public static String getValidationMessage(Context context, String rawDid) {
        String normalized = normalizeDid(rawDid);
        if (normalized.isEmpty()) {
            return context.getString(R.string.phone_number_required);
        }
        if (normalized.length() < MIN_DID_DIGITS) {
            return context.getString(R.string.phone_number_invalid_min_digits);
        }
        return null;
    }

    public static String prepareDidForSubmission(Context context, String rawDid) {
        String validationMessage = getValidationMessage(context, rawDid);
        if (validationMessage != null) {
            throw new IllegalArgumentException(validationMessage);
        }
        return normalizeDid(rawDid);
    }

    public static boolean isPhoneNumberConfigured(Context context, ServalBatPhoneApplication app) {
        if (hasStoredConfiguredDid(context)) {
            return true;
        }
        if (app == null) {
            return true;
        }
        if (app.getState() == ServalBatPhoneApplication.State.RequireDidName) {
            return false;
        }
        if (app.server == null) {
            return true;
        }
        try {
            KeyringIdentity identity = app.server.getIdentity();
            if (identity != null && isConfiguredDid(identity.did)) {
                persistConfiguredDid(context, identity.did);
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to verify configured DID", e);
        }
        return false;
    }

    public static void persistConfiguredDid(Context context, String did) {
        if (context == null) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String normalized = normalizeDid(did);
        if (normalized.length() >= MIN_DID_DIGITS) {
            prefs.edit().putString(KEY_CONFIGURED_DID, normalized).apply();
        } else {
            prefs.edit().remove(KEY_CONFIGURED_DID).apply();
        }
    }

    public static boolean hasStoredConfiguredDid(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return isConfiguredDid(prefs.getString(KEY_CONFIGURED_DID, null));
    }
}

