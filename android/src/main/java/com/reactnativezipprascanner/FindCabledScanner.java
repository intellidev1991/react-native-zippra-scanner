package com.reactnativezipprascanner;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.reactnativezipprascanner.application.Application;
import com.reactnativezipprascanner.helpers.ScannerAppEngine;
import com.zebra.scannercontrol.BarCodeView;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.FirmwareUpdateEvent;
import com.zebra.scannercontrol.IDcsSdkApiDelegate;
import com.reactnativezipprascanner.helpers.CustomProgressDialog;

import java.util.ArrayList;

public class FindCabledScanner extends BaseActivity implements ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate {
  private FrameLayout llBarcode;
  private static ArrayList<DCSScannerInfo> mSNAPIList=new ArrayList<DCSScannerInfo>();
  static MyAsyncTask cmdExecTask=null;
  private static CustomProgressDialog progressDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_cabled_scanner);

    mSNAPIList.clear();
    updateScannersList();
    for(DCSScannerInfo device:getActualScannersList()){
      if(device.getConnectionType() == DCSSDKDefs.DCSSDK_CONN_TYPES.DCSSDK_CONNTYPE_USB_SNAPI){
        mSNAPIList.add(device);
      }
    }

    llBarcode = (FrameLayout) findViewById(R.id.scan_to_connect_barcode);
    if(mSNAPIList.isEmpty()){
      // No SNAPI Scanners
      getSnapiBarcode();
      Toast.makeText(this, "Please scan the barcode to connect", Toast.LENGTH_SHORT).show();
    }else if(mSNAPIList.size() >1){
      // Multiple SNAPI scanners. Show the dialog and navigate to available scanner list.
      Toast.makeText(this, "1", Toast.LENGTH_SHORT).show();

    }else {
      // Only one SNAPI scanner available
      if(mSNAPIList.get(0).isActive()){
        // Available scanner is active. Navigate to active scanner
        finish();
        sendEvent("SCANNER_ESTABLISHED", "Connected");
        Toast.makeText(this, "Scanner Connected", Toast.LENGTH_SHORT).show();

      }else{
        // Try to connect available scanner
        cmdExecTask=new MyAsyncTask(mSNAPIList.get(0));
        cmdExecTask.execute();
        Toast.makeText(this, "Try to connect available scanner", Toast.LENGTH_SHORT).show();
      }
    }
  }


  private class MyAsyncTask extends AsyncTask<Void,DCSScannerInfo,Boolean> {
    private DCSScannerInfo  scanner;
    public MyAsyncTask(DCSScannerInfo scn){
      this.scanner=scn;
    }
    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      if(!isFinishing()) {
        progressDialog = new CustomProgressDialog(FindCabledScanner.this, "Connecting To scanner. Please Wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();
      }
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
      DCSSDKDefs.DCSSDK_RESULT result =connect(scanner.getScannerID());
      if(result== DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS){
        finish();
        return true;
      }
      else {
        return false;
      }
    }

    @Override
    protected void onPostExecute(Boolean b) {
      super.onPostExecute(b);
      if(!isFinishing()) {
        if (progressDialog != null && progressDialog.isShowing())
          progressDialog.dismiss();

      }
      Intent returnIntent = new Intent();
      if (!b) {
        setResult(RESULT_CANCELED, returnIntent);
        Toast.makeText(getApplicationContext(), "Unable to communicate with scanner", Toast.LENGTH_SHORT).show();
        getSnapiBarcode();
      }

    }
  }

  private void getSnapiBarcode() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -1);
        BarCodeView barCodeView = Application.sdkHandler.dcssdkGetUSBSNAPIWithImagingBarcode();
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
        if(barCodeView != null) {
          llBarcode.addView(barCodeView, layoutParams);
        }
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
            double x = Math.pow(mWidthPixels/dm.xdpi,2);
            double y = Math.pow(mHeightPixels/dm.ydpi,2);
            screenInches = Math.sqrt(x+y);
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
