import * as React from 'react';

import { StyleSheet, View, Button, DeviceEventEmitter, Text} from 'react-native';
import { findCabledScanner, findBluetoothScanner, requestBluethoothAccess, setupApi, getActiveScannersList } from 'react-native-zippra-scanner';

export default function App() {
  const [code, setCode] = React.useState("");

  React.useEffect(() => {
    requestBluethoothAccess().then(setupApi);

    DeviceEventEmitter.addListener('SCANNER_DISAPPEARED', () => console.log('SCANNER_DIsAPPEARED'));
    DeviceEventEmitter.addListener('SCANNER_ESTABLISHED', () => console.log('SCANNER_ESTABLISHED'));
    DeviceEventEmitter.addListener('SESSION_TERMINATED', () => console.log('SESSION_TERMINATED'));
    DeviceEventEmitter.addListener('BARCODE_RECEIVED', (value: string) => setCode(value));

  }, [])

  const getDevices = () => {
    getActiveScannersList().then((devices) => {
      console.log(JSON.stringify((devices)))
    })
  }

  return (
    <View style={styles.container}>
      <Text>Result: {code}</Text>
      <Button style={styles.openButton} onPress={() =>  findBluetoothScanner("A4:C7:4B:3B:38:F5")} title={'Open bluetooth barcode'} />
      <Button style={styles.openButton} onPress={findCabledScanner} title={'Open usb barcode'} />
      <Button style={styles.openButton} onPress={getDevices} title={'Get Devices'} />
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
    backgroundColor: "#000000"
  },
});
