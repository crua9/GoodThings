package com.techreviewsandhelp.goodthings.settings;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.techreviewsandhelp.goodthings.AlarmReceiver;
import com.techreviewsandhelp.goodthings.AsyncSync;
import com.techreviewsandhelp.goodthings.HowToActivity;
import com.techreviewsandhelp.goodthings.R;
import com.dropbox.core.android.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.techreviewsandhelp.goodthings.TimePreference;

import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity {
    private static Context mContext;
    private static Resources mResources;
    private static SharedPreferences mSharedPreferences;
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 111;
    private static GoogleApiClient mGoogleApiClient;
    static SettingsActivity mSettingsActivity;
    DataSyncPreferenceFragment mDataSyncPreferenceFragment = null;

    public final static String DROP_KEY = "dropbox_key";
    public final static String DRIVE_KEY = "google_drive_key";
    final static String DROP_TOKEN_KEY = "drop_tok";
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue;
            stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        mResources = getResources();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSettingsActivity = this;
        setupActionBar();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                    showToastMessage("Google Drive connected");
                }
                else {
                    if (mDataSyncPreferenceFragment != null){
                        CheckBoxPreference drivePref = (CheckBoxPreference)mDataSyncPreferenceFragment.findPreference(DRIVE_KEY);
                        drivePref.setChecked(false);
                    }
                    mSharedPreferences.edit().putBoolean(DRIVE_KEY,false).apply();
                    showToastMessage("Google Drive connection error");
                }
                break;
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || InfoPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    static void showToastMessage(String message){
        Toast.makeText(mSettingsActivity,message,Toast.LENGTH_SHORT).show();
    }
