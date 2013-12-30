package com.dwak.lastcall;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

public class LastCallExtension extends DashClockExtension {

    private static final String TAG = LastCallExtension.class.getSimpleName();
    public static final String PREF_DIAL = "pref_dial";

    @Override
    protected void onUpdateData(int arg0) {
        final SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String mLastCallNumber = "";
        String mLastCallName = "";
        String[] projection = new String[]{CallLog.Calls.NUMBER};
        Cursor cur = null;
        Cursor cur2 = null;
        try {
            cur = getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls.DATE + " desc");
        } catch (CursorIndexOutOfBoundsException e) {
            Log.d(TAG, "Cursor out of bounds, no calls");
        }

        if (cur != null) {
            if (cur.getCount() != 0) {
                cur.moveToFirst();
                mLastCallNumber = cur.getString(0);
                Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode("tel:" + mLastCallNumber));
                cur2 = getContentResolver().query(uri, new String[]{PhoneLookup.DISPLAY_NAME}, null, null, null);

                if (cur2 != null) {
                    if (cur2.moveToFirst()) {
                        mLastCallName = cur2.getString(0);
                    } else {
                        mLastCallName = "Unknown";
                    }

                    cur2.close();
                } else {
                    mLastCallName = "Unknown";
                }
            } else {
                mLastCallName = "No Call History";
                mLastCallNumber = "";
            }
            cur.close();
        }

        final boolean isDirectDial = mSharedPreferences.getBoolean(getString(R.string.pref_dial), false);
        Intent dialIntent;
        if(isDirectDial){
            dialIntent = new Intent(getApplicationContext(), LastCallDirectDialActivity.class);
            dialIntent.putExtra("tel", mLastCallNumber);
        }
        else {
            dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + mLastCallNumber));
        }

        if (!mLastCallNumber.equals("")) {
            final String expandedBody = "Click to " + (isDirectDial ? "Call" : "Dial") + "!";
            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.ic_launcher)
                    .status(mLastCallName)
                    .expandedTitle(mLastCallName + ": (" + mLastCallNumber + ")")
                    .expandedBody(expandedBody)
                    .clickIntent(dialIntent));
        } else {
            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.ic_launcher)
                    .status(mLastCallName)
                    .expandedTitle(mLastCallName));
        }
    }

}
