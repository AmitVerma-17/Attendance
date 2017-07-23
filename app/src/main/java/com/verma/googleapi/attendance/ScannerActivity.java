package com.verma.googleapi.attendance;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.zxing.Result;

import java.util.Calendar;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler{

    private MediaPlayer mp ;
    private ZXingScannerView zXingScannerView;
    static final int CAMERA = 0x5;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        preferences= getSharedPreferences("AttendanceList",MODE_PRIVATE);
        mp = MediaPlayer.create(ScannerActivity.this, R.raw.beep);
        zXingScannerView =new ZXingScannerView(getApplicationContext());
        scan();
    }
    public void scan( ){
        if(hasPermissions()){
            openScanner();
        }
        else{
            requestPerms();
        }
    }

    private void openScanner(){
        setContentView(zXingScannerView);
        zXingScannerView.setResultHandler(this);
        zXingScannerView.startCamera();
    }

    @Override
    public void handleResult(Result result) {

        editor= preferences.edit();

        //QR code result will be in the format (roll_number name) separated by space as delimiter

        String input[]=result.getText().split("\\s+");

        //input[0]=roll_Number ; input[1]=name
        //key shall be roll_Number and the value will be stored as the corresponding name
        String date = Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        String month = Integer.toString(Calendar.getInstance().get(Calendar.MONTH) + 1);
        String key = input[0] + "_" + date + "_" + month;
        String value = input[1];
        editor.putString(key,value);
        editor.commit();
        mp.start();
        zXingScannerView.resumeCameraPreview(this);

    }
    @Override
    protected void onPause() {
        super.onPause();
        zXingScannerView.stopCamera();
    }

    private boolean hasPermissions(){
        int res=0;

        String[] permission = new String[]{Manifest.permission.CAMERA};

        for(String perms:permission){
            res = checkCallingOrSelfPermission(perms);
            if(!(res== PackageManager.PERMISSION_GRANTED)){
                return false;
            }
        }
        return  true;
    }

    private void requestPerms(){
        String[] permissions = new String[]{Manifest.permission.CAMERA};
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            requestPermissions(permissions,CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        boolean allowed=true;

        switch(requestCode){

            case CAMERA:

                for(int res : grantResults){

                    allowed = allowed && (res == PackageManager.PERMISSION_GRANTED);
                }

                break;
            default:
                allowed=false;
                break;
        }

        if(allowed){
            openScanner();
        }
        else{
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA));
                Toast.makeText(this,"Camera permission denied.",Toast.LENGTH_SHORT).show();
            }
        }
    }


}
