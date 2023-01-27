package com.kiss.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.kiss.KissApplication;
import com.kiss.DataHandler;
import com.kiss.dataprovider.ContactsProvider;
import com.kiss.pojo.ContactsPojo;
import com.kiss.utils.PackageManagerUtils;

public class IncomingCallHandler extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        // Only handle calls received
        if (!"android.intent.action.PHONE_STATE".equals(intent.getAction())) {
            return;
        }

        try {
            DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
            ContactsProvider contactsProvider = dataHandler.getContactsProvider();

            // Stop if contacts are not enabled
            if (contactsProvider == null) {
                return;
            }

            if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

                if (phoneNumber == null) {
                    // Skipping (private call)
                    return;
                }

                ContactsPojo contactPojo = contactsProvider.findByPhone(phoneNumber);
                if (contactPojo != null) {
                    dataHandler.addToHistory(contactPojo.getHistoryId());
                }
            }
        } catch (Exception e) {
            Log.e("Phone Receive Error", " " + e);
        }
    }

    public static void setEnabled(Context context, boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PackageManagerUtils.enableComponent(context, IncomingCallHandler.class, false);
        } else {
            PackageManagerUtils.enableComponent(context, IncomingCallHandler.class, enabled);
        }

    }
}
