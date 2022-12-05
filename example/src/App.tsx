import * as React from 'react';

import { StyleSheet, View, Text, Image } from 'react-native';
import {  BarCodeView, generatePairingBarcode} from 'react-native-zippra-scanner';

export default function App() {
  const [ViewToShow, setResult] = React.useState<number | undefined>(null);

  React.useEffect(() => {
    setTimeout(() => {
      generatePairingBarcode();

    }, 2000);
  }, [])

  return (
    <View style={styles.container}>
      {/* <Text>Result: {result}</Text> */}

      <BarCodeView 
        style={{
          backgroundColor: 'blue',
          height: 100,
          width: 100
        }} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'red'
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
