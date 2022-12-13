package com.reactnativezipprascanner;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import com.reactnativezipprascanner.application.Application;
import com.zebra.scannercontrol.SDKHandler;


@ReactModule(name = ZippraScannerModule.NAME)
public class ZippraScannerModule extends ReactContextBaseJavaModule {
  public static final String NAME = "ZippraScanner";
  public static ReactContext MainContext;


  public ZippraScannerModule(ReactApplicationContext reactContext) {
    super(reactContext);

    MainContext = reactContext;
    Application.sdkHandler = new SDKHandler(MainContext, true);
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
