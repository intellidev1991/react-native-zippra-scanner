package com.reactnativezipprascanner;

import static com.zebra.scannercontrol.DCSSDKDefs.DCSSDK_CONN_TYPES.DCSSDK_CONNTYPE_BT_NORMAL;
import static com.reactnativezipprascanner.application.Application.virtualTetherEventOccurred;
import static com.reactnativezipprascanner.application.Application.virtualTetherHostActivated;
import static com.reactnativezipprascanner.helpers.Constants.DEBUG_TYPE.TYPE_DEBUG;
import static com.reactnativezipprascanner.helpers.Constants.VIRTUAL_TETHER_AUDIO_ALARM_PATTERN;
import static com.reactnativezipprascanner.helpers.Constants.VIRTUAL_TETHER_EVENT_NOTIFY;
import static com.reactnativezipprascanner.helpers.Constants.VIRTUAL_TETHER_HOST_NOTIFICATION_CHANNEL_ID;
import static com.reactnativezipprascanner.helpers.Constants.logAsMessage;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.FirmwareUpdateEvent;
import com.zebra.scannercontrol.IDcsSdkApiDelegate;
import com.zebra.scannercontrol.SDKHandler;
import com.reactnativezipprascanner.application.Application;
import com.reactnativezipprascanner.helpers.BackgroundSoundService;
import com.reactnativezipprascanner.helpers.Barcode;
import com.reactnativezipprascanner.helpers.Constants;
import com.reactnativezipprascanner.helpers.Foreground;
import com.reactnativezipprascanner.helpers.ManagedVibrator;
import com.reactnativezipprascanner.helpers.ScannerAppEngine;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BaseActivity extends AppCompatActivity implements ScannerAppEngine, IDcsSdkApiDelegate {
  protected static String TAG;
  protected static DCSSDKDefs.DCSSDK_BT_PROTOCOL selectedProtocol;
  protected static DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG selectedConfig;
  private static ArrayList<IScannerAppEngineDevConnectionsDelegate> mDevConnDelegates = new ArrayList<IScannerAppEngineDevConnectionsDelegate>();
  private static ArrayList<DCSScannerInfo> mScannerInfoList;
  private static ArrayList<DCSScannerInfo> mOfflineScannerInfoList;
  public static int lastConnectedScannerID = 0;
  private static ArrayList<IScannerAppEngineDevEventsDelegate> mDevEventsDelegates = new ArrayList<IScannerAppEngineDevEventsDelegate>();

  ManagedVibrator vibrator;
  // The Handler that gets information back from the BluetoothChatService
  protected final Handler mHandler = initializeHandler();
  static boolean waitingForFWReboot = false;
  boolean virtualTetherEnable = false;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
//    setContentView(R.layout.activity_main);

    Configuration configuration = getResources().getConfiguration();
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      if (configuration.smallestScreenWidthDp < Application.minScreenWidth) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      }
    } else {
      if (configuration.screenWidthDp < Application.minScreenWidth) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      }
    }
    mScannerInfoList = Application.mScannerInfoList;
    mOfflineScannerInfoList = new ArrayList<DCSScannerInfo>();
    TAG = getClass().getSimpleName();
    if (Application.sdkHandler == null) {
      Application.sdkHandler = new SDKHandler(this, true);
    }
    virtualTetherEventOccurred = getIntent().getBooleanExtra(VIRTUAL_TETHER_EVENT_NOTIFY, false);
    Application.sdkHandler.dcssdkSetDelegate(this);
    initializeDcsSdkWithAppSettings();

    SharedPreferences virtualTetherSettings = getSharedPreferences(Constants.PREFS_NAME, 0);
    virtualTetherEnable =  virtualTetherSettings.getBoolean(Constants.PREF_VIRTUAL_TETHER_SCANNER_SETTINGS, false);
  }

  private Handler initializeHandler() {
    if (Application.globalMsgHandler != null)
      return Application.globalMsgHandler;
    return null;
  }


  @Override
  protected void onResume() {
    super.onResume();
    Application.sdkHandler.dcssdkSetDelegate(this);
//        Register a dynamic receiver to handle the various RFID Reader Events when the app is in foreground
//        Actions to be handled should be registered here
    IntentFilter filter = new IntentFilter(Constants.ACTION_SCANNER_CONNECTED);
    filter.addAction(Constants.ACTION_SCANNER_DISCONNECTED);
    filter.addAction(Constants.ACTION_SCANNER_AVAILABLE);
    filter.addAction(Constants.ACTION_SCANNER_CONN_FAILED);
//
////        Use a positive priority
    filter.setPriority(2);
    registerReceiver(onNotification, filter);
    TAG = getClass().getSimpleName();
  }

  @Override
  protected void onPause() {
    super.onPause();
    unregisterReceiver(onNotification);
  }

  @Override
  public void initializeDcsSdkWithAppSettings() {
    // Restore preferences
    SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
    vibrator = new ManagedVibrator(getApplicationContext());
    Application.MOT_SETTING_OPMODE = settings.getInt(Constants.PREF_OPMODE, DCSSDK_CONNTYPE_BT_NORMAL.value);

    Application.MOT_SETTING_SCANNER_DETECTION = settings.getBoolean(Constants.PREF_SCANNER_DETECTION, true);
    Application.MOT_SETTING_EVENT_IMAGE = settings.getBoolean(Constants.PREF_EVENT_IMAGE, true);
    Application.MOT_SETTING_EVENT_VIDEO = settings.getBoolean(Constants.PREF_EVENT_VIDEO, true);
    Application.MOT_SETTING_EVENT_BINARY_DATA = settings.getBoolean(Constants.PREF_EVENT_BINARY_DATA, true);

    Application.MOT_SETTING_EVENT_ACTIVE = settings.getBoolean(Constants.PREF_EVENT_ACTIVE, true);
    Application.MOT_SETTING_EVENT_AVAILABLE = settings.getBoolean(Constants.PREF_EVENT_AVAILABLE, true);
    Application.MOT_SETTING_EVENT_BARCODE = settings.getBoolean(Constants.PREF_EVENT_BARCODE, true);

    Application.MOT_SETTING_NOTIFICATION_AVAILABLE = settings.getBoolean(Constants.PREF_NOTIFY_AVAILABLE, false);
    Application.MOT_SETTING_NOTIFICATION_ACTIVE = settings.getBoolean(Constants.PREF_NOTIFY_ACTIVE, false);
    Application.MOT_SETTING_NOTIFICATION_BARCODE = settings.getBoolean(Constants.PREF_NOTIFY_BARCODE, false);

    Application.MOT_SETTING_NOTIFICATION_IMAGE = settings.getBoolean(Constants.PREF_NOTIFY_IMAGE, false);
    Application.MOT_SETTING_NOTIFICATION_VIDEO = settings.getBoolean(Constants.PREF_NOTIFY_VIDEO, false);
    Application.MOT_SETTING_NOTIFICATION_BINARY_DATA = settings.getBoolean(Constants.PREF_NOTIFY_BINARY_DATA, false);



    int notifications_mask = 0;
    if (Application.MOT_SETTING_EVENT_AVAILABLE) {
      notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value);
    }
    if (Application.MOT_SETTING_EVENT_ACTIVE) {
      notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value);
    }
    if (Application.MOT_SETTING_EVENT_BARCODE) {
      notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value);
    }
    if (Application.MOT_SETTING_EVENT_IMAGE) {
      notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_IMAGE.value);
    }
    if (Application.MOT_SETTING_EVENT_VIDEO) {
      notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_VIDEO.value);
    }
    if (Application.MOT_SETTING_EVENT_BINARY_DATA) {
      notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BINARY_DATA.value);
    }
    Application.sdkHandler.dcssdkSubsribeForEvents(notifications_mask);
  }

  void setWaitingForFWReboot(boolean _waitingForFWReboot) {
    waitingForFWReboot = _waitingForFWReboot;
    Log.i("*************", "setWaitingForFWReboot " + String.valueOf(waitingForFWReboot));
  }

  /* ###################################################################### */
  /* ########## Utility functions ######################################### */
  /* ###################################################################### */
  @Override
  public void showMessageBox(String message) {
    Toast.makeText(this,
      message, Toast.LENGTH_LONG).show();
  }

  @Override
  public int showBackgroundNotification(String text) {
    //TODO - Handle the callback from SDK Handler
    return 0;
  }

  @Override
  public int dismissBackgroundNotifications() {
    //TODO - Handle the callback from SDK Handler
    return 0;
  }

  /**
   * Checks if the application is being sent in the background (i.e behind
   * another application's Activity).
   *
   * @param context the context
   * @return <code>true</code> if another application will be above this one.
   */

  @Override
  public boolean isInBackgroundMode(final Context context) {
    return Foreground.get().isBackground();
  }

  /* ###################################################################### */
  /* ########## API calls for UI View Controllers ######################### */
  /* ###################################################################### */
  @Override
  public void addDevListDelegate(IScannerAppEngineDevListDelegate delegate) {
    if (Application.mDevListDelegates == null)
      Application.mDevListDelegates = new ArrayList<IScannerAppEngineDevListDelegate>();
    Application.mDevListDelegates.add(delegate);
  }

  @Override
  public void addDevConnectionsDelegate(IScannerAppEngineDevConnectionsDelegate delegate) {
    if (mDevConnDelegates == null)
      mDevConnDelegates = new ArrayList<IScannerAppEngineDevConnectionsDelegate>();
    mDevConnDelegates.add(delegate);
  }

  @Override
  public void addDevEventsDelegate(IScannerAppEngineDevEventsDelegate delegate) {
    if (mDevEventsDelegates == null)
      mDevEventsDelegates = new ArrayList<IScannerAppEngineDevEventsDelegate>();
    mDevEventsDelegates.add(delegate);
  }

  @Override
  public void removeDevListDelegate(IScannerAppEngineDevListDelegate delegate) {
    if (Application.mDevListDelegates != null)
      Application.mDevListDelegates.remove(delegate);
  }

  @Override
  public void removeDevConnectiosDelegate(IScannerAppEngineDevConnectionsDelegate delegate) {
    if (mDevConnDelegates != null)
      mDevConnDelegates.remove(delegate);
  }

  @Override
  public void removeDevEventsDelegate(ScannerAppEngine.IScannerAppEngineDevEventsDelegate delegate) {
    if (mDevEventsDelegates != null)
      mDevEventsDelegates.remove(delegate);
  }

  @Override
  public List<DCSScannerInfo> getActualScannersList() {
    return mScannerInfoList;
  }

  @Override
  public DCSScannerInfo getScannerInfoByIdx(int dev_index) {
    if (mScannerInfoList != null)
      return mScannerInfoList.get(dev_index);
    else
      return null;
  }

  @Override
  public DCSScannerInfo getScannerByID(int scannerId) {
    if (mScannerInfoList != null) {
      for (DCSScannerInfo scannerInfo : mScannerInfoList) {
        if (scannerInfo != null && scannerInfo.getScannerID() == scannerId)
          return scannerInfo;
      }
    }
    return null;
  }

  @Override
  public void raiseDeviceNotificationsIfNeeded() {

  }

  /* ###################################################################### */
  /* ########## Interface for DCS SDK ##################################### */
  /* ###################################################################### */
  @Override
  public void updateScannersList() {
    if (Application.sdkHandler != null) {
      mScannerInfoList.clear();
      ArrayList<DCSScannerInfo> scannerTreeList = new ArrayList<DCSScannerInfo>();
      Application.sdkHandler.dcssdkGetAvailableScannersList(scannerTreeList);
      Application.sdkHandler.dcssdkGetActiveScannersList(scannerTreeList);
      createFlatScannerList(scannerTreeList);
    }
  }
  //
  private void createFlatScannerList(ArrayList<DCSScannerInfo> scannerTreeList) {
    for (DCSScannerInfo s :
      scannerTreeList) {
      addToScannerList(s);
    }
  }

  private void addToScannerList(DCSScannerInfo s) {
    mScannerInfoList.add(s);
    if (s.getAuxiliaryScanners() != null) {
      for (DCSScannerInfo aux :
        s.getAuxiliaryScanners().values()) {
        addToScannerList(aux);
      }
    }
  }


  @Override
  public DCSSDKDefs.DCSSDK_RESULT connect(int scannerId) {
    if (Application.sdkHandler != null) {
      Application.setScannerDisconnectedIntention(false);
      resetVirtualTetherHostConfigurations();
//            if (ScannersActivity.curAvailableScanner != null) {
//                Application.sdkHandler.dcssdkTerminateCommunicationSession(ScannersActivity.curAvailableScanner.getScannerId());
//
//            }
      return Application.sdkHandler.dcssdkEstablishCommunicationSession(scannerId);
    } else {
      return DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE;
    }
  }

  @Override
  public void disconnect(int scannerId) {
    if (Application.sdkHandler != null) {
      Application.setScannerDisconnectedIntention(true);
      Application.setAnyScannerConnectedStatus(false);
      DCSSDKDefs.DCSSDK_RESULT ret = Application.sdkHandler.dcssdkTerminateCommunicationSession(scannerId);
//            ScannersActivity.curAvailableScanner = null;
      updateScannersList();
    }
  }

  @Override
  public DCSSDKDefs.DCSSDK_RESULT setAutoReconnectOption(int scannerId, boolean enable) {
    DCSSDKDefs.DCSSDK_RESULT ret;
    if (Application.sdkHandler != null) {
      ret = Application.sdkHandler.dcssdkEnableAutomaticSessionReestablishment(enable, scannerId);
      return ret;
    }
    return DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE;
  }

  @Override
  public void enableScannersDetection(boolean enable) {
    if (Application.sdkHandler != null) {
      Application.sdkHandler.dcssdkEnableAvailableScannersDetection(enable);
    }
  }

  @Override
  public void enableBluetoothScannerDiscovery(boolean enable) {
    if (Application.sdkHandler != null) {
      Application.sdkHandler.dcssdkEnableBluetoothScannersDiscovery(enable);
    }
  }

  @Override
  public void configureNotificationAvailable(boolean enable) {
    if (Application.sdkHandler != null) {
      if (enable) {
        Application.sdkHandler.dcssdkSubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value |
          DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value);
      } else {
        Application.sdkHandler.dcssdkUnsubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value |
          DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value);
      }
    }
  }

  @Override
  public void configureNotificationActive(boolean enable) {
    if (Application.sdkHandler != null) {
      if (enable) {
        Application.sdkHandler.dcssdkSubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value |
          DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value);
      } else {
        Application.sdkHandler.dcssdkUnsubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value |
          DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value);
      }
    }
  }

  @Override
  public void configureNotificationBarcode(boolean enable) {
    if (Application.sdkHandler != null) {
      if (enable) {
        Application.sdkHandler.dcssdkSubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value);
      } else {
        Application.sdkHandler.dcssdkUnsubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value);
      }
    }
  }

  @Override
  public void configureNotificationImage(boolean enable) {
    if (Application.sdkHandler != null) {
      if (enable) {
        Application.sdkHandler.dcssdkSubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_IMAGE.value);
      } else {
        Application.sdkHandler.dcssdkUnsubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_IMAGE.value);
      }
    }
  }

  @Override
  public void configureNotificationVideo(boolean enable) {
    if (Application.sdkHandler != null) {
      if (enable) {
        Application.sdkHandler.dcssdkSubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_VIDEO.value);
      } else {
        Application.sdkHandler.dcssdkUnsubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_VIDEO.value);
      }
    }
  }

  @Override
  public void configureOperationalMode(DCSSDKDefs.DCSSDK_MODE mode) {
    if (Application.sdkHandler != null) {
      Application.sdkHandler.dcssdkSetOperationalMode(mode);
    }
  }

  @Override
  public boolean executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE opCode, String inXML, StringBuilder outXML, int scannerID) {
    if (Application.sdkHandler != null) {
      if (outXML == null) {
        outXML = new StringBuilder();
      }
      DCSSDKDefs.DCSSDK_RESULT result = Application.sdkHandler.dcssdkExecuteCommandOpCodeInXMLForScanner(opCode, inXML, outXML, scannerID);
      if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS)
        return true;
      else if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE)
        return false;
    }
    return false;
  }

  @Override
  public boolean executeSSICommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE opCode, String inXML, StringBuilder outXML, int scannerID) {
    if (Application.sdkHandler != null) {
      if (outXML == null) {
        outXML = new StringBuilder();
      }
      DCSSDKDefs.DCSSDK_RESULT result = Application.sdkHandler.dcssdkExecuteSSICommandOpCodeInXMLForScanner(opCode, inXML, outXML, scannerID);
      if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS)
        return true;
      else if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE)
        return false;
    }
    return false;
  }


  /* ###################################################################### */
  /* ########## IDcsSdkApiDelegate Protocol implementation ################ */
  /* ###################################################################### */
  @Override
  public void dcssdkEventScannerAppeared(DCSScannerInfo availableScanner) {
    dataHandler.obtainMessage(Constants.SCANNER_APPEARED, availableScanner).sendToTarget();
    Application.setScannerDisconnectedIntention(false);
  }

  @Override
  public void dcssdkEventScannerDisappeared(int scannerID) {
    dataHandler.obtainMessage(Constants.SCANNER_DISAPPEARED, scannerID).sendToTarget();

  }

  @Override
  public void dcssdkEventCommunicationSessionEstablished(DCSScannerInfo activeScanner) {
//        showMessageBox("dcssdkEventCommunicationSessionEstablished");
    dataHandler.obtainMessage(Constants.SESSION_ESTABLISHED, activeScanner).sendToTarget();
    resetVirtualTetherHostConfigurations();
    reconnectScannerAfterVirtualTetherHostEvent(activeScanner);
  }

  @Override
  public void dcssdkEventCommunicationSessionTerminated(int scannerID) {
    dataHandler.obtainMessage(Constants.SESSION_TERMINATED, scannerID).sendToTarget();
    virtualTetherHostAlarm(scannerID);

  }

  @Override
  public void dcssdkEventBarcode(byte[] barcodeData, int barcodeType, int fromScannerID) {
        Barcode barcode = new Barcode(barcodeData, barcodeType, fromScannerID);
        dataHandler.obtainMessage(Constants.BARCODE_RECEIVED, barcode).sendToTarget();
  }

  @Override
  public void dcssdkEventFirmwareUpdate(FirmwareUpdateEvent firmwareUpdateEvent) {
    dataHandler.obtainMessage(Constants.FW_UPDATE_EVENT, firmwareUpdateEvent).sendToTarget();
  }

  @Override
  public void dcssdkEventAuxScannerAppeared(DCSScannerInfo newTopology, DCSScannerInfo auxScanner) {
    dataHandler.obtainMessage(Constants.AUX_SCANNER_CONNECTED, auxScanner).sendToTarget();
  }


  @Override
  public void dcssdkEventImage(byte[] imageData, int fromScannerID) {
    dataHandler.obtainMessage(Constants.IMAGE_RECEIVED, imageData).sendToTarget();
  }

  @Override
  public void dcssdkEventVideo(byte[] videoFrame, int fromScannerID) {
    dataHandler.obtainMessage(Constants.VIDEO_RECEIVED, videoFrame).sendToTarget();
  }

  @Override
  public void dcssdkEventBinaryData(byte[] binaryData, int fromScannerID) {
    // todo: implement this
    logAsMessage(TYPE_DEBUG, TAG, "BinaryData Event received no.of bytes : " + binaryData.length + " for Scanner ID : " + fromScannerID);
  }

  /**
   * Method to retrieve barcodes
   *
   * @param scannerId data tobe used to retrieve barcodes
   */
  public ArrayList<Barcode> getBarcodeData(int scannerId) {
    ArrayList<Barcode> barcodes = new ArrayList<Barcode>();
    for (Barcode barcode : Application.barcodeData) {
      if (barcode.getFromScannerID() == scannerId) {
        barcodes.add(barcode);
      }
    }
    return barcodes;
  }

  public void clearBarcodeData() {
    Application.barcodeData.clear();
  }

  /**
   * Handle the events about virtual tether host configurations
   *
   * @param scannerID disappeared scanner id
   */
  public void virtualTetherHostAlarm(int scannerID) {

    boolean firmwareRebootStatus = getIntent().getBooleanExtra(Constants.FW_REBOOT, false);
    if(!Application.getScannerDisconnectedIntention() && Application.currentConnectedScannerID != -1 && !firmwareRebootStatus) {

      SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
      if (settings.getBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_FEEDBACK, false)) {
        boolean startVirtualTetherInForeground = false;


        if (settings.getBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_VIBRATION_ALARM, false)) {
          if (isInBackgroundMode(getApplicationContext())) {
            createNotificationChannel();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
              AudioAttributes attributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();
              vibrator.vibrate(VIRTUAL_TETHER_AUDIO_ALARM_PATTERN, 0, attributes);
            } else {
              vibrator.vibrate(VIRTUAL_TETHER_AUDIO_ALARM_PATTERN, 0);
            }

            showVirtualTetherBackgroundNotification(scannerID);

          }
//                    else if ((ScannersActivity.curAvailableScanner != null) && (scannerID == ScannersActivity.curAvailableScanner.getScannerId())) {
//                        virtualTetherHostActivated = true;
//                        intent.putExtra(Constants.VIRTUAL_TETHER_EVENT_NOTIFY, true);
//
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                            AudioAttributes attributes = new AudioAttributes.Builder()
//                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                                    .setUsage(AudioAttributes.USAGE_ALARM)
//                                    .build();
//                            vibrator.vibrate(VIRTUAL_TETHER_AUDIO_ALARM_PATTERN, 0, attributes);
//                        } else {
//                            vibrator.vibrate(VIRTUAL_TETHER_AUDIO_ALARM_PATTERN, 0);
//                        }
//                        startVirtualTetherInForeground = true;
//                    } else {
//                    }

        }
        if (settings.getBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_AUDIO_ALARM, false)) {
          if (isInBackgroundMode(getApplicationContext())) {
            createNotificationChannel();
            startVirtualTetherAudioAlarm();
            showVirtualTetherBackgroundNotification(scannerID);


          }
//                    else if ((ScannersActivity.curAvailableScanner != null) && (scannerID == ScannersActivity.curAvailableScanner.getScannerId())) {
//                        virtualTetherHostActivated = true;
//                        intent.putExtra(Constants.VIRTUAL_TETHER_EVENT_NOTIFY, true);
//                        startVirtualTetherAudioAlarm();
//                        startVirtualTetherInForeground = true;
//                    } else {
//                    }

        }

        if (settings.getBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_POPUP_MESSAGE, false)) {
          if (isInBackgroundMode(getApplicationContext())) {
            createNotificationChannel();
            showVirtualTetherBackgroundNotification(scannerID);


          }
//                    else if ((ScannersActivity.curAvailableScanner != null) && (scannerID == ScannersActivity.curAvailableScanner.getScannerId())) {
//                        virtualTetherHostActivated = true;
//                        intent.putExtra(Constants.VIRTUAL_TETHER_EVENT_NOTIFY, true);
//                        startVirtualTetherInForeground = true;
//                    } else {
//                    }

        }

        if (settings.getBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_SCREEN_FLASH, false)) {
          if (isInBackgroundMode(getApplicationContext())) {
            createNotificationChannel();
            showVirtualTetherBackgroundNotification(scannerID);


          }
//                    else if ((ScannersActivity.curAvailableScanner != null) && (scannerID == ScannersActivity.curAvailableScanner.getScannerId())) {
//                        virtualTetherHostActivated = true;
//                        intent.putExtra(Constants.VIRTUAL_TETHER_EVENT_NOTIFY, true);
//                        startVirtualTetherInForeground = true;
//                    } else {
//                    }

        }
        if(startVirtualTetherInForeground){
//                    startActivity(intent);
        }

      } else {
//                Intent intent = new Intent(this, HomeActivity.class);
//                if (!isInBackgroundMode(getApplicationContext())) {
//                    startActivity(intent);
//                } else {
//                    startActivity(intent);
//                    Intent startMain = new Intent(Intent.ACTION_MAIN);
//                    startMain.addCategory(Intent.CATEGORY_HOME);
//                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(startMain);
//                }
      }

    }
  }

  /**
   * Method to start background sound service
   */
  private void startVirtualTetherAudioAlarm() {
    Intent musicintent = new Intent(getApplicationContext(), BackgroundSoundService.class);
    startService(musicintent);
  }

  /**
   * Create the NotificationChannel to show notification in background mode
   */
  private void createNotificationChannel() {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            String id = getString(R.string.virtual_tether_notification_channel_id);
//            CharSequence name = getString(R.string.virtual_tether_notification_channel_name);
//            String description = getString(R.string.virtual_tether_notification_channel_description);
//            int importance = NotificationManager.IMPORTANCE_HIGH;
//            NotificationChannel channel = new NotificationChannel(id, name, importance);
//            channel.setDescription(description);
//            NotificationManager notificationManager = getSystemService(NotificationManager.class);
//            notificationManager.createNotificationChannel(channel);
    }
  }

  /**
   * This method is to show notification in background mode
   */
  private void showVirtualTetherBackgroundNotification(int scannerID) {

//        virtualTetherHostActivated = true;
//        String channelId = getString(R.string.virtual_tether_notification_channel_id);
//        // Define resultIntent with the target activity class to be launched when user click the notification.
//        Intent resultIntent;
//        resultIntent = new Intent(this, VirtualTetherSettings.class);
//        resultIntent.putExtra(Constants.VIRTUAL_TETHER_EVENT_NOTIFY, true);
//        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        stackBuilder.addNextIntentWithParentStack(resultIntent);
//        PendingIntent resultPendingIntent =
//                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//        NotificationCompat.Builder NotificationBuilder = new NotificationCompat.Builder(this, channelId)
//                .setSmallIcon(R.drawable.app_icon_small)
//                .setContentText(VIRTUAL_TETHER_HOST_BACKGROUND_MODE_NOTIFICATION);
//        NotificationBuilder.setContentIntent(resultPendingIntent);
//
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
//        notificationManager.notify(VIRTUAL_TETHER_HOST_NOTIFICATION_CHANNEL_ID, NotificationBuilder.build());
  }
  /**
   * This method is responsible for resetting the virtual tether host configurations for the connected scanner
   */
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

  /**
   * This method is responsible for stop virtual tether host alarm for the reconnected event
   */
  public void reconnectScannerAfterVirtualTetherHostEvent(DCSScannerInfo activeScanner) {

    boolean hasVibrator = vibrator.hasVibrator();

    if (virtualTetherEventOccurred || virtualTetherHostActivated) {

      if (hasVibrator) {
        vibrator.cancel();
      }
      stopVirtualTetherAudioAlarm();
      NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
      notificationManager.cancel(VIRTUAL_TETHER_HOST_NOTIFICATION_CHANNEL_ID);
      virtualTetherHostActivated = false;
    }

  }

  /**
   * This method is to stop the audio alarm background service
   */
  private void stopVirtualTetherAudioAlarm() {
    Intent intent = new Intent(getApplicationContext(), BackgroundSoundService.class);
    stopService(intent);
  }

  /**
   * Receiver to handle the events about RFID Reader
   */
  private BroadcastReceiver onNotification = new BroadcastReceiver() {
    public void onReceive(Context ctxt, Intent i) {

      //Since the application is in foreground, show a dialog.
      Toast.makeText(ctxt, i.getStringExtra(Constants.NOTIFICATIONS_TEXT), Toast.LENGTH_SHORT).show();

      //Abort the broadcast since it has been handled.
      abortBroadcast();
    }
  };

