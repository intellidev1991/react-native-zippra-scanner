package com.reactnativezipprascanner;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

public class BarCodeCustomView extends SimpleViewManager {
  public static final String REACT_CLASS = "BarCodeView";
  private LinearLayout llBarcode;
  private ReactApplicationContext themedReactContext;

  public BarCodeCustomView(ReactApplicationContext reactContext) {
  }

  @NonNull
  @Override
  public String getName() {
    return REACT_CLASS;
  }

 @NonNull
 @Override
 protected View createViewInstance(@NonNull ThemedReactContext themedReactContext) {
   final View view = new View(themedReactContext);
   return view;
 }

}
