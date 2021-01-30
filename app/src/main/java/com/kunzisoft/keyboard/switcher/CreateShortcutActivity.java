package com.kunzisoft.keyboard.switcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

/**
 * Activity to create a shortcut to show the Keyboard Switcher.
 */
public class CreateShortcutActivity extends AppCompatActivity {

    private static final String SHORTCUT_ID = "show_keyboard_switcher";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String shortcutLabel = getString(R.string.shortcut_short_label_show_keyboard_switcher);

        IconCompat shortcutIcon =
                IconCompat.createWithResource(this, R.drawable.ic_shortcut_24dp);

        Intent intent = new Intent(this, KeyboardManagerActivity.class);
        intent.setAction(Intent.ACTION_VIEW);

        ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(this, SHORTCUT_ID)
                .setIcon(shortcutIcon)
                .setShortLabel(shortcutLabel)
                .setIntent(intent)
                .build();

        Intent shortcutIntentResult =
                ShortcutManagerCompat.createShortcutResultIntent(this, shortcutInfo);

        setResult(Activity.RESULT_OK, shortcutIntentResult);
        finish();
    }
}
