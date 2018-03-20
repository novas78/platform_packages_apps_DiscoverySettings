package com.discovery.settings.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.discovery.settings.preference.PackageListAdapter;
import com.discovery.settings.preference.PackageListAdapter.PackageItem;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeadsUpSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener {

    private static final int DIALOG_WHITELIST_APPS = 0;

    private PackageListAdapter mPackageAdapter;
    private PackageManager mPackageManager;
    private PreferenceGroup mWhitelistPrefList;
    private Preference mAddWhitelistPref;

    private String mWhitelistPackageList;
    private Map<String, Package> mWhitelistPackages;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get launch-able applications
        addPreferencesFromResource(R.xml.heads_up_settings);
        mPackageManager = getPackageManager();
        mPackageAdapter = new PackageListAdapter(getActivity());

        mWhitelistPrefList = (PreferenceGroup) findPreference("whitelist_applications");
        mWhitelistPrefList.setOrderingAsAdded(false);

        mWhitelistPackages = new HashMap<String, Package>();

        mAddWhitelistPref = findPreference("add_whitelist_packages");

        mAddWhitelistPref.setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCustomApplicationPrefs();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DISCOVERY_SETTINGS;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (dialogId == DIALOG_WHITELIST_APPS) {
            return MetricsProto.MetricsEvent.DISCOVERY_SETTINGS;
        }
        return 0;
    }

    /**
     * Utility classes and supporting methods
     */
    @Override
    public Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Dialog dialog;
        switch (id) {
            case DIALOG_WHITELIST_APPS:
                final ListView list = new ListView(getActivity());
                list.setAdapter(mPackageAdapter);

                builder.setTitle(R.string.profile_choose_app);
                builder.setView(list);
                dialog = builder.create();

                list.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // Add empty application definition, the user will be able to edit it later
                        PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                        addCustomApplicationPref(info.packageName);
                        dialog.cancel();
                    }
                });
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    /**
     * Application class
     */
    private static class Package {
        public String name;
        /**
         * Stores all the application values in one call
         * @param name
         */
        public Package(String name) {
            this.name = name;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(name);
            return builder.toString();
        }

        public static Package fromString(String value) {
            if (TextUtils.isEmpty(value)) {
                return null;
            }

            try {
                Package item = new Package(value);
                return item;
            } catch (NumberFormatException e) {
                return null;
            }
        }

    };

    private void refreshCustomApplicationPrefs() {
        if (!parsePackageList()) {
            return;
        }

        // Add the Application Preferences
        if (mWhitelistPrefList != null) {
            mWhitelistPrefList.removeAll();

            for (Package pkg : mWhitelistPackages.values()) {
                try {
                    Preference pref = createPreferenceFromInfo(pkg);
                    mWhitelistPrefList.addPreference(pref);
                } catch (PackageManager.NameNotFoundException e) {
                    // Do nothing
                }
            }
        }

        // Keep these at the top
        mAddWhitelistPref.setOrder(0);
        // Add 'add' options
        mWhitelistPrefList.addPreference(mAddWhitelistPref);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mAddWhitelistPref) {
            showDialog(DIALOG_WHITELIST_APPS);
        } else {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.dialog_delete_title)
                    .setMessage(R.string.dialog_delete_message)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            removeCustomApplicationPref(preference.getKey());
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null);

        builder.show();
        }
        return true;
    }

     private void addCustomApplicationPref(String packageName) {
        Package pkg = mWhitelistPackages.get(packageName);
        if (pkg == null) {
            pkg = new Package(packageName);
            mWhitelistPackages.put(packageName, pkg);
            savePackageList(false);
            refreshCustomApplicationPrefs();
        }
    }

    private Preference createPreferenceFromInfo(Package pkg)
            throws PackageManager.NameNotFoundException {
        PackageInfo info = mPackageManager.getPackageInfo(pkg.name,
                PackageManager.GET_META_DATA);
        Preference pref =
                new Preference(getActivity());

        pref.setKey(pkg.name);
        pref.setTitle(info.applicationInfo.loadLabel(mPackageManager));
        pref.setIcon(info.applicationInfo.loadIcon(mPackageManager));
        pref.setPersistent(false);
        pref.setOnPreferenceClickListener(this);
        return pref;
    }

    private void removeCustomApplicationPref(String packageName) {
        if (mWhitelistPackages.remove(packageName) != null) {
            savePackageList(false);
            refreshCustomApplicationPrefs();
        }
    }

    private boolean parsePackageList() {

        final String whitelistString = Settings.System.getString(getContentResolver(),
                Settings.System.HEADS_UP_WHITELIST_VALUES);

        if (TextUtils.equals(mWhitelistPackageList, whitelistString)) {
            return false;
        }
            mWhitelistPackageList = whitelistString;
            mWhitelistPackages.clear();

        if (whitelistString != null) {
            final String[] array = TextUtils.split(whitelistString, "\\|");
            for (String item : array) {
                if (TextUtils.isEmpty(item)) {
                    continue;
                }
                Package pkg = Package.fromString(item);
                if (pkg != null) {
                    mWhitelistPackages.put(pkg.name, pkg);
                }
            }
        }

        return true;
    }

     private void savePackageList(boolean preferencesUpdated) {
        List<String> settings = new ArrayList<String>();
        for (Package app : mWhitelistPackages.values()) {
            settings.add(app.toString());
        }
        final String value = TextUtils.join("|", settings);
        if (preferencesUpdated) {
                mWhitelistPackageList = value;
            }
        Settings.System.putString(getContentResolver(),
                Settings.System.HEADS_UP_WHITELIST_VALUES, value);
    }
}
