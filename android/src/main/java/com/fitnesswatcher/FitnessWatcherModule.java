package com.fitnesswatcher;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

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

import com.yc.pedometer.info.SportsModesInfo;
import com.yc.pedometer.sdk.BLEServiceOperate;
import com.yc.pedometer.sdk.BluetoothLeService;
import com.yc.pedometer.sdk.DeviceScanInterfacer;
import com.yc.pedometer.sdk.ICallback;
import com.yc.pedometer.sdk.ICallbackStatus;
import com.yc.pedometer.sdk.OnServerCallbackListener;
import com.yc.pedometer.sdk.WriteCommandToBLE;
import com.yc.pedometer.update.Updates;
import com.yc.pedometer.utils.GBUtils;
import com.yc.pedometer.utils.GlobalVariable;
import com.yc.pedometer.utils.MultipleSportsModesUtils;

import java.util.ArrayList;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;


class FitnessWatcherModule extends ReactContextBaseJavaModule implements DeviceScanInterfacer, ICallback, OnServerCallbackListener {

    private final ReactApplicationContext reactContext;

    private String TAG="DeviceScanActivity";
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private boolean mScanning;

    private final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private final long SCAN_PERIOD = 10000;
    private BLEServiceOperate mBLEServiceOperate;

    private boolean isSupportBle4_0;

    private final int CONNECTED = 1;
    private final int CONNECTING = 2;
    private final int DISCONNECTED = 3;
    private int CURRENT_STATUS = DISCONNECTED;

    private final int UPDATE_STEP_UI_MSG = 0;
    private final int UPDATE_SLEEP_UI_MSG = 1;
    private final int DISCONNECT_MSG = 18;
    private final int CONNECTED_MSG = 19;
    private final int UPDATA_REAL_RATE_MSG = 20;
    private final int RATE_SYNC_FINISH_MSG = 21;
    private final int OPEN_CHANNEL_OK_MSG = 22;
    private final int CLOSE_CHANNEL_OK_MSG = 23;
    private final int TEST_CHANNEL_OK_MSG = 24;
    private final int OFFLINE_SWIM_SYNC_OK_MSG = 25;
    private final int UPDATA_REAL_BLOOD_PRESSURE_MSG = 29;
    private final int OFFLINE_BLOOD_PRESSURE_SYNC_OK_MSG = 30;
    private final int SERVER_CALL_BACK_OK_MSG = 31;
    private final int OFFLINE_SKIP_SYNC_OK_MSG = 32;
    private final int test_mag1 = 35;
    private final int test_mag2 = 36;
    private final int OFFLINE_STEP_SYNC_OK_MSG = 37;
    private final int UPDATE_SPORTS_TIME_DETAILS_MSG = 38;
    private static final int SHOW_SET_PASSWORD_MSG = 26;
    private static final int SHOW_INPUT_PASSWORD_MSG = 27;
    private static final int SHOW_INPUT_PASSWORD_AGAIN_MSG = 28;

    private final int UNIVERSAL_INTERFACE_SDK_TO_BLE_SUCCESS_MSG = 39;//sdk发送数据到ble完成，并且校验成功，返回状态
    private final int UNIVERSAL_INTERFACE_SDK_TO_BLE_FAIL_MSG = 40;   //sdk发送数据到ble完成，但是校验失败，返回状态
    private final int UNIVERSAL_INTERFACE_BLE_TO_SDK_SUCCESS_MSG = 41;//ble发送数据到sdk完成，并且校验成功，返回数据
    private final int UNIVERSAL_INTERFACE_BLE_TO_SDK_FAIL_MSG = 42;   //ble发送数据到sdk完成，但是校验失败，返回状态

    private final int RATE_OF_24_HOUR_SYNC_FINISH_MSG = 43;

