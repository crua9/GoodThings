package com.techreviewsandhelp.goodthings;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.techreviewsandhelp.goodthings.settings.SettingsActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    EnterFragment enterFragment;
    MaterialCalendarFragment materialCalendarFragment;
    AchievFragment achievFragment;
    private final int RESOLVE_CONNECTION_REQUEST_CODE = 111;
    GoogleApiClient mGoogleApiClient;
    public SharedPreferences sharedPreferences;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("lyfecycles", "Main.onStart");

    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        Log.d("lyfecycles", "Main.onResumeFragments");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("lyfecycles", "Main.onCreate");
        long startTime = System.currentTimeMillis();
        super.onCreate(savedInstanceState);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean("firstStart",true)){
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this,R.style.AppTheme_AlertDialog);
            alertBuilder.setView(R.layout.alert_first_start)
                    .setPositiveButton("Open settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                            startActivity(intent);
                            sharedPreferences.edit().putBoolean("firstStart",false).apply();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            sharedPreferences.edit().putBoolean("firstStart",false).apply();
                        }
                    })
                    .show();
        }

        setContentView(R.layout.activity_main);

        //Fragments for each tab
        enterFragment = EnterFragment.newInstance();
        materialCalendarFragment = MaterialCalendarFragment.newInstance();
        achievFragment = AchievFragment.newInstance();

        //Set PagerAdapter pages from Fragments above
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(),
                                                            enterFragment,
                                                            materialCalendarFragment,
                                                            achievFragment);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(3);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 1) {
                    //Refresh calendar markers
                    MaterialCalendarView materialCalendarView = materialCalendarFragment.materialCalendarView;
                    GoodThingsTableHelper.refreshCalendarMarkers(getApplicationContext(), materialCalendarView);
                    GoodThingsTableHelper.refreshAchievements(getApplicationContext(), materialCalendarFragment);
                }
                if (position == 2) {
                    Log.d("lyfecycles", "Main.pageChanged");
                    //GoodThingsTableHelper.refreshAchievements(getApplicationContext(), achievFragment);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        GoodThingsTableHelper.getDbInstance(this);
        AlarmReceiver.setAlarm(getApplicationContext());
        Log.v("loadingTime", Long.toString(System.currentTimeMillis() - startTime));


    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                Toast.makeText(this,"Google Drive connection error",Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
        Log.d("GoogleDrive", connectionResult.toString());
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("GoogleDrive", "connected to services");
        (new AsyncSync(AsyncSync.SAVE_TO_GOOGLE_DRIVE,this,mGoogleApiClient,materialCalendarFragment)).execute();
        }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("GoogleDrive", "GoogleApiClient connection suspended");
    }

    public void connectGoogleApi(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        Log.d("lyfecycles", "Main.onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("lyfecycles", "Main.onRestoreInstanceState");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("lyfecycles", "Main.onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("lyfecycles", "Main.onStop");
    }

    @Override
    protected void onDestroy() {
        GoodThingsTableHelper.closeDB();
        super.onDestroy();
        Log.d("lyfecycles", "Main.onDestroy");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("lyfecycles", "Main.onResume");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this,SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                    Toast.makeText(this,"Google Drive Connected",Toast.LENGTH_SHORT).show();
                }
                break;
            default:{
                if (resultCode == RESULT_OK){
                    mViewPager = (ViewPager) findViewById(R.id.container);
                    mViewPager.setCurrentItem(0);

                    EditText editThings = (EditText)findViewById(R.id.editThings);
                    EditText editDeals = (EditText)findViewById(R.id.editDeals);
                    EditText editBetter = (EditText)findViewById(R.id.editBetter);

                    enterFragment.setNoteId(data.getStringExtra("_id"));
                    enterFragment.setNoteDate(data.getStringExtra("date"));
                    editThings.setText(data.getStringExtra("field1"));
                    editDeals.setText(data.getStringExtra("field2"));
                    editBetter.setText(data.getStringExtra("field3"));

                    Log.d("MainActivity","Get noteId:"+data.getStringExtra("_id"));
                }
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private Fragment tab0;
        private Fragment tab1;
        private Fragment tab2;
        public SectionsPagerAdapter(FragmentManager fm, Fragment tab0, Fragment tab1, Fragment tab2) {
            super(fm);
            this.tab0 = tab0;
            this.tab1 = tab1;
            this.tab2 = tab2;
        }

        @Override
        public Fragment getItem(int position) {
            Log.d("lyfecycles","Main.Pager.getItem");
            // getItem is called to instantiate the fragment for the given page.
            switch (position)
            {
                case 0:
                    return tab0;//EnterFragment.newInstance();
                case 1:
                    return tab1;//MaterialCalendarFragmentNewInstance();
                case 2:
                    return tab2; //AchievFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show tabs count.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Add good things";
                case 1:
                    return "My Good Calendar";
                case 2:
                    return "Achievements";
            }
            return null;
        }
    }
}
