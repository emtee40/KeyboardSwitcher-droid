package com.kunzisoft.keyboard.switcher.boot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.kunzisoft.keyboard.switcher.KeyboardSwitcherService;
import com.kunzisoft.keyboard.switcher.R;

import static com.kunzisoft.keyboard.switcher.KeyboardSwitcherService.FLOATING_BUTTON_START;
import static com.kunzisoft.keyboard.switcher.KeyboardSwitcherService.NOTIFICATION_START;

/**
 * Utility class to show keyboard button at startup
 */
public class StartWorker extends Worker {

    private final Context mContext;

    public StartWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            Intent intent = new Intent(mContext, KeyboardSwitcherService.class);
            mContext.stopService(intent);
            if (preferences.getBoolean(mContext.getString(R.string.settings_notification_key), false)) {
                intent.setAction(NOTIFICATION_START);
                ContextCompat.startForegroundService(mContext, intent);
            }
            if (preferences.getBoolean(mContext.getString(R.string.settings_floating_button_key), false)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(getApplicationContext())) {
                        intent.setAction(FLOATING_BUTTON_START);
                        mContext.startService(intent);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
        return Result.success();
    }
}