    private BluetoothLeService mBluetoothLeService;
    private WriteCommandToBLE mWriteCommand;
    private Updates mUpdates;
    private SharedPreferences sp;

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
        sp = reactContext.getSharedPreferences(GlobalVariable.SettingSP, 0);
        mWriteCommand = WriteCommandToBLE.getInstance(reactContext);
        mUpdates = Updates.getInstance(reactContext);
        mUpdates.setHandler(mHandler);
        mUpdates.registerBroadcastReceiver();
        mUpdates.setOnServerCallbackListener(this);

        boolean isConnectSuccessfully = mBLEServiceOperate.connect(deviceAddress);

        CURRENT_STATUS = CONNECTING;

        //upDateTodaySwimData();
        //upDateTodaySkipData();

        //callback for result
        callback.invoke("Is successfully connect? "+ isConnectSuccessfully);
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

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DISCONNECT_MSG:
                    //connect_status.setText(getString(R.string.disconnect));
                    CURRENT_STATUS = DISCONNECTED;
                    //Toast.makeText(mContext, "disconnect or connect falie", Toast.LENGTH_SHORT).show();

                    String lastConnectAddr0 = sp.getString(
                            GlobalVariable.LAST_CONNECT_DEVICE_ADDRESS_SP,
                            "00:00:00:00:00:00");
                    boolean connectResute0 = mBLEServiceOperate
                            .connect(lastConnectAddr0);
                    Log.i(TAG, "connectResute0=" + connectResute0);

