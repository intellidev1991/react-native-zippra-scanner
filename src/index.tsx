import { NativeModules, Platform} from 'react-native';

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

export function generatePairingBarcode(): Promise<any> {
  return ZippraScanner.openBarcodeActivity();
}


export function requestAccess(): Promise<any> {
  return ZippraScanner.requestAccess();
}


export function setupApi(): Promise<any> {
  return ZippraScanner.setupApi();
}


export function getActiveScannersList(): Promise<any> {
  return ZippraScanner.getActiveScannersList();
}