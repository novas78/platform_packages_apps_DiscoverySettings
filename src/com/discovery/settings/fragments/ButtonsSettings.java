/*
 * Copyright (C) 2016-17 Discovery
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

package com.discovery.settings.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.support.v7.preference.Preference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import android.util.Log;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ButtonsSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, Indexable {
    private static final String TAG = "ButtonsSettings";

    private static final int KEY_MASK_HOME = 0x01;
    private static final int KEY_MASK_BACK = 0x02;
    private static final int KEY_MASK_MENU = 0x04;
    private static final int KEY_MASK_ASSIST = 0x08;
    private static final int KEY_MASK_APP_SWITCH = 0x10;
    private static final int KEY_MASK_CAMERA = 0x20;

    private static final String KEY_NAVIGATION_BAR          = "navigation_bar";
    private static final String KEY_BUTTON_BRIGHTNESS       = "button_brightness";
    private static final String KEY_BACKLIGHT_TIMEOUT       = "backlight_timeout";

    private static final String KEY_HOME_LONG_PRESS        = "hardware_keys_home_long_press";
    private static final String KEY_HOME_DOUBLE_TAP        = "hardware_keys_home_double_tap";
    private static final String KEY_BACK_PRESS             = "hardware_keys_back_press";
    private static final String KEY_BACK_LONG_PRESS        = "hardware_keys_back_long_press";
    private static final String KEY_BACK_DOUBLE_TAP        = "hardware_keys_back_double_tap";
    private static final String KEY_MENU_PRESS             = "hardware_keys_menu_press";
    private static final String KEY_MENU_LONG_PRESS        = "hardware_keys_menu_long_press";
    private static final String KEY_MENU_DOUBLE_TAP        = "hardware_keys_menu_double_tap";
    private static final String KEY_ASSIST_PRESS           = "hardware_keys_assist_press";
    private static final String KEY_ASSIST_LONG_PRESS      = "hardware_keys_assist_long_press";
    private static final String KEY_ASSIST_DOUBLE_TAP      = "hardware_keys_assist_double_tap";
    private static final String KEY_APP_SWITCH_PRESS       = "hardware_keys_app_switch_press";
    private static final String KEY_APP_SWITCH_LONG_PRESS  = "hardware_keys_app_switch_long_press";
    private static final String KEY_APP_SWITCH_DOUBLE_TAP  = "hardware_keys_app_switch_double_tap";
    private static final String KEY_CAMERA_PRESS           = "hardware_keys_camera_press";
    private static final String KEY_CAMERA_LONG_PRESS      = "hardware_keys_camera_long_press";
    private static final String KEY_CAMERA_DOUBLE_TAP      = "hardware_keys_camera_double_tap";

    private static final String KEY_HOME_WAKE              = "home_wake_key";
    private static final String KEY_HOME_VIBRATION         = "home_press_vibration";

    private static final String KEY_CATEGORY_HOME          = "home_key";
    private static final String KEY_CATEGORY_BACK          = "back_key";
    private static final String KEY_CATEGORY_MENU          = "menu_key";
    private static final String KEY_CATEGORY_ASSIST        = "assist_key";
    private static final String KEY_CATEGORY_APP_SWITCH    = "app_switch_key";
    private static final String KEY_CATEGORY_CAMERA        = "camera_key";

    private static final String EMPTY_STRING = "";

    private Handler mHandler;

    private int mDeviceHardwareKeys;
    private boolean mRemoveHomePressVib;

    private ListPreference mHomeLongPressAction;
    private ListPreference mHomeDoubleTapAction;
    private ListPreference mBackPressAction;
    private ListPreference mBackLongPressAction;
    private ListPreference mBackDoubleTapAction;
    private ListPreference mMenuPressAction;
    private ListPreference mMenuLongPressAction;
    private ListPreference mMenuDoubleTapAction;
    private ListPreference mAssistPressAction;
    private ListPreference mAssistLongPressAction;
    private ListPreference mAssistDoubleTapAction;
    private ListPreference mAppSwitchPressAction;
    private ListPreference mAppSwitchLongPressAction;
    private ListPreference mAppSwitchDoubleTapAction;
    private ListPreference mCameraPressAction;
    private ListPreference mCameraLongPressAction;
    private ListPreference mCameraDoubleTapAction;

    private SwitchPreference mHomeWakeKey;
    private SwitchPreference mHomePressVibration;

    private SwitchPreference mNavigationBar;
    private SwitchPreference mButtonBrightness;
    private ListPreference mBacklightTimeout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.buttons_settings);

        mHandler = new Handler();

        final Resources res = getActivity().getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        mDeviceHardwareKeys = res.getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        mRemoveHomePressVib = res.getBoolean(
                com.android.internal.R.bool.config_removeHomePressVib);

        /* Navigation Bar */
        mNavigationBar = (SwitchPreference) findPreference(KEY_NAVIGATION_BAR);
        if (mNavigationBar != null) {
            if (mDeviceHardwareKeys > 0) {
                mNavigationBar.setOnPreferenceChangeListener(this);
            } else {
                prefScreen.removePreference(mNavigationBar);
            }
        }

        /* Button Brightness */
        mButtonBrightness = (SwitchPreference) findPreference(KEY_BUTTON_BRIGHTNESS);
        if (mButtonBrightness != null) {
            int defaultButtonBrightness = res.getInteger(
                    com.android.internal.R.integer.config_buttonBrightnessSettingDefault);
            if (defaultButtonBrightness > 0) {
                mButtonBrightness.setOnPreferenceChangeListener(this);
            } else {
                prefScreen.removePreference(mButtonBrightness);
            }
        }

        mBacklightTimeout =
                (ListPreference) findPreference(KEY_BACKLIGHT_TIMEOUT);
        // Backlight
        if (mBacklightTimeout != null) {
            int defaultButtonBrightness = res.getInteger(
                    com.android.internal.R.integer.config_buttonBrightnessSettingDefault);
            if (defaultButtonBrightness > 0) {
                mBacklightTimeout.setOnPreferenceChangeListener(this);
            } else {
                prefScreen.removePreference(mBacklightTimeout);
            }
            int BacklightTimeout = Settings.System.getInt(getContentResolver(),
                    Settings.System.BUTTON_BACKLIGHT_TIMEOUT, 5000);
            mBacklightTimeout.setValue(Integer.toString(BacklightTimeout));
            mBacklightTimeout.setSummary(mBacklightTimeout.getEntry());
        }

        /* Home key wake device */
        mHomeWakeKey = (SwitchPreference) findPreference(KEY_HOME_WAKE);
        if (mHomeWakeKey != null) {
            if (mDeviceHardwareKeys > 0) {
                mHomeWakeKey.setOnPreferenceChangeListener(this);
            } else {
                prefScreen.removePreference(mHomeWakeKey);
            }
        }

        /* Disable home key vibration */
        mHomePressVibration = (SwitchPreference) findPreference(KEY_HOME_VIBRATION);
        if (mHomePressVibration != null) {
            if (mDeviceHardwareKeys > 0 && mRemoveHomePressVib == true) {
                mHomePressVibration.setOnPreferenceChangeListener(this);
            } else {
                prefScreen.removePreference(mHomePressVibration);
            }
        }

        /* Home Key Long Press */
        int defaultLongPressOnHardwareHomeBehavior = res.getInteger(
                com.android.internal.R.integer.config_longPressOnHardwareHomeBehavior);
        int longPressOnHardwareHomeBehavior = Settings.System.getIntForUser(resolver,
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION,
                    defaultLongPressOnHardwareHomeBehavior,
                    UserHandle.USER_CURRENT);
        mHomeLongPressAction = initActionList(KEY_HOME_LONG_PRESS, longPressOnHardwareHomeBehavior);

        /* Home Key Double Tap */
        int defaultDoubleTapOnHardwareHomeBehavior = res.getInteger(
                com.android.internal.R.integer.config_doubleTapOnHardwareHomeBehavior);
        int doubleTapOnHardwareHomeBehavior = Settings.System.getIntForUser(resolver,
                    Settings.System.KEY_HOME_DOUBLE_TAP_ACTION,
                    defaultDoubleTapOnHardwareHomeBehavior,
                    UserHandle.USER_CURRENT);
        mHomeDoubleTapAction = initActionList(KEY_HOME_DOUBLE_TAP, doubleTapOnHardwareHomeBehavior);

        /* Back Key Press */
        int defaultPressOnHardwareBackBehavior = res.getInteger(
                com.android.internal.R.integer.config_pressOnHardwareBackBehavior);
        int pressOnHardwareBackBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_BACK_ACTION,
                defaultPressOnHardwareBackBehavior,
                UserHandle.USER_CURRENT);
        mBackPressAction = initActionList(KEY_BACK_PRESS, pressOnHardwareBackBehavior);

        /* Back Key Long Press */
        int defaultLongPressOnHardwareBackBehavior = res.getInteger(
                com.android.internal.R.integer.config_longPressOnHardwareBackBehavior);
        int longPressOnHardwareBackBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_BACK_LONG_PRESS_ACTION,
                defaultLongPressOnHardwareBackBehavior,
                UserHandle.USER_CURRENT);
        mBackLongPressAction = initActionList(KEY_BACK_LONG_PRESS, longPressOnHardwareBackBehavior);

        /* Back Key Double Tap */
        int defaultDoubleTapOnHardwareBackBehavior = res.getInteger(
                com.android.internal.R.integer.config_doubleTapOnHardwareBackBehavior);
        int doubleTapOnHardwareBackBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_BACK_DOUBLE_TAP_ACTION,
                defaultDoubleTapOnHardwareBackBehavior,
                UserHandle.USER_CURRENT);
        mBackDoubleTapAction = initActionList(KEY_BACK_DOUBLE_TAP, doubleTapOnHardwareBackBehavior);

        /* Menu Key Press */
        int defaultPressOnHardwareMenuBehavior = res.getInteger(
                com.android.internal.R.integer.config_pressOnHardwareMenuBehavior);
        int pressOnHardwareMenuBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_MENU_ACTION,
                defaultPressOnHardwareMenuBehavior,
                UserHandle.USER_CURRENT);
        mMenuPressAction = initActionList(KEY_MENU_PRESS, pressOnHardwareMenuBehavior);

        /* Menu Key Long Press */
        int defaultLongPressOnHardwareMenuBehavior = res.getInteger(
                com.android.internal.R.integer.config_longPressOnHardwareMenuBehavior);
        int longPressOnHardwareMenuBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_MENU_LONG_PRESS_ACTION,
                defaultLongPressOnHardwareMenuBehavior,
                UserHandle.USER_CURRENT);
        mMenuLongPressAction = initActionList(KEY_MENU_LONG_PRESS, longPressOnHardwareMenuBehavior);

        /* Menu Key Double Tap */
        int defaultDoubleTapOnHardwareMenuBehavior = res.getInteger(
                com.android.internal.R.integer.config_doubleTapOnHardwareMenuBehavior);
        int doubleTapOnHardwareMenuBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_MENU_DOUBLE_TAP_ACTION,
                defaultDoubleTapOnHardwareMenuBehavior,
                UserHandle.USER_CURRENT);
        mMenuDoubleTapAction = initActionList(KEY_MENU_DOUBLE_TAP, doubleTapOnHardwareMenuBehavior);

        /* Assist Key Press */
        int defaultPressOnHardwareAssistBehavior = res.getInteger(
                com.android.internal.R.integer.config_pressOnHardwareAssistBehavior);
        int pressOnHardwareAssistBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_ASSIST_ACTION,
                defaultPressOnHardwareAssistBehavior,
                UserHandle.USER_CURRENT);
        mAssistPressAction = initActionList(KEY_ASSIST_PRESS, pressOnHardwareAssistBehavior);

        /* Assist Key Long Press */
        int defaultLongPressOnHardwareAssistBehavior = res.getInteger(
                com.android.internal.R.integer.config_longPressOnHardwareAssistBehavior);
        int longPressOnHardwareAssistBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_ASSIST_LONG_PRESS_ACTION,
                defaultLongPressOnHardwareAssistBehavior,
                UserHandle.USER_CURRENT);
        mAssistLongPressAction = initActionList(KEY_ASSIST_LONG_PRESS, longPressOnHardwareAssistBehavior);

        /* Assist Key Double Tap */
        int defaultDoubleTapOnHardwareAssistBehavior = res.getInteger(
                com.android.internal.R.integer.config_doubleTapOnHardwareAssistBehavior);
        int doubleTapOnHardwareAssistBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_ASSIST_DOUBLE_TAP_ACTION,
                defaultDoubleTapOnHardwareAssistBehavior,
                UserHandle.USER_CURRENT);
        mAssistDoubleTapAction = initActionList(KEY_ASSIST_DOUBLE_TAP, doubleTapOnHardwareAssistBehavior);

        /* AppSwitch Key Press */
        int defaultPressOnHardwareAppSwitchBehavior = res.getInteger(
                com.android.internal.R.integer.config_pressOnHardwareAppSwitchBehavior);
        int pressOnHardwareAppSwitchBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_APP_SWITCH_ACTION,
                defaultPressOnHardwareAppSwitchBehavior,
                UserHandle.USER_CURRENT);
        mAppSwitchPressAction = initActionList(KEY_APP_SWITCH_PRESS, pressOnHardwareAppSwitchBehavior);

        /* AppSwitch Key Long Press */
        int defaultLongPressOnHardwareAppSwitchBehavior = res.getInteger(
                com.android.internal.R.integer.config_longPressOnHardwareAppSwitchBehavior);
        int longPressOnHardwareAppSwitchBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION,
                defaultLongPressOnHardwareAppSwitchBehavior,
                UserHandle.USER_CURRENT);
        mAppSwitchLongPressAction = initActionList(KEY_APP_SWITCH_LONG_PRESS, longPressOnHardwareAppSwitchBehavior);

        /* AppSwitch Key Double Tap */
        int defaultDoubleTapOnHardwareAppSwitchBehavior = res.getInteger(
                com.android.internal.R.integer.config_doubleTapOnHardwareAppSwitchBehavior);
        int doubleTapOnHardwareAppSwitchBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_APP_SWITCH_DOUBLE_TAP_ACTION,
                defaultDoubleTapOnHardwareAppSwitchBehavior,
                UserHandle.USER_CURRENT);
        mAppSwitchDoubleTapAction = initActionList(KEY_APP_SWITCH_DOUBLE_TAP, doubleTapOnHardwareAppSwitchBehavior);

        /* Camera Key Press */
        int defaultPressOnHardwareCameraBehavior = res.getInteger(
                com.android.internal.R.integer.config_pressOnHardwareCameraBehavior);
        int pressOnHardwareCameraBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_CAMERA_ACTION,
                defaultPressOnHardwareCameraBehavior,
                UserHandle.USER_CURRENT);
        mCameraPressAction = initActionList(KEY_CAMERA_PRESS, pressOnHardwareCameraBehavior);

        /* Camera Key Long Press */
        int defaultLongPressOnHardwareCameraBehavior = res.getInteger(
                com.android.internal.R.integer.config_longPressOnHardwareCameraBehavior);
        int longPressOnHardwareCameraBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_CAMERA_LONG_PRESS_ACTION,
                defaultLongPressOnHardwareCameraBehavior,
                UserHandle.USER_CURRENT);
        mCameraLongPressAction = initActionList(KEY_CAMERA_LONG_PRESS, longPressOnHardwareCameraBehavior);

        /* Camera Key Double Tap */
        int defaultDoubleTapOnHardwareCameraBehavior = res.getInteger(
                com.android.internal.R.integer.config_doubleTapOnHardwareCameraBehavior);
        int doubleTapOnHardwareCameraBehavior = Settings.System.getIntForUser(resolver,
                Settings.System.KEY_CAMERA_DOUBLE_TAP_ACTION,
                defaultDoubleTapOnHardwareCameraBehavior,
                UserHandle.USER_CURRENT);
        mCameraDoubleTapAction = initActionList(KEY_CAMERA_DOUBLE_TAP, doubleTapOnHardwareCameraBehavior);
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DISCOVERY;
    }

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        if (list != null) {
            list.setValue(Integer.toString(value));
            list.setSummary(list.getEntry());
            list.setOnPreferenceChangeListener(this);
        }
        return list;
    }

    private boolean handleOnPreferenceTreeClick(Preference preference) {
        if (preference != null && preference == mNavigationBar) {
            mNavigationBar.setEnabled(false);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mNavigationBar.setEnabled(true);
                }
            }, 1000);
            return true;
        }
        return false;
    }

    private boolean handleOnPreferenceChange(Preference preference, Object newValue) {
        final String setting = getSystemPreferenceString(preference);

        if (TextUtils.isEmpty(setting)) {
            // No system setting.
            return false;
        }

        if (preference != null && preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            String value = (String) newValue;
            int index = listPref.findIndexOfValue(value);
            listPref.setSummary(listPref.getEntries()[index]);
            Settings.System.putIntForUser(getContentResolver(), setting, Integer.valueOf(value),
                    UserHandle.USER_CURRENT);
        } else if (preference != null && preference instanceof SwitchPreference) {
            boolean state = false;
            if (newValue instanceof Boolean) {
                state = (Boolean) newValue;
            } else if (newValue instanceof String) {
                state = Integer.valueOf((String) newValue) != 0;
            }
            Settings.System.putIntForUser(getContentResolver(), setting, state ? 1 : 0,
                    UserHandle.USER_CURRENT);
        }

        return true;
    }

    private String getSystemPreferenceString(Preference preference) {
        if (preference == null) {
            return EMPTY_STRING;
        } else if (preference == mNavigationBar) {
            return Settings.System.NAVIGATION_BAR_ENABLED;
        } else if (preference == mButtonBrightness) {
            return Settings.System.BUTTON_BRIGHTNESS_ENABLED;
        } else if (preference == mBacklightTimeout) {
            return Settings.System.BUTTON_BACKLIGHT_TIMEOUT;
        } else if (preference == mHomeWakeKey) {
            return Settings.System.HOME_WAKE_SCREEN;
        } else if (preference == mHomePressVibration) {
            return Settings.System.HOME_PRESS_VIBRATION;
        } else if (preference == mHomeLongPressAction) {
            return Settings.System.KEY_HOME_LONG_PRESS_ACTION;
        } else if (preference == mHomeDoubleTapAction) {
            return Settings.System.KEY_HOME_DOUBLE_TAP_ACTION;
        } else if (preference == mBackPressAction) {
            return Settings.System.KEY_BACK_ACTION;
        } else if (preference == mBackLongPressAction) {
            return Settings.System.KEY_BACK_LONG_PRESS_ACTION;
        } else if (preference == mBackDoubleTapAction) {
            return Settings.System.KEY_BACK_DOUBLE_TAP_ACTION;
        } else if (preference == mMenuPressAction) {
            return Settings.System.KEY_MENU_ACTION;
        } else if (preference == mMenuLongPressAction) {
            return Settings.System.KEY_MENU_LONG_PRESS_ACTION;
        } else if (preference == mMenuDoubleTapAction) {
            return Settings.System.KEY_MENU_DOUBLE_TAP_ACTION;
        } else if (preference == mAssistPressAction) {
            return Settings.System.KEY_ASSIST_ACTION;
        } else if (preference == mAssistLongPressAction) {
            return Settings.System.KEY_ASSIST_LONG_PRESS_ACTION;
        } else if (preference == mAssistDoubleTapAction) {
            return Settings.System.KEY_ASSIST_DOUBLE_TAP_ACTION;
        } else if (preference == mAppSwitchPressAction) {
            return Settings.System.KEY_APP_SWITCH_ACTION;
        } else if (preference == mAppSwitchLongPressAction) {
            return Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION;
        } else if (preference == mAppSwitchDoubleTapAction) {
            return Settings.System.KEY_APP_SWITCH_DOUBLE_TAP_ACTION;
        } else if (preference == mCameraPressAction) {
            return Settings.System.KEY_CAMERA_ACTION;
        } else if (preference == mCameraLongPressAction) {
            return Settings.System.KEY_CAMERA_LONG_PRESS_ACTION;
        } else if (preference == mCameraDoubleTapAction) {
            return Settings.System.KEY_CAMERA_DOUBLE_TAP_ACTION;
        }

        return EMPTY_STRING;
    }

    private void reload() {
        final ContentResolver resolver = getActivity().getContentResolver();

        final boolean hasHome = (mDeviceHardwareKeys & KEY_MASK_HOME) != 0;
        final boolean hasMenu = (mDeviceHardwareKeys & KEY_MASK_MENU) != 0;
        final boolean hasBack = (mDeviceHardwareKeys & KEY_MASK_BACK) != 0;
        final boolean hasAssist = (mDeviceHardwareKeys & KEY_MASK_ASSIST) != 0;
        final boolean hasAppSwitch = (mDeviceHardwareKeys & KEY_MASK_APP_SWITCH) != 0;
        final boolean hasCamera = (mDeviceHardwareKeys & KEY_MASK_CAMERA) != 0;

        final boolean navigationBarEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.NAVIGATION_BAR_ENABLED, 0, UserHandle.USER_CURRENT) != 0;

        final boolean buttonBrightnessEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.BUTTON_BRIGHTNESS_ENABLED, 0, UserHandle.USER_CURRENT) != 0;

        final boolean homeWakeEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.HOME_WAKE_SCREEN, 1, UserHandle.USER_CURRENT) != 0;

        final boolean homePressVibrationEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.HOME_PRESS_VIBRATION, 0, UserHandle.USER_CURRENT) != 0;

        if (mNavigationBar != null) {
            mNavigationBar.setChecked(navigationBarEnabled);
        }

        if (mButtonBrightness != null) {
            mButtonBrightness.setChecked(buttonBrightnessEnabled);
        }

        if (mHomeWakeKey != null) {
            mHomeWakeKey.setChecked(homeWakeEnabled);
        }

        if (mHomePressVibration != null) {
            mHomePressVibration.setChecked(homePressVibrationEnabled);
        }

        final PreferenceScreen prefScreen = getPreferenceScreen();

        final PreferenceCategory homeCategory =
                (PreferenceCategory) prefScreen.findPreference(KEY_CATEGORY_HOME);

        final PreferenceCategory backCategory =
                (PreferenceCategory) prefScreen.findPreference(KEY_CATEGORY_BACK);

        final PreferenceCategory menuCategory =
                (PreferenceCategory) prefScreen.findPreference(KEY_CATEGORY_MENU);

        final PreferenceCategory assistCategory =
                (PreferenceCategory) prefScreen.findPreference(KEY_CATEGORY_ASSIST);

        final PreferenceCategory appSwitchCategory =
                (PreferenceCategory) prefScreen.findPreference(KEY_CATEGORY_APP_SWITCH);

        final PreferenceCategory cameraCategory =
                (PreferenceCategory) prefScreen.findPreference(KEY_CATEGORY_CAMERA);

        if (mDeviceHardwareKeys != 0 && mButtonBrightness != null) {
            mButtonBrightness.setEnabled(!navigationBarEnabled);
        } else if (mDeviceHardwareKeys == 0 && mButtonBrightness != null) {
            prefScreen.removePreference(mButtonBrightness);
        }

        if (hasHome) {
            mHomeWakeKey.setEnabled(!navigationBarEnabled);
        } else {
            prefScreen.removePreference(mHomeWakeKey);
        }

        if (hasHome) {
            mHomePressVibration.setEnabled(!navigationBarEnabled);
        } else {
            prefScreen.removePreference(mHomePressVibration);
        }

        if (hasHome && homeCategory != null) {
            homeCategory.setEnabled(!navigationBarEnabled);
        } else if (!hasHome && homeCategory != null) {
            prefScreen.removePreference(homeCategory);
        }

        if (hasBack && backCategory != null) {
            backCategory.setEnabled(!navigationBarEnabled);
        } else if (!hasBack && backCategory != null) {
            prefScreen.removePreference(backCategory);
        }

        if (hasMenu && menuCategory != null) {
            menuCategory.setEnabled(!navigationBarEnabled);
        } else if (!hasMenu && menuCategory != null) {
            prefScreen.removePreference(menuCategory);
        }

        if (hasAssist && assistCategory != null) {
            assistCategory.setEnabled(!navigationBarEnabled);
        } else if (!hasAssist && assistCategory != null) {
            prefScreen.removePreference(assistCategory);
        }

        if (hasAppSwitch && appSwitchCategory != null) {
            appSwitchCategory.setEnabled(!navigationBarEnabled);
        } else if (!hasAppSwitch && appSwitchCategory != null) {
            prefScreen.removePreference(appSwitchCategory);
        }

        if (hasCamera && cameraCategory != null) {
            cameraCategory.setEnabled(true /*!navigationBarEnabled*/);
        } else if (!hasCamera && cameraCategory != null) {
            prefScreen.removePreference(cameraCategory);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean handled = handleOnPreferenceChange(preference, newValue);
        if (handled) {
            if (preference == mBacklightTimeout) {
                String BacklightTimeout = (String) newValue;
                int BacklightTimeoutValue = Integer.parseInt(BacklightTimeout);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.BUTTON_BACKLIGHT_TIMEOUT, BacklightTimeoutValue);
                int BacklightTimeoutIndex = mBacklightTimeout
                        .findIndexOfValue(BacklightTimeout);
                mBacklightTimeout
                        .setSummary(mBacklightTimeout.getEntries()[BacklightTimeoutIndex]);
            }
            reload();
        }
        return handled;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final boolean handled = handleOnPreferenceTreeClick(preference);
        // return super.onPreferenceTreeClick(preferenceScreen, preference);
        return handled;
    }

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.buttons_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final ArrayList<String> result = new ArrayList<String>();

                final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
                final int myUserId = UserHandle.myUserId();
                final boolean isSecondaryUser = myUserId != UserHandle.USER_OWNER;

                // TODO: Implement search index provider.

                return result;
            }
        };
}
