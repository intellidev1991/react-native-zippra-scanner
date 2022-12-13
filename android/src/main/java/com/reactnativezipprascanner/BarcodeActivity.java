package com.reactnativezipprascanner;

import com.reactnativezipprascanner.application.Application;
import com.reactnativezipprascanner.helpers.Constants;
import com.reactnativezipprascanner.helpers.ScannerAppEngine;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.FirmwareUpdateEvent;
import com.zebra.scannercontrol.IDcsSdkApiDelegate;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.facebook.react.ReactActivity;
import com.zebra.scannercontrol.BarCodeView;
import com.zebra.scannercontrol.DCSSDKDefs;
import android.app.Activity;


import java.util.List;

import static com.reactnativezipprascanner.application.Application.virtualTetherEventOccurred;
import static com.reactnativezipprascanner.application.Application.virtualTetherHostActivated;

public class BarcodeActivity extends BaseActivity implements ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate {
  private FrameLayout llBarcode;
  private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 10;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_barcode);

    initialize();

    if (ContextCompat.checkSelfPermission(this,
      Manifest.permission.BLUETOOTH_CONNECT)
      != PackageManager.PERMISSION_GRANTED) {
      // No explanation needed, we can request the permission.
      ActivityCompat.requestPermissions(this,
        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
        ACCESS_FINE_LOCATION_REQUEST_CODE);
    }else{
//      initialize();
    }
    final Button button = (Button) findViewById(R.id.button3);
    button.setOnClickListener(new Button.OnClickListener() {
      public void onClick(View v) {
        // your handler code here
        Activity activity = ZippraScannerModule.MainContext.getCurrentActivity();
        Intent intent = new Intent(activity, ActiveSanner.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ZippraScannerModule.MainContext.startActivity(intent);
      }
    });
  }

 void openActiveScanner() {
   Activity activity = ZippraScannerModule.MainContext.getCurrentActivity();
   Intent intent = new Intent(activity, ActiveSanner.class);
   intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   ZippraScannerModule.MainContext.startActivity(intent);
  }


  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String permissions[], @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case ACCESS_FINE_LOCATION_REQUEST_CODE: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
          && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // permission was granted, yay!
          initialize();

        } else {
//          finish();
          // permission denied, boo! Disable the
          // functionality that depends on this permission.
        }
        return;
      }
    }
  }

  private void initialize() {
    initializeDcsSdk();
  }



  private void initializeDcsSdk() {
    if (Application.sdkHandler != null){
      Application.sdkHandler.dcssdkEnableAvailableScannersDetection(true);
      Application.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL);
      Application.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_SNAPI);
      Application.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_LE);
      Application.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_USB_CDC);

      llBarcode = (FrameLayout) findViewById(R.id.scan_to_connect_barcode);

      generatePairingBarcode();
    }
  }


  private void generatePairingBarcode() {

    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -1);
    BarCodeView barCodeView = Application.sdkHandler.dcssdkGetPairingBarcode(DCSSDKDefs.DCSSDK_BT_PROTOCOL.SSI_BT_CRADLE_HOST, DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG.SET_FACTORY_DEFAULTS);
//    if(barCodeView!=null) {
//      updateBarcodeView(layoutParams, barCodeView);
//    }else{
      Log.i("Log:", "IN");
      // SDK was not able to determine Bluetooth MAC. So call the dcssdkGetPairingBarcode with BT Address.

//      btAddress= getDeviceBTAddress(settings);
//      if(btAddress.equals("")){
//        llBarcode.removeAllViews();
//      }else {
    String btAddress = "A4:C7:4B:3B:38:F5";

//    showMessageBox(btAddress);

    Application.sdkHandler.dcssdkSetBTAddress(btAddress);
        barCodeView = Application.sdkHandler.dcssdkGetPairingBarcode(DCSSDKDefs.DCSSDK_BT_PROTOCOL.SSI_BT_CRADLE_HOST, DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG.SET_FACTORY_DEFAULTS, btAddress);
          if (barCodeView != null) {
            updateBarcodeView(layoutParams, barCodeView);
          }
//      }
//    }
  }


  private void updateBarcodeView(LinearLayout.LayoutParams layoutParams, BarCodeView barCodeView) {
    Display display = getWindowManager().getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);
    int width = size.x;
    int height = size.y;

    int orientation =this.getResources().getConfiguration().orientation;
    int x = width * 9 / 10;
    int y = x / 3;
    if(getDeviceScreenSize()>6){ // TODO: Check 6 is ok or not
      if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        x =  width /2;
        y = x/3;
      }else {
        x =  width *2/3;
        y = x/3;
      }
    }
    barCodeView.setSize(x, y);
    llBarcode.addView(barCodeView, layoutParams);
  }

  private double getDeviceScreenSize() {
    double screenInches = 0;
    WindowManager windowManager = getWindowManager();
    Display display = windowManager.getDefaultDisplay();

    int mWidthPixels;
    int mHeightPixels;

    try {
      Point realSize = new Point();
      Display.class.getMethod("getRealSize", Point.class).invoke(display, realSize);
      mWidthPixels = realSize.x;
      mHeightPixels = realSize.y;
      DisplayMetrics dm = new DisplayMetrics();
      getWindowManager().getDefaultDisplay().getMetrics(dm);
      double x = Math.pow(mWidthPixels / dm.xdpi, 2);
      double y = Math.pow(mHeightPixels / dm.ydpi, 2);
      screenInches = Math.sqrt(x + y);
    } catch (Exception ignored) {
    }
    return screenInches;
  }


  public void showMessageBox(String message) {
    AlertDialog alertDialog = new AlertDialog.Builder(this)
      .setTitle("Success")
      .setMessage(message)
      .show();
  }

  public void resetVirtualTetherHostConfigurations() {
    if (!virtualTetherEventOccurred || !virtualTetherHostActivated) {
      SharedPreferences.Editor settingsEditor = getSharedPreferences(Constants.PREFS_NAME, 0).edit();
      settingsEditor.putBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_FEEDBACK, false).apply();
      settingsEditor.putBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_VIBRATION_ALARM, false).apply();
      settingsEditor.putBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_AUDIO_ALARM, false).apply();
      settingsEditor.putBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_SCREEN_FLASH, false).apply();
      settingsEditor.putBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_POPUP_MESSAGE, false).apply();
      settingsEditor.putBoolean(Constants.PREF_VIRTUAL_TETHER_SCANNER_SETTINGS, false).apply();
    }
  }


  @Override
  public boolean scannerHasAppeared(int scannerID) {
    return false;
  }

  @Override
  public boolean scannerHasDisappeared(int scannerID) {
    return false;
  }

  @Override
  public boolean scannerHasConnected(int scannerID) {
    showMessageBox("scannerHasConnected : " + String.valueOf(scannerID));
    return true;
  }

  @Override
  public boolean scannerHasDisconnected(int scannerID) {
    return false;
  }
}