                    break;
                case CONNECTED_MSG:
                    //connect_status.setText(getString(R.string.connected));
                    mBluetoothLeService.setRssiHandler(mHandler);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!Thread.interrupted()) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                if (mBluetoothLeService != null) {
                                    mBluetoothLeService.readRssi();
                                }
                            }
                        }
                    }).start();
                    CURRENT_STATUS = CONNECTED;
                    //Toast.makeText(mContext, "connected", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public void OnResult(boolean result, int status) {
        // TODO Auto-generated method stub
        Log.i(TAG, "result=" + result + ",status=" + status);
        switch (status) {
            case ICallbackStatus.OFFLINE_STEP_SYNC_OK:
                mHandler.sendEmptyMessage(OFFLINE_STEP_SYNC_OK_MSG);
                break;
            case ICallbackStatus.OFFLINE_SLEEP_SYNC_OK:
                break;
            case ICallbackStatus.SYNC_TIME_OK:// (时间在同步在SDK内部已经帮忙同步，你不需要同步时间了，sdk内部同步时间完成会自动回调到这里)
                //同步时间成功后，会回调到这里，延迟20毫秒，获取固件版本
                // delay 20ms  send
                // to read
                // localBleVersion
                // mWriteCommand.sendToReadBLEVersion();
                break;
            case ICallbackStatus.GET_BLE_VERSION_OK:// 获取固件版本成功后会回调到这里，延迟20毫秒，设置身高体重到手环
                // localBleVersion
                // finish,
                // then sync
                // step
                // mWriteCommand.syncAllStepData();
                break;
            case ICallbackStatus.DISCONNECT_STATUS:
                mHandler.sendEmptyMessage(DISCONNECT_MSG);
                break;
            case ICallbackStatus.CONNECTED_STATUS:
                mHandler.sendEmptyMessage(CONNECTED_MSG);
                mHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        mWriteCommand.sendToQueryPasswardStatus();
                    }
                }, 600);// 2.2.1版本修改

                break;

            case ICallbackStatus.DISCOVERY_DEVICE_SHAKE:
                Log.d(TAG, "摇一摇拍照");
                // Discovery device Shake
                break;
            case ICallbackStatus.OFFLINE_RATE_SYNC_OK:
                mHandler.sendEmptyMessage(RATE_SYNC_FINISH_MSG);
                break;
            case ICallbackStatus.OFFLINE_24_HOUR_RATE_SYNC_OK:
                mHandler.sendEmptyMessage(RATE_OF_24_HOUR_SYNC_FINISH_MSG);
                break;
            case ICallbackStatus.SET_METRICE_OK: // 设置公制单位成功
                break;
            case ICallbackStatus.SET_INCH_OK: //// 设置英制单位成功
                break;
            case ICallbackStatus.SET_FIRST_ALARM_CLOCK_OK: // 设置第1个闹钟OK
                break;
            case ICallbackStatus.SET_SECOND_ALARM_CLOCK_OK: //设置第2个闹钟OK
                break;
            case ICallbackStatus.SET_THIRD_ALARM_CLOCK_OK: // 设置第3个闹钟OK
                break;
            case ICallbackStatus.SEND_PHONE_NAME_NUMBER_OK:
                mWriteCommand.sendQQWeChatVibrationCommand(5);
                break;
            case ICallbackStatus.SEND_QQ_WHAT_SMS_CONTENT_OK:
                mWriteCommand.sendQQWeChatVibrationCommand(1);
                break;
            case ICallbackStatus.PASSWORD_SET:
                Log.d(TAG, "没设置过密码，请设置4位数字密码");
                mHandler.sendEmptyMessage(SHOW_SET_PASSWORD_MSG);
                break;
            case ICallbackStatus.PASSWORD_INPUT:
                Log.d(TAG, "已设置过密码，请输入已设置的4位数字密码");
                mHandler.sendEmptyMessage(SHOW_INPUT_PASSWORD_MSG);
                break;
            case ICallbackStatus.PASSWORD_AUTHENTICATION_OK:
                Log.d(TAG, "验证成功或者设置密码成功");
                break;
            case ICallbackStatus.PASSWORD_INPUT_AGAIN:
                Log.d(TAG, "验证失败或者设置密码失败，请重新输入4位数字密码，如果已设置过密码，请输入已设置的密码");
                mHandler.sendEmptyMessage(SHOW_INPUT_PASSWORD_AGAIN_MSG);
                break;
            case ICallbackStatus.OFFLINE_SWIM_SYNCING:
                Log.d(TAG, "游泳数据同步中");
                break;
            case ICallbackStatus.OFFLINE_SWIM_SYNC_OK:
                Log.d(TAG, "游泳数据同步完成");
                mHandler.sendEmptyMessage(OFFLINE_SWIM_SYNC_OK_MSG);
                break;
            case ICallbackStatus.OFFLINE_BLOOD_PRESSURE_SYNCING:
                Log.d(TAG, "血压数据同步中");
                break;
            case ICallbackStatus.OFFLINE_BLOOD_PRESSURE_SYNC_OK:
                Log.d(TAG, "血压数据同步完成");
                mHandler.sendEmptyMessage(OFFLINE_BLOOD_PRESSURE_SYNC_OK_MSG);
                break;
            case ICallbackStatus.OFFLINE_SKIP_SYNCING:
                Log.d(TAG, "跳绳数据同步中");
                break;
            case ICallbackStatus.OFFLINE_SKIP_SYNC_OK:
                Log.d(TAG, "跳绳数据同步完成");
                mHandler.sendEmptyMessage(OFFLINE_SKIP_SYNC_OK_MSG);
                break;
            case ICallbackStatus.MUSIC_PLAYER_START_OR_STOP:
                Log.d(TAG, "音乐播放/暂停");
                break;
            case ICallbackStatus.MUSIC_PLAYER_NEXT_SONG:
                Log.d(TAG, "音乐下一首");
                break;
            case ICallbackStatus.MUSIC_PLAYER_LAST_SONG:
                Log.d(TAG, "音乐上一首");
                break;
            case ICallbackStatus.OPEN_CAMERA_OK:
                Log.d(TAG, "打开相机ok");
                break;
            case ICallbackStatus.CLOSE_CAMERA_OK:
                Log.d(TAG, "关闭相机ok");
                break;
            case ICallbackStatus.PRESS_SWITCH_SCREEN_BUTTON:
                Log.d(TAG, "表示按键1短按下，用来做切换屏,表示切换了手环屏幕");
                mHandler.sendEmptyMessage(test_mag1);
                break;
            case ICallbackStatus.PRESS_END_CALL_BUTTON:
                Log.d(TAG, "表示按键1长按下，一键拒接来电");
                break;
            case ICallbackStatus.PRESS_TAKE_PICTURE_BUTTON:
                Log.d(TAG, "表示按键2短按下，用来做一键拍照");
                break;
            case ICallbackStatus.PRESS_SOS_BUTTON:
                Log.d(TAG, "表示按键3短按下，用来做一键SOS");
                mHandler.sendEmptyMessage(test_mag2);
                break;
            case ICallbackStatus.PRESS_FIND_PHONE_BUTTON:
                Log.d(TAG, "表示按键按下，手环查找手机的功能。");

                break;
            case ICallbackStatus.READ_ONCE_AIR_PRESSURE_TEMPERATURE_SUCCESS:
                Log.d(TAG, "读取当前气压传感器气压值和温度值成功，数据已保存到数据库，查询请调用查询数据库接口，返回的数据中，最新的一条为本次读取的数据");
                break;
            case ICallbackStatus.SYNC_HISORY_AIR_PRESSURE_TEMPERATURE_SUCCESS:
                Log.d(TAG, "同步当天历史数据成功，包括气压传感器气压值和温度值，数据已保存到数据库，查询请调用查询数据库接口");
                break;
            case ICallbackStatus.SYNC_HISORY_AIR_PRESSURE_TEMPERATURE_FAIL:
                Log.d(TAG, "同步当天历史数据失败，数据不保存");
                break;
            default:
                break;
        }
    }

    @Override
    public void OnDataResult(boolean result, int status, byte[] data) {
        StringBuilder stringBuilder = null;
        if (data != null && data.length > 0) {
            stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data) {
                stringBuilder.append(String.format("%02X", byteChar));
            }
            Log.i(TAG, "BLE---->APK data =" + stringBuilder.toString());
        }
