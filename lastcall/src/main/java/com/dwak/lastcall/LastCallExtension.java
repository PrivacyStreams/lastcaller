package com.dwak.lastcall;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.github.privacystreams.commons.list.ListOperators;
import com.github.privacystreams.communication.Call;
import com.github.privacystreams.communication.Contact;
import com.github.privacystreams.core.Item;
import com.github.privacystreams.core.UQI;
import com.github.privacystreams.core.exceptions.PSException;
import com.github.privacystreams.core.purposes.Purpose;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LastCallExtension extends DashClockExtension {

    private static final String TAG = LastCallExtension.class.getSimpleName();
    public static final String PREF_DIAL = "pref_dial";
    public static final String PREF_DISPLAY_TIME = "pref_display_time";
    private Date mLastCallTime;
    SimpleDateFormat format = new SimpleDateFormat("EE MMM dd hh:mm:ss");
    private String mLastCallTimeString;

    @Override
    protected void onUpdateData(int arg0) {
        final SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final UQI uqi = new UQI(this);
        String mLastCallNumber = "";
        String mLastCallName = "";
        try {
            Item lastCall = uqi.getData(Call.getLogs(), Purpose.UTILITY("show lock screen"))
                    .sortBy(Call.TIMESTAMP).reverse()
                    .getFirst().asItem();
            if (lastCall == null) mLastCallName = "No Call History";
            else {
                mLastCallNumber = lastCall.getValueByField(Call.CONTACT);
                mLastCallTime = new Date((Long) lastCall.getValueByField(Call.TIMESTAMP));
                mLastCallTimeString = format.format(mLastCallTime);
                mLastCallName = uqi.getData(Contact.getAll(), Purpose.UTILITY("show lock screen"))
                        .filter(ListOperators.contains(Contact.PHONES, mLastCallNumber))
                        .getFirst().getField(Contact.NAME);
                if (mLastCallName == null) mLastCallName = "Unknown";
            }
        } catch (PSException e) {
            e.printStackTrace();
        }

        final boolean isDirectDial = mSharedPreferences.getBoolean(getString(R.string.pref_dial), false);
        final boolean displayTime = mSharedPreferences.getBoolean(PREF_DISPLAY_TIME, false);
        Intent dialIntent;
        if (isDirectDial) {
            dialIntent = new Intent(getApplicationContext(), LastCallDirectDialActivity.class);
            dialIntent.putExtra("tel", mLastCallNumber);
        }
        else {
            dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + mLastCallNumber));
        }

        if (!mLastCallNumber.equals("")) {
            final String expandedBody;
            if(displayTime){
                expandedBody = mLastCallTimeString;
            }
            else {
                expandedBody = "Click to " + (isDirectDial
                        ? "Call"
                        : "Dial") + "!";
            }
            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.ic_launcher)
                    .status(mLastCallName)
                    .expandedTitle(mLastCallName + ": (" + mLastCallNumber + ")")
                    .expandedBody(expandedBody)
                    .clickIntent(dialIntent));
        }
        else {
            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.ic_launcher)
                    .status(mLastCallName)
                    .expandedTitle(mLastCallName));
        }
    }

}
