package com.kunzisoft.keyboard.switcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.kunzisoft.keyboard.switcher.utils.Utilities;

public class KeyboardSwitcherBroadcastReceiver extends BroadcastReceiver {

    private static final String ACTION_SHOW_KEYBOARD_SWITCHER
            = "com.kunzisoft.keyboard.switcher.SHOW_KEYBOARD_SWITCHER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        if (intent.getAction().equals(ACTION_SHOW_KEYBOARD_SWITCHER)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent chooserIntent = new Intent(context, KeyboardManagerActivity.class);
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

                context.startActivity(chooserIntent);
            } else {
                Utilities.chooseAKeyboard(context);
            }
        }
    }
}
