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

package com.stario.launcher.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.FloatRange;

import com.google.gson.Gson;
import com.stario.launcher.BuildConfig;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.services.AccessibilityService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Utils {
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5X Build/MMB29P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/W.X.Y.Z Mobile Safari/537.36 (compatible; Googlebot/2.1; +https://www.google.com/bot.html)";

    private static final String TAG = "com.stario.Utils";
    private static final ExecutorService executorPool = Executors.newFixedThreadPool(10);
    private static final String[] IPV4_APIS = {
            "https://checkip.amazonaws.com/",
            "https://ipv4.icanhazip.com/",
            "https://ipv4.seeip.org",
            "https://api.ipify.org/"
    };
    private static Gson gson;

    public static Future<?> submitTask(Runnable runnable) {
        return executorPool.submit(runnable);
    }

    public static <O> CompletableFuture<O> submitTask(Callable<O> callable) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callable.call();
            } catch (Exception exception) {
                Log.e(TAG, "submitTask: ", exception);

                return null;
            }
        }, executorPool);
    }

    public static Gson getGsonInstance() {
        if (gson == null) {
            gson = new Gson();
        }

        return gson;
    }

    public static boolean isMinimumSDK(int SDK) {
        return Build.VERSION.SDK_INT >= SDK;
    }

    public static double toFahrenheit(double celsius) {
        return (celsius * 1.8d) + 32;
    }

    public static double msToMph(double speed) {
        return speed * 2.237d;
    }

    public static UUID intToUUID(int value) {
        return UUID.nameUUIDFromBytes(ByteBuffer.allocate(Integer.SIZE / Byte.SIZE)
                .putInt(value).array());
    }

    public static double getGenericInterpolatedValue(@FloatRange(from = 0, to = 1) double value) {
        return value < 0.5 ? 4 * value * value * value : 1 - Math.pow(-2 * value + 2, 3) / 2;
    }

    public static LauncherActivityInfo getMainActivity(LauncherApps launcherApps, String packageName, UserHandle handle) {
        List<LauncherActivityInfo> activityInfoList = launcherApps.getActivityList(packageName, handle);

        if (!activityInfoList.isEmpty()) {
            return activityInfoList.get(0);
        }

        return null;
    }

    public static LauncherActivityInfo getMainActivity(Context context, String packageName, UserHandle handle) {
        return getMainActivity(context.getSystemService(LauncherApps.class), packageName, handle);
    }

    public static boolean isMainProfile(UserHandle handle) {
        return handle != null && handle.equals(ProfileManager.getOwner());
    }

    public static boolean isProfileAvailable(Context context, UserHandle handle) {
        return context.getSystemService(UserManager.class).isUserUnlocked(handle);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;

        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static String getPublicIPAddress() {
        for (String api : IPV4_APIS) {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new URL(api).openStream(),
                                StandardCharsets.UTF_8));

                return reader.readLine();
            } catch (Exception exception) {
                Log.e(TAG, "getPublicIPAddress: ", exception);
            }
        }

        return null;
    }

    public static boolean isNotificationServiceEnabled(Context context) {
        String flat = android.provider.Settings.Secure.getString(context.getContentResolver(),
                "enabled_notification_listeners");

        if (!TextUtils.isEmpty(flat)) {
            String[] names = flat.split(":");

            for (String name : names) {
                final ComponentName component = ComponentName.unflattenFromString(name);

                if (component != null) {
                    if (TextUtils.equals(BuildConfig.APPLICATION_ID, component.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isAccessibilityServiceEnabled(Context context) {
        int accessibilityEnabled = 0;
        String service = BuildConfig.APPLICATION_ID + "/" + AccessibilityService.class.getCanonicalName();

        try {
            accessibilityEnabled = android.provider.Settings.Secure
                    .getInt(context.getApplicationContext().getContentResolver(),
                            android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (android.provider.Settings.SettingNotFoundException exception) {
            Log.e(TAG, "Error finding setting, default accessibility not found: " + exception.getMessage());
        }

        TextUtils.SimpleStringSplitter stringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = android.provider.Settings.Secure
                    .getString(context.getApplicationContext().getContentResolver(),
                            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            if (settingValue != null) {
                stringColonSplitter.setString(settingValue);

                while (stringColonSplitter.hasNext()) {
                    String accessibilityService = stringColonSplitter.next();

                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
