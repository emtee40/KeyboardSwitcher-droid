package com.kunzisoft.keyboard.switcher.boot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import com.kunzisoft.keyboard.switcher.KeyboardSwitcherService;
import com.kunzisoft.keyboard.switcher.R;

import static com.kunzisoft.keyboard.switcher.KeyboardSwitcherService.FLOATING_BUTTON_START;
import static com.kunzisoft.keyboard.switcher.KeyboardSwitcherService.NOTIFICATION_START;

/**
 * Utility class to show keyboard button at startup
 */
public class BootUpActivity extends AppCompatActivity{

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = new Intent(this, KeyboardSwitcherService.class);

        stopService(intent);
        if (preferences.getBoolean(getString(R.string.settings_notification_key), false)) {
            intent.setAction(NOTIFICATION_START);
            startService(intent);
        }
        else if (preferences.getBoolean(getString(R.string.settings_floating_button_key), false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(getApplicationContext())) {
                    intent.setAction(FLOATING_BUTTON_START);
                    startService(intent);
                }
            }
        }

        finish();
    }
}
