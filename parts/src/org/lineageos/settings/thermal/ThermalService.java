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

import android.app.ActivityTaskManager;
import android.app.ActivityTaskManager.RootTaskInfo;
import android.app.IActivityTaskManager;
import android.app.Service;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class ThermalService extends Service {

    private static final String TAG = "ThermalService";

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private ThermalUtils mThermalUtils;
    private IActivityTaskManager mActivityTaskManager;
    private PowerManager mPowerManager;
    private SharedPreferences mSharedPrefs;
    private boolean mDestroyed;
    private int mAppliedState = -1;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateThermalProfile();
        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener =
            (preferences, key) -> {
                if (ThermalUtils.THERMAL_CONTROL.equals(key)) {
                    mHandler.post(this::updateThermalProfile);
                }
            };

    private final TaskStackListener mTaskListener = new TaskStackListener() {
        @Override
        public void onTaskStackChanged() {
            mHandler.post(ThermalService.this::updateThermalProfile);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mThermalUtils = new ThermalUtils(this);
        mPowerManager = getSystemService(PowerManager.class);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPrefs.registerOnSharedPreferenceChangeListener(mPreferenceListener);
        mActivityTaskManager = ActivityTaskManager.getService();
        try {
            mActivityTaskManager.registerTaskStackListener(mTaskListener);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to register task stack listener", e);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mIntentReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        updateThermalProfile();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateThermalProfile();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateThermalProfile() {
        if (mDestroyed) {
            return;
        }
        String packageName = null;
        if (mPowerManager.isInteractive()) {
            try {
                RootTaskInfo info = mActivityTaskManager.getFocusedRootTaskInfo();
                if (info != null && info.topActivity != null) {
                    packageName = info.topActivity.getPackageName();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to read foreground task", e);
                return;
            }
        }
        int state = mThermalUtils.getStateForPackage(packageName);
        if (state != mAppliedState) {
            mThermalUtils.setThermalProfile(packageName);
            mAppliedState = state;
        }
    }

    @Override
    public void onDestroy() {
        mDestroyed = true;
        try {
            mActivityTaskManager.unregisterTaskStackListener(mTaskListener);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to unregister task stack listener", e);
        }
        unregisterReceiver(mIntentReceiver);
        mSharedPrefs.unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
        mHandler.removeCallbacksAndMessages(null);
        mThermalUtils.setDefaultThermalProfile();
        super.onDestroy();
    }
}
