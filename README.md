# react-native-zippra-scanner

Zebra scanner for react native ("https://www.zebra.com/us/en/support-downloads/software/developer-tools/scanner-sdk-for-android.html")

## Installation

```sh
npm react-native-zepra-scanner-1

npm expo install react-native-zepra-scanner-1
```

## Usage

```js
import * as React from 'react';

import {
  StyleSheet,
  View,
  Button,
  DeviceEventEmitter,
  Text,
} from 'react-native';
import {
  findCabledScanner,
  findBluetoothScanner,
  requestBluethoothAccess,
  setupApi,
  getActiveScannersList,
  Listeners,
} from 'react-native-zippra-scanner';

export default function App() {
  const [code, setCode] = React.useState('');

  React.useEffect(() => {
    requestBluethoothAccess().then(setupApi);

    DeviceEventEmitter.addListener(Listeners.SCANNER_DISAPPEARED, () =>
      console.log('SCANNER_DISAPPEARED')
    );
    DeviceEventEmitter.addListener(Listeners.SCANNER_ESTABLISHED, () =>
      console.log('SCANNER_ESTABLISHED')
    );
    DeviceEventEmitter.addListener(Listeners.SESSION_TERMINATED, () =>
      console.log('SESSION_TERMINATED')
    );
    DeviceEventEmitter.addListener(
      Listeners.BARCODE_RECEIVED,
      (value: string) => setCode(value)
    );
  }, []);

  const getDevices = () => {
    getActiveScannersList().then((devices) => {
      console.log(JSON.stringify(devices));
    });
  };

  return (
    <View style={styles.container}>
      <Text>Result: {code}</Text>
      <Button
        style={styles.openButton}
        onPress={() => findBluetoothScanner('A4:C7:4B:3B:38:F5')}
        title={'Open bluetooth barcode'}
      />
      <Button
        style={styles.openButton}
        onPress={findCabledScanner}
        title={'Open usb barcode'}
      />
      <Button
        style={styles.openButton}
        onPress={getDevices}
        title={'Get Devices'}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'space-around',
  },
  openButton: {
    width: 120,
    height: 60,
    backgroundColor: '#000000',
  },
});
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
