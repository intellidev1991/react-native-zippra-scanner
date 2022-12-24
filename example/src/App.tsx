import * as React from 'react';

import { StyleSheet, View, Button, DeviceEventEmitter } from 'react-native';
import { findCabledScanner, findBluetoothScanner, requestAccess, setupApi, getActiveScannersList } from 'react-native-zippra-scanner';

export default function App() {

  React.useEffect(() => {
    setTimeout(() => {

      requestAccess().then((response) => {
        // alert(response);
        setupApi();
      }).catch((error) => {
        // alert(error);
      })
    }, 1000);

    DeviceEventEmitter.addListener('Test', () => alert('SCANNER_APPEARED'));
    DeviceEventEmitter.addListener('SCANNER_DISAPPEARED', () => alert('SCANNER_DIsAPPEARED'));
    DeviceEventEmitter.addListener('SCANNER_ESTABLISHED', () => alert('SCANNER_ESTABLISHED'));
    DeviceEventEmitter.addListener('SESSION_TERMINATED', () => alert('SESSION_TERMINATED'));
    DeviceEventEmitter.addListener('BARCODE_RECEIVED', (value) => alert(value));

  }, [])

  const getDevices = () => {
    getActiveScannersList().then((devices) => {

      alert(JSON.stringify((devices)))

    })
  }

  return (
    <View style={styles.container}>
      {/* <Text>Result: {result}</Text> */}
      <Button style={styles.openButton} onPress={() => {
        findBluetoothScanner("A4:C7:4B:3B:38:F5");
      }} title={'Open bluetooth barcode'} />
      <Button style={styles.openButton} onPress={findCabledScanner} title={'Open usb barcode'} />
      <Button style={styles.openButton} onPress={getDevices} title={'Get Devices'} />
      {/* <Pressable style={styles.openButton} onPress={generatePairingBarcode} /> */}
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
    backgroundColor: "#000000",
    // marginBottom: 20
  },
});
