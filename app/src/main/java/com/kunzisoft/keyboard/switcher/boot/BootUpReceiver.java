package com.kunzisoft.keyboard.switcher.boot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.work.BackoffPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.kunzisoft.keyboard.switcher.R;

import java.util.concurrent.TimeUnit;

/**
 * Broadcast receiver for "action boot completed"
 */
public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String action = intent.getAction();
        if (preferences.getBoolean(context.getString(R.string.settings_launch_startup_key), true)
                && action != null
                && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            // To show the button, else bug in new android version
            WorkManager workManager = WorkManager.getInstance(context);
            workManager.enqueue(new OneTimeWorkRequest.Builder(StartWorker.class)
                    .setBackoffCriteria(
                            BackoffPolicy.LINEAR,
                            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                            TimeUnit.MILLISECONDS)
                    .build());
        }
    }
}