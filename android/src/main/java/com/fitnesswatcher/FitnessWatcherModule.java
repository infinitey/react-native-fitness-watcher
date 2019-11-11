package com.fitnesswatcher;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Callback;

import com.yc.pedometer.sdk.BLEServiceOperate;

public class FitnessWatcherModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    private BLEServiceOperate mBLEServiceOperate;
    private boolean isSupportBle4_0;

    public FitnessWatcherModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "FitnessWatcher";
    }

    @ReactMethod
    public void sampleMethod(String stringArgument, int numberArgument, Callback callback) {
        // TODO: Implement some actually useful functionality
        mBLEServiceOperate = BLEServiceOperate.getInstance(reactContext);
        isSupportBle4_0 = mBLEServiceOperate.isSupportBle4_0();

        callback.invoke("Received numberArgument: " + numberArgument + " stringArgument: " + stringArgument + "mBLEService" + isSupportBle4_0);
    }

    @ReactMethod
    public void initializeFitnessWatcher(String appName, ReadableMap options, Callback callback) {

        //initialize BLE service instance
        mBLEServiceOperate = BLEServiceOperate.getInstance(reactContext);
        isSupportBle4_0 = mBLEServiceOperate.isSupportBle4_0();

        //check if device supports BLE 4.0, if not disable the BLE feature.
        if(mBLEServiceOperate != null) {
            if(isSupportBle4_0) {
                //set Device Scan Listener

                //check if BLE is enabled, if not, turn on Bluetooth

            }
        }

//        WritableMap response = Arguments.createMap();
//        response.putString("result", "success");
//        callback.invoke(null, response);
    }

    @ReactMethod
    public void getListOfDevices(Callback callback) {

        //check if BLE is enabled, if not, turn on Bluetooth

        //scan for list of devices

        //stop scanning for list of devices

        //return list of devices

    }

    @ReactMethod
    public void connecToBLEDevice(Callback callback) {
        //boolean isConnectSuccessfully = mBLEServiceOperate.connect(deviceAddress)

        //callback for result
    }
}
