package com.techreviewsandhelp.goodthings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.DbxFiles;
import com.dropbox.core.v2.DbxUsers;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AsyncSync extends AsyncTask<Void, Void, Void> {
    public static final int SAVE_TO_SD = 0;
    public static final int LOAD_FROM_SD = 1;
    public static final int SAVE_TO_DROPBOX = 2;
    public static final int LOAD_FROM_DROPBOX = 3;
    public static final int SAVE_TO_GOOGLE_DRIVE = 4;
    public static final int LOAD_FROM_GOOGLE_DRIVE = 5;
    final String dbPath = "/data/data/" + getClass().getPackage().getName() + "/databases/good_things.db";
    final String backupFileName = "good_things.backup";
    ProgressDialog progressDialog;

    Context mContext;
    Activity mActivity;
    MaterialCalendarView materialCalendarView;          //need to refresh after backup restored
    MaterialCalendarFragment materialCalendarFragment;  //need to refresh after backup restored
    AchievFragment achievFragment;                      //need to refresh after backup restored
    int mAction;
    boolean result;
    boolean fileNotFoundResult;
    GoogleApiClient mGoogleApiClient;

    /**
     * Ctor for saving to SD and Dropbox
     * @param action
     * @param context
     */
    public AsyncSync(int action, Context context) {
        mContext = context;
        mAction = action;
    }

    /**
     * Ctor for loading from SD and DropBox. Main Thread paused by ProgressDialog, to prevent database using while loading/
     * @param action const for action
     * @param context context
     * @param materialCalendarFragment uses to refresh calendar markers and coins quantity
     */
    public AsyncSync(int action, Context context, MaterialCalendarFragment materialCalendarFragment) {
        mContext = context;
        this.materialCalendarFragment = materialCalendarFragment;
        if (materialCalendarFragment!=null) this.materialCalendarView = materialCalendarFragment.materialCalendarView;
        mAction = action;
    }

    /**
     * Ctor for Google Drive
     * @param action const for action
     * @param activity uses for progress dialog
     * @param googleApiClient uses to connect to Google API
     * @param materialCalendarFragment uses to refresh calendar markers and coins quantity
     */
    public AsyncSync(int action, Activity activity, GoogleApiClient googleApiClient,MaterialCalendarFragment materialCalendarFragment){
        mActivity = activity;
        mAction = action;
        mGoogleApiClient = googleApiClient;
        mContext = mActivity.getApplicationContext();
        this.materialCalendarFragment = materialCalendarFragment;
        try {
            this.materialCalendarView = materialCalendarFragment.materialCalendarView;
        }
        catch (NullPointerException e){
            Log.w("AsyncSync","materialCalendarView is null");
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        boolean online = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTING) {
            online = true;
        }

        if (mAction == LOAD_FROM_SD || ((mAction == LOAD_FROM_DROPBOX || mAction == LOAD_FROM_GOOGLE_DRIVE) && online)) {
            progressDialog = new ProgressDialog(mContext);
            if (mAction == LOAD_FROM_GOOGLE_DRIVE)
                progressDialog = new ProgressDialog(mActivity);
            GoodThingsTableHelper.closeDB();
            progressDialog.setMessage("Please wait, while DB is loading...");
            progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (!GoodThingsTableHelper.isDbOpen())
                        GoodThingsTableHelper.getDbInstance(mContext);
                    if (materialCalendarFragment != null) {
                        GoodThingsTableHelper.refreshCalendarMarkers(mContext, materialCalendarView);
                        GoodThingsTableHelper.refreshAchievements(mContext, materialCalendarFragment);
                    }
                }
            });
            progressDialog.show();
        }
        else if (mAction == LOAD_FROM_DROPBOX || mAction == LOAD_FROM_GOOGLE_DRIVE){
            Toast.makeText(mContext, "You are offline", Toast.LENGTH_SHORT).show();
            this.cancel(true);
        }
        if (online) {
            switch (mAction) {
                case SAVE_TO_DROPBOX:
                case LOAD_FROM_DROPBOX: {
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
                    if (pref.getString("drop_tok", null) == null)
                        Toast.makeText(mContext, "Turn on DropBox in Settings", Toast.LENGTH_LONG).show();
                    break;
                }
                case SAVE_TO_GOOGLE_DRIVE:
                case LOAD_FROM_GOOGLE_DRIVE: {
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
                    if (!pref.getBoolean("google_drive_key", false))
                        Toast.makeText(mContext, "Turn on Google Drive in Settings", Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
        else if (mAction == SAVE_TO_DROPBOX || mAction == LOAD_FROM_DROPBOX || mAction == SAVE_TO_GOOGLE_DRIVE || mAction == LOAD_FROM_DROPBOX){
            Toast.makeText(mContext, "Backup not saved. You are offline.", Toast.LENGTH_SHORT).show();
            this.cancel(true);
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        switch (mAction) {
            case SAVE_TO_SD:
            case LOAD_FROM_SD: {
                result = doSdCardBackup(mAction);
                break;
            }
            case SAVE_TO_DROPBOX:
            case LOAD_FROM_DROPBOX:{
                result = doDropBoxBackup(mAction);
                break;
            }
            case SAVE_TO_GOOGLE_DRIVE:{
                result = doDriveSaveBackup();
                break;
            }
            case LOAD_FROM_GOOGLE_DRIVE:{
                result = doDriveLoadBackup();
                break;
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (result) {
            switch (mAction) {
                case SAVE_TO_SD: {
                    Toast.makeText(mContext, "The database successfully saved to SD card", Toast.LENGTH_SHORT).show();
                    break;
                }
                case LOAD_FROM_SD: {
                    progressDialog.dismiss();
                    Toast.makeText(mContext, "The database successfully loaded from SD card", Toast.LENGTH_SHORT).show();
                    break;
                }
                case SAVE_TO_DROPBOX:{
                    Toast.makeText(mContext, "The database successfully saved to DropBox", Toast.LENGTH_SHORT).show();
                    break;
                }
                case LOAD_FROM_DROPBOX:{
                    progressDialog.dismiss();
                    Toast.makeText(mContext, "The database successfully loaded from DropBox", Toast.LENGTH_SHORT).show();
                    break;
                }
                case SAVE_TO_GOOGLE_DRIVE:{
                    Toast.makeText(mContext, "The database successfully saved to Google Drive", Toast.LENGTH_SHORT).show();
                    break;
                }
                case LOAD_FROM_GOOGLE_DRIVE:{
                    progressDialog.dismiss();
                    Toast.makeText(mContext, "The database successfully loaded from Google Drive", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
        else if (fileNotFoundResult) Toast.makeText(mContext, "Backup file not found", Toast.LENGTH_SHORT).show();
        else Toast.makeText(mContext, "Backup Error", Toast.LENGTH_SHORT).show();
        if (progressDialog!=null) progressDialog.cancel();
    }

    boolean doDriveLoadBackup(){
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, backupFileName))
                .build();

        DriveApi.MetadataBufferResult getRootFolderResult = Drive.DriveApi.getRootFolder(mGoogleApiClient).queryChildren(mGoogleApiClient, query).await();
        if (!getRootFolderResult.getStatus().isSuccess()){
            Log.d("GoogleDrive","Error to get root folder" + getRootFolderResult.toString());
            return false;
        }
        MetadataBuffer metadataBuffer = getRootFolderResult.getMetadataBuffer();
        if (metadataBuffer.getCount() > 0) {
            DriveFile driveFile = getRootFolderResult.getMetadataBuffer().get(0).getDriveId().asDriveFile();
            Log.d("GoogleDrive", "File opening");
            DriveApi.DriveContentsResult openFileResult = driveFile.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await();

            if (!openFileResult.getStatus().isSuccess()) {
                Log.d("GoogleDrive", "Open file error " + openFileResult.toString());
                return false;
            }

            Date modifiedDate = metadataBuffer.get(0).getModifiedDate();
            Log.d("GoogleDrive","Modified date: "+modifiedDate);

            // DriveContents object contains pointers
            // to the actual byte stream
            DriveContents contents = openFileResult.getDriveContents();
            InputStream inputStream = contents.getInputStream();
            OutputStream outputStream;
            try {
                outputStream = new FileOutputStream(dbPath);
            } catch (IOException e){
                e.printStackTrace();
                return false;
            }

            if (!writeToFrom(inputStream, outputStream)) return false;

            contents.discard(mGoogleApiClient);
            metadataBuffer.release();
            return true;
        } else {
            metadataBuffer.release();
            fileNotFoundResult = true;
            Log.d("GoogleDrive", "File not found");
            return false;
        }
    }

    boolean doDriveSaveBackup(){
        DriveApi.DriveContentsResult openFileResult;

        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, backupFileName))
                .build();

        DriveApi.MetadataBufferResult queryResult = Drive.DriveApi.getRootFolder(mGoogleApiClient).queryChildren(mGoogleApiClient, query).await();
        if (!queryResult.getStatus().isSuccess()){
            Log.d("GoogleDrive", "Error to get root folder" + queryResult.toString());
            return false;
        }
        MetadataBuffer metadataBuffer = queryResult.getMetadataBuffer();
        //If file exists
        if (metadataBuffer.getCount() > 0) {
            DriveFile driveFile = queryResult.getMetadataBuffer().get(0).getDriveId().asDriveFile();
            Log.d("GoogleDrive", "File opening");
            openFileResult = driveFile.open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).await();
        }
        //If file not exists
        else {
            DriveApi.DriveContentsResult driveContentsResult = Drive.DriveApi.newDriveContents(mGoogleApiClient).await();
            if (!driveContentsResult.getStatus().isSuccess()) {
                Log.d("GoogleDrive", "Error while trying to create new file contents " + driveContentsResult.toString());
                return false;
            }

            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(backupFileName)
                    .setMimeType("application/x-sqlite3")
                    .build();

            DriveFolder.DriveFileResult driveFileResult = Drive.DriveApi.getRootFolder(mGoogleApiClient).createFile(mGoogleApiClient, changeSet, driveContentsResult.getDriveContents()).await();
            if (!driveFileResult.getStatus().isSuccess()) {
                Log.d("GoogleDrive", "Error while trying to create the file " + driveFileResult.toString());
                return false;
            }

            Log.d("GoogleDrive", "Created a file in App Folder: "
                    + driveFileResult.getDriveFile().getDriveId());
            DriveFile driveFile = driveFileResult.getDriveFile();
            openFileResult = driveFile.open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).await();
        }
        if (!openFileResult.getStatus().isSuccess()) {
            Log.d("GoogleDrive", "Open file error " + openFileResult.toString());
            return false;
        }

        DriveContents openedContent = openFileResult.getDriveContents();
        OutputStream outputStream = openedContent.getOutputStream();
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(dbPath);
        } catch (FileNotFoundException e){
            Log.e("GoogleDrive","DB file not found");
            return false;
        }
        if (!writeToFrom(inputStream, outputStream)) return false;

        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                .setLastViewedByMeDate(new Date())
                .build();

        com.google.android.gms.common.api.Status status = openedContent.commit(mGoogleApiClient, metadataChangeSet).await();
        if (status.isSuccess()) {
            Log.d("GoogleDrive", "File Saved");
            return true;
        }
        Log.d("GoogleDrive", status.toString());

        return false;
    }

    private boolean writeToFrom(InputStream inputStream, OutputStream outputStream) {
        try {
            // Transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            Log.d("writeToFrom", "File saved to stream");

            // Close the streams
            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    boolean doSdCardBackup(int action) {
        String inFileName;
        String outFileName;

        if (action == SAVE_TO_SD) {
            inFileName = dbPath;
            outFileName = Environment.getExternalStorageDirectory() + "/" + backupFileName;
        } else {
            inFileName = Environment.getExternalStorageDirectory() + "/" + backupFileName;
            outFileName = dbPath;
        }

        InputStream inputStream;
        OutputStream outputStream;
        try{
            inputStream = new FileInputStream(inFileName);
            outputStream = new FileOutputStream(outFileName);
        } catch (FileNotFoundException e){
            Log.e("SaveToSD","DB file not found");
            fileNotFoundResult = true;
            return false;
        }
        return writeToFrom(inputStream,outputStream);
    }


    boolean doDropBoxBackup(int action) {

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            if (pref.getBoolean("dropbox_key", false)) {
                String token = pref.getString("drop_tok", null);
                DbxRequestConfig config = new DbxRequestConfig("My Good Day", Locale.getDefault().toString());
                DbxClientV2 client;
                if (token == null) return false;
                client = new DbxClientV2(config, token);

                try {
                    DbxUsers.FullAccount fullAccount = client.users.getCurrentAccount();
                    Log.v("DropAccount", fullAccount.toString());

                    try {
                        if (action == SAVE_TO_DROPBOX) {
                            InputStream inputFileStream = new FileInputStream(dbPath);
                            DbxFiles.Metadata metadata = client.files.uploadBuilder("/"+backupFileName).mode(DbxFiles.WriteMode.overwrite()).run(inputFileStream);
                        }
                        else{
                            OutputStream outputStream = new FileOutputStream(dbPath);
                            DbxFiles.DownloadBuilder downloadBuilder = client.files.downloadBuilder("/"+backupFileName);
                            DbxFiles.Metadata metadata = downloadBuilder.run(outputStream);
                            String metadataString = metadata.toJson(false);
                            String clientModified;
                            try {
                                JSONObject metaJson = new JSONObject(metadataString);
                                clientModified = metaJson.getString("client_modified");
                                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                                Date modifiedDate = new Date();
                                try {
                                    modifiedDate = formatter.parse(clientModified);
                                }catch (ParseException e){
                                    e.printStackTrace();
                                }
                                Log.d("JSON", modifiedDate.toString());
                            }catch (JSONException e){
                                Log.e("JSON error", e.getMessage());
                            }
                            Log.d("DropBox",metadata.toJson(false));
                        }
                        return true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (DbxException e) {
                    e.printStackTrace();
                    if (e.getMessage().contains("not_found")) fileNotFoundResult = true;
                }
            }

        return false;
    }
}

