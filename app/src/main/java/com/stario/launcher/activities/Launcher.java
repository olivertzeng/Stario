/*
 * Copyright (C) 2025 Răzvan Albu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.stario.launcher.activities;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.stario.launcher.R;
import com.stario.launcher.activities.settings.Settings;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.glance.Glance;
import com.stario.launcher.glance.extensions.GlanceDialogExtension;
import com.stario.launcher.glance.extensions.calendar.Calendar;
import com.stario.launcher.glance.extensions.media.Media;
import com.stario.launcher.glance.extensions.weather.Weather;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.SheetDialogFragment;
import com.stario.launcher.sheet.SheetsFocusController;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.common.lock.ClosingAnimationView;
import com.stario.launcher.ui.common.lock.LockDetector;
import com.stario.launcher.ui.popup.PopupMenu;
import com.stario.launcher.ui.utils.HomeWatcher;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.ui.utils.animation.Animation;
import com.stario.launcher.ui.utils.animation.WallpaperAnimator;
import com.stario.launcher.utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Launcher extends ThemedActivity {
    public static final int MAX_BACKGROUND_ALPHA = 230;
    public static final String INTENT_KILL_TASK_ID_EXTRA = "com.stario.launcher.INTENT_KILL_TASK_ID_EXTRA";
    public static final String INTENT_SHEET_CLASS_EXTRA = "com.stario.launcher.INTENT_SHEET_CLASS_EXTRA";
    public static final String ACTION_REMOVE_SHEET = "com.stario.launcher.ACTION_REMOVE_SHEET";
    public static final String ACTION_MOVE_SHEET = "com.stario.launcher.ACTION_MOVE_SHEET";
    public static final String ACTION_ADD_SHEET = "com.stario.launcher.ACTION_ADD_SHEET";
    public static final String ACTION_KILL_TASK = "com.stario.launcher.ACTION_KILL_TASK";

    private BroadcastReceiver screenOnReceiver;
    private SheetsFocusController controller;
    private BroadcastReceiver killReceiver;
    private ClosingAnimationView main;
    private HomeWatcher homeWatcher;
    private View statusBarContrast;
    private LockDetector detector;
    private View navBarContrast;
    private View decorView;
    private Glance glance;

    public Launcher() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // On some devices, the application is killed when recents screen is open
        // Maybe multiple tasks might affect it?
        // https://github.com/albu-razvan/Stario/issues/104#issue-2836388598

        Intent intent = new Intent(ACTION_KILL_TASK);
        intent.putExtra(INTENT_KILL_TASK_ID_EXTRA, getTaskId());
        intent.setPackage(getPackageName());
        sendBroadcast(intent);

        killReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int task = intent.getIntExtra(INTENT_KILL_TASK_ID_EXTRA, getTaskId());

                if (task != getTaskId()) {
                    finishAndRemoveTask();
                }
            }
        };

        homeWatcher = new HomeWatcher(this);
        homeWatcher.setOnHomePressedListener(() -> setContrastVisibility(View.GONE));

        if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
            registerReceiver(killReceiver, new IntentFilter(ACTION_KILL_TASK), RECEIVER_EXPORTED);
        } else {
            //noinspection UnspecifiedRegisterReceiverFlag
            registerReceiver(killReceiver, new IntentFilter(ACTION_KILL_TASK));
        }

        Vibrations.from(this);
        ProfileManager.from(this);

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_SPLIT_TOUCH);
        UiUtils.enforceLightSystemUI(getWindow());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher);

        controller = findViewById(R.id.controller);
        main = findViewById(R.id.main);
        detector = findViewById(R.id.detector);
        decorView = window.getDecorView();

        Measurements.measure(getRoot(), (insets) -> {
            controller.setPadding(0, Measurements.getSysUIHeight(), 0,
                    Measurements.getNavHeight() + Measurements.dpToPx(20));

            return insets;
        });

        statusBarContrast = findViewById(R.id.status_bar_contrast);
        navBarContrast = findViewById(R.id.navigation_bar_contrast);

        statusBarContrast.setOnTouchListener((v, event) -> false);
        navBarContrast.setOnTouchListener((v, event) -> false);

        Measurements.addNavListener(value -> {
            navBarContrast.getLayoutParams().height = (int) (value * 1.5f);
            navBarContrast.requestLayout();
        });

        Measurements.addStatusBarListener(value -> {
            statusBarContrast.getLayoutParams().height = (int) (value * 1.5f);
            statusBarContrast.requestLayout();
        });

        UiUtils.Notch.applyNotchMargin(controller, UiUtils.Notch.CENTER);
        controller.setOnLongClickListener((v) -> {
            Vibrations.getInstance().vibrate();

            displayLauncherOptions(this, controller);
            return true;
        });

        screenOnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setShowWhenLocked(false);
                main.reset();
            }
        };

        if (Utils.isMinimumSDK(Build.VERSION_CODES.TIRAMISU)) {
            registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON), RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        }

        prepareWallpaperTransitions();

        attachSheets(controller);
        attachGlance();
    }

    private void attachGlance() {
        glance = new Glance(this);
        glance.attach(detector);

        GlanceDialogExtension.TransitionListener listener = slideOffset ->
                animateSheet(slideOffset, false);

        Calendar calendar = new Calendar();
        glance.attachViewExtension(calendar);
        glance.attachDialogExtension(new Media(), Gravity.BOTTOM, listener);
        glance.attachDialogExtension(new Weather(), Gravity.BOTTOM, listener);
    }

    private void attachSheets(SheetsFocusController controller) {
        controller.setSlideListener(Launcher.this::animateSheet);
        controller.addSheetDialog(this, SheetDialogFragment.IMPLEMENTATIONS);

        class Local {
            Class<? extends SheetDialogFragment>[] getClasses(Intent intent) {
                if (intent == null) {
                    return null;
                }

                Serializable extra = intent.getSerializableExtra(INTENT_SHEET_CLASS_EXTRA);

                if (extra == null) {
                    return null;
                }

                List<Class<? extends SheetDialogFragment>> classes = new ArrayList<>();

                if (extra instanceof Class<?> &&
                        SheetDialogFragment.class.isAssignableFrom((Class<?>) extra)) {
                    // noinspection unchecked
                    classes.add((Class<? extends SheetDialogFragment>) extra);
                } else if (extra.getClass().isArray()) {
                    // noinspection DataFlowIssue
                    Object[] array = (Object[]) extra;

                    for (Object element : array) {
                        if (element instanceof Class<?> &&
                                SheetDialogFragment.class.isAssignableFrom((Class<?>) element)) {
                            // noinspection unchecked
                            classes.add((Class<? extends SheetDialogFragment>) element);
                        }
                    }
                }

                if (classes.isEmpty()) {
                    return null;
                }

                // noinspection ToArrayCallWithZeroLengthArrayArgument, unchecked
                return classes.toArray(new Class[classes.size()]);
            }
        }
        Local local = new Local();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ADD_SHEET);
        filter.addAction(ACTION_MOVE_SHEET);
        filter.addAction(ACTION_REMOVE_SHEET);

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Class<? extends SheetDialogFragment>[] classes = local.getClasses(intent);
                if (classes == null) {
                    return;
                }

                String action = intent.getAction();
                if (action == null) {
                    return;
                }

                switch (action) {
                    case ACTION_ADD_SHEET: {
                        controller.addSheetDialog(Launcher.this, classes);
                        break;
                    }
                    case ACTION_MOVE_SHEET: {
                        controller.moveSheetDialog(Launcher.this, classes);
                        break;
                    }
                    case ACTION_REMOVE_SHEET: {
                        controller.removeSheetDialog(classes);
                        break;
                    }
                }
            }
        }, filter);
    }

    public void displayLauncherOptions(Launcher activity, SheetsFocusController controller) {
        PopupMenu menu = new PopupMenu(activity);

        Resources resources = activity.getResources();

        menu.add(new PopupMenu.Item(resources.getString(R.string.settings),
                ResourcesCompat.getDrawable(resources, R.drawable.ic_settings, activity.getTheme()),
                view -> {
                    menu.dismiss();

                    view.post(() -> {
                        Intent intent = new Intent(activity, Settings.class);

                        activity.startActivity(intent,
                                ActivityOptions.makeSceneTransitionAnimation(activity).toBundle());
                    });
                }));

        menu.add(new PopupMenu.Item(resources.getString(R.string.wallpaper),
                ResourcesCompat.getDrawable(resources, R.drawable.ic_palette, activity.getTheme()),
                view -> {
                    menu.dismiss();

                    view.post(() -> {
                        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .putExtra("com.android.wallpaper.LAUNCH_SOURCE", "app_launched_launcher");

                        activity.startActivity(intent,
                                ActivityOptions.makeScaleUpAnimation(view, 0, 0,
                                        view.getWidth(), view.getHeight()).toBundle());
                    });
                }));

        menu.showAtLocation(activity, controller, controller.getLastX(),
                controller.getLastY(), PopupMenu.PIVOT_CENTER_HORIZONTAL, false);
    }

    private void animateSheet(float slideOffset) {
        animateSheet(slideOffset, true);
    }

    private void animateSheet(float slideOffset, boolean scale) {
        if (Float.isNaN(slideOffset)) {
            slideOffset = 0;
        }

        boolean value = slideOffset < 0.5f ||
                getAttributeData(android.R.attr.windowLightStatusBar) == 0;
        controller.updateSheetSystemUI(value);
        glance.updateSheetSystemUI(value);

        float targetAlpha = 1f - slideOffset * slideOffset * 4f;
        slideOffset = slideOffset * slideOffset;

        if (targetAlpha > 0) {
            controller.setAlpha((float) Math.sqrt(targetAlpha));

            if (scale) {
                float scaleFactor = 1f - slideOffset / 3;
                controller.setScaleY(scaleFactor);
                controller.setScaleX(scaleFactor);
            }

            controller.setVisibility(View.VISIBLE);
        } else {
            controller.setVisibility(View.INVISIBLE);
        }

        updateWallpaperZoom(slideOffset);
    }

    private void updateWallpaperZoom(float zoom) {
        if (decorView.getWindowToken() != null) {
            if (Utils.isMinimumSDK(Build.VERSION_CODES.R)) {
                zoom = Math.max(Math.min(1, zoom), 0);

                WallpaperAnimator.updateZoom(this, zoom);
            }
        }
    }

    private void prepareWallpaperTransitions() {
        Window window = getWindow();

        if (window == null) {
            return;
        }

        Transition enterTransition = window.getEnterTransition();
        if (enterTransition != null) {
            enterTransition.addListener(new TransitionListenerAdapter() {
                @Override
                public void onTransitionStart(Transition transition) {
                    WallpaperAnimator.animateZoom(Launcher.this, null, 0f);
                }
            });
        }

        Transition exitTransition = window.getExitTransition();
        if (exitTransition != null) {
            exitTransition.addListener(new TransitionListenerAdapter() {
                @Override
                public void onTransitionStart(Transition transition) {
                    WallpaperAnimator.animateZoom(Launcher.this, null, 1f);
                }
            });
        }

    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(screenOnReceiver);
        } catch (Exception exception) {
            Log.e("Launcher", "onDestroy: Screen On receiver was not registered.");
        }

        try {
            unregisterReceiver(killReceiver);
        } catch (Exception exception) {
            Log.e("Launcher", "onDestroy: Kill receiver was not registered.");
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        glance.update();

        super.onResume();

        WallpaperAnimator.animateZoom(this, null, 0f);
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();

        setContrastVisibility(View.VISIBLE);
    }

    private void setContrastVisibility(int visible) {
        if (visible == View.VISIBLE) {
            navBarContrast.animate().alpha(1)
                    .setDuration(Animation.LONG.getDuration());
            statusBarContrast.animate().alpha(1)
                    .setDuration(Animation.LONG.getDuration());
        } else {
            navBarContrast.animate().alpha(0)
                    .setDuration(Animation.BRIEF.getDuration());
            statusBarContrast.animate().alpha(0)
                    .setDuration(Animation.BRIEF.getDuration());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        homeWatcher.startWatch();
    }

    @Override
    protected void onStop() {
        prepareWallpaperTransitions();
        WallpaperAnimator.animateZoom(this, null, 1f);

        setContrastVisibility(View.GONE);

        homeWatcher.stopWatch();

        super.onStop();

        setShowWhenLocked(false);
        main.reset();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            setContrastVisibility(View.VISIBLE);
        }

        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean hasWindowFocus() {
        return super.hasWindowFocus() || !(controller.hasSheetFocus() || glance.hasFocus());
    }

    @Override
    protected boolean isOpaque() {
        return false;
    }

    @Override
    protected boolean isAffectedByBackGesture() {
        return false;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
    }
}