//		if (status == ICallbackStatus.OPEN_CHANNEL_OK) {// 打开通道OK
//			mHandler.sendEmptyMessage(OPEN_CHANNEL_OK_MSG);
//		} else if (status == ICallbackStatus.CLOSE_CHANNEL_OK) {// 关闭通道OK
//			mHandler.sendEmptyMessage(CLOSE_CHANNEL_OK_MSG);
//		} else if (status == ICallbackStatus.BLE_DATA_BACK_OK) {// 测试通道OK，通道正常
//			mHandler.sendEmptyMessage(TEST_CHANNEL_OK_MSG);
//		}
        switch (status) {
            case ICallbackStatus.OPEN_CHANNEL_OK:// 打开通道OK
                mHandler.sendEmptyMessage(OPEN_CHANNEL_OK_MSG);
                break;
            case ICallbackStatus.CLOSE_CHANNEL_OK:// 关闭通道OK
                mHandler.sendEmptyMessage(CLOSE_CHANNEL_OK_MSG);
                break;
            case ICallbackStatus.BLE_DATA_BACK_OK:// 测试通道OK，通道正常
                mHandler.sendEmptyMessage(TEST_CHANNEL_OK_MSG);
                break;
            //========通用接口回调 Universal Interface   start====================
            case ICallbackStatus.UNIVERSAL_INTERFACE_SDK_TO_BLE_SUCCESS://sdk发送数据到ble完成，并且校验成功，返回状态
                mHandler.sendEmptyMessage(UNIVERSAL_INTERFACE_SDK_TO_BLE_SUCCESS_MSG);
                break;
            case ICallbackStatus.UNIVERSAL_INTERFACE_SDK_TO_BLE_FAIL://sdk发送数据到ble完成，但是校验失败，返回状态
                mHandler.sendEmptyMessage(UNIVERSAL_INTERFACE_SDK_TO_BLE_FAIL_MSG);
                break;
            case ICallbackStatus.UNIVERSAL_INTERFACE_BLE_TO_SDK_SUCCESS://ble发送数据到sdk完成，并且校验成功，返回数据
                mHandler.sendEmptyMessage(UNIVERSAL_INTERFACE_BLE_TO_SDK_SUCCESS_MSG);
                break;
            case ICallbackStatus.UNIVERSAL_INTERFACE_BLE_TO_SDK_FAIL://ble发送数据到sdk完成，但是校验失败，返回状态
                mHandler.sendEmptyMessage(UNIVERSAL_INTERFACE_BLE_TO_SDK_SUCCESS_MSG);
                break;
            //========通用接口回调 Universal Interface   end====================
            case ICallbackStatus.CUSTOMER_ID_OK://回调 客户id
                if (result) {
                    Log.d(TAG, "客户ID = " + GBUtils.getInstance(reactContext).customerIDAsciiByteToString(data));
                }

                break;
            case ICallbackStatus.DO_NOT_DISTURB_CLOSE://回调 勿扰模式关闭
                if (data != null && data.length >= 2) {
                    switch (data[1]) {
                        case 0:
                            Log.d(TAG, "勿扰模式已关闭。勿扰时间段之外的时间，关闭手环振动和关闭信息提醒的开关处于关闭状态，即有振动和信息提醒");
                            break;
                        case 2:
                            Log.d(TAG, "勿扰模式已关闭。勿扰时间段之外的时间，关闭手环振动的开关处于打开状态，关闭信息提醒的开关处于关闭状态，即无振动但有信息提醒");
                            break;
                        case 4:
                            Log.d(TAG, "勿扰模式已关闭。勿扰时间段之外的时间，关闭手环振动的开关处于关闭状态，关闭信息提醒的开关处于打开状态，即有振动但无信息提醒");
                            break;
                        case 6:
                            Log.d(TAG, "勿扰模式已关闭。勿扰时间段之外的时间，关闭手环振动和关闭信息提醒的开关处于打开状态，即都没有振动和信息提醒");
                            break;
                    }
                }
                break;
            case ICallbackStatus.DO_NOT_DISTURB_OPEN://回调 勿扰模式打开
                if (data != null && data.length >= 2) {
                    switch (data[1]) {
                        case 0:
                            Log.d(TAG, "勿扰模式已打开。勿扰时间段之外的时间，关闭手环振动和关闭信息提醒的开关处于关闭状态，即有振动和信息提醒");
                            break;
                        case 2:
                            Log.d(TAG, "勿扰模式已打开。勿扰时间段之外的时间，关闭手环振动的开关处于打开状态，关闭信息提醒的开关处于关闭状态，即无振动但有信息提醒");
                            break;
                        case 4:
                            Log.d(TAG, "勿扰模式已打开。勿扰时间段之外的时间，关闭手环振动的开关处于关闭状态，关闭信息提醒的开关处于打开状态，即有振动但无信息提醒");
                            break;
                        case 6:
                            Log.d(TAG, "勿扰模式已打开。勿扰时间段之外的时间，关闭手环振动和关闭信息提醒的开关处于打开状态，即都没有振动和信息提醒");
                            break;
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onIbeaconWriteCallback(boolean result, int ibeaconSetOrGet,
                                       int ibeaconType, String data) {
        // public static final int IBEACON_TYPE_UUID = 0;// Ibeacon
        // 指令类型,设置UUID/获取UUID
        // public static final int IBEACON_TYPE_MAJOR = 1;// Ibeacon
        // 指令类型,设置major/获取major
        // public static final int IBEACON_TYPE_MINOR = 2;// Ibeacon
        // 指令类型,设置minor/获取minor
        // public static final int IBEACON_TYPE_DEVICE_NAME = 3;// Ibeacon
        // 指令类型,设置蓝牙device name/获取蓝牙device name
        // public static final int IBEACON_SET = 0;// Ibeacon
        // 设置(设置UUID/设置major,设置minor,设置蓝牙device name)
        // public static final int IBEACON_GET = 1;// Ibeacon
        // 获取(设置UUID/设置major,设置minor,设置蓝牙device name)
        Log.d(TAG, "onIbeaconWriteCallback 设置或获取结果result =" + result
                + ",ibeaconSetOrGet =" + ibeaconSetOrGet + ",ibeaconType ="
                + ibeaconType + ",数据data =" + data);
        if (result) {// success
            switch (ibeaconSetOrGet) {
                case GlobalVariable.IBEACON_SET:
                    switch (ibeaconType) {
                        case GlobalVariable.IBEACON_TYPE_UUID:
                            Log.d(TAG, "设置UUID成功,data =" + data);
                            break;
                        case GlobalVariable.IBEACON_TYPE_MAJOR:
                            Log.d(TAG, "设置major成功,data =" + data);
                            break;
                        case GlobalVariable.IBEACON_TYPE_MINOR:
                            Log.d(TAG, "设置minor成功,data =" + data);
                            break;
                        case GlobalVariable.IBEACON_TYPE_DEVICE_NAME:
                            Log.d(TAG, "设置device name成功,data =" + data);
                            break;
                        case GlobalVariable.IBEACON_TYPE_TX_POWER:
                            Log.d(TAG, "设置TX power成功,data =" + data);
                            break;
                        case GlobalVariable.IBEACON_TYPE_ADVERTISING_INTERVAL:
                            Log.d(TAG, "设置advertising interval成功,data =" + data);
                            break;

                        default:
                            break;
                    }
                    break;
                case GlobalVariable.IBEACON_GET:
                    switch (ibeaconType) {
                        case GlobalVariable.IBEACON_TYPE_UUID:
                            Log.d(TAG, "获取UUID成功,data =" + data);
                            break;
                        case GlobalVariable.IBEACON_TYPE_MAJOR:
                            Log.d(TAG, "获取major成功,data =" + data);
                            break;
                        case GlobalVariable.IBEACON_TYPE_MINOR:
                            Log.d(TAG, "获取minor成功,data =" + data);
                            break;
                        case GlobalVariable.IBEACON_TYPE_DEVICE_NAME:
                            Log.d(TAG, "获取device name成功,data =" + data);
                            break;
                        case GlobalVariable.IBEACON_TYPE_TX_POWER:
                            Log.d(TAG, "获取TX power成功,data =" + data);
                            break;
                        case GlobalVariable.IBEACON_TYPE_ADVERTISING_INTERVAL:
                            Log.d(TAG, "获取advertising interval,data =" + data);
                            break;

                        default:
                            break;
                    }
                    break;

                default:
                    break;
            }

        } else {// fail
            switch (ibeaconSetOrGet) {
                case GlobalVariable.IBEACON_SET:
                    switch (ibeaconType) {
                        case GlobalVariable.IBEACON_TYPE_UUID:
                            Log.d(TAG, "设置UUID失败");
                            break;
                        case GlobalVariable.IBEACON_TYPE_MAJOR:
                            Log.d(TAG, "设置major失败");
                            break;
                        case GlobalVariable.IBEACON_TYPE_MINOR:
                            Log.d(TAG, "设置minor失败");
                            break;
                        case GlobalVariable.IBEACON_TYPE_DEVICE_NAME:
                            Log.d(TAG, "设置device name失败");
                            break;

                        default:
                            break;
                    }
                    break;
                case GlobalVariable.IBEACON_GET:
                    switch (ibeaconType) {
                        case GlobalVariable.IBEACON_TYPE_UUID:
                            Log.d(TAG, "获取UUID失败");
                            break;
                        case GlobalVariable.IBEACON_TYPE_MAJOR:
                            Log.d(TAG, "获取major失败");
                            break;
                        case GlobalVariable.IBEACON_TYPE_MINOR:
                            Log.d(TAG, "获取minor失败");
                            break;
                        case GlobalVariable.IBEACON_TYPE_DEVICE_NAME:
                            Log.d(TAG, "获取device name失败");
                            break;

                        default:
                            break;
                    }
                    break;

                default:
                    break;
            }
        }

    }

    @Override
    public void onCharacteristicWriteCallback(int status) {// add 20170221
        // 写入操作的系统回调，status = 0为写入成功，其他或无回调表示失败
        Log.d(TAG, "Write System callback status = " + status);
    }

    @Override
    public void onQueryDialModeCallback(boolean result, int screenWith,
                                        int screenHeight, int screenCount) {// 查询表盘方式回调
        Log.d(TAG, "result =" + result + ",screenWith =" + screenWith
                + ",screenHeight =" + screenHeight + ",screenCount ="
                + screenCount);
    }

    @Override
    public void onControlDialCallback(boolean result, int leftRightHand,
                                      int dialType) {// 控制表盘切换和左右手切换回调
        switch (leftRightHand) {
            case GlobalVariable.LEFT_HAND_WEAR:
                Log.d(TAG, "设置左手佩戴成功");
                break;
            case GlobalVariable.RIGHT_HAND_WEAR:
                Log.d(TAG, "设置右手佩戴成功");
                break;
            case GlobalVariable.NOT_SET_UP:
                Log.d(TAG, "不设置，保持上次佩戴方式成功");
                break;

            default:
                break;
        }
        switch (dialType) {
            case GlobalVariable.SHOW_VERTICAL_ENGLISH_SCREEN:
                Log.d(TAG, "设置显示竖屏英文界面成功");
                break;
            case GlobalVariable.SHOW_VERTICAL_CHINESE_SCREEN:
                Log.d(TAG, "设置显示竖屏中文界面成功");
                break;
            case GlobalVariable.SHOW_HORIZONTAL_SCREEN:
                Log.d(TAG, "设置显示横屏成功");
                break;
            case GlobalVariable.NOT_SET_UP:
                Log.d(TAG, "不设置，默认上次显示的屏幕成功");
                break;

            default:
                break;
        }
    }

    @Override
    public void onSportsTimeCallback(boolean result, String calendar,int sportsTime,
                                     int timeType) {

//        if (timeType == GlobalVariable.SPORTS_TIME_TODAY) {
//
//            Log.d(TAG, "今天的运动时间  calendar =" + calendar + ",sportsTime ="
//                    + sportsTime);
//            resultBuilder.append("\n" + calendar + "," + sportsTime
//                    + getResources().getString(R.string.fminute));
//            mHandler.sendEmptyMessage(UPDATE_SPORTS_TIME_DETAILS_MSG);
//
//        } else if (timeType == GlobalVariable.SPORTS_TIME_HISTORY_DAY) {// 7天的运动时间
//            Log.d(TAG, "7天的运动时间  calendar =" + calendar
//                    + ",sportsTime =" + sportsTime);
//            resultBuilder.append("\n" + calendar + "," + sportsTime
//                    + getResources().getString(R.string.fminute));
//            mHandler.sendEmptyMessage(UPDATE_SPORTS_TIME_DETAILS_MSG);
//        }
    }

    @Override
    public void OnResultSportsModes(boolean result, int status, boolean switchStatus, int sportsModes, SportsModesInfo info) {
        MultipleSportsModesUtils.LLogI("OnResultSportsModes  result =" + result + ",status =" + status + ",switchStatus =" + switchStatus
                + ",sportsModes =" + sportsModes + ",info =" + info);
        switch (status) {
            case ICallbackStatus.CONTROL_MULTIPLE_SPORTS_MODES://多运动模式及运动心率 控制开关，模式
                break;
            case ICallbackStatus.INQUIRE_MULTIPLE_SPORTS_MODES://多运动模式及运动心率 查询当前模式和开关
                break;
            case ICallbackStatus.SYNC_MULTIPLE_SPORTS_MODES_START://多运动模式及运动心率 开始同步，返回此次同步有多少次运动
                //注意：sportsModes 在这个case有点特殊，sportsModes为返回此次同步有多少次运动
                break;
            case ICallbackStatus.SYNC_MULTIPLE_SPORTS_MODES://多运动模式及运动心率 某一种运动模式同步完成
                break;
            case ICallbackStatus.MULTIPLE_SPORTS_MODES_REAL://多运动模式及运动心率 实时数据，只有实时数据时，SportsModesInfo才不为空，其他的都为空
                break;
        }
    }

    public void OnServerCallback(int status) {
        Log.i(TAG, "服务器回调 OnServerCallback status =" + status);

        mHandler.sendEmptyMessage(SERVER_CALL_BACK_OK_MSG);

    }
}
