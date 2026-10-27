/*
 * Copyright (C) 2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.thermal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserHandle;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.FileUtils;

public final class ThermalUtils {

    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_BENCHMARK = 1;
    protected static final int STATE_BROWSER = 2;
    protected static final int STATE_CAMERA = 3;
    protected static final int STATE_DIALER = 4;
    protected static final int STATE_GAMING = 5;
    protected static final int STATE_STREAMING = 6;
    static final String THERMAL_CONTROL = "thermal_control";
    private static final String THERMAL_STATE_DEFAULT = "0";
    private static final String THERMAL_STATE_BENCHMARK = "10";
    private static final String THERMAL_STATE_BROWSER = "11";
    private static final String THERMAL_STATE_CAMERA = "12";
    private static final String THERMAL_STATE_DIALER = "8";
    private static final String THERMAL_STATE_GAMING = "9";
    private static final String THERMAL_STATE_STREAMING = "14";

    private static final String THERMAL_BENCHMARK = "thermal.benchmark=";
    private static final String THERMAL_BROWSER = "thermal.browser=";
    private static final String THERMAL_CAMERA = "thermal.camera=";
    private static final String THERMAL_DIALER = "thermal.dialer=";
    private static final String THERMAL_GAMING = "thermal.gaming=";
    private static final String THERMAL_STREAMING = "thermal.streaming=";

    private static final String THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig";

    private static final String[] PROFILE_KEYS = {
            THERMAL_BENCHMARK, THERMAL_BROWSER, THERMAL_CAMERA,
            THERMAL_DIALER, THERMAL_GAMING, THERMAL_STREAMING
    };
    private static final String[] THERMAL_STATES = {
            THERMAL_STATE_DEFAULT, THERMAL_STATE_BENCHMARK, THERMAL_STATE_BROWSER,
            THERMAL_STATE_CAMERA, THERMAL_STATE_DIALER, THERMAL_STATE_GAMING,
            THERMAL_STATE_STREAMING
    };

    private final SharedPreferences mSharedPrefs;

    protected ThermalUtils(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void startService(Context context) {
        if (FileUtils.fileExists(THERMAL_SCONFIG)) {
            context.startServiceAsUser(new Intent(context, ThermalService.class),
                    UserHandle.CURRENT);
        }
    }

    private String[] getProfiles() {
        String stored = mSharedPrefs.getString(THERMAL_CONTROL, null);
        String[] profiles = PROFILE_KEYS.clone();
        if (stored != null) {
            for (String profile : stored.split(":", -1)) {
                for (int i = 0; i < PROFILE_KEYS.length; i++) {
                    if (profile.startsWith(PROFILE_KEYS[i])) {
                        profiles[i] = profile;
                        break;
                    }
                }
            }
        }
        String normalized = String.join(":", profiles);
        if (!normalized.equals(stored)) {
            mSharedPrefs.edit().putString(THERMAL_CONTROL, normalized).apply();
        }
        return profiles;
    }

    protected void writePackage(String packageName, int mode) {
        if (packageName == null || packageName.isEmpty()
                || mode < STATE_DEFAULT || mode > STATE_STREAMING) {
            return;
        }
        String[] profiles = getProfiles();
        for (int i = 0; i < profiles.length; i++) {
            StringBuilder profile = new StringBuilder(PROFILE_KEYS[i]);
            for (String entry : profiles[i].substring(PROFILE_KEYS[i].length()).split(",")) {
                if (!entry.isEmpty() && !entry.equals(packageName)) {
                    profile.append(entry).append(',');
                }
            }
            if (mode == i + 1) {
                profile.append(packageName).append(',');
            }
            profiles[i] = profile.toString();
        }
        mSharedPrefs.edit().putString(THERMAL_CONTROL, String.join(":", profiles)).apply();
    }

    protected int getStateForPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return STATE_DEFAULT;
        }
        String[] profiles = getProfiles();
        for (int i = 0; i < profiles.length; i++) {
            for (String entry : profiles[i].substring(PROFILE_KEYS[i].length()).split(",")) {
                if (entry.equals(packageName)) {
                    return i + 1;
                }
            }
        }
        return STATE_DEFAULT;
    }

    protected void setDefaultThermalProfile() {
        FileUtils.writeLine(THERMAL_SCONFIG, THERMAL_STATE_DEFAULT);
    }

    protected void setThermalProfile(String packageName) {
        FileUtils.writeLine(THERMAL_SCONFIG, THERMAL_STATES[getStateForPackage(packageName)]);
    }
}
