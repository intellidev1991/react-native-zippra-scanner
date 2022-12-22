package com.reactnativezipprascanner;

import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.reactnativezipprascanner.application.Application;
import com.reactnativezipprascanner.helpers.ScannerAppEngine;
import com.zebra.scannercontrol.BarCodeView;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;

import java.util.ArrayList;

public class FindBluetoothScanner extends BaseActivity implements ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate {
  private FrameLayout llBarcode;
  private static ArrayList<DCSScannerInfo> mSNAPIList=new ArrayList<DCSScannerInfo>();
  static String btAddress = "A4:C7:4B:3B:38:F5";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_barcode);

    llBarcode = (FrameLayout) findViewById(R.id.scan_to_connect_barcode);

    generatePairingBarcode();
  }


  private void generatePairingBarcode() {
    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -1);
    BarCodeView barCodeView = Application.sdkHandler.dcssdkGetPairingBarcode(DCSSDKDefs.DCSSDK_BT_PROTOCOL.SSI_BT_CRADLE_HOST, DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG.SET_FACTORY_DEFAULTS);
    if(barCodeView!=null) {
      updateBarcodeView(layoutParams, barCodeView);
    }else{
      // SDK was not able to determine Bluetooth MAC. So call the dcssdkGetPairingBarcode with BT Address.

//      btAddress= getDeviceBTAddress(settings);
//      if(btAddress.equals("")){
//        llBarcode.removeAllViews();
//      }else {
      Application.sdkHandler.dcssdkSetBTAddress(btAddress);
      barCodeView = Application.sdkHandler.dcssdkGetPairingBarcode(DCSSDKDefs.DCSSDK_BT_PROTOCOL.SSI_BT_CRADLE_HOST, DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG.SET_FACTORY_DEFAULTS, btAddress);
      if (barCodeView != null) {
        updateBarcodeView(layoutParams, barCodeView);
      }
//      }
    }
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



  @Override
  public void showMessageBox(String message) {
    Toast.makeText(this,
      message, Toast.LENGTH_LONG).show();
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
    return true;
  }

  @Override
  public boolean scannerHasDisconnected(int scannerID) {
    return false;
  }

}
