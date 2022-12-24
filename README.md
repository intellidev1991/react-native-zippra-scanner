# react-native-zippra-scanner
Zebra scanner for react native ("https://www.zebra.com/us/en/support-downloads/software/developer-tools/scanner-sdk-for-android.html")
## Installation

```sh
npm install react-native-zippra-scanner

Download sdk folder: ("https://drive.google.com/file/d/1NUedeRy3AKSOgmNNjm9lkzEotno8AOzR/view")

Extract the file then add it => {Your Project}/android/BarcodeScannerLibrary

Add this to {Your Project/android/app/build.gradle}
dependencies {
    // all modules
    +implementation fileTree(dir: "libs", include: ["*.jar", "*.aar"])
    +implementation project(':BarcodeScannerLibrary')
}

Add this inside settings.gradle
include ':BarcodeScannerLibrary'


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
        // .. change this with bluetooth address of you mobile
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
