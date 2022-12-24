# react-native-zippra-scanner
Zippra scanner
## Installation

```sh
npm install react-native-zippra-scanner
```

## Usage

```js
import { 
    requestBluethoothAccess, 
    findCabledScanner, 
    findBluetoothScanner, 
    setupApi
} from "react-native-zippra-scanner";

// ...

const App = () => {
    useEffect(() => {
        // .. request access to bluetooth promise then setup main config of module
        requestBluethoothAccess(setupApi)

        // .. add listeners 
        DeviceEventEmitter.addListener('BARCODE_RECEIVED', (value: string) => {
            console.log({
                value
            })
        });

        DeviceEventEmitter.addListener('SCANNER_DISAPPEARED', () => alert('SCANNER_DISAPPEARED'));
        DeviceEventEmitter.addListener('SCANNER_ESTABLISHED', () => alert('SCANNER_ESTABLISHED'));
        DeviceEventEmitter.addListener('SESSION_TERMINATED', () => alert('SESSION_TERMINATED'));
    }, [])

    const conncetUsbButtonPressed = () => {
        // .. connect the device before open this page
        findCabledScanner();
    }

    const connectBluethoothButtonPressed = () => {
        const bluetoothAddressOfYoutMobile = "A4:C7:4B:3B:38:F5";
        findBluetoothScanner(bluetoothAddressOfYoutMobile);
    }

    return (
        <View>
            <Button title={"Connect usb device"} onPress={conncetUsbButtonPressed} />
            <Button title={"Connect bluetooth device"} onPress={connectBluethoothButtonPressed} />
        </View>
    )
}


```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