//Handler to show the data on UI

  protected Handler dataHandler = new Handler() {
    boolean notificaton_processed = false;
    boolean result = false;
    boolean found = false;


    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case Constants.BARCODE_RECEIVED:
          logAsMessage(TYPE_DEBUG, TAG, "Barcode Received");
          Barcode barcode = (Barcode) msg.obj;
          String s = new String(barcode.getBarcodeData(), StandardCharsets.UTF_8);
          sendEvent("BARCODE_RECEIVED", s);

          break;
        case Constants.SESSION_ESTABLISHED:
          sendEvent("SCANNER_ESTABLISHED", "SDSDSDSDSD");

          DCSScannerInfo activeScanner = (DCSScannerInfo) msg.obj;
          notificaton_processed = false;
          result = false;
          setAutoReconnectOption(activeScanner.getScannerID(), true);
          sendEvent("SCANNER_ESTABLISHED", "SDSDSDSDSD");
          /* notify connections delegates */
          if (mDevConnDelegates != null) {
            for (IScannerAppEngineDevConnectionsDelegate delegate : mDevConnDelegates) {
              if (delegate != null) {
                result = delegate.scannerHasConnected(activeScanner.getScannerID());
                if (result) {
                                /*
                                 DevConnections delegates should NOT display any UI alerts,
                                 so from UI notification side the event is not processed
                                 */
                  notificaton_processed = false;
                }
              }
            }
          }

          /* update dev list */
          found = false;
          if (mScannerInfoList != null) {
            for (DCSScannerInfo ex_info : mScannerInfoList) {
              if (ex_info.getScannerID() == activeScanner.getScannerID()) {
                mScannerInfoList.remove(ex_info);
                Application.barcodeData.clear();
                found = true;
                break;
              }
            }
          }

          if (mOfflineScannerInfoList != null) {
            for (DCSScannerInfo off_info : mOfflineScannerInfoList) {
              if (off_info.getScannerID() == activeScanner.getScannerID()) {
                mOfflineScannerInfoList.remove(off_info);
                break;
              }
            }
          }

          if (mScannerInfoList != null)
            mScannerInfoList.add(activeScanner);

          /* notify dev list delegates */
          if (Application.mDevListDelegates != null) {
            for (IScannerAppEngineDevListDelegate delegate : Application.mDevListDelegates) {
              if (delegate != null) {
                result = delegate.scannersListHasBeenUpdated();
                if (result) {
                                    /*
                                     DeList delegates should NOT display any UI alerts,
                                     so from UI notification side the event is not processed
                                     */
                  notificaton_processed = false;
                  resetVirtualTetherHostConfigurations();
                }
              }
            }
          }

          //TODO - Showing notifications in foreground and background mode

          if (Application.MOT_SETTING_NOTIFICATION_ACTIVE && !notificaton_processed && !virtualTetherEventOccurred) {
            StringBuilder notification_Msg = new StringBuilder();
            if (!found) {
              notification_Msg.append(activeScanner.getScannerName()).append(" has appeared and connected");

            } else {
              notification_Msg.append(activeScanner.getScannerName()).append(" has connected");

            }
            if (isInBackgroundMode(getApplicationContext())) {
              Intent intent = new Intent();
              intent.setAction(Constants.ACTION_SCANNER_CONNECTED);
              intent.putExtra(Constants.NOTIFICATIONS_TEXT, notification_Msg.toString());
              intent.putExtra(Constants.NOTIFICATIONS_TYPE, Constants.SESSION_ESTABLISHED);
              sendOrderedBroadcast(intent, null);
            } else {
              Toast.makeText(getApplicationContext(), notification_Msg.toString(), Toast.LENGTH_SHORT).show();
            }
          }

          break;
        case Constants.SESSION_TERMINATED:
          int scannerID = (Integer) msg.obj;
          String scannerName = "";
          notificaton_processed = false;
          result = false;

          /* notify connections delegates */
          for (IScannerAppEngineDevConnectionsDelegate delegate : mDevConnDelegates) {
            if (delegate != null) {
              result = delegate.scannerHasDisconnected(scannerID);
              if (result) {
                            /*
                             DevConnections delegates should NOT display any UI alerts,
                             so from UI notification side the event is not processed
                             */
                notificaton_processed = false;
              }
            }
          }

          DCSScannerInfo scannerInfo = getScannerByID(scannerID);
          mOfflineScannerInfoList.add(scannerInfo);
          if (scannerInfo != null) {
            scannerName = scannerInfo.getScannerName();
//                        ScannersActivity.curAvailableScanner = null;
          }
          updateScannersList();

          /* notify dev list delegates */
          for (IScannerAppEngineDevListDelegate delegate : Application.mDevListDelegates) {
            if (delegate != null) {
              result = delegate.scannersListHasBeenUpdated();
              if (result) {
                                /*
                                 DeList delegates should NOT display any UI alerts,
                                 so from UI notification side the event is not processed
                                 */
                notificaton_processed = false;
              }
            }
          }

          if (Application.MOT_SETTING_NOTIFICATION_ACTIVE && !notificaton_processed && !virtualTetherEnable  && !virtualTetherEventOccurred) {
            if (isInBackgroundMode(getApplicationContext())) {
              Intent intent = new Intent();
              intent.setAction(Constants.ACTION_SCANNER_DISCONNECTED);
              intent.putExtra(Constants.NOTIFICATIONS_TEXT, scannerName + " has disconnected");
              intent.putExtra(Constants.NOTIFICATIONS_TYPE, Constants.SESSION_TERMINATED);
              sendOrderedBroadcast(intent, null);
            } else {
              Toast.makeText(getApplicationContext(), scannerName + " has disconnected", Toast.LENGTH_SHORT).show();
            }
          }
          break;
        case Constants.SCANNER_APPEARED:
        case Constants.AUX_SCANNER_CONNECTED:
          notificaton_processed = false;
          result = false;
          DCSScannerInfo availableScanner = (DCSScannerInfo) msg.obj;

          /* notify connections delegates */
          for (IScannerAppEngineDevConnectionsDelegate delegate : mDevConnDelegates) {
            if (delegate != null) {
              result = delegate.scannerHasAppeared(availableScanner.getScannerID());
              if (result) {
                            /*
                             DevConnections delegates should NOT display any UI alerts,
                             so from UI notification side the event is not processed
                             */
                notificaton_processed = false;
              }
            }
          }

          /* update dev list */
          for (DCSScannerInfo ex_info : mScannerInfoList) {
            if (ex_info.getScannerID() == availableScanner.getScannerID()) {
              mScannerInfoList.remove(ex_info);
              break;
            }
          }

          mScannerInfoList.add(availableScanner);

          /* notify dev list delegates */
          for (IScannerAppEngineDevListDelegate delegate : Application.mDevListDelegates) {
            if (delegate != null) {
              result = delegate.scannersListHasBeenUpdated();
              if (result) {
                            /*
                             DeList delegates should NOT display any UI alerts,
                             so from UI notification side the event is not processed
                             */

                notificaton_processed = false;
              }
            }
          }

          //TODO - Showing notifications in foreground and background mode

          if (Application.MOT_SETTING_NOTIFICATION_AVAILABLE && !notificaton_processed && !found && !virtualTetherEventOccurred && !virtualTetherEnable ) {
            if (isInBackgroundMode(getApplicationContext())) {
              Intent intent = new Intent();
              intent.setAction(Constants.ACTION_SCANNER_CONNECTED);
              intent.putExtra(Constants.NOTIFICATIONS_TEXT, availableScanner.getScannerName() + " has appeared");
              intent.putExtra(Constants.NOTIFICATIONS_TYPE, Constants.SCANNER_APPEARED);
              sendOrderedBroadcast(intent, null);
            } else {
              Toast.makeText(getApplicationContext(), availableScanner.getScannerName() + " has appeared", Toast.LENGTH_SHORT).show();
            }
          }
          break;
        case Constants.SCANNER_DISAPPEARED:
          notificaton_processed = false;
          result = false;
          scannerID = (Integer) msg.obj;
          scannerName = "";
          /* notify connections delegates */
          for (IScannerAppEngineDevConnectionsDelegate delegate : mDevConnDelegates) {
            if (delegate != null) {
              result = delegate.scannerHasDisappeared(scannerID);
              if (result) {
                            /*
                             DevConnections delegates should NOT display any UI alerts,
                             so from UI notification side the event is not processed
                             */
                notificaton_processed = false;
              }
            }
          }

          /* update dev list */
          found = false;
          for (DCSScannerInfo ex_info : mScannerInfoList) {
            if (ex_info.getScannerID() == scannerID) {
              /* find scanner with ID in dev list */
              mScannerInfoList.remove(ex_info);
              scannerName = ex_info.getScannerName();
              found = true;
              break;
            }
          }

          if (!found) {
            for (DCSScannerInfo off_info : mOfflineScannerInfoList) {
              if (off_info.getScannerID() == scannerID) {
                scannerName = off_info.getScannerName();
                break;
              }
            }
            logAsMessage(TYPE_DEBUG, TAG, "ScannerAppEngine:dcssdkEventScannerDisappeared: scanner is not in list");
          }

          /* notify dev list delegates */
          for (IScannerAppEngineDevListDelegate delegate : Application.mDevListDelegates) {
            if (delegate != null) {
              result = delegate.scannersListHasBeenUpdated();
              if (result) {
                            /*
                             DeList delegates should NOT display any UI alerts,
                             so from UI notification side the event is not processed
                             */
                notificaton_processed = false;
              }
            }
          }

          //TODO - Showing notifications in foreground and background mode
          if (Application.MOT_SETTING_NOTIFICATION_AVAILABLE && !notificaton_processed && !found && !virtualTetherEnable && !virtualTetherEventOccurred) {
            StringBuilder notification_Msg = new StringBuilder();
            notification_Msg.append(scannerName).append(" has disappeared");
            if (isInBackgroundMode(getApplicationContext())) {
              Intent intent = new Intent();
              intent.setAction(Constants.ACTION_SCANNER_CONNECTED);
              intent.putExtra(Constants.NOTIFICATIONS_TEXT, notification_Msg.toString());
              intent.putExtra(Constants.NOTIFICATIONS_TYPE, Constants.SCANNER_DISAPPEARED);
              sendOrderedBroadcast(intent, null);
            } else {
              Toast.makeText(getApplicationContext(), notification_Msg.toString(), Toast.LENGTH_SHORT).show();
            }

          }

//                    if ((ScannersActivity.curAvailableScanner != null) && (scannerID == ScannersActivity.curAvailableScanner.getScannerId())) {
//                        ScannersActivity.curAvailableScanner = null;
//                    }
          break;
      }
    }
  };

  public void sendEvent(String eventName, @Nullable String params) {
    ZippraScannerModule.MainContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit(eventName, params);
  }

}
