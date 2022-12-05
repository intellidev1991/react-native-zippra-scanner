package com.reactnativezipprascanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import com.zebra.scannercontrol.SDKHandler;

//import com.reactnativezipprascanner.BarCodeView;
//import com.reactnativezipprascanner.DCSSDKDefs;
//import com.reactnativezipprascanner.SDKHandler;
//import com.reactnativezipprascanner.app.helpers.ScannerAppEngine;
//import com.reactnativezipprascanner.app.helpers.Barcode;
//import com.reactnativezipprascanner.app.helpers.Foreground;


@ReactModule(name = ZippraScannerModule.NAME)
public class ZippraScannerModule extends ReactContextBaseJavaModule {
  public static final String NAME = "ZippraScanner";
  public  static SDKHandler sdkHandler;
  public ReactContext MainContext;

  public ZippraScannerModule(ReactApplicationContext reactContext) {
    super(reactContext);

    sdkHandler = new SDKHandler(reactContext, true);
    MainContext = reactContext;
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void openBarcodeActivity() {
    Activity activity = getCurrentActivity();
    if (activity != null) {
      Intent intent = new Intent(activity, BarcodeActivity.class);
      activity.startActivity(intent);
    }
  }


}
