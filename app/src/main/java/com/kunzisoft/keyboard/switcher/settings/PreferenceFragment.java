package com.kunzisoft.keyboard.switcher.settings;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

import com.kunzisoft.androidclearchroma.ChromaPreferenceFragmentCompat;
import com.kunzisoft.keyboard.switcher.KeyboardSwitcherService;
import com.kunzisoft.keyboard.switcher.R;
import com.kunzisoft.keyboard.switcher.dialogs.WarningFloatingButtonDialog;
import com.kunzisoft.keyboard.switcher.utils.Utilities;

import static com.kunzisoft.keyboard.switcher.KeyboardSwitcherService.FLOATING_BUTTON_START;
import static com.kunzisoft.keyboard.switcher.KeyboardSwitcherService.FLOATING_BUTTON_STOP;
import static com.kunzisoft.keyboard.switcher.KeyboardSwitcherService.NOTIFICATION_START;
import static com.kunzisoft.keyboard.switcher.KeyboardSwitcherService.NOTIFICATION_STOP;

public class PreferenceFragment extends ChromaPreferenceFragmentCompat {

    /* https://stackoverflow.com/questions/7569937/unable-to-add-window-android-view-viewrootw44da9bc0-permission-denied-for-t
    code to post/handler request for permission
    */
    private final static int REQUEST_CODE = 6517;

    private SwitchPreference preferenceNotification;
    private SwitchPreference preferenceFloatingButton;

    private boolean tryToOpenExternalDialog;

	@Override
	public void onResume() {
		super.onResume();

		tryToOpenExternalDialog = false;
		// To unchecked the preference floating button if not allowed by the system
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!Settings.canDrawOverlays(getActivity())) {
				if (preferenceFloatingButton != null)
					preferenceFloatingButton.setChecked(false);
			}
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
				&& preferenceFloatingButton.isChecked()) {
			preferenceNotification.setEnabled(false);
		}
	}

	@Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // add listeners for non-default actions
        findPreference(getString(R.string.settings_ime_available_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						Utilities.openAvailableKeyboards(getContext());
						return false;
					}
				});
        findPreference(getString(R.string.settings_ime_change_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						Utilities.chooseAKeyboard(getContext());
						return false;
					}
				});

        preferenceNotification = findPreference(getString(R.string.settings_notification_key));
        preferenceNotification.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (preferenceNotification.isChecked()) {
					if (getActivity() != null) {
						Intent intent = new Intent(getActivity(), KeyboardSwitcherService.class);
						intent.setAction(NOTIFICATION_START);
						getActivity().startService(intent);
					}
				} else {
					stopKeyboardSwitcherService();
				}
				return false;
			}
		});

        preferenceFloatingButton = findPreference(getString(R.string.settings_floating_button_key));
        preferenceFloatingButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (preferenceFloatingButton.isChecked()) {
					preferenceFloatingButton.setChecked(false);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						WarningFloatingButtonDialog dialogFragment = new WarningFloatingButtonDialog();
						if (getParentFragmentManager() != null)
							dialogFragment.show(getParentFragmentManager(), "warning_floating_button_dialog");
					} else {
						startFloatingButtonAndCheckButton();
					}
				} else {
					stopFloatingButtonAndUncheckedButton();
				}
				return false;
			}
		});

        findPreference(getString(R.string.settings_floating_button_lock_key))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						SwitchPreference switchPreference = (SwitchPreference) preference;
						switchPreference.setChecked((Boolean) newValue);
						restartFloatingButtonAndCheckedButton();
						return false;
					}
				});
        findPreference(getString(R.string.settings_floating_size_key))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						SeekBarPreference seekBarPreference = (SeekBarPreference) preference;
						seekBarPreference.setValue((int) newValue);
						restartFloatingButtonAndCheckedButton();
						return false;
					}
				});
    }

    @Override
    /*
     * To manage color selection
     */
    public void onPositiveButtonClick(@ColorInt int color) {
        super.onPositiveButtonClick(color);
        restartFloatingButtonAndCheckedButton();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean drawOverlayPermissionAllowed() {
    	if (getActivity() != null) {
			/* check if we already  have permission to draw over other apps */
			if (Settings.canDrawOverlays(getActivity())) {
				return true;
			} else {
				try {
					/* if not construct intent to request permission */
					tryToOpenExternalDialog = true;
					Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
							Uri.parse("package:" + getActivity().getPackageName()));
					/* request permission via start activity for result */
					startActivityForResult(intent, REQUEST_CODE);
				} catch (ActivityNotFoundException e) {
					if (getContext() != null)
						new AlertDialog.Builder(getContext())
								.setMessage(R.string.error_overlay_permission_request)
								.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {}
								}).create().show();
				}
			}
		}
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /* check if received result code
         is equal our requested code for draw permission  */
        if (requestCode == REQUEST_CODE) {
            /* if so check once again if we have permission */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(getActivity())) {
                    startFloatingButtonAndCheckButton();
                }
            }
        }
    }

	/**
	 * Method used to not destroy the main activity when an external dialog is requested
	 * @return 'true' if an external dialog is requested
	 */
	public boolean isTryingToOpenExternalDialog() {
    	return tryToOpenExternalDialog;
	}

	private void startFloatingButton() {
		if (getActivity() != null) {
			Intent intent = new Intent(getActivity(), KeyboardSwitcherService.class);
			intent.setAction(FLOATING_BUTTON_START);
			getActivity().startService(intent);
		}
	}

	private void stopKeyboardSwitcherService() {
		if (getActivity() != null) {
			Intent intent = new Intent(getActivity(), KeyboardSwitcherService.class);
			if (!preferenceNotification.isChecked() && !preferenceFloatingButton.isChecked()) {
				getActivity().stopService(intent);
			} else if (preferenceNotification.isChecked() && !preferenceFloatingButton.isChecked()) {
				intent.setAction(FLOATING_BUTTON_STOP);
				getActivity().startService(intent);
			} else if (!preferenceNotification.isChecked() && preferenceFloatingButton.isChecked()) {
				intent.setAction(NOTIFICATION_STOP);
				getActivity().startService(intent);
			}
		}
	}

    /*
    ------ Floating Button Service ------
    */

    void startFloatingButtonAndCheckButton() {
		stopKeyboardSwitcherService();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (drawOverlayPermissionAllowed()) {
				startFloatingButton();
			} else {
				if (preferenceFloatingButton != null)
					preferenceFloatingButton.setChecked(false);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
					preferenceNotification.setEnabled(false);
			}
		} else {
			startFloatingButton();
		}
        if (preferenceFloatingButton != null)
            preferenceFloatingButton.setChecked(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			preferenceNotification.setEnabled(false);
    }

    void stopFloatingButtonAndUncheckedButton() {
    	stopKeyboardSwitcherService();
        if (preferenceFloatingButton != null)
            preferenceFloatingButton.setChecked(false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			preferenceNotification.setEnabled(true);
    }

    private void restartFloatingButtonAndCheckedButton() {
        // Restart service
        if (getActivity() != null) {
            getActivity().stopService(new Intent(getActivity(), KeyboardSwitcherService.class));
        }
		startFloatingButtonAndCheckButton();
    }
}