import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-zippra-scanner' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const ZippraScanner = NativeModules.ZippraScanner
  ? NativeModules.ZippraScanner
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export enum Listeners {
  BARCODE_RECEIVED = 'BARCODE_RECEIVED',
  SESSION_TERMINATED = 'SESSION_TERMINATED',
  SCANNER_DISAPPEARED = 'SCANNER_DISAPPEARED',
  SCANNER_ESTABLISHED = 'SCANNER_ESTABLISHED',
}

export function findCabledScanner() {
  return ZippraScanner.findCabledScanner();
}

export function findBluetoothScanner(BluetoothAddress: String) {
  return ZippraScanner.findBluetoothScanner(BluetoothAddress);
}

export function requestBluethoothAccess(): Promise<any> {
  return ZippraScanner.requestAccess();
}

export function setupApi(): Promise<any> {
  return ZippraScanner.setupApi();
}

export function getActiveScannersList(): Promise<any> {
  return ZippraScanner.getActiveScannersList();
}

export function init() {
  return ZippraScanner.init();
}