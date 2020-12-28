package com.kunzisoft.keyboard.switcher;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.kunzisoft.keyboard.switcher.utils.Utilities;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class KeyboardSwitcherService extends Service implements OnTouchListener, OnClickListener {

    public static final String CHANNEL_ID_KEYBOARD = "com.kunzisoft.keyboard.notification.channel";
    public static final String CHANNEL_NAME_KEYBOARD = "Keyboard switcher notification";

    public static final int NOTIFICATION_ID = 45;

    public static String NOTIFICATION_START= "NOTIFICATION_START";
    public static String NOTIFICATION_STOP= "NOTIFICATION_STOP";
    public static String FLOATING_BUTTON_START= "FLOATING_BUTTON_START";
    public static String FLOATING_BUTTON_STOP = "FLOATING_BUTTON_STOP";

    private SharedPreferences preferences;
    private static final String POSITION_PORTRAIT = "POSITION_PORTRAIT";
    private static final String POSITION_LANDSCAPE = "POSITION_LANDSCAPE";

    private View topLeftView;
    private View bottomRightView;

    private ImageView overlayedButton;
    private boolean moving;
    private WindowManager windowManager;

    private boolean lockedButton;

    private PositionOrientation currentPosition = new PositionOrientation();

    private static class PositionOrientation implements Parcelable {
        @DrawableRes
        int overlayedButtonResourceId = R.drawable.ic_keyboard_white_32dp;
        int[] positionToSave = {0, 0};
        float[] offset = {0F, 0F};
        int[] originalPosition = {0, 0};

        PositionOrientation() {}

        protected PositionOrientation(Parcel in) {
            overlayedButtonResourceId = in.readInt();
            positionToSave = in.createIntArray();
            offset = in.createFloatArray();
            originalPosition = in.createIntArray();
        }

        public static final Creator<PositionOrientation> CREATOR = new Creator<PositionOrientation>() {
            @Override
            public PositionOrientation createFromParcel(Parcel in) {
                return new PositionOrientation(in);
            }

            @Override
            public PositionOrientation[] newArray(int size) {
                return new PositionOrientation[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(overlayedButtonResourceId);
            parcel.writeIntArray(positionToSave);
            parcel.writeFloatArray(offset);
            parcel.writeIntArray(originalPosition);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
	    return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    }

    private NotificationCompat.Builder notificationBuilder() {
        // To keep the notification active
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_KEYBOARD,
                    CHANNEL_NAME_KEYBOARD,
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManagerCompat.from(this).createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID_KEYBOARD)
                .setSmallIcon(R.drawable.ic_notification_white_24dp)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setContentTitle(this.getString(R.string.notification_keyboard_title))
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setContentText(this.getString(R.string.notification_keyboard_content_text))
                .setContentIntent(Utilities.getPendingIntent(this, 500L)); // Trick 500ms delay to show the dialog
    }

    private void removeNotification() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, notificationBuilder().build());
        }

        if (intent != null
                && intent.getAction() != null) {
            if (intent.getAction().equals(NOTIFICATION_START)
                && preferences.getBoolean(getString(R.string.settings_notification_key), false)) {
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notificationBuilder().build());
            }

            if (intent.getAction().equals(NOTIFICATION_STOP)) {
                removeNotification();
            }

            if (intent.getAction().equals(FLOATING_BUTTON_START)
                && preferences.getBoolean(getString(R.string.settings_floating_button_key), false)) {
                createRemoteView();
            } else {
                eraseRemoteView();
            }

            if (intent.getAction().equals(FLOATING_BUTTON_STOP)) {
                eraseRemoteView();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void createRemoteView() {
        try {
            // check Button Position
            lockedButton = preferences.getBoolean(getString(R.string.settings_floating_button_lock_key), false);

            int typeFilter = LayoutParams.TYPE_SYSTEM_ALERT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                typeFilter = LayoutParams.TYPE_APPLICATION_OVERLAY;
            }

            overlayedButton = new ImageView(this);
            @ColorRes int color = preferences.getInt(getString(R.string.settings_colors_key),
                    ContextCompat.getColor(this, R.color.colorPrimary));
            overlayedButton.setImageResource(R.drawable.ic_keyboard_white_32dp);
            overlayedButton.setColorFilter(color);
            overlayedButton.setAlpha((color >> 24) & 0xff);
            overlayedButton.setOnTouchListener(this);
            overlayedButton.setOnClickListener(this);

            // Point reference on top left
            topLeftView = new View(this);
            LayoutParams topLeftParams =
                    new LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT,
                            typeFilter,
                            LayoutParams.FLAG_NOT_FOCUSABLE
                                    | LayoutParams.FLAG_NOT_TOUCH_MODAL,
                            PixelFormat.TRANSLUCENT);
            topLeftParams.gravity = Gravity.START | Gravity.TOP;
            topLeftParams.x = 0;
            topLeftParams.y = 0;
            topLeftParams.width = 0;
            topLeftParams.height = 0;
            windowManager.addView(topLeftView, topLeftParams);

            // Point reference on bottom right
            bottomRightView = new View(this);
            LayoutParams bottomRightParams =
                    new LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT,
                            typeFilter,
                            LayoutParams.FLAG_NOT_FOCUSABLE
                                    | LayoutParams.FLAG_NOT_TOUCH_MODAL,
                            PixelFormat.TRANSLUCENT);
            bottomRightParams.gravity = Gravity.END | Gravity.BOTTOM;
            bottomRightParams.x = 0;
            bottomRightParams.y = 0;
            bottomRightParams.width = 0;
            bottomRightParams.height = 0;
            windowManager.addView(bottomRightView, bottomRightParams);

            LayoutParams overlayedButtonParams =
                    new LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT,
                            typeFilter,
                            LayoutParams.FLAG_NOT_FOCUSABLE
                                    | LayoutParams.FLAG_NOT_TOUCH_MODAL,
                            PixelFormat.TRANSLUCENT);
            overlayedButtonParams.gravity = Gravity.CENTER;
            overlayedButtonParams.x = 0;
            overlayedButtonParams.y = 0;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
                && preferences.contains(POSITION_PORTRAIT)) {
                PositionOrientation positionPortrait = (new Gson()).fromJson(preferences.getString(POSITION_PORTRAIT, null), PositionOrientation.class);
                overlayedButtonParams.x = positionPortrait.positionToSave[0];
                overlayedButtonParams.y = positionPortrait.positionToSave[1];
                overlayedButton.setImageResource(positionPortrait.overlayedButtonResourceId);
                currentPosition = positionPortrait;
            }
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                && preferences.contains(POSITION_LANDSCAPE)) {
                PositionOrientation positionLandscape = (new Gson()).fromJson(preferences.getString(POSITION_LANDSCAPE, null), PositionOrientation.class);
                overlayedButtonParams.x = positionLandscape.positionToSave[0];
                overlayedButtonParams.y = positionLandscape.positionToSave[1];
                overlayedButton.setImageResource(positionLandscape.overlayedButtonResourceId);
                currentPosition = positionLandscape;
            }
            int defaultSize = (int) (32 * getResources().getDisplayMetrics().density);
            int sizeMultiplier = preferences.getInt(getString(R.string.settings_floating_size_key), 50);
            overlayedButtonParams.width = defaultSize * sizeMultiplier / 100;
            overlayedButtonParams.height = defaultSize * sizeMultiplier / 100;

            windowManager.addView(overlayedButton, overlayedButtonParams);
        } catch (Exception e) {
            Log.e("KeyboardSwitcherService", "Unable to show floating button", e);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        eraseRemoteView();
        createRemoteView();
    }

    private void setOverlayedDrawableResource(@DrawableRes int newDrawableResourceId) {
        if (newDrawableResourceId != currentPosition.overlayedButtonResourceId) {
            currentPosition.overlayedButtonResourceId = newDrawableResourceId;
            overlayedButton.setImageResource(currentPosition.overlayedButtonResourceId);
        }
    }

    private void getPositionOnScreen(MotionEvent event) {
        int[] location = new int[2];
        if (overlayedButton != null)
            overlayedButton.getLocationOnScreen(location);

        currentPosition.originalPosition[0] = (int) (location[0] + event.getX());
        currentPosition.originalPosition[1] = (int) (location[1] + event.getY());
    }

    private void savePreferencePosition(int position) {
        SharedPreferences.Editor editor = preferences.edit();
        if (position == Configuration.ORIENTATION_LANDSCAPE) {
            editor.putString(POSITION_LANDSCAPE, (new Gson()).toJson(currentPosition));
        } else {
            editor.putString(POSITION_PORTRAIT, (new Gson()).toJson(currentPosition));
        }
        editor.apply();
    }

    private void drawButton(View view, int x, int y) {
        if (topLeftView != null
            && bottomRightView != null) {
            int[] topLeftLocationOnScreen = new int[2];
            topLeftView.getLocationOnScreen(topLeftLocationOnScreen);

            int[] bottomRightLocationOnScreen = new int[2];
            bottomRightView.getLocationOnScreen(bottomRightLocationOnScreen);

            WindowManager.LayoutParams params = (LayoutParams) overlayedButton.getLayoutParams();

            // To stick the button on the edge
            if (x <= view.getMeasuredWidth() / 2) {
                x = topLeftLocationOnScreen[0];
                setOverlayedDrawableResource(R.drawable.ic_keyboard_left_white_32dp);
            } else if (x >= bottomRightLocationOnScreen[0] - view.getMeasuredWidth() / 2) {
                x = bottomRightLocationOnScreen[0];
                setOverlayedDrawableResource(R.drawable.ic_keyboard_right_white_32dp);
            } else {
                setOverlayedDrawableResource(R.drawable.ic_keyboard_white_32dp);
            }

            params.x = x - (bottomRightLocationOnScreen[0] + topLeftLocationOnScreen[0]) / 2;
            params.y = y - (bottomRightLocationOnScreen[1] + topLeftLocationOnScreen[1]) / 2;
            currentPosition.positionToSave[0] = params.x;
            currentPosition.positionToSave[1] = params.y;

            windowManager.updateViewLayout(overlayedButton, params);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {

    	// Consume the touch and click if the button is locked
        if (lockedButton) {
			if (event.getAction() == MotionEvent.ACTION_UP) {
                view.playSoundEffect(android.view.SoundEffectConstants.CLICK);
                onClick(view);
            }
			return true;
		}

		float x = event.getRawX();
		float y = event.getRawY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            moving = false;

            getPositionOnScreen(event);

            currentPosition.offset[0] = currentPosition.originalPosition[0] - x;
            currentPosition.offset[1] = currentPosition.originalPosition[1] - y;

        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int newX = (int) (currentPosition.offset[0] + x);
            int newY = (int) (currentPosition.offset[1] + y);

            int deltaMoveX = view.getMeasuredWidth() * 3/4;
            int deltaMoveY = view.getMeasuredHeight() * 3/4;

            if (Math.abs(newX - currentPosition.originalPosition[0]) < deltaMoveX
                    && Math.abs(newY - currentPosition.originalPosition[1]) < deltaMoveY
                    && !moving) {
                return false;
            }

            drawButton(view, newX, newY);
            moving = true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            savePreferencePosition(getResources().getConfiguration().orientation);
            return moving;
        }

        return false;
    }

    @Override
    public void onClick(final View view) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(this, KeyboardManagerActivity.class);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Utilities.chooseAKeyboard(this);
        }
    }

    private void eraseRemoteView() {
        try {
            windowManager.removeView(overlayedButton);
            windowManager.removeView(topLeftView);
            windowManager.removeView(bottomRightView);
        } catch (Exception ignored) {}
        overlayedButton = null;
        topLeftView = null;
        bottomRightView = null;
    }

    @Override
    public void onDestroy() {
        if (overlayedButton != null) {
            savePreferencePosition(getResources().getConfiguration().orientation);
            eraseRemoteView();
        }
        removeNotification();
        super.onDestroy();
    }
}
