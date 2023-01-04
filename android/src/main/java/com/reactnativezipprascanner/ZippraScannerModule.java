package com.reactnativezipprascanner;
import android.Manifest;

import android.app.Activity;
import com.reactnativezipprascanner.application.Application;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.module.annotations.ReactModule;
import com.reactnativezipprascanner.helpers.Constants;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.SDKHandler;

@ReactModule(name = ZippraScannerModule.NAME)
public class ZippraScannerModule extends ReactContextBaseJavaModule {
  public static final String NAME = "ZippraScanner";
  public static ReactContext MainContext;
  private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 10;

  public static String message = "";
  public static String okText = "";

  public ZippraScannerModule(ReactApplicationContext reactContext) {
    super(reactContext);

    MainContext = reactContext;

    if (Application.sdkHandler == null) {
      Application.sdkHandler = new SDKHandler(reactContext, true);
    }

  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }


  @ReactMethod
  public void findCabledScanner() {
    Activity activity = getCurrentActivity();
    if (activity != null) {
      Intent intent = new Intent(activity, FindCabledScanner.class);
      activity.startActivity(intent);
    }
  }

  @ReactMethod
  public void findBluetoothScanner(String bluetoothAddress) {
    Activity activity = getCurrentActivity();
    if (activity != null) {
      Intent intent = new Intent(activity, FindBluetoothScanner.class);
      intent.putExtra("BluetoothAddress", bluetoothAddress);
      activity.startActivity(intent);
    }
  }

  @ReactMethod
  public void requestAccess(final Promise promise) {
    if (ContextCompat.checkSelfPermission(MainContext,  Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
      // No explanation needed, we can request the permission.
      try {
        ActivityCompat.requestPermissions(MainContext.getCurrentActivity(),
          new String[]{Manifest.permission.BLUETOOTH_CONNECT},
          ACCESS_FINE_LOCATION_REQUEST_CODE);

        promise.resolve("success");
      } catch (Exception error) {
        promise.resolve("error");
      }
    } else {
      promise.resolve("success");
    }
  }

  @ReactMethod
  private void setupApi() {
    initializeDcsSdk();
  }

  public void showMessageBox(String message) {
    Toast.makeText(MainContext,
      message, Toast.LENGTH_LONG).show();
  }

  @ReactMethod
  public void getActiveScannersList(final Promise promise) {
    WritableMap resultData = new WritableNativeMap();
    resultData.putString("data", Application.sdkHandler.dcssdkGetActiveScannersList().toString());
    promise.resolve(resultData);
  }

  private void initializeDcsSdk() {
    Application.sdkHandler.dcssdkEnableAvailableScannersDetection(true);
    Application.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL);
    Application.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_SNAPI);
    Application.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_LE);
    Application.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_USB_CDC);

    IntentFilter filter = new IntentFilter(Constants.ACTION_SCANNER_CONNECTED);
    filter.addAction(Constants.ACTION_SCANNER_DISCONNECTED);
    filter.addAction(Constants.ACTION_SCANNER_AVAILABLE);
    filter.addAction(Constants.ACTION_SCANNER_CONN_FAILED);

    filter.setPriority(2);

    MainContext.registerReceiver(onNotification, filter);

    broadcastSCAisListening();

  }

  private void broadcastSCAisListening() {
    Intent intent = new Intent();
    intent.setAction("com.zebra.scannercontrol.LISTENING_STARTED");
    MainContext.sendBroadcast(intent);
  }

  private BroadcastReceiver onNotification = new BroadcastReceiver() {
    public void onReceive(Context ctxt, Intent i) {

      //Since the application is in foreground, show a dialog.
      Toast.makeText(ctxt, i.getStringExtra(Constants.NOTIFICATIONS_TEXT), Toast.LENGTH_SHORT).show();

      //Abort the broadcast since it has been handled.
      abortBroadcast();
    }
  };


  @ReactMethod
  public void setTitles(String _message, String _ok) {
    message = _message;
    okText = _ok;
  }

}
