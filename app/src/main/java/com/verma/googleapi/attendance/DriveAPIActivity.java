/*
package com.verma.googleapi.attendance;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.verma.googleapi.attendance.R;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class DriveAPIActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener{

    private static final String TAG = "Google Drive Activity";
    private static final int REQUEST_CODE_RESOLUTION = 1;
    private static final  int REQUEST_CODE_OPENER = 2;
    private GoogleApiClient mGoogleApiClient;
    private boolean fileOperation = false;
    private DriveId mFileId;
    public DriveFile file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    */
/**
     * Called when the activity will start interacting with the user.
     * At this point your activity is at the top of the activity stack,
     * with user input going to it.
     *//*


    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient == null) {

            */
/**
             * Create the API client and bind it to an instance variable.
             * We use this instance as the callback for connection and connection failures.
             * Since no account name is passed, the user is prompted to choose.
             *//*

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {

            // disconnect Google API client connection
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());

        if (!result.hasResolution()) {

            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }

        */
/**
         *  The failure has a resolution. Resolve it.
         *  Called typically when the app is not yet authorized, and an  authorization
         *  dialog is displayed to the user.
         *//*


        try {

            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);

        } catch (SendIntentException e) {

            Log.e(TAG, &quot;Exception while starting resolution activity&quot;, e);
        }
    }

    */
/**
     * It invoked when Google API client connected
     * @param connectionHint
     *//*

    @Override
    public void onConnected(Bundle connectionHint) {

        Toast.makeText(getApplicationContext(), &quot;Connected&quot;, Toast.LENGTH_LONG).show();
    }

    */
/**
     * It invoked when connection suspend
     * @param cause
     *//*

    @Override
    public void onConnectionSuspended(int cause) {

        Log.i(TAG, &quot;GoogleApiClient connection suspended&quot;);
    }

    public void onClickCreateFile(View view){
        fileOperation = true;

        // create new contents resource
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(driveContentsCallback);

    }

    public void onClickOpenFile(View view){
        fileOperation = false;

        // create new contents resource
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(driveContentsCallback);
    }

    */
/**
     *  Open list of folder and file of the Google Drive
     *//*

    public void OpenFileFromGoogleDrive(){

        IntentSender intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[] { &quot;text/plain&quot;, &quot;text/html&quot; })
                .build(mGoogleApiClient);
        try {
            startIntentSenderForResult(

                    intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);

        } catch (SendIntentException e) {

            Log.w(TAG, &quot;Unable to send intent&quot;, e);
        }

    }

    */
/**
     * This is Result result handler of Drive contents.
     * this callback method call CreateFileOnGoogleDrive() method
     * and also call OpenFileFromGoogleDrive() method,
     * send intent onActivityResult() method to handle result.
     *//*

    final ResultCallback&lt;DriveContentsResult&gt; driveContentsCallback =
            new ResultCallback&lt;DriveContentsResult&gt;() {
        @Override
        public void onResult(DriveContentsResult result) {

            if (result.getStatus().isSuccess()) {

                if (fileOperation == true) {

                    CreateFileOnGoogleDrive(result);

                } else {

                    OpenFileFromGoogleDrive();

                }
            }

        }
    };

    */
/**
     * Create a file in root folder using MetadataChangeSet object.
     * @param result
     *//*

    public void CreateFileOnGoogleDrive(DriveContentsResult result){

        final DriveContents driveContents = result.getDriveContents();

        // Perform I/O off the UI thread.
        new Thread() {
            @Override
            public void run() {
                // write content to DriveContents
                OutputStream outputStream = driveContents.getOutputStream();
                Writer writer = new OutputStreamWriter(outputStream);
                try {
                    writer.write(&quot;Hello abhay!&quot;);
                    writer.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }

                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(&quot;abhaytest2&quot;)
                        .setMimeType(&quot;text/plain&quot;)
                        .setStarred(true).build();

                // create a file in root folder
                Drive.DriveApi.getRootFolder(mGoogleApiClient)
                        .createFile(mGoogleApiClient, changeSet, driveContents)
                        .setResultCallback(fileCallback);
            }
        }.start();
    }

    */
/**
     * Handle result of Created file
     *//*

    final private ResultCallback&lt;DriveFolder.DriveFileResult&gt; fileCallback = new
    ResultCallback&lt;DriveFolder.DriveFileResult&gt;() {
        @Override
        public void onResult(DriveFolder.DriveFileResult result) {
            if (result.getStatus().isSuccess()) {

                Toast.makeText(getApplicationContext(), &quot;file created: &quot;+&quot;&quot;+
                        result.getDriveFile().getDriveId(), Toast.LENGTH_LONG).show();

            }

            return;

        }
    };

    */
/**
     *  Handle Response of selected file
     * @param requestCode
     * @param resultCode
     * @param data
     *//*

    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent data) {
        switch (requestCode) {

            case REQUEST_CODE_OPENER:

                if (resultCode == RESULT_OK) {

                    mFileId = (DriveId) data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);

                    Log.e(&quot;file id&quot;, mFileId.getResourceId() + &quot;&quot;);

                    String url = &quot;https://drive.google.com/open?id=&quot;+ mFileId.getResourceId();
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }

                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

}*/