/*-----------------------------------Fragment preferences below-----------------------------------*/

    /**
     * This fragment shows info preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class InfoPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_info);
            setHasOptionsMenu(true);

            Preference preference = findPreference("contact_key");
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("plain/text");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"craigbennettii@techreviewsandhelp.com"});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                    startActivity(emailIntent);
                    return true;
                }
            });
            Preference helpPreference = findPreference("help_key");
            helpPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(mContext, HowToActivity.class);
                    startActivity(intent);
                    return true;
                }
            });
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            CheckBoxPreference notificationSwitch = (CheckBoxPreference) findPreference("notification_key");
            notificationSwitch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
                    if (!checkBoxPreference.isChecked()) {
                        AlarmReceiver.clearNotifications(mContext);
                    }
                    return true;
                }
            });

            Preference endOfDayPref = findPreference("end_of_day");
            Long defVal = System.currentTimeMillis();
            endOfDayPref.setDefaultValue(defVal);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            ((SettingsActivity)this.getActivity()).mDataSyncPreferenceFragment = this;

            CheckBoxPreference dropboxPref = (CheckBoxPreference) findPreference(DROP_KEY);
            dropboxPref.setOnPreferenceClickListener(onDropBoxPrefClickListener);

            CheckBoxPreference drivePref = (CheckBoxPreference) findPreference(DRIVE_KEY);
            drivePref.setOnPreferenceClickListener(onGoogleDrivePrefClickListener);

            Preference saveToSdPref = findPreference("save_to_sd_key");
            saveToSdPref.setOnPreferenceClickListener(onSaveToSdClickListener);

            Preference loadFromSdPref = findPreference("load_from_sd_key");
            loadFromSdPref.setOnPreferenceClickListener(onLoadFromSdClickListener);

            Preference saveToDropPref = findPreference("save_to_drop_key");
            saveToDropPref.setOnPreferenceClickListener(onSaveToDropClickListener);

            Preference loadFromDropPref = findPreference("load_from_drop_key");
            loadFromDropPref.setOnPreferenceClickListener(onLoadFromDropClickListener);

            Preference saveToDrivePref = findPreference("save_to_drive_key");
            saveToDrivePref.setOnPreferenceClickListener(onSaveToDriveClickListener);

            Preference loadFromDrivePref = findPreference("load_from_drive_key");
            loadFromDrivePref.setOnPreferenceClickListener(onLoadFromDriveClickListener);
        }

        Preference.OnPreferenceClickListener onDropBoxPrefClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                CheckBoxPreference dropboxPr = (CheckBoxPreference) preference;
                if (dropboxPr.isChecked()) {
                    CheckBoxPreference drivePref = ((CheckBoxPreference)findPreference(DRIVE_KEY));
                    drivePref.setChecked(false);
                    mSharedPreferences.edit().putBoolean(DRIVE_KEY,false).apply();

                    Auth.startOAuth2Authentication(getActivity(), getResources().getString(R.string.dropbox_app_key));
                }
                else {
                    mSharedPreferences.edit().remove(DROP_TOKEN_KEY).apply();
                    showToastMessage("Dropbox disconnected");
                }
                return true;
            }
        };

        Preference.OnPreferenceClickListener onGoogleDrivePrefClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                CheckBoxPreference gDrivePr = (CheckBoxPreference) preference;
                if (gDrivePr.isChecked()) {
                    CheckBoxPreference dropPref = ((CheckBoxPreference) findPreference(DROP_KEY));
                    dropPref.setChecked(false);
                    mSharedPreferences.edit().putBoolean(DROP_KEY, false).apply();

                    mGoogleApiClient = new GoogleApiClient.Builder(mSettingsActivity.getApplicationContext())
                            .addApi(Drive.API)
                            .addScope(Drive.SCOPE_FILE)
                            .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(Bundle bundle) {
                                    if (bundle != null) Log.d("GoogleDrive", bundle.toString());
                                    Log.d("GoogleDrive", "Connected to services");
                                }

                                @Override
                                public void onConnectionSuspended(int i) {
                                    Log.i("GoogleDrive", "GoogleApiClient connection suspended");
                                }
                            })
                            .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                                @Override
                                public void onConnectionFailed(ConnectionResult connectionResult) {
                                    if (connectionResult.hasResolution()) {
                                        try {
                                            connectionResult.startResolutionForResult(mSettingsActivity, RESOLVE_CONNECTION_REQUEST_CODE);
                                        } catch (IntentSender.SendIntentException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), mSettingsActivity, 0).show();
                                    }
                                    Log.d("GoogleDrive", connectionResult.toString());
                                }
                            })
                            .build();
                    mGoogleApiClient.connect();
                } else {
                    if (mGoogleApiClient != null) {
                        mGoogleApiClient.disconnect();
                        showToastMessage("Google Drive disconnected");
                    }
                }
                return true;
            }
        };

        Preference.OnPreferenceClickListener onSaveToSdClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                (new AsyncSync(AsyncSync.SAVE_TO_SD,mSettingsActivity)).execute();
                return true;
            }
        };

        Preference.OnPreferenceClickListener onLoadFromSdClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                (new AsyncSync(AsyncSync.LOAD_FROM_SD,mSettingsActivity,null)).execute();
                return true;
            }
        };

        Preference.OnPreferenceClickListener onSaveToDropClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mSharedPreferences.getBoolean(SettingsActivity.DROP_KEY,false))
                    (new AsyncSync(AsyncSync.SAVE_TO_DROPBOX,mSettingsActivity)).execute();
                else showToastMessage("Turn On Dropbox in Settings");
                return true;
            }
        };

        Preference.OnPreferenceClickListener onLoadFromDropClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mSharedPreferences.getBoolean(SettingsActivity.DROP_KEY,false))
                    (new AsyncSync(AsyncSync.LOAD_FROM_DROPBOX,mSettingsActivity,null)).execute();
                else showToastMessage("Turn On Dropbox in Settings");
                return true;
            }
        };

        Preference.OnPreferenceClickListener onSaveToDriveClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mSharedPreferences.getBoolean(SettingsActivity.DRIVE_KEY,false))
                    connectToGoogleApi(true);
                else showToastMessage("Turn On Google Drive in Settings");
                return true;
            }
        };

        Preference.OnPreferenceClickListener onLoadFromDriveClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mSharedPreferences.getBoolean(SettingsActivity.DRIVE_KEY,false))
                    connectToGoogleApi(false);
                else showToastMessage("Turn On Google Drive in Settings");
                return true;
            }
        };

        /**
         * Method to save or load Google Drive backup
         * @param toSave true to save, false to load
         */
        void connectToGoogleApi(final boolean toSave){
            if (mSharedPreferences.getBoolean(SettingsActivity.DRIVE_KEY,false)) {
                mGoogleApiClient = new GoogleApiClient.Builder(mSettingsActivity)
                        .addApi(Drive.API)
                        .addScope(Drive.SCOPE_FILE)
                        .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.d("GoogleDrive", "connected to services");
                                if (toSave) (new AsyncSync(AsyncSync.SAVE_TO_GOOGLE_DRIVE,mSettingsActivity,mGoogleApiClient,null)).execute();
                                else (new AsyncSync(AsyncSync.LOAD_FROM_GOOGLE_DRIVE,mSettingsActivity,mGoogleApiClient,null)).execute();
                            }

                            @Override
                            public void onConnectionSuspended(int i) {

                            }
                        })
                        .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                            @Override
                            public void onConnectionFailed(ConnectionResult connectionResult) {
                                if (connectionResult.hasResolution()) {
                                    try {
                                        connectionResult.startResolutionForResult(mSettingsActivity, RESOLVE_CONNECTION_REQUEST_CODE);
                                    } catch (IntentSender.SendIntentException e) {
                                        showToastMessage("Google Drive connection error");
                                        e.printStackTrace();
                                    }
                                } else {
                                    GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), mSettingsActivity, 0).show();
                                }
                                Log.d("GoogleDrive", connectionResult.toString());
                            }
                        })
                        .build();
                mGoogleApiClient.connect();
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onResume() {
            super.onResume();
            Log.i("FragmentSync", "resumed");

            CheckBoxPreference dropboxPref = (CheckBoxPreference) findPreference(DROP_KEY);
            if (dropboxPref.isChecked()) {
                String accessToken = null;
                if (mSharedPreferences.contains(DROP_TOKEN_KEY)) {
                    accessToken = mSharedPreferences.getString(DROP_TOKEN_KEY, null);
                }
                if (accessToken == null) {
                    accessToken = Auth.getOAuth2Token();
                    if (accessToken != null) {
                        mSharedPreferences.edit().putString(DROP_TOKEN_KEY, accessToken).apply();
                        showToastMessage("DropBox connected");
                        Log.v("token", accessToken);
                    } else {
                        dropboxPref.setChecked(false);
                        showToastMessage("DropBox connection error");
                    }

                }
            }
        }
    }
}
