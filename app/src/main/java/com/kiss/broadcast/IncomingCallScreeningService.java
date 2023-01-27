package com.kiss.broadcast;

import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.kiss.KissApplication;
import com.kiss.DataHandler;
import com.kiss.dataprovider.ContactsProvider;
import com.kiss.pojo.ContactsPojo;

@RequiresApi(api = Build.VERSION_CODES.N)
public class IncomingCallScreeningService extends CallScreeningService {

    @Override
    public void onScreenCall(@NonNull Call.Details callDetails) {
        respondToCall(callDetails, new CallResponse.Builder().build());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("enable-phone-history", false) && callDetails.getHandle() != null) {
            String phoneNumber = callDetails.getHandle().getSchemeSpecificPart();
            if (!TextUtils.isEmpty(phoneNumber)) {
                DataHandler dataHandler = KissApplication.getApplication(this).getDataHandler();
                ContactsProvider contactsProvider = dataHandler.getContactsProvider();
                if (contactsProvider != null) {
                    ContactsPojo contactPojo = contactsProvider.findByPhone(phoneNumber);
                    if (contactPojo != null) {
                        dataHandler.addToHistory(contactPojo.getHistoryId());
                    }
                }
            }
        }
    }
}
