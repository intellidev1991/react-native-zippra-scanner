package com.reactnativezipprascanner;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.facebook.react.ReactActivity;
import com.zebra.scannercontrol.BarCodeView;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.SDKHandler;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


//import com.reactnativezipprascanner.BarCodeView;
//import com.reactnativezipprascanner.DCSSDKDefs;
//import com.reactnativezipprascanner.DCSScannerInfo;
//import com.reactnativezipprascanner.SDKHandler;

/*import com.reactnativezipprascanner.application.Application;
import com.reactnativezipprascanner.helpers.Constants;*/


public class BarcodeActivity extends ReactActivity {
  private FrameLayout llBarcode;
  //  private NavigationView navigationView;
  Menu menu;
  MenuItem pairNewScannerMenu;
  private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 10;
  private static final int MAX_ALPHANUMERIC_CHARACTERS = 12;
  private static final int MAX_BLUETOOTH_ADDRESS_CHARACTERS = 17;
  private static final String DEFAULT_EMPTY_STRING = "";
  private static final String COLON_CHARACTER = ":";
  public static final String BLUETOOTH_ADDRESS_VALIDATOR = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";
  static boolean firstRun = true;
  Dialog dialog;
  Dialog dialogBTAddress;
  static String btAddress;
  static String userEnteredBluetoothAddress;

//  private GoogleApiClient googleApiClient;
//  DCSSDKDefs.DCSSDK_BT_PROTOCOL selectedProtocol;
//  DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG selectedConfig;

  protected static final int REQUEST_CHECK_SETTINGS = 0x1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_barcode);

    initialize();
  }

  private void initialize() {
    if (ContextCompat.checkSelfPermission(this,
      Manifest.permission.ACCESS_FINE_LOCATION)
      != PackageManager.PERMISSION_GRANTED) {
      // No explanation needed, we can request the permission.
      ActivityCompat.requestPermissions(this,
        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
        ACCESS_FINE_LOCATION_REQUEST_CODE);
    } else {
      initializeDcsSdk();
    }
//    llBarcode = (FrameLayout) findViewById(R.id.scan_to_connect_barcode);
//    setTitle("Pair New Scanner");
//    broadcastSCAisListening();
  }


  private void initializeDcsSdk() {
    ZippraScannerModule.sdkHandler.dcssdkEnableAvailableScannersDetection(true);
//    ZippraScannerModule.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL);
    ZippraScannerModule.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_SNAPI);
    ZippraScannerModule.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_LE);
    ZippraScannerModule.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_USB_CDC);

    llBarcode = (FrameLayout) findViewById(R.id.scan_to_connect_barcode);

    generatePairingBarcode();
  }


  private void generatePairingBarcode() {
    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -1);
    BarCodeView barCodeView = ZippraScannerModule.sdkHandler.dcssdkGetPairingBarcode(DCSSDKDefs.DCSSDK_BT_PROTOCOL.SSI_BT_LE, DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG.SET_FACTORY_DEFAULTS);
    if(barCodeView!=null) {
      updateBarcodeView(layoutParams, barCodeView);
    }else{
      // SDK was not able to determine Bluetooth MAC. So call the dcssdkGetPairingBarcode with BT Address.

//      btAddress= getDeviceBTAddress(settings);
//      if(btAddress.equals("")){
//        llBarcode.removeAllViews();
//      }else {
      ZippraScannerModule.sdkHandler.dcssdkSetBTAddress(btAddress);
        barCodeView = ZippraScannerModule.sdkHandler.dcssdkGetPairingBarcode(DCSSDKDefs.DCSSDK_BT_PROTOCOL.SSI_BT_LE, DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG.SET_FACTORY_DEFAULTS, btAddress);
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
  protected void onResume() {
    super.onResume();
//
//    SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
////    TextView txtBarcodeType = (TextView)findViewById(R.id.scan_to_connect_barcode_type);
////    TextView txtScannerConfiguration = (TextView)findViewById(R.id.scan_to_connect_scanner_config);
//    String sourceString = "";
////    txtBarcodeType.setText(Html.fromHtml(sourceString));
////    txtScannerConfiguration.setText("");
//    boolean dntShowMessage = settings.getBoolean(Constants.PREF_DONT_SHOW_INSTRUCTIONS, false);
//    int barcode = settings.getInt(Constants.PREF_PAIRING_BARCODE_TYPE, 0);
//    boolean setDefaults = settings.getBoolean(Constants.PREF_PAIRING_BARCODE_CONFIG, true);
//    int protocolInt = settings.getInt(Constants.PREF_COMMUNICATION_PROTOCOL_TYPE, 0);
//    String strProtocol = "SSI over Bluetooth LE";
//    llBarcode = (FrameLayout) findViewById(R.id.scan_to_connect_barcode);
//    DCSSDKDefs.DCSSDK_BT_PROTOCOL protocol = DCSSDKDefs.DCSSDK_BT_PROTOCOL.LEGACY_B;
//    DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG config = DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG.KEEP_CURRENT;
//    if(barcode ==0){
//      sourceString = "STC Barcode ";
//      switch (protocolInt){
//        case 0:
//          protocol = DCSSDKDefs.DCSSDK_BT_PROTOCOL.SSI_BT_LE;//SSI over Bluetooth LE
//          strProtocol = "Bluetooth LE";
//          break;
//        case 1:
//          protocol = DCSSDKDefs.DCSSDK_BT_PROTOCOL.SSI_BT_CRADLE_HOST;//SSI over Classic Bluetooth
//          strProtocol = "SSI over Classic Bluetooth";
//          break;
//        default:
//          protocol = DCSSDKDefs.DCSSDK_BT_PROTOCOL.SSI_BT_LE;//SSI over Bluetooth LE
//          break;
//      }
//      if(setDefaults){
//        config = DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG.SET_FACTORY_DEFAULTS;
//      }else{
//      }
//    }else{
//      sourceString = "Legacy Pairing ";
//    }
//    selectedProtocol = protocol;
//    selectedConfig = config;
//    generatePairingBarcode();
//    if(dialogBTAddress == null && firstRun && !dntShowMessage){
//    }
//
//
//    if ((barcode == 0 )
//      && (setDefaults == true)
//      && (protocolInt == 0)) {
//    } else {
//      if(barcode ==0){
//      }else{
//      }
//
//    }

  }
}
