package com.fitnesswatcher;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import com.facebook.react.bridge.ReadableMap; //pass from JS to Java
import com.facebook.react.bridge.ReadableArray; //pass from JS to Java
import com.facebook.react.bridge.WritableArray; //pass from Java to JS
import com.facebook.react.bridge.WritableMap; //pass from Java to JS

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;

import com.yc.pedometer.sdk.BLEServiceOperate;
import com.yc.pedometer.sdk.DeviceScanInterfacer;

import java.util.ArrayList;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;


class FitnessWatcherModule extends ReactContextBaseJavaModule implements DeviceScanInterfacer {

    private final ReactApplicationContext reactContext;

    private String TAG="DeviceScanActivity";
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private final long SCAN_PERIOD = 10000;
    private BLEServiceOperate mBLEServiceOperate;

    private boolean isSupportBle4_0;

    private Context context;

    public FitnessWatcherModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "FitnessWatcher";
    }


    @ReactMethod
    public void initializeFitnessWatcher(final Callback callback) {

        //initialize BLE service instance
        mHandler = new Handler();
        mBLEServiceOperate = BLEServiceOperate.getInstance(reactContext);
        isSupportBle4_0 = mBLEServiceOperate.isSupportBle4_0();

        //check if device supports BLE 4.0, if not disable the BLE feature.
        if(mBLEServiceOperate != null) {
            if(isSupportBle4_0) {
                //check if BLE is enabled, if not, turn on Bluetooth
               if(mBLEServiceOperate.isBleEnabled()) {
                   mBLEServiceOperate.setDeviceScanListener(this);

                   mLeDeviceListAdapter = new LeDeviceListAdapter();

                   //scan for list of devices
                   scanLeDevice(true);
                   callback.invoke("800", "Initialisation is successful." );

               } else {
                   callback.invoke("702", "Bluetooth is not enabled." );
               }
            } else {
                callback.invoke("701", "This device does not support BLE 4.0." );
            }
        } else {
            callback.invoke("700", "Failed to create service instance." );
        }
    }

    @ReactMethod
    public void getListOfDevices(Callback callback) {
        WritableArray listOfDevices = Arguments.createArray();
        WritableMap device;
        ArrayList<BleDevices> mLeDevices = mLeDeviceListAdapter.getmLeDevices();

        for(int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
            device = Arguments.createMap();
            device.putString("name", mLeDevices.get(i).getName());
            device.putString("address",  mLeDevices.get(i).getAddress());
            device.putInt("rssi", mLeDevices.get(i).getRssi());
            listOfDevices.pushMap(device);
        }
        //return list of devices
        if(listOfDevices.size() > 0) {
            callback.invoke(listOfDevices);
        }  else {
            if(mLeDeviceListAdapter.getCount() < 1) {
                callback.invoke("Error: No mLeDevices detected.");
            } else if (listOfDevices.size() < 1) {
                callback.invoke("Error: No Bluetooth devices detected.");
            }
        }

    }

    @ReactMethod
    public void connectToBLEDevice(String deviceAddress, Callback callback) {
        //boolean isConnectSuccessfully = mBLEServiceOperate.connect(deviceAddress)

        //callback for result
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBLEServiceOperate.stopLeScan();
                    //invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBLEServiceOperate.startLeScan();
            Log.i(TAG,"startLeScan");
        } else {
            mScanning = false;
            mBLEServiceOperate.stopLeScan();
        }
    }


    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BleDevices> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            // mLeDevices = new ArrayList<BluetoothDevice>();
            mLeDevices = new ArrayList<>();
            //mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BleDevices device) {
            boolean repeat = false;
            for (int i = 0; i < mLeDevices.size(); i++) {
                if (mLeDevices.get(i).getAddress().equals(device.getAddress())) {
                    mLeDevices.remove(i);
                    repeat = true;
                    mLeDevices.add(i, device);
                }
            }
            if (!repeat) {
                mLeDevices.add(device);
            }
        }

        public BleDevices getDevice(int position) {
            return mLeDevices.get(position);
        }

        public ArrayList<BleDevices> getmLeDevices() { return mLeDevices; }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            return view;
        }
    }

    @Override
    public void LeScanCallback(final BluetoothDevice device, final int rssi) {
        // TODO Auto-generated method stub
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // mLeDeviceListAdapter.addDevice(device);
				Log.i(TAG,"scanning device="+device);
                if (device != null) {
                    BleDevices mBleDevices = new BleDevices(device.getName(),
                            device.getAddress(), rssi);
                    mLeDeviceListAdapter.addDevice(mBleDevices);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }

            }
        });
    }
}
