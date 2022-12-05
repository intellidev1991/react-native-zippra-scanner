import { NativeModules, Platform, requireNativeComponent, View } from 'react-native';

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

const BarCodeView = requireNativeComponent<any>('BarCodeView', {
  name: 'BarCodeView',
  propTypes: {
    ...View.propTypes,
  },
});

export { BarCodeView }