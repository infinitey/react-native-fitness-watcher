package com.fitnesswatcher;

import android.content.Context;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Callback;

import com.yc.pedometer.sdk.BLEServiceOperate;

public class FitnessWatcherModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private Context mContext;

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
        mBLEServiceOperate = BLEServiceOperate.getInstance(mContext);
        isSupportBle4_0 = mBLEServiceOperate.isSupportBle4_0();

        if(mBLEServiceOperate != null) {
            if(isSupportBle4_0) {

            }
        }

        callback.invoke("Received numberArgument: " + numberArgument + " stringArgument: " + stringArgument + "mBLEService" + isSupportBle4_0);
    }

    @ReactMethod
    public void initializeFitnessWatcher(String appName, ReadableMap options, Callback callback) {
        mBLEServiceOperate = BLEServiceOperate.getInstance(mContext);
        isSupportBle4_0 = mBLEServiceOperate.isSupportBle4_0();

        if(mBLEServiceOperate != null) {
            if(isSupportBle4_0) {

            }
        }


//        FirebaseOptions.Builder builder = new FirebaseOptions.Builder();
//
//        builder.setApiKey(options.getString("apiKey"));
//        builder.setApplicationId(options.getString("appId"));
//        builder.setProjectId(options.getString("projectId"));
//        builder.setDatabaseUrl(options.getString("databaseURL"));
//        builder.setStorageBucket(options.getString("storageBucket"));
//        builder.setGcmSenderId(options.getString("messagingSenderId"));
//        // todo firebase sdk has no client id setter
//
//        FirebaseApp.initializeApp(getReactApplicationContext(), builder.build(), appName);
//
//        WritableMap response = Arguments.createMap();
//        response.putString("result", "success");
//        callback.invoke(null, response);
    }
}
