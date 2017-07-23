package com.verma.googleapi.attendance;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        EasyPermissions.PermissionCallbacks, View.OnClickListener {

    GoogleAccountCredential mCredential;

    ProgressDialog mProgress;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static final int REQUEST_ACCOUNT_PICKER_CHANGE = 1004;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {SheetsScopes.SPREADSHEETS};
    private SharedPreferences preferences, preferencesSheet;
    private SharedPreferences.Editor editor, editorSheet;
    private Button submitSheetIdButton, scanButton, backButton, btnDatePicker;
    private EditText sheetId;
    private String spreadsheetId;
    private Toolbar toolbar;
    private int mYear, mMonth, mDay;
    private TextView dateLabel;
    Intent intent;
    private static final int OPEN_REQUEST_CODE = 41;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setPreferences();
        setElements();
        setDrawerLayout();
        btnDatePicker.setOnClickListener(this);


        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Sheets API ...");

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        spreadSheetIdPref();
    }

    /**
     * Sets the initial preferences values
     */
    private void setPreferences() {
        preferences = getSharedPreferences("AttendanceList", MODE_PRIVATE);
        editor = preferences.edit();

        preferencesSheet = getSharedPreferences("SheetIdList", MODE_PRIVATE);
        editorSheet = preferencesSheet.edit();
    }

    /**
     * Initiate the elements here
     */
    private void setElements() {
        submitSheetIdButton = (Button) findViewById(R.id.submitSheetID);
        scanButton = (Button) findViewById(R.id.scanAttendance);
        sheetId = (EditText) findViewById(R.id.sheetID);
        backButton = (Button) findViewById(R.id.back);
        btnDatePicker = (Button) findViewById(R.id.datePicker);
        dateLabel = (TextView) findViewById(R.id.textView3);

        submitSheetIdButton.setVisibility(View.GONE);
        sheetId.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
        btnDatePicker.setVisibility(View.GONE);
        dateLabel.setVisibility(View.GONE);


    }

    /**
     * Drawer Layout settings.
     */
    private void setDrawerLayout() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    @Override
    public void onClick(View v) {

        if (v == btnDatePicker) {

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    new DatePickerDialog.OnDateSetListener() {

                        @Override
                        public void onDateSet(DatePicker view, int year,
                                              int monthOfYear, int dayOfMonth) {
                            mYear = year;
                            mMonth = monthOfYear;
                            mDay = dayOfMonth;
                            updateDisplay();

                        }
                    }, mYear, mMonth, mDay);
            datePickerDialog.show();
        }

    }

    private void updateDisplay() {
        dateLabel.setText(
                new StringBuilder()
                        .append("Submit attendance for date : ")
                        .append(mDay).append("/")
                        .append(mMonth + 1).append("/")
                        .append(mYear).append(" "));
    }


    private void setCurrentDate() {
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        updateDisplay();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_upload) {
            boolean flag = false;
            Map<String, ?> allEntries = preferences.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                flag = true;
                break;
            }
            if (flag) {
                getResultsFromApi();

            } else {
                Toast.makeText(this, "No attendance taken. Please scan " +
                        "attendance first.", Toast.LENGTH_SHORT).show();
            }

        } else if (id == R.id.nav_sheet_id) {
            sheetId.setVisibility(View.VISIBLE);
            submitSheetIdButton.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.VISIBLE);
            btnDatePicker.setVisibility(View.VISIBLE);
            dateLabel.setVisibility(View.VISIBLE);
            scanButton.setVisibility(View.GONE);
            setCurrentDate();


        } else if (id == R.id.nav_open_file) {
            openFile();
            //signOut();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    /**
     * Sign Out
     */
    private void signOut() {
        mCredential.setSelectedAccountName("");
        startActivityForResult(
                mCredential.newChooseAccountIntent(),
                REQUEST_ACCOUNT_PICKER_CHANGE);

    }


    /**
     * Row number
     */
    private String getRow(String rollNumber) {
        int r = Integer.parseInt(rollNumber) - 100 + 2;
        return r + "";
    }

    /**
     * Scanner Activity
     *
     * @param view
     */
    public void scan(View view) {
        Intent i = new Intent(this, ScannerActivity.class);
        startActivity(i);
    }

    /**
     * Marks Attendance Manually
     *
     * @param view
     */
    public void markAttendance(View view) {
        String attendance = sheetId.getText().toString();
        if (!attendance.equals("") && attendance != null) {
            if (Integer.parseInt(attendance) < 101 || Integer.parseInt(attendance) > 250) {
                Toast.makeText(this, "ID does not exist",
                        Toast.LENGTH_SHORT).show();
            } else {
                editor.putString(attendance + "_" + mDay + "_" + mMonth, "Manually Added");
                editor.commit();
                sheetId.getText().clear();
                Toast.makeText(this, "Attendance saved.",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please enter the valid ID",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void backToActivity(View view) {
        scanButton.setVisibility(View.VISIBLE);
        submitSheetIdButton.setVisibility(View.GONE);
        sheetId.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
        btnDatePicker.setVisibility(View.GONE);
        dateLabel.setVisibility(View.GONE);
    }

    private void spreadSheetIdPref() {
        editorSheet.putString("6", "19cdP8Aqd-xW0cGtvHAswOIkwjeNXGIjYNnyeOhYuOKg");
        editorSheet.putString("7", "1iFz0zx5_VLKokhqmQ61GHKWJJaAYffjt5WRTaFzRx04");
        editorSheet.putString("8", "1hTKtaDG5F7VaZ1NdTSz2TLI5TpculHYOHzGX4DvWZUI");
        editorSheet.commit();
    }


    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            Toast.makeText(this, "No network connection available.", Toast.LENGTH_SHORT).show();
        } else {
            new MainActivity.MakeRequestTask(mCredential, this).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this, "This app requires Google Play Services. Please install" +
                                    "Google Play Services on your device and relaunch this app.",
                            Toast.LENGTH_SHORT).show();

                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;

            case REQUEST_ACCOUNT_PICKER_CHANGE:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                    }
                }
                break;


            case OPEN_REQUEST_CODE:


                if (data != null) {
                    Uri currentUri = data.getData();

                    try {
                        String content =
                                readFileContent(currentUri);

                    } catch (IOException e) {
                        // Handle error here
                    }
                }
                break;

        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);

    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Google Sheets API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;
        private Context context;
        private boolean updateSuccessful = false;
        private boolean sheetIdExhausted = false;

        MakeRequestTask(GoogleAccountCredential credential, Context context) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            this.context = context;
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Attendance")
                    .build();
        }

        /**
         * Background task to call Google Sheets API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return updateAttendance();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of names and majors of students in a sample spreadsheet:
         * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
         *
         * @return List of names and majors
         * @throws IOException
         */
        private List<String> updateAttendance() throws IOException {
            HashMap<String, List<ValueRange>> dataSet = new HashMap<>();
            List<String> results = new ArrayList<String>();
            List<ValueRange> data = null;
            String column;
            String row;
            String cellRange;
            List<List<Object>> values = Arrays.asList(
                    Arrays.asList(
                            (Object) "P"
                    )
            );

            Map<String, ?> allEntries = preferences.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String[] key = entry.getKey().split("_");
                row = getRow(key[0]);
                column = excelColumnName(Integer.parseInt(key[1]) + 2);
                cellRange = column + row + ":" + column + row;

                String month_temp = key[2];
                if (dataSet.get(month_temp) != null) {
                    data = dataSet.get(month_temp);
                    data.add(new ValueRange()
                            .setRange(cellRange)
                            .setValues(values));
                    dataSet.put(month_temp, data);
                } else {
                    data = new ArrayList<ValueRange>();
                    data.add(new ValueRange()
                            .setRange(cellRange)
                            .setValues(values));
                    dataSet.put(month_temp, data);
                }


            }

            Iterator it = dataSet.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                spreadsheetId = preferencesSheet.getString((String) pair.getKey(), null);
                if (spreadsheetId == null) {
                    sheetIdExhausted = true;
                    return null;
                }
                else {
                    data = (List<ValueRange>) pair.getValue();
                    BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
                            .setValueInputOption("RAW")
                            .setData(data);
                    BatchUpdateValuesResponse result =
                            mService.spreadsheets().values().batchUpdate(spreadsheetId, body).execute();
                    it.remove(); // avoids a ConcurrentModificationException
                }
            }


            preferences.edit().clear().commit();
            updateSuccessful = true;
            return results;

        }

        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (updateSuccessful) {
                Toast.makeText(context, "Successfully updated to Google Sheets.", Toast.LENGTH_SHORT).show();
            } else if (sheetIdExhausted) {
                Toast.makeText(context, "SheetIDs exhausted. Create new Sheet.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to update to Google Sheets.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    Toast.makeText(context, "The following error occurred:\n"
                            + mLastError.getMessage(), Toast.LENGTH_SHORT).show();

                }
            } else {
                Toast.makeText(context, "Request cancelled.", Toast.LENGTH_SHORT).show();

            }
        }

        // Function to return Excel column name for a given column number
        private String excelColumnName(int colNum) {
            int Base = 26;
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            String colName = "";

            while (colNum > 0) {
                int position = colNum % Base;
                colName = (position == 0 ? 'Z' : chars.charAt(position > 0 ? position - 1 : 0)) + colName;
                colNum = (colNum - 1) / Base;
            }
            return colName;
        }

    }

    public void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, OPEN_REQUEST_CODE);
    }

    private String readFileContent(Uri uri) throws IOException {

        InputStream inputStream =
                getContentResolver().openInputStream(uri);
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(
                        inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String currentline;
        editor.clear().commit();
        while ((currentline = reader.readLine()) != null) {
            List<String> items = Arrays.asList(currentline.split("\\s*,\\s*"));
            for (String item : items) {
                String input[] = item.split("_");
                String id = input[0];
                if (Integer.parseInt(id) >= 101 && Integer.parseInt(id) < 250)
                    editor.putString(item, "Read from File");
                editor.commit();
            }
            stringBuilder.append(currentline + "\n");
        }
        inputStream.close();
        return stringBuilder.toString();
    }
}
