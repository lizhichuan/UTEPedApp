package com.yc.peddemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.yc.peddemo.customview.CustomPasswordDialog;
import com.yc.peddemo.customview.CustomProgressDialog;
import com.yc.pedometer.info.BPVOneDayInfo;
import com.yc.pedometer.info.RateOneDayInfo;
import com.yc.pedometer.info.SevenDayWeatherInfo;
import com.yc.pedometer.info.SkipDayInfo;
import com.yc.pedometer.info.SleepTimeInfo;
import com.yc.pedometer.info.StepOneDayAllInfo;
import com.yc.pedometer.info.SwimDayInfo;
import com.yc.pedometer.sdk.BLEServiceOperate;
import com.yc.pedometer.sdk.BloodPressureChangeListener;
import com.yc.pedometer.sdk.BluetoothLeService;
import com.yc.pedometer.sdk.DataProcessing;
import com.yc.pedometer.sdk.ICallback;
import com.yc.pedometer.sdk.ICallbackStatus;
import com.yc.pedometer.sdk.OnServerCallbackListener;
import com.yc.pedometer.sdk.RateChangeListener;
import com.yc.pedometer.sdk.ServiceStatusCallback;
import com.yc.pedometer.sdk.SleepChangeListener;
import com.yc.pedometer.sdk.StepChangeListener;
import com.yc.pedometer.sdk.UTESQLOperate;
import com.yc.pedometer.sdk.WriteCommandToBLE;
import com.yc.pedometer.update.Updates;
import com.yc.pedometer.utils.CalendarUtils;
import com.yc.pedometer.utils.GetFunctionList;
import com.yc.pedometer.utils.GlobalVariable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CommonWebviewActivity extends Activity implements View.OnClickListener,
        ICallback, ServiceStatusCallback, OnServerCallbackListener {
    private WebView webView;

    private TextView rssi_tv, tv_steps, tv_distance,
            tv_calorie, tv_sleep, tv_deep, tv_light, tv_awake, show_result,
            tv_rate, tv_lowest_rate, tv_verage_rate, tv_highest_rate;
    private EditText et_height, et_weight, et_sedentary_period;
    private Button btn_confirm, btn_sync_step, btn_sync_sleep, update_ble,
            read_ble_version, read_ble_battery, set_ble_time,
            bt_sedentary_open, bt_sedentary_close, btn_sync_rate,
            btn_rate_start, btn_rate_stop, unit, push_message_content, open_camera, close_camera, sync_skip;

    private Button today_sports_time, seven_days_sports_time, universal_interface;
    private DataProcessing mDataProcessing;
    private CustomProgressDialog mProgressDialog;
    private UTESQLOperate mySQLOperate;
    // private PedometerUtils mPedometerUtils;
    private WriteCommandToBLE mWriteCommand;
    private Context mContext;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

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

    private final int UNIVERSAL_INTERFACE_SDK_TO_BLE_SUCCESS_MSG = 39;//sdk发送数据到ble完成，并且校验成功，返回状态
    private final int UNIVERSAL_INTERFACE_SDK_TO_BLE_FAIL_MSG = 40;   //sdk发送数据到ble完成，但是校验失败，返回状态
    private final int UNIVERSAL_INTERFACE_BLE_TO_SDK_SUCCESS_MSG = 41;//ble发送数据到sdk完成，并且校验成功，返回数据
    private final int UNIVERSAL_INTERFACE_BLE_TO_SDK_FAIL_MSG = 42;   //ble发送数据到sdk完成，但是校验失败，返回状态


    private final long TIME_OUT_SERVER = 10000;
    private final long TIME_OUT = 120000;
    private boolean isUpdateSuccess = false;
    private int mSteps = 0;
    private float mDistance = 0f;
    private float mCalories = 0, mRunCalories = 0, mWalkCalories = 0;
    private int mRunSteps, mRunDurationTime, mWalkSteps, mWalkDurationTime;
    private float mRunDistance, mWalkDistance;
    private boolean isFirstOpenAPK = false;
    private int currentDay = 1;
    private int lastDay = 0;
    private String currentDayString = "20101202";
    private String lastDayString = "20101201";
    private static final int NEW_DAY_MSG = 3;
    protected static final String TAG = "MainActivity";
    private Updates mUpdates;
    private BLEServiceOperate mBLEServiceOperate;
    private BluetoothLeService mBluetoothLeService;
    // caicai add for sdk
    public static final String EXTRAS_DEVICE_NAME = "device_name";
    public static final String EXTRAS_DEVICE_ADDRESS = "device_address";
    private final int CONNECTED = 1;
    private final int CONNECTING = 2;
    private final int DISCONNECTED = 3;
    private int CURRENT_STATUS = DISCONNECTED;

    private String mDeviceName;
    private String mDeviceAddress;

    private int tempRate = 70;
    private int tempStatus;
    private long mExitTime = 0;

    private Button test_channel;
    private StringBuilder resultBuilder = new StringBuilder();

    private TextView swim_time, swim_stroke_count, swim_calorie,
            tv_low_pressure, tv_high_pressure, skip_time, skip_count, skip_calorie;
    private Button btn_sync_swim, btn_sync_pressure, btn_start_pressure,
            btn_stop_pressure;

    private int high_pressure, low_pressure;
    private int tempBloodPressureStatus;
    private Button ibeacon_command;
    private Spinner setOrReadSpinner, ibeaconStatusSpinner;
    private List<String> ibeaconStatusSpinnerList = new ArrayList<String>();
    private List<String> SetOrReadSpinnerList = new ArrayList<String>();
    private ArrayAdapter<String> aibeaconStatusAdapter;
    private ArrayAdapter<String> setOrReadAdapter;
    private int ibeaconStatus = GlobalVariable.IBEACON_TYPE_UUID;
    private int ibeaconSetOrRead = GlobalVariable.IBEACON_SET;
    private int leftRightHand = GlobalVariable.LEFT_HAND_WEAR;
    private int dialType = GlobalVariable.SHOW_HORIZONTAL_SCREEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_webview);
        mContext = getApplicationContext();
        sp = mContext.getSharedPreferences(GlobalVariable.SettingSP, 0);
        editor = sp.edit();
        mySQLOperate = UTESQLOperate.getInstance(mContext);// 2.2.1版本修改
        mBLEServiceOperate = BLEServiceOperate.getInstance(mContext);
        Log.d("onServiceConnected", "setServiceStatusCallback前 mBLEServiceOperate =" + mBLEServiceOperate);
        mBLEServiceOperate.setServiceStatusCallback(this);
        Log.d("onServiceConnected", "setServiceStatusCallback后 mBLEServiceOperate =" + mBLEServiceOperate);
        // 如果没在搜索界面提前实例BLEServiceOperate的话，下面这4行需要放到OnServiceStatuslt
        mBluetoothLeService = mBLEServiceOperate.getBleService();
        if (mBluetoothLeService != null) {
            mBluetoothLeService.setICallback(this);
        }
        Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mRegisterReceiver();
        mfindViewById();
        mWriteCommand = WriteCommandToBLE.getInstance(mContext);
        mUpdates = Updates.getInstance(mContext);
        mUpdates.setHandler(mHandler);// 获取升级操作信息
        mUpdates.registerBroadcastReceiver();
        mUpdates.setOnServerCallbackListener(this);
        Log.d("onServerDiscorver", "MainActivity_onCreate   mUpdates  ="
                + mUpdates);


        mBLEServiceOperate.connect(mDeviceAddress);

        CURRENT_STATUS = CONNECTING;
        upDateTodaySwimData();
        upDateTodaySkipData();
    }

    private void mRegisterReceiver() {
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(GlobalVariable.READ_BATTERY_ACTION);
        mFilter.addAction(GlobalVariable.READ_BLE_VERSION_ACTION);
        registerReceiver(mReceiver, mFilter);
    }

    private void mfindViewById() {
        webView = (WebView) findViewById(R.id.webview);
        et_height = (EditText) findViewById(R.id.et_height);
        et_weight = (EditText) findViewById(R.id.et_weight);
        et_sedentary_period = (EditText) findViewById(R.id.et_sedentary_period);
//        connect_status = (TextView) findViewById(R.id.connect_status);
        rssi_tv = (TextView) findViewById(R.id.rssi_tv);
        tv_steps = (TextView) findViewById(R.id.tv_steps);
        tv_distance = (TextView) findViewById(R.id.tv_distance);
        tv_calorie = (TextView) findViewById(R.id.tv_calorie);
        tv_sleep = (TextView) findViewById(R.id.tv_sleep);
        tv_deep = (TextView) findViewById(R.id.tv_deep);
        tv_light = (TextView) findViewById(R.id.tv_light);
        tv_awake = (TextView) findViewById(R.id.tv_awake);
        tv_rate = (TextView) findViewById(R.id.tv_rate);
        tv_lowest_rate = (TextView) findViewById(R.id.tv_lowest_rate);
        tv_verage_rate = (TextView) findViewById(R.id.tv_verage_rate);
        tv_highest_rate = (TextView) findViewById(R.id.tv_highest_rate);
        show_result = (TextView) findViewById(R.id.show_result);
        btn_confirm = (Button) findViewById(R.id.btn_confirm);
        bt_sedentary_open = (Button) findViewById(R.id.bt_sedentary_open);
        bt_sedentary_close = (Button) findViewById(R.id.bt_sedentary_close);
        btn_sync_step = (Button) findViewById(R.id.btn_sync_step);
        btn_sync_sleep = (Button) findViewById(R.id.btn_sync_sleep);
        btn_sync_rate = (Button) findViewById(R.id.btn_sync_rate);
        btn_rate_start = (Button) findViewById(R.id.btn_rate_start);
        btn_rate_stop = (Button) findViewById(R.id.btn_rate_stop);
        btn_confirm.setOnClickListener(this);
        bt_sedentary_open.setOnClickListener(this);
        bt_sedentary_close.setOnClickListener(this);
        btn_sync_step.setOnClickListener(this);
        btn_sync_sleep.setOnClickListener(this);
        btn_sync_rate.setOnClickListener(this);
        btn_rate_start.setOnClickListener(this);
        btn_rate_stop.setOnClickListener(this);
        read_ble_version = (Button) findViewById(R.id.read_ble_version);
        read_ble_version.setOnClickListener(this);
        read_ble_battery = (Button) findViewById(R.id.read_ble_battery);
        read_ble_battery.setOnClickListener(this);
        set_ble_time = (Button) findViewById(R.id.set_ble_time);
        set_ble_time.setOnClickListener(this);
        update_ble = (Button) findViewById(R.id.update_ble);
        update_ble.setOnClickListener(this);
        et_height.setText(sp.getString(GlobalVariable.PERSONAGE_HEIGHT, "175"));
        et_weight.setText(sp.getString(GlobalVariable.PERSONAGE_WEIGHT, "60"));

        mDataProcessing = DataProcessing.getInstance(mContext);
        mDataProcessing.setOnStepChangeListener(mOnStepChangeListener);
        mDataProcessing.setOnSleepChangeListener(mOnSleepChangeListener);
        mDataProcessing.setOnRateListener(mOnRateListener);
        mDataProcessing.setOnBloodPressureListener(mOnBloodPressureListener);

        Button open_alarm = (Button) findViewById(R.id.open_alarm);
        open_alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mWriteCommand.sendToSetAlarmCommand(1, GlobalVariable.EVERYDAY,
                        16, 25, true, 5);// 新增最后一个参数，振动次数//2.2.1版本修改
            }
        });
        Button close_alarm = (Button) findViewById(R.id.close_alarm);
        close_alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                // 2.2.1版本修改
                mWriteCommand.sendToSetAlarmCommand(1, GlobalVariable.EVERYDAY,
                        16, 23, false, 5);// 新增最后一个参数，振动次数
            }
        });

        Log.d("onStepHandler", "main_mDataProcessing =" + mDataProcessing);

        unit = (Button) findViewById(R.id.unit);
        unit.setOnClickListener(this);
        test_channel = (Button) findViewById(R.id.test_channel);
        test_channel.setOnClickListener(this);
        push_message_content = (Button) findViewById(R.id.push_message_content);
        push_message_content.setOnClickListener(this);

        btn_sync_swim = (Button) findViewById(R.id.btn_sync_swim);
        btn_sync_swim.setOnClickListener(this);
        swim_time = (TextView) findViewById(R.id.swim_time);
        swim_stroke_count = (TextView) findViewById(R.id.swim_stroke_count);
        swim_calorie = (TextView) findViewById(R.id.swim_calorie);

        tv_low_pressure = (TextView) findViewById(R.id.tv_low_pressure);
        tv_high_pressure = (TextView) findViewById(R.id.tv_high_pressure);
        btn_sync_pressure = (Button) findViewById(R.id.btn_sync_pressure);
        btn_start_pressure = (Button) findViewById(R.id.btn_start_pressure);
        btn_stop_pressure = (Button) findViewById(R.id.btn_stop_pressure);

        btn_sync_pressure.setOnClickListener(this);
        btn_start_pressure.setOnClickListener(this);
        btn_stop_pressure.setOnClickListener(this);
        initIbeacon();
        open_camera = (Button) findViewById(R.id.open_camera);
        close_camera = (Button) findViewById(R.id.close_camera);
        open_camera.setOnClickListener(this);
        close_camera.setOnClickListener(this);

        skip_time = (TextView) findViewById(R.id.skip_time);
        skip_count = (TextView) findViewById(R.id.skip_count);
        skip_calorie = (TextView) findViewById(R.id.skip_calorie);
        sync_skip = (Button) findViewById(R.id.sync_skip);
        sync_skip.setOnClickListener(this);


        today_sports_time = (Button) findViewById(R.id.today_sports_time);
        today_sports_time.setOnClickListener(this);
        seven_days_sports_time = (Button) findViewById(R.id.seven_days_sports_time);
        seven_days_sports_time.setOnClickListener(this);

        universal_interface = (Button) findViewById(R.id.universal_interface);
        universal_interface.setOnClickListener(this);

        setWebView(CommonWebviewActivity.this, webView);
        webView.addJavascriptInterface(new MyJavascriptInterface(this), "injectedObject");
        webView.loadUrl("https://prog.njcool.cn/running_h5/#/?macAddress=" + mDeviceAddress);
    }

    /**
     * 计步监听 在这里更新UI
     */
    private StepChangeListener mOnStepChangeListener = new StepChangeListener() {
        @Override
        public void onStepChange(StepOneDayAllInfo info) {
            if (info != null) {
                mSteps = info.getStep();
                mDistance = info.getDistance();
                mCalories = info.getCalories();

                mRunSteps = info.getRunSteps();
                mRunCalories = info.getRunCalories();
                mRunDistance = info.getRunDistance();
                mRunDurationTime = info.getRunDurationTime();

                mWalkSteps = info.getWalkSteps();
                mWalkCalories = info.getWalkCalories();
                mWalkDistance = info.getWalkDistance();
                mWalkDurationTime = info.getWalkDurationTime();
            }
            Log.d("onStepHandler", "mSteps =" + mSteps + ",mDistance ="
                    + mDistance + ",mCalories =" + mCalories + ",mRunSteps ="
                    + mRunSteps + ",mRunCalories =" + mRunCalories
                    + ",mRunDistance =" + mRunDistance + ",mRunDurationTime ="
                    + mRunDurationTime + ",mWalkSteps =" + mWalkSteps
                    + ",mWalkCalories =" + mWalkCalories + ",mWalkDistance ="
                    + mWalkDistance + ",mWalkDurationTime ="
                    + mWalkDurationTime);
            webView.loadUrl("javascript:updateTodaySteps('" + mSteps + "')");
            mHandler.sendEmptyMessage(UPDATE_STEP_UI_MSG);
        }
    };
    /**
     * 睡眠监听 在这里更新UI
     */
    private SleepChangeListener mOnSleepChangeListener = new SleepChangeListener() {

        @Override
        public void onSleepChange() {
            mHandler.sendEmptyMessage(UPDATE_SLEEP_UI_MSG);
        }

    };

    //current rate
    public void updateCurrentRate(String rate) {
        System.out.println("updateCurrentRate=" + rate);

//        // 无参数调用
//        webView.loadUrl("javascript:javacalljs()");
// 传递参数调用
        getOneDayRateinfo(CalendarUtils.getCalendar(0));
//        upDateTodayStepData();
        webView.loadUrl("javascript:updateCurrentRate('" + rate + "')");
    }

    //currentRate + "," + lowestValue + "," + averageValue + "," + highestValue
    public void updateTodayRate(String rate) {
        System.out.println("updateTodayRate=" + rate);
        // 无参数调用
//        webView.loadUrl("javascript:javacalljs()");
// 传递参数调用
        //  webView.loadUrl("javascript:javacalljswithargs('" + "android传入到网页里的数据，有参" + "')");
    }

    private RateChangeListener mOnRateListener = new RateChangeListener() {

        @Override
        public void onRateChange(int rate, int status) {
            tempRate = rate;
            tempStatus = status;
            Log.i("BluetoothLeService", "Rate_tempRate =" + tempRate);
            mHandler.sendEmptyMessage(UPDATA_REAL_RATE_MSG);
        }
    };
    private BloodPressureChangeListener mOnBloodPressureListener = new BloodPressureChangeListener() {

        @Override
        public void onBloodPressureChange(int hightPressure, int lowPressure,
                                          int status) {
            tempBloodPressureStatus = status;
            high_pressure = hightPressure;
            low_pressure = lowPressure;
            mHandler.sendEmptyMessage(UPDATA_REAL_BLOOD_PRESSURE_MSG);
        }
    };
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RATE_SYNC_FINISH_MSG:
                    UpdateUpdataRateMainUI(CalendarUtils.getCalendar(-1));
//                    Toast.makeText(mContext, "Rate sync finish", 0).show();
                    break;
                case UPDATA_REAL_RATE_MSG:
                    tv_rate.setText(tempRate + "");// 实时跳变
                    updateCurrentRate(tempRate + "");
                    btn_sync_rate.performClick();
                    if (tempStatus == GlobalVariable.RATE_TEST_FINISH) {
                        UpdateUpdataRateMainUI(CalendarUtils.getCalendar(-1));
//                        Toast.makeText(mContext, "Rate test finish", 0).show();
                    }
                    break;
                case GlobalVariable.GET_RSSI_MSG:
                    Bundle bundle = msg.getData();
                    rssi_tv.setText(bundle.getInt(GlobalVariable.EXTRA_RSSI) + "");
                    break;
                case UPDATE_STEP_UI_MSG:
                    updateSteps(mSteps);
                    updateCalories(mCalories);
                    updateDistance(mDistance);

                    Log.d("onStepHandler", "mSteps =" + mSteps + ",mDistance ="
                            + mDistance + ",mCalories =" + mCalories);
                    break;
                case UPDATE_SLEEP_UI_MSG:
                    querySleepInfo();
                    Log.d("getSleepInfo", "UPDATE_SLEEP_UI_MSG");
                    break;
                case NEW_DAY_MSG:
//				mySQLOperate.updateStepSQL();//2.5.2版本删除
                    // mySQLOperate.updateSleepSQL();//2.2.1版本删除
                    mySQLOperate.updateRateSQL();
//				mySQLOperate.isDeleteRefreshTable();//2.5.2版本删除
                    resetValues();
                    break;
                case GlobalVariable.START_PROGRESS_MSG:
                    Log.i(TAG, "(Boolean) msg.obj=" + (Boolean) msg.obj);
                    isUpdateSuccess = (Boolean) msg.obj;
                    Log.i(TAG, "BisUpdateSuccess=" + isUpdateSuccess);
                    startProgressDialog();
                    mHandler.postDelayed(mDialogRunnable, TIME_OUT);
                    break;
                case GlobalVariable.DOWNLOAD_IMG_FAIL_MSG:
                    Toast.makeText(CommonWebviewActivity.this, R.string.download_fail, 1)
                            .show();
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                    if (mDialogRunnable != null)
                        mHandler.removeCallbacks(mDialogRunnable);
                    break;
                case GlobalVariable.DISMISS_UPDATE_BLE_DIALOG_MSG:
                    Log.i(TAG, "(Boolean) msg.obj=" + (Boolean) msg.obj);
                    isUpdateSuccess = (Boolean) msg.obj;
                    Log.i(TAG, "BisUpdateSuccess=" + isUpdateSuccess);
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                    if (mDialogRunnable != null) {
                        mHandler.removeCallbacks(mDialogRunnable);
                    }

                    if (isUpdateSuccess) {
                        Toast.makeText(
                                mContext,
                                getResources().getString(
                                        R.string.ble_update_successful), 0).show();
                    }
                    break;
                case GlobalVariable.SERVER_IS_BUSY_MSG:
                    Toast.makeText(mContext,
                            getResources().getString(R.string.server_is_busy), 0)
                            .show();
                    break;
                case DISCONNECT_MSG:
//                    connect_status.setText(getString(R.string.disconnect));
                    CURRENT_STATUS = DISCONNECTED;
                    Toast.makeText(mContext, "disconnect or connect falie", 0)
                            .show();

                    String lastConnectAddr0 = sp.getString(
                            GlobalVariable.LAST_CONNECT_DEVICE_ADDRESS_SP,
                            "00:00:00:00:00:00");
                    boolean connectResute0 = mBLEServiceOperate
                            .connect(lastConnectAddr0);
                    Log.i(TAG, "connectResute0=" + connectResute0);

                    break;
                case CONNECTED_MSG:
//                    connect_status.setText(getString(R.string.connected));
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
                    Toast.makeText(mContext, "connected", 0).show();
                    btn_rate_start.performClick();
                    break;

                case GlobalVariable.UPDATE_BLE_PROGRESS_MSG: // (新) 增加固件升级进度
                    int schedule = msg.arg1;
                    Log.i("zznkey", "schedule =" + schedule);
                    if (mProgressDialog == null) {
                        startProgressDialog();
                    }
                    mProgressDialog.setSchedule(schedule);
                    break;
                case OPEN_CHANNEL_OK_MSG:// 打开通道OK
                    test_channel.setText(getResources().getString(
                            R.string.open_channel_ok));
                    resultBuilder.append(getResources().getString(
                            R.string.open_channel_ok)
                            + ",");
                    show_result.setText(resultBuilder.toString());

                    mWriteCommand.sendAPDUToBLE(WriteCommandToBLE
                            .hexString2Bytes(testKey1));
                    break;
                case CLOSE_CHANNEL_OK_MSG:// 关闭通道OK
                    test_channel.setText(getResources().getString(
                            R.string.close_channel_ok));
                    resultBuilder.append(getResources().getString(
                            R.string.close_channel_ok)
                            + ",");
                    show_result.setText(resultBuilder.toString());
                    break;
                case TEST_CHANNEL_OK_MSG:// 通道测试OK
                    test_channel.setText(getResources().getString(
                            R.string.test_channel_ok));
                    resultBuilder.append(getResources().getString(
                            R.string.test_channel_ok)
                            + ",");
                    show_result.setText(resultBuilder.toString());
                    mWriteCommand.closeBLEchannel();
                    break;

                case SHOW_SET_PASSWORD_MSG:
                    showPasswordDialog(GlobalVariable.PASSWORD_TYPE_SET);
                    break;
                case SHOW_INPUT_PASSWORD_MSG:
                    showPasswordDialog(GlobalVariable.PASSWORD_TYPE_INPUT);
                    break;
                case SHOW_INPUT_PASSWORD_AGAIN_MSG:
                    showPasswordDialog(GlobalVariable.PASSWORD_TYPE_INPUT_AGAIN);
                    break;
                case OFFLINE_SWIM_SYNC_OK_MSG:
                    upDateTodaySwimData();
                    show_result.setText(mContext.getResources().getString(
                            R.string.sync_swim_finish));
                    Toast.makeText(CommonWebviewActivity.this,
                            getResources().getString(R.string.sync_swim_finish), 0)
                            .show();
                    break;

                case UPDATA_REAL_BLOOD_PRESSURE_MSG:
                    tv_low_pressure.setText(low_pressure + "");// 实时跳变
                    tv_high_pressure.setText(high_pressure + "");// 实时跳变
                    if (tempBloodPressureStatus == GlobalVariable.BLOOD_PRESSURE_TEST_FINISH) {
                        UpdateBloodPressureMainUI(CalendarUtils.getCalendar(0));
                        Toast.makeText(
                                mContext,
                                getResources().getString(R.string.test_pressure_ok),
                                0).show();
                    }
                    break;
                case OFFLINE_BLOOD_PRESSURE_SYNC_OK_MSG:
                    UpdateBloodPressureMainUI(CalendarUtils.getCalendar(0));
                    Toast.makeText(CommonWebviewActivity.this,
                            getResources().getString(R.string.sync_pressure_ok), 0)
                            .show();
                    break;
                case SERVER_CALL_BACK_OK_MSG:
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                    if (mDialogServerRunnable != null) {
                        mHandler.removeCallbacks(mDialogServerRunnable);
                    }
                    String localVersion = sp.getString(
                            GlobalVariable.IMG_LOCAL_VERSION_NAME_SP, "0");
                    int status = mUpdates.getBLEVersionStatus(localVersion);
                    Log.d(TAG, "固件升级 VersionStatus =" + status);
                    if (status == GlobalVariable.OLD_VERSION_STATUS) {
                        updateBleDialog();// update remind
                    } else if (status == GlobalVariable.NEWEST_VERSION_STATUS) {
                        Toast.makeText(mContext,
                                getResources().getString(R.string.ble_is_newest), 0)
                                .show();
                    }/*
                     * else if (status == GlobalVariable.FREQUENT_ACCESS_STATUS) {
                     * Toast.makeText( mContext, getResources().getString(
                     * R.string.frequent_access_server), 0) .show(); }
                     */
                    break;
                case OFFLINE_SKIP_SYNC_OK_MSG:
                    upDateTodaySkipData();
                    Toast.makeText(CommonWebviewActivity.this,
                            getResources().getString(R.string.sync_skip_finish), 0)
                            .show();
                    show_result.setText(mContext.getResources().getString(
                            R.string.sync_skip_finish));
                    break;
                case test_mag1:
                    Toast.makeText(CommonWebviewActivity.this, "表示按键1短按下，用来做切换屏,表示切换了手环屏幕", 0)//
                            .show();
                    show_result.setText("表示按键1短按下，用来做切换屏,表示切换了手环屏幕");
                    break;
                case test_mag2:
                    Toast.makeText(CommonWebviewActivity.this, "表示按键3短按下，用来做一键SOS", 0)
                            .show();
                    show_result.setText("表示按键3短按下，用来做一键SOS");
                    break;
                case OFFLINE_STEP_SYNC_OK_MSG:
                    Toast.makeText(CommonWebviewActivity.this, "计步数据同步成功", 0)
                            .show();
                    show_result.setText("计步数据同步成功");
                    break;
                case UPDATE_SPORTS_TIME_DETAILS_MSG:
                    show_result.setText(resultBuilder.toString());
                    break;
                case UNIVERSAL_INTERFACE_SDK_TO_BLE_SUCCESS_MSG:
                    show_result.setText("sdk发送数据到ble完成，并且校验成功，返回状态");
                    break;
                case UNIVERSAL_INTERFACE_SDK_TO_BLE_FAIL_MSG:
                    show_result.setText("sdk发送数据到ble完成，但是校验失败，返回状态");
                    break;
                case UNIVERSAL_INTERFACE_BLE_TO_SDK_SUCCESS_MSG:
                    show_result.setText("ble发送数据到sdk完成，并且校验成功，返回数据");
                    break;
                case UNIVERSAL_INTERFACE_BLE_TO_SDK_FAIL_MSG:
                    show_result.setText("ble发送数据到sdk完成，但是校验失败，返回状态");
                    break;

                default:
                    break;
            }
        }
    };

    /*
     * 获取一天最新心率值、最高、最低、平均心率值
     */
    private void UpdateUpdataRateMainUI(String calendar) {
        // UTESQLOperate mySQLOperate = UTESQLOperate.getInstance(mContext);
        RateOneDayInfo mRateOneDayInfo = mySQLOperate
                .queryRateOneDayMainInfo(calendar);
        if (mRateOneDayInfo != null) {
            int currentRate = mRateOneDayInfo.getCurrentRate();
            int lowestValue = mRateOneDayInfo.getLowestRate();
            int averageValue = mRateOneDayInfo.getVerageRate();
            int highestValue = mRateOneDayInfo.getHighestRate();
            // current_rate.setText(currentRate + "");
            if (currentRate == 0) {
                tv_rate.setText("--");
            } else {
                tv_rate.setText(currentRate + "");
            }
            if (lowestValue == 0) {
                tv_lowest_rate.setText("--");
            } else {
                tv_lowest_rate.setText(lowestValue + "");
            }
            if (averageValue == 0) {
                tv_verage_rate.setText("--");
            } else {
                tv_verage_rate.setText(averageValue + "");
            }
            if (highestValue == 0) {
                tv_highest_rate.setText("--");
            } else {
                tv_highest_rate.setText(highestValue + "");
            }
            updateTodayRate(currentRate + "," + lowestValue + "," + averageValue + "," + highestValue);
        } else {
            tv_rate.setText("--");
            updateTodayRate("");
        }
    }

    /*
     * 获取一天各测试时间点和心率值
     */
    private void getOneDayRateinfo(String calendar) {
        // UTESQLOperate mySQLOperate = UTESQLOperate.getInstance(mContext);
        List<RateOneDayInfo> mRateLastDayInfoList = mySQLOperate
                .queryRateOneDayDetailInfo(CalendarUtils.getCalendar(-1));
        List<RateOneDayInfo> mRateOneDayInfoList = mySQLOperate
                .queryRateOneDayDetailInfo(calendar);
        JSONArray json = new JSONArray();
        if (mRateLastDayInfoList != null && mRateLastDayInfoList.size() > 0) {
            int size = mRateLastDayInfoList.size();
            int[] rateValue = new int[size];
            int[] timeArray = new int[size];

            for (int i = 0; i < size; i++) {
                rateValue[i] = mRateLastDayInfoList.get(i).getRate();
                timeArray[i] = mRateLastDayInfoList.get(i).getTime();
                Log.d(TAG, "rateValue[" + i + "]=" + rateValue[i]
                        + "timeArray[" + i + "]=" + timeArray[i]);
                try {
                    JSONObject jo = new JSONObject();
                    jo.put("date", CalendarUtils.getCalendar(-1));
                    jo.put("rateValue", rateValue[i]);
                    jo.put("timeArray", timeArray[i]);
                    json.put(jo);
                }catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        } else {

        }
        if (mRateOneDayInfoList != null && mRateOneDayInfoList.size() > 0) {
            int size = mRateOneDayInfoList.size();
            int[] rateValue = new int[size];
            int[] timeArray = new int[size];

            for (int i = 0; i < size; i++) {
                rateValue[i] = mRateOneDayInfoList.get(i).getRate();
                timeArray[i] = mRateOneDayInfoList.get(i).getTime();
                Log.d(TAG, "rateValue[" + i + "]=" + rateValue[i]
                        + "timeArray[" + i + "]=" + timeArray[i]);
                try {
                    JSONObject jo = new JSONObject();
                    jo.put("date", CalendarUtils.getCalendar(0));
                    jo.put("rateValue", rateValue[i]);
                    jo.put("timeArray", timeArray[i]);
                    json.put(jo);
                }catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        } else {

        }
        webView.loadUrl("javascript:updateOneDayRate('" + json + "')");
    }

    private void startProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = CustomProgressDialog
                    .createDialog(CommonWebviewActivity.this);
            mProgressDialog.setMessage(getResources().getString(
                    R.string.ble_updating));
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }

    private Runnable mDialogRunnable = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            // mDownloadButton.setText(R.string.suota_update_succeed);
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
            mHandler.removeCallbacks(mDialogRunnable);
            if (!isUpdateSuccess) {
                Toast.makeText(CommonWebviewActivity.this,
                        getResources().getString(R.string.ble_fail_update), 0)
                        .show();
                mUpdates.clearUpdateSetting();
            } else {
                isUpdateSuccess = false;
                Toast.makeText(
                        CommonWebviewActivity.this,
                        getResources()
                                .getString(R.string.ble_update_successful), 0)
                        .show();
            }

        }
    };
    private Runnable mDialogServerRunnable = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            // mDownloadButton.setText(R.string.suota_update_succeed);
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
            mHandler.removeCallbacks(mDialogServerRunnable);
            Toast.makeText(CommonWebviewActivity.this,
                    getResources().getString(R.string.server_is_busy), 0)
                    .show();
        }
    };

    private void updateSteps(int steps) {
        Log.d("upDateSteps", "steps =" + steps);
        String stepString = "0";
        if (steps <= 0) {
        } else {
            stepString = "" + steps;
        }

        tv_steps.setText(stepString);

    }


    private void updateCalories(float mCalories) {
        if (mCalories <= 0) {
            tv_calorie.setText(mContext.getResources().getString(
                    R.string.zero_kilocalorie));
        } else {
            tv_calorie.setText("" + mCalories + " "
                    + mContext.getResources().getString(R.string.kilocalorie));
        }

    }

    private void updateDistance(float mDistance) {
        if (mDistance < 0.01) {
            tv_distance.setText(mContext.getResources().getString(
                    R.string.zero_kilometers));

        } else {
            tv_distance.setText(mDistance + " "
                    + mContext.getResources().getString(R.string.kilometers));
        }
//		else if (mDistance >= 100) {
//			tv_distance.setText(("" + mDistance).substring(0, 3) + " "
//					+ mContext.getResources().getString(R.string.kilometers));
//		} else {
//			tv_distance.setText(("" + (mDistance + 0.000001f)).substring(0, 4)
//					+ " "
//					+ mContext.getResources().getString(R.string.kilometers));
//		}

    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        boolean ble_connecte = sp.getBoolean(GlobalVariable.BLE_CONNECTED_SP,
                false);
//        if (ble_connecte) {
//            connect_status.setText(getString(R.string.connected));
//        } else {
//            connect_status.setText(getString(R.string.disconnect));
//        }
        JudgeNewDayWhenResume();

    }

    private void JudgeNewDayWhenResume() {
        isFirstOpenAPK = sp.getBoolean(GlobalVariable.FIRST_OPEN_APK, true);
        editor.putBoolean(GlobalVariable.FIRST_OPEN_APK, false);
        editor.commit();
        lastDay = sp.getInt(GlobalVariable.LAST_DAY_NUMBER_SP, 0);
        lastDayString = sp.getString(GlobalVariable.LAST_DAY_CALLENDAR_SP,
                "20101201");
        Calendar c = Calendar.getInstance();
        currentDay = c.get(Calendar.DAY_OF_YEAR);
        currentDayString = CalendarUtils.getCalendar(0);

        if (isFirstOpenAPK) {
            lastDay = currentDay;
            lastDayString = currentDayString;
            editor = sp.edit();
            editor.putInt(GlobalVariable.LAST_DAY_NUMBER_SP, lastDay);
            editor.putString(GlobalVariable.LAST_DAY_CALLENDAR_SP,
                    lastDayString);
            editor.commit();
        } else {

            if (currentDay != lastDay) {
                if ((lastDay + 1) == currentDay || currentDay == 1) { // 连续的日期
                    mHandler.sendEmptyMessage(NEW_DAY_MSG);
                } else {
//					mySQLOperate.insertLastDayStepSQL(lastDayString);//2.5.2版本删除
                    // mySQLOperate.updateSleepSQL();//2.2.1版本删除
                    resetValues();
                }
                lastDay = currentDay;
                lastDayString = currentDayString;
                editor.putInt(GlobalVariable.LAST_DAY_NUMBER_SP, lastDay);
                editor.putString(GlobalVariable.LAST_DAY_CALLENDAR_SP,
                        lastDayString);
                editor.commit();
            } else {
                Log.d("b1offline", "currentDay == lastDay");
            }
        }

    }

    private void resetValues() {
        editor.putInt(GlobalVariable.YC_PED_UNFINISH_HOUR_STEP_SP, 0);
        editor.putInt(GlobalVariable.YC_PED_UNFINISH_HOUR_VALUE_SP, 0);
        editor.putInt(GlobalVariable.YC_PED_LAST_HOUR_STEP_SP, 0);
        editor.commit();
        tv_steps.setText("0");
        tv_calorie.setText(mContext.getResources().getString(
                R.string.zero_kilocalorie));
        tv_distance.setText(mContext.getResources().getString(
                R.string.zero_kilometers));
        tv_sleep.setText("0");
        tv_deep.setText(mContext.getResources().getString(
                R.string.zero_hour_zero_minute));
        tv_light.setText(mContext.getResources().getString(
                R.string.zero_hour_zero_minute));
        tv_awake.setText(mContext.getResources().getString(R.string.zero_count));

        tv_rate.setText("--");
        tv_lowest_rate.setText("--");
        tv_verage_rate.setText("--");
        tv_highest_rate.setText("--");
    }

    @Override
    public void onClick(View v) {
        boolean ble_connecte = sp.getBoolean(GlobalVariable.BLE_CONNECTED_SP,
                false);
        switch (v.getId()) {
            case R.id.btn_confirm:

                if (ble_connecte) {
                    String height = et_height.getText().toString();
                    String weight = et_weight.getText().toString();
                    if (height.equals("") || weight.equals("")) {
                        Toast.makeText(mContext, "身高或体重不能为空", 0).show();
                    } else {

                        int Height = Integer.valueOf(height);
                        int Weight = Integer.valueOf(weight);
                        mWriteCommand.sendStepLenAndWeightToBLE(Height, Weight, 5,
                                10000, true, true, 150, true, 20, false);
                        // 设置步长，体重，灭屏时间5s,目标步数，抬手亮屏开关true为开，false为关；最高心率提醒，true为开，false为关；
                        //设置最高心率提醒的值；性别true为男，false为女；20为年龄（范围0~255）；最后一个参数手环防丢功能，true为开启，false为关闭
                    }
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
//			List<RateOneDayInfo> mRateOneDayInfoList = new ArrayList<RateOneDayInfo>();
//			mRateOneDayInfoList =mySQLOperate.queryRateOneDayDetailInfo(CalendarUtils.getCalendar(0));
//			Log.d(TAG, "mRateOneDayInfoList ="+mRateOneDayInfoList);
//			if (mRateOneDayInfoList!=null) {
//				for (int i = 0; i < mRateOneDayInfoList.size(); i++) {
//					int time = mRateOneDayInfoList.get(i).getTime();
//					int rate = mRateOneDayInfoList.get(i).getRate();
//					Log.d(TAG, "mRateOneDayInfoList time ="+time+",rate ="+rate);
//				}
//			}else {
//
//			}
//			RateOneDayInfo mRateOneDayInfo = null;
//			mRateOneDayInfo =mySQLOperate.queryRateOneDayMainInfo(CalendarUtils.getCalendar(0));
//			if (mRateOneDayInfo!=null) {
//				 int lowestRate;
//				 int verageRate;
//				 int highestRate;
//				 int currentRate;
//			}
//			List<StepOneDayAllInfo> list = mySQLOperate.queryRunWalkAllDay();
//			if (list != null) {
//				for (int i = 0; i < list.size(); i++) {
//					String calendar = list.get(i).getCalendar();
//					int step = list.get(i).getStep();
//					int runSteps = list.get(i).getRunSteps();
//					int walkSteps = list.get(i).getWalkSteps();
//					Log.d(TAG, "queryRunWalkAllDay calendar =" + calendar
//							+ ",step =" + step + ",runSteps =" + runSteps
//							+ ",walkSteps =" + walkSteps);
//				}
//			}
                break;
            case R.id.bt_sedentary_open:
                String period = et_sedentary_period.getText().toString();
                if (period.equals("")) {
                    Toast.makeText(mContext, "Please input remind peroid", 0)
                            .show();
                } else {
                    int period_time = Integer.valueOf(period);
                    if (period_time < 30) {
                        Toast.makeText(
                                mContext,
                                "Please make sure period_time more than 30 minutes",
                                0).show();
                    } else {
                        if (ble_connecte) {
                            mWriteCommand.sendSedentaryRemindCommand(
                                    GlobalVariable.OPEN_SEDENTARY_REMIND,
                                    period_time);
                        } else {
                            Toast.makeText(mContext,
                                    getString(R.string.disconnect),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }

//			StepOneDayAllInfo mStepInfo =mySQLOperate.queryRunWalkInfo("20171106");
//			if (mStepInfo != null) {
//				String calendar ="";
//				 int step = mStepInfo.getStep();
//				 int mCaloriesValue = mStepInfo.getCalories();
//				 float distance = mStepInfo.getDistance();
//				// 跑步
//				int runSteps = mStepInfo.getRunSteps();
//				int runCalories = mStepInfo.getRunCalories();
//				float runDistance = mStepInfo.getRunDistance();
//				int runDurationTime = mStepInfo.getRunDurationTime();
//				String runHourDetails = mStepInfo.getRunHourDetails();
//				// 走路
//				 int walkSteps = mStepInfo.getWalkSteps();
//				 int walkCalories = mStepInfo.getWalkCalories();
//				 float walkDistance = mStepInfo.getWalkDistance();
//				 int walkDurationTime = mStepInfo.getWalkDurationTime();
//				 String walkHourDetails = mStepInfo.getWalkHourDetails();
//				int totalSteps = runSteps + walkSteps;
//
//
//				Log.d(TAG, "queryRunWalkInfo calendar ="+calendar+",step ="+step+",mCaloriesValue ="+mCaloriesValue+",distance ="+distance);
//				Log.d(TAG, "queryRunWalkInfo runSteps ="+runSteps+",runCalories ="+runCalories+",runDistance ="+runDistance+",runDurationTime ="+runDurationTime);
//				Log.d(TAG, "queryRunWalkInfo walkSteps ="+walkSteps+",walkCalories ="+walkCalories+",walkDistance ="+walkDistance+",walkDurationTime ="+walkDurationTime);
//			}
                break;
            case R.id.bt_sedentary_close:
                if (ble_connecte) {
                    mWriteCommand.sendSedentaryRemindCommand(
                            GlobalVariable.CLOSE_SEDENTARY_REMIND, 0);
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_sync_step:
                if (ble_connecte) {
                    mWriteCommand.syncAllStepData();
//				mWriteCommand.syncAllSwimData();
//				mWriteCommand.syncAllSkipData();
//				mySQLOperate.querySkipDayInfo("20170629");
//				mySQLOperate.querySwimDayInfo("20170629");
//				mySQLOperate.queryRunWalkInfo("20170629");
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.btn_sync_sleep:
                if (ble_connecte) {
                    mWriteCommand.syncAllSleepData();
                    // mWriteCommand.syncWeatherToBLE(mContext, "桂林市"); //测试天气接口
                    // mWriteCommand.syncWeatherToBLE(mContext, "深圳市");
//				mWriteCommand.queryDialMode();//测试查询表盘切换方式
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_sync_rate:
                if (ble_connecte) {
                    mWriteCommand.syncAllRateData();
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_rate_start:
                if (ble_connecte) {
                    mWriteCommand
                            .sendRateTestCommand(GlobalVariable.RATE_TEST_START);
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_rate_stop:
                if (ble_connecte) {
                    mWriteCommand
                            .sendRateTestCommand(GlobalVariable.RATE_TEST_STOP);
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.read_ble_version:
                if (ble_connecte) {
                    mWriteCommand.sendToReadBLEVersion(); // 发送请求BLE版本号
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }


                break;
            case R.id.read_ble_battery:
//			StepOneDayAllInfo allInfo = mySQLOperate
//					.queryRunWalkInfo("20170724");
//			Log.d(TAG, "allInfo =" + allInfo);
//			if (allInfo != null) {
//				// 走路跑步不区分
//				int steps = allInfo.getStep();
//				int calories = allInfo.getCalories();
//				float distance = allInfo.getDistance();
//				// 跑步
//				int runSteps = allInfo.getRunSteps();
//				int runCalories = allInfo.getRunCalories();
//				float runDistance = allInfo.getRunDistance();
//				int runDurationTime = allInfo.getRunDurationTime();
//				// 走路
//				int walkSteps = allInfo.getWalkSteps();
//				int walkCalories = allInfo.getWalkCalories();
//				float walkDistance = allInfo.getWalkDistance();
//				int walkDurationTime = allInfo.getWalkDurationTime();
//				Log.d(TAG, " steps =" + steps + ",calories =" + calories
//						+ ",distance" + distance);
//				Log.d(TAG, " runSteps =" + runSteps + ",runCalories ="
//						+ runCalories + ",runDistance" + runDistance
//						+ ",runDurationTime=" + runDurationTime);
//				Log.d(TAG, " walkSteps =" + walkSteps + ",walkCalories ="
//						+ walkCalories + ",walkDistance" + walkDistance
//						+ ",walkDurationTime=" + walkDurationTime);
//				int hourStep = 0;
//				int time = 0;
//				int startTime =0;
//				int endTime =0;
//				int useTime =0;
//				ArrayList<StepOneHourInfo> hourInfos = allInfo
//						.getStepOneHourArrayInfo();
//				for (int i = 0; i < hourInfos.size(); i++) {
//					time = hourInfos.get(i).getTime();
//					hourStep = hourInfos.get(i).getStep();
//					Log.d(TAG, "走路跑步不区分 time =" + time + ",hourStep ="
//							+ hourStep);
//				}
//				ArrayList<StepRunHourInfo> hourRunInfos = allInfo
//						.getStepRunHourArrayInfo();
//				for (int i = 0; i < hourRunInfos.size(); i++) {
//					time = hourRunInfos.get(i).getTime();
//					hourStep = hourRunInfos.get(i).getRunSteps();
//					startTime =hourRunInfos.get(i).getStartRunTime();
//					endTime =hourRunInfos.get(i).getEndRunTime();
//					useTime =hourRunInfos.get(i).getRunDurationTime();
//					Log.d(TAG, " 跑步 time =" + time + ",hourStep =" + hourStep+ ",startTime =" + startTime+ ",endTime =" + endTime+ ",useTime =" + useTime);
//
//				}
//				ArrayList<StepWalkHourInfo> hourWalkInfos = allInfo
//						.getStepWalkHourArrayInfo();
//				for (int i = 0; i < hourWalkInfos.size(); i++) {
//					time = hourWalkInfos.get(i).getTime();
//					hourStep = hourWalkInfos.get(i).getWalkSteps();
//					startTime =hourWalkInfos.get(i).getStartWalkTime();
//					endTime =hourWalkInfos.get(i).getEndWalkTime();
//					useTime =hourWalkInfos.get(i).getWalkDurationTime();
//					Log.d(TAG, " 走路 time =" + time + ",hourStep =" + hourStep+ ",startTime =" + startTime+ ",endTime =" + endTime+ ",useTime =" + useTime);
//
//				}
//			} else {
//
//			}

                if (ble_connecte) {
                    mWriteCommand.sendToReadBLEBattery();// 请求获取电量指令
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.set_ble_time:
                if (ble_connecte) {
                    // mWriteCommand.sendDisturbToBle(true, true, true, false, 0, 0,
                    // 0, 0);
                    mWriteCommand.syncBLETime();
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.update_ble:
                boolean ble_connected = sp.getBoolean(
                        GlobalVariable.BLE_CONNECTED_SP, false);

                if (ble_connected) {
                    mWriteCommand.queryDeviceFearture();
                    if (isNetworkAvailable(mContext)) {
                        String localVersion = sp.getString(
                                GlobalVariable.IMG_LOCAL_VERSION_NAME_SP, "0");
                        if (!localVersion.equals("0")) {
                            int status = mUpdates
                                    .accessServerersionStatus(localVersion);
                            if (status == GlobalVariable.FREQUENT_ACCESS_STATUS) {
                                Toast.makeText(
                                        mContext,
                                        getResources().getString(
                                                R.string.frequent_access_server), 0)
                                        .show();
                            } else {
                                startProgressDialog();
                                mHandler.postDelayed(mDialogServerRunnable,
                                        TIME_OUT_SERVER);
                            }

                            // int status = mUpdates
                            // .accessServerersionStatus(localVersion);
                            // Log.d(TAG, "固件升级 VersionStatus =" + status);
                            // if (status == GlobalVariable.OLD_VERSION_STATUS) {
                            // updateBleDialog();// update remind
                            // } else if (status ==
                            // GlobalVariable.NEWEST_VERSION_STATUS) {
                            // Toast.makeText(
                            // mContext,
                            // getResources().getString(
                            // R.string.ble_is_newest), 0).show();
                            // } else if (status ==
                            // GlobalVariable.FREQUENT_ACCESS_STATUS) {
                            // Toast.makeText(
                            // mContext,
                            // getResources().getString(
                            // R.string.frequent_access_server), 0)
                            // .show();
                            // }
                        } else {
                            Toast.makeText(
                                    mContext,
                                    getResources().getString(
                                            R.string.read_ble_version_first), 0)
                                    .show();
                        }
                    } else {
                        Toast.makeText(
                                mContext,
                                getResources().getString(
                                        R.string.confire_is_network_available), 0)
                                .show();

                    }
                } else {
                    Toast.makeText(
                            mContext,
                            getResources().getString(
                                    R.string.please_connect_bracelet), 0).show();
                }
                break;
            // case 11:
            // mWriteCommand.sendToSetAlarmCommand(1, (byte) 33, 12, 22, true);
            // break;

            case R.id.unit:
                boolean ble_connected3 = sp.getBoolean(
                        GlobalVariable.BLE_CONNECTED_SP, false);
                if (ble_connected3) {
                    if (unit.getText()
                            .toString()
                            .equals(getResources()
                                    .getString(R.string.metric_system))) {
                        editor.putBoolean(GlobalVariable.IS_METRIC_UNIT_SP, true);
                        editor.commit();
                        mWriteCommand.sendUnitAndHourFormatToBLE();
                        unit.setText(getResources().getString(R.string.inch_system));
                    } else {
                        editor.putBoolean(GlobalVariable.IS_METRIC_UNIT_SP, false);
                        editor.commit();
                        mWriteCommand.sendUnitAndHourFormatToBLE();//
                        // mWriteCommand.sendUnitAndHourFormatToBLE(unitType,
                        // hourFormat);//也可以传如参数设置
                        // unitType == GlobalVariable.UNIT_TYPE_METRICE 公制单位
                        // unitType == GlobalVariable.UNIT_TYPE_IMPERIAL 英制单位
                        // hourFormat == GlobalVariable.HOUR_FORMAT_24 24小时制
                        // hourFormat == GlobalVariable.HOUR_FORMAT_12 12小时制
                        unit.setText(getResources().getString(
                                R.string.metric_system));
                    }
                } else {
                    Toast.makeText(
                            mContext,
                            getResources().getString(
                                    R.string.please_connect_bracelet), 0).show();
                }

                break;
            case R.id.test_channel:
                boolean ble_connected4 = sp.getBoolean(
                        GlobalVariable.BLE_CONNECTED_SP, false);
                if (ble_connected4) {
                    if (test_channel
                            .getText()
                            .toString()
                            .equals(getResources().getString(R.string.test_channel))
                            || test_channel
                            .getText()
                            .toString()
                            .equals(getResources().getString(
                                    R.string.test_channel_ok))
                            || test_channel
                            .getText()
                            .toString()
                            .equals(getResources().getString(
                                    R.string.close_channel_ok))) {
                        resultBuilder = new StringBuilder();
                        mWriteCommand.openBLEchannel();
                    } else {
                        Toast.makeText(
                                mContext,
                                getResources().getString(R.string.channel_testting),
                                0).show();
                    }
                } else {
                    Toast.makeText(
                            mContext,
                            getResources().getString(
                                    R.string.please_connect_bracelet), 0).show();
                }

                break;
            case R.id.push_message_content:
                if (ble_connecte) {
                    String pushContent = getResources().getString(
                            R.string.push_message_content);// 推送的内容
                    mWriteCommand
                            .sendTextToBle(pushContent, GlobalVariable.TYPE_QQ);

                    // mWriteCommand.sendTextToBle(pushContent,GlobalVariable.TYPE_WECHAT);
                    // editor.putString(GlobalVariable.SMS_RECEIVED_NUMBER,
                    // "18045811234");//保存推送短信的号码,短信推送时，必须
                    // editor.commit();
                    // mWriteCommand.sendTextToBle(pushContent,GlobalVariable.TYPE_SMS);
                    // mWriteCommand.sendTextToBle(pushContent,GlobalVariable.TYPE_PHONE);

                    show_result.setText(pushContent);
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_sync_swim:
                if (ble_connecte) {
                    if (GetFunctionList.isSupportFunction(mContext, GlobalVariable.IS_SUPPORT_SWIMMING)) {
                        mWriteCommand.syncAllSwimData();
                        show_result.setText(mContext.getResources().getString(
                                R.string.sync_swim));
                    } else {
                        Toast.makeText(mContext, getString(R.string.not_support_swim),
                                Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_sync_pressure:
                if (ble_connecte) {
                    mWriteCommand.syncAllBloodPressureData();
                    show_result.setText(mContext.getResources().getString(
                            R.string.sync_pressure));
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_start_pressure:
                if (ble_connecte) {
                    mWriteCommand
                            .sendBloodPressureTestCommand(GlobalVariable.BLOOD_PRESSURE_TEST_START);
                    show_result.setText(mContext.getResources().getString(
                            R.string.start_pressure));
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }


                break;
            case R.id.btn_stop_pressure:
                if (ble_connecte) {
                    mWriteCommand
                            .sendBloodPressureTestCommand(GlobalVariable.BLOOD_PRESSURE_TEST_STOP);
                    show_result.setText(mContext.getResources().getString(
                            R.string.stop_pressure));
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.ibeacon_command:
                if (ble_connecte) {
                    if (GetFunctionList.isSupportFunction(mContext,
                            GlobalVariable.IS_SUPPORT_IBEACON)) {// 先判断是否支持ibeacon功能
                        switch (ibeaconSetOrRead) {
                            case GlobalVariable.IBEACON_SET:// 设置
                                switch (ibeaconStatus) {
                                    case GlobalVariable.IBEACON_TYPE_UUID:
                                        // 注意：在ibeacon
                                        // 中，UUID的数据长度固定为16byte的ASIIC,，如30313233343536373031323334353637
                                        mWriteCommand.sendIbeaconSetCommand(
                                                "30313233343536373031323334353637",
                                                GlobalVariable.IBEACON_TYPE_UUID);// 设置UUID
                                        break;
                                    case GlobalVariable.IBEACON_TYPE_MAJOR:
                                        // //major和minor固定长度为2byte的数字，如0224
                                        mWriteCommand.sendIbeaconSetCommand("0224",
                                                GlobalVariable.IBEACON_TYPE_MAJOR);// 设置major
                                        break;
                                    case GlobalVariable.IBEACON_TYPE_MINOR:
                                        // //major和minor固定长度为2byte的数字，如0424
                                        mWriteCommand.sendIbeaconSetCommand("3424",
                                                GlobalVariable.IBEACON_TYPE_MINOR);// 设置minor
                                        break;
                                    case GlobalVariable.IBEACON_TYPE_DEVICE_NAME:
                                        // //Device
                                        // name的长度范必须大于0小于14byte的ASIIC，如3031323334353637303132333435
                                        mWriteCommand.sendIbeaconSetCommand(
                                                "3031323334353637303132333435",
                                                GlobalVariable.IBEACON_TYPE_DEVICE_NAME);// 设置蓝牙device
                                        // name
                                        break;
                                    case GlobalVariable.IBEACON_TYPE_TX_POWER:
                                        // TX_POWER（数据范围 1~0xfe，由客户设置)；
                                        mWriteCommand.sendIbeaconSetCommand("78", GlobalVariable.IBEACON_TYPE_TX_POWER);// 设置TX_POWER
                                        break;
                                    case GlobalVariable.IBEACON_TYPE_ADVERTISING_INTERVAL:
                                        // advertising interval（数据范围1~20，单位为100ms，默认800ms每次）
                                        mWriteCommand.sendIbeaconSetCommand("14", GlobalVariable.IBEACON_TYPE_ADVERTISING_INTERVAL);// 设置advertising interval
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            case GlobalVariable.IBEACON_GET:// 获取
                                switch (ibeaconStatus) {
                                    case GlobalVariable.IBEACON_TYPE_UUID:
                                        // //获取UUID
                                        mWriteCommand
                                                .sendIbeaconGetCommand(GlobalVariable.IBEACON_TYPE_UUID);
                                        break;
                                    case GlobalVariable.IBEACON_TYPE_MAJOR:
                                        // //获取major
                                        mWriteCommand
                                                .sendIbeaconGetCommand(GlobalVariable.IBEACON_TYPE_MAJOR);
                                        break;
                                    case GlobalVariable.IBEACON_TYPE_MINOR:
                                        // 获取minor
                                        mWriteCommand
                                                .sendIbeaconGetCommand(GlobalVariable.IBEACON_TYPE_MINOR);
                                        break;
                                    case GlobalVariable.IBEACON_TYPE_DEVICE_NAME:
                                        // //获取device name
                                        mWriteCommand
                                                .sendIbeaconGetCommand(GlobalVariable.IBEACON_TYPE_DEVICE_NAME);
                                        break;
                                    case GlobalVariable.IBEACON_TYPE_TX_POWER:
                                        // //获取TX_POWER
                                        mWriteCommand
                                                .sendIbeaconGetCommand(GlobalVariable.IBEACON_TYPE_TX_POWER);
                                        break;
                                    case GlobalVariable.IBEACON_TYPE_ADVERTISING_INTERVAL:
                                        // //获取advertising interval
                                        mWriteCommand
                                                .sendIbeaconGetCommand(GlobalVariable.IBEACON_TYPE_ADVERTISING_INTERVAL);
                                        break;
                                    default:
                                        break;
                                }
                                break;

                            default:
                                break;
                        }
                    } else {
                        Toast.makeText(mContext, "不支持ibeacon功能", Toast.LENGTH_SHORT)
                                .show();
                    }

                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.open_camera:
                if (ble_connecte) {
                    mWriteCommand.NotifyBLECameraOpenOrNot(true);
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.close_camera:
                if (ble_connecte) {
                    mWriteCommand.NotifyBLECameraOpenOrNot(false);
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.sync_skip:
                if (ble_connecte) {
                    if (GetFunctionList.isSupportFunction(mContext, GlobalVariable.IS_SUPPORT_SKIP)) {
                        mWriteCommand.syncAllSkipData();
                        show_result.setText(mContext.getResources().getString(
                                R.string.sync_skip));
                    } else {
                        Toast.makeText(mContext, getString(R.string.not_support_skip),
                                Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.today_sports_time:
                if (ble_connecte) {
                    resultBuilder = new StringBuilder();
                    resultBuilder.append(getString(R.string.today_sports_time) + ":");
                    mWriteCommand.sendKeyToGetSportsTime(GlobalVariable.SPORTS_TIME_TODAY);
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.seven_days_sports_time:
                if (ble_connecte) {
                    resultBuilder = new StringBuilder();
                    resultBuilder.append(getString(R.string.seven_days_sports_time) + ":");
                    mWriteCommand.sendKeyToGetSportsTime(GlobalVariable.SPORTS_TIME_HISTORY_DAY);
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.universal_interface:
                if (ble_connecte) {
                    mWriteCommand.universalInterface(WriteCommandToBLE
                            .hexString2Bytes(universalKey));
                } else {
                    Toast.makeText(mContext, getString(R.string.disconnect),
                            Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if (CURRENT_STATUS == CONNECTING) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("设备连接中，强制退出将关闭蓝牙，确认吗？");
            builder.setTitle(mContext.getResources().getString(R.string.tip));
            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
                                    .getDefaultAdapter();
                            if (mBluetoothAdapter == null) {
                                finish();
                            }
                            if (mBluetoothAdapter.isEnabled()) {
                                mBluetoothAdapter.disable();// 关闭蓝牙
                            }
                            finish();
                        }
                    });
            builder.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.create().show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean updateBleDialog() {

        final AlertDialog alert = new AlertDialog.Builder(this).setCancelable(
                false).create();
        alert.show();
        window = alert.getWindow();
        window.setContentView(R.layout.update_dialog_layout);
        Button btn_yes = (Button) window.findViewById(R.id.btn_yes);
        Button btn_no = (Button) window.findViewById(R.id.btn_no);
        TextView update_warn_tv = (TextView) window
                .findViewById(R.id.update_warn_tv);
        update_warn_tv.setText(getResources().getString(
                R.string.find_new_version_ble));

        btn_yes.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isNetworkAvailable(mContext)) {
                    mUpdates.startUpdateBLE();
                } else {
                    Toast.makeText(
                            mContext,
                            getResources().getString(
                                    R.string.confire_is_network_available), 0)
                            .show();
                }

                alert.dismiss();
            }
        });
        btn_no.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mUpdates.clearUpdateSetting();
                alert.dismiss();
            }

        });
        return false;

    }

    /**
     * 获取某一天睡眠详细，并更新睡眠UI CalendarUtils.getCalendar(0)代表今天，也可写成"20141101"
     * CalendarUtils.getCalendar(-1)代表昨天，也可写成"20141031"
     * CalendarUtils.getCalendar(-2)代表前天，也可写成"20141030" 以此类推
     */
    private void querySleepInfo() {
        SleepTimeInfo sleepTimeInfo = mySQLOperate.querySleepInfo(CalendarUtils
                .getCalendar(0));
        int deepTime, lightTime, awakeCount, sleepTotalTime;
        if (sleepTimeInfo != null) {
            deepTime = sleepTimeInfo.getDeepTime();
            lightTime = sleepTimeInfo.getLightTime();
            awakeCount = sleepTimeInfo.getAwakeCount();
            sleepTotalTime = sleepTimeInfo.getSleepTotalTime();

            int[] colorArray = sleepTimeInfo.getSleepStatueArray();// 绘图中不同睡眠状态可用不同颜色表示，颜色自定义
            int[] timeArray = sleepTimeInfo.getDurationTimeArray();
            int[] timePointArray = sleepTimeInfo.getTimePointArray();

            Log.d("getSleepInfo", "Calendar=" + CalendarUtils.getCalendar(0)
                    + ",timeArray =" + timeArray + ",timeArray.length ="
                    + timeArray.length + ",colorArray =" + colorArray
                    + ",colorArray.length =" + colorArray.length
                    + ",timePointArray =" + timePointArray
                    + ",timePointArray.length =" + timePointArray.length);

            double total_hour = ((float) sleepTotalTime / 60f);
            DecimalFormat df1 = new DecimalFormat("0.0"); // 保留1位小数，带前导零

            int deep_hour = deepTime / 60;
            int deep_minute = (deepTime - deep_hour * 60);
            int light_hour = lightTime / 60;
            int light_minute = (lightTime - light_hour * 60);
            int active_count = awakeCount;
            String total_hour_str = df1.format(total_hour);

            if (total_hour_str.equals("0.0")) {
                total_hour_str = "0";
            }
            tv_sleep.setText(total_hour_str);
            tv_deep.setText(deep_hour + " "
                    + mContext.getResources().getString(R.string.hour) + " "
                    + deep_minute + " "
                    + mContext.getResources().getString(R.string.minute));
            tv_light.setText(light_hour + " "
                    + mContext.getResources().getString(R.string.hour) + " "
                    + light_minute + " "
                    + mContext.getResources().getString(R.string.minute));
            tv_awake.setText(active_count + " "
                    + mContext.getResources().getString(R.string.count));
        } else {
            Log.d("getSleepInfo", "sleepTimeInfo =" + sleepTimeInfo);
            tv_sleep.setText("0");
            tv_deep.setText(mContext.getResources().getString(
                    R.string.zero_hour_zero_minute));
            tv_light.setText(mContext.getResources().getString(
                    R.string.zero_hour_zero_minute));
            tv_awake.setText(mContext.getResources().getString(
                    R.string.zero_count));
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(GlobalVariable.READ_BLE_VERSION_ACTION)) {
                String version = intent
                        .getStringExtra(GlobalVariable.INTENT_BLE_VERSION_EXTRA);
                if (sp.getBoolean(BluetoothLeService.IS_RK_PLATFORM_SP, false)) {
                    show_result.setText("version="
                            + version
                            + ","
                            + sp.getString(
                            GlobalVariable.PATH_LOCAL_VERSION_NAME_SP,
                            ""));
                } else {
                    show_result.setText("version=" + version);
                }

            } else if (action.equals(GlobalVariable.READ_BATTERY_ACTION)) {
                int battery = intent.getIntExtra(
                        GlobalVariable.INTENT_BLE_BATTERY_EXTRA, -1);
                show_result.setText("battery=" + battery);

            }
        }
    };
    private Window window;

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
        } else {
            NetworkInfo[] info = cm.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("onServerDiscorver", "MainActivity_onDestroy");
        GlobalVariable.BLE_UPDATE = false;
        btn_rate_stop.performClick();
        mUpdates.unRegisterBroadcastReceiver();
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            // TODO: handle exception
        }

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        if (mDialogRunnable != null)
            mHandler.removeCallbacks(mDialogRunnable);

        mBLEServiceOperate.disConnect();
    }

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

    private final String testKey1 = "00a4040008A000000333010101000003330101010000333010101000033301010100003330101010000033301010100333010101000033301010100003330101010000333010101000033301010100003330101010000333010101000033301010100003330101010000333010101000033301010100a4040008A0000003330101010000033301010100003330101010000333010101000033301010100000333010101003330101010000333010101000033301010100003330101010000333010101000033301010100003330101010000333010101000033301010100003330101010000333010101";
    private final String universalKey = "040008A00000033301010100000333010101000033301010100003330101010000333010101000003330100333010101000033301010100003330101010000333010101000033301010100003330101010000333010101000033301010100003330101010000333010";

    @Override
    public void OnDataResult(boolean result, int status, byte[] data) {
        StringBuilder stringBuilder = null;
        if (data != null && data.length > 0) {
            stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data) {
                stringBuilder.append(String.format("%02X", byteChar));
            }
            Log.i("testChannel",
                    "BLE---->APK data =" + stringBuilder.toString());
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
            default:
                break;
        }

    }

    @Override
    public void onCharacteristicWriteCallback(int status) {// add 20170221
        // 写入操作的系统回调，status = 0为写入成功，其他或无回调表示失败
        Log.d(TAG, "Write System callback status = " + status);
    }

    @Override
    public void OnServerCallback(int status) {
        Log.d(TAG, "服务器回调 OnServerCallback status =" + status);

        mHandler.sendEmptyMessage(SERVER_CALL_BACK_OK_MSG);

    }

    @Override
    public void OnServiceStatuslt(int status) {
        if (status == ICallbackStatus.BLE_SERVICE_START_OK) {
            Log.d("onServiceConnected", "OnServiceStatuslt mBluetoothLeService11 =" + mBluetoothLeService);
            if (mBluetoothLeService == null) {
                mBluetoothLeService = mBLEServiceOperate.getBleService();
                mBluetoothLeService.setICallback(this);
                Log.d("onServiceConnected", "OnServiceStatuslt mBluetoothLeService22 =" + mBluetoothLeService);
            }
        }
    }

    private static final int SHOW_SET_PASSWORD_MSG = 26;
    private static final int SHOW_INPUT_PASSWORD_MSG = 27;
    private static final int SHOW_INPUT_PASSWORD_AGAIN_MSG = 28;

    private boolean isPasswordDialogShowing = false;
    private String password = "";

    private void showPasswordDialog(final int type) {
        Log.d("CustomPasswordDialog", "showPasswordDialog");
        if (isPasswordDialogShowing) {
            Log.d("CustomPasswordDialog", "已有对话框弹出");
            return;
        }
        CustomPasswordDialog.Builder builder = new CustomPasswordDialog.Builder(
                CommonWebviewActivity.this, mTextWatcher);
        builder.setPositiveButton(getResources().getString(R.string.confirm),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (password.length() == 4) {
                            Log.d("CustomPasswordDialog", "密码是4位  password =" + password);
                            dialog.dismiss();
                            isPasswordDialogShowing = false;

                            mWriteCommand.sendToSetOrInputPassward(password,
                                    type);
                        }
                    }
                });
        builder.setNegativeButton(getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        isPasswordDialogShowing = false;
                    }
                });
        builder.create().show();

        if (type == GlobalVariable.PASSWORD_TYPE_SET) {
            builder.setTittle(mContext.getResources().getString(
                    R.string.set_password_for_band));
        } else if (type == GlobalVariable.PASSWORD_TYPE_INPUT_AGAIN) {
            builder.setTittle(mContext.getResources().getString(
                    R.string.input_password_for_band_again));
        } else {
            builder.setTittle(mContext.getResources().getString(
                    R.string.input_password_for_band));
        }
        isPasswordDialogShowing = true;
    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            password = s.toString();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            // TODO Auto-generated method stub
        }

        @Override
        public void afterTextChanged(Editable s) {
            // TODO Auto-generated method stub
        }
    };

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    /**
     * 激活设备管理权限
     *
     * @return
     */
    private boolean isEnabled() {
        String pkgName = getPackageName();
        Log.w("ellison", "---->pkgName = " + pkgName);
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName
                        .unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void upDateTodaySwimData() {
        // TODO Auto-generated method stub
//		SwimInfo mSwimInfo = mySQLOperate.querySwimData(CalendarUtils
//				.getCalendar(0));// 传入日期，0为今天，-1为昨天，-2为前天。。。。
        SwimDayInfo mSwimInfo = mySQLOperate.querySwimDayInfo(CalendarUtils
                .getCalendar(0));// 传入日期，0为今天，-1为昨天，-2为前天。。。。
        if (mSwimInfo != null) {
            swim_time.setText(mSwimInfo.getUseTime() + "");
            swim_stroke_count.setText(mSwimInfo.getCount() + "");
            swim_calorie.setText(mSwimInfo.getCalories() + "");
        }
    }

    ;

    /*
     * 获取一天最新心率值、最高、最低、平均心率值
     */
    private void UpdateBloodPressureMainUI(String calendar) {
        // UTESQLOperate mySQLOperate = new UTESQLOperate(mContext);
        List<BPVOneDayInfo> mBPVOneDayListInfo = mySQLOperate
                .queryBloodPressureOneDayInfo(calendar);
        if (mBPVOneDayListInfo != null) {
            int highPressure = 0;
            int lowPressure = 0;
            int time = 0;
            for (int i = 0; i < mBPVOneDayListInfo.size(); i++) {
                highPressure = mBPVOneDayListInfo.get(i)
                        .getHightBloodPressure();
                lowPressure = mBPVOneDayListInfo.get(i).getLowBloodPressure();
                time = mBPVOneDayListInfo.get(i).getBloodPressureTime();
            }
            Log.d("MySQLOperate", "highPressure =" + highPressure
                    + ",lowPressure =" + lowPressure);
            // current_rate.setText(currentRate + "");
            if (highPressure == 0) {
                tv_high_pressure.setText("--");

            } else {
                tv_high_pressure.setText(highPressure + "");

            }
            if (lowPressure == 0) {
                tv_low_pressure.setText("--");
            } else {
                tv_low_pressure.setText(lowPressure + "");
            }

        } else {
            tv_high_pressure.setText("--");
            tv_low_pressure.setText("--");

        }
    }

    private void initIbeacon() {
        // TODO Auto-generated method stub
        ibeacon_command = (Button) findViewById(R.id.ibeacon_command);
        ibeacon_command.setOnClickListener(this);
        ibeaconStatusSpinner = (Spinner) findViewById(R.id.ibeacon_status);
        setOrReadSpinner = (Spinner) findViewById(R.id.SetOrReadSpinner);
        ibeaconStatusSpinnerList.add("UUID");
        ibeaconStatusSpinnerList.add("major");
        ibeaconStatusSpinnerList.add("minor");
        ibeaconStatusSpinnerList.add("device name");
        ibeaconStatusSpinnerList.add("TX power");
        ibeaconStatusSpinnerList.add("advertising interval");
//		ibeaconStatusSpinnerList.add("横屏");
//		ibeaconStatusSpinnerList.add("竖屏英文");
//		ibeaconStatusSpinnerList.add("竖屏中文");
//		ibeaconStatusSpinnerList.add("不设置");
        aibeaconStatusAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, ibeaconStatusSpinnerList);
        aibeaconStatusAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ibeaconStatusSpinner.setAdapter(aibeaconStatusAdapter);
        ibeaconStatusSpinner
                .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view, int position, long id) {
                        // TODO Auto-generated method stub
                        Log.d(TAG,
                                "选择了 "
                                        + aibeaconStatusAdapter
                                        .getItem(position));

                        if (position == 0) {
                            ibeaconStatus = GlobalVariable.IBEACON_TYPE_UUID;
//							if (GetFunctionList.isSupportFunction(mContext,
//									GlobalVariable.IS_SUPPORT_DIAL_SWITCH)) {
//								dialType = GlobalVariable.SHOW_HORIZONTAL_SCREEN;
//								mWriteCommand
//										.controlDialSwitchAandLeftRightHand(
//												leftRightHand, dialType);
//							}
                        } else if (position == 1) {
                            ibeaconStatus = GlobalVariable.IBEACON_TYPE_MAJOR;
//							if (GetFunctionList.isSupportFunction(mContext,
//									GlobalVariable.IS_SUPPORT_DIAL_SWITCH)) {
//							dialType =GlobalVariable.SHOW_VERTICAL_ENGLISH_SCREEN;
//							mWriteCommand.controlDialSwitchAandLeftRightHand(leftRightHand, dialType);
//							}
                        } else if (position == 2) {
                            ibeaconStatus = GlobalVariable.IBEACON_TYPE_MINOR;
//							if (GetFunctionList.isSupportFunction(mContext,
//									GlobalVariable.IS_SUPPORT_DIAL_SWITCH)) {
//							dialType =GlobalVariable.SHOW_VERTICAL_CHINESE_SCREEN;
//							mWriteCommand.controlDialSwitchAandLeftRightHand(leftRightHand, dialType);
//							}
                        } else if (position == 3) {
                            ibeaconStatus = GlobalVariable.IBEACON_TYPE_DEVICE_NAME;
//							if (GetFunctionList.isSupportFunction(mContext,
//									GlobalVariable.IS_SUPPORT_DIAL_SWITCH)) {
//							dialType =GlobalVariable.NOT_SET_UP;
//							mWriteCommand.controlDialSwitchAandLeftRightHand(leftRightHand, dialType);
//							}
                        } else if (position == 4) {
                            ibeaconStatus = GlobalVariable.IBEACON_TYPE_TX_POWER;
                        } else if (position == 5) {
                            ibeaconStatus = GlobalVariable.IBEACON_TYPE_ADVERTISING_INTERVAL;
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // TODO Auto-generated method stub

                    }
                });

        SetOrReadSpinnerList.add("设置");
        SetOrReadSpinnerList.add("获取");
//		SetOrReadSpinnerList.add("左手");
//		SetOrReadSpinnerList.add("右手");
//		SetOrReadSpinnerList.add("不设置");

        setOrReadAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, SetOrReadSpinnerList);
        setOrReadAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setOrReadSpinner.setAdapter(setOrReadAdapter);
        setOrReadSpinner
                .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view, int position, long id) {
                        // TODO Auto-generated method stub
                        Log.d(TAG, "选择了 " + setOrReadAdapter.getItem(position)/*+",支持表盘 ="+GetFunctionList.isSupportFunction(mContext,
								GlobalVariable.IS_SUPPORT_DIAL_SWITCH)*/);
                        if (position == 0) {
                            ibeaconSetOrRead = GlobalVariable.IBEACON_SET;
//							if (GetFunctionList.isSupportFunction(mContext,
//									GlobalVariable.IS_SUPPORT_DIAL_SWITCH)) {
//							leftRightHand =GlobalVariable.LEFT_HAND_WEAR;
//							mWriteCommand.controlDialSwitchAandLeftRightHand(leftRightHand, dialType);
//							}
                        } else if (position == 1) {
                            ibeaconSetOrRead = GlobalVariable.IBEACON_GET;
//							if (GetFunctionList.isSupportFunction(mContext,
//									GlobalVariable.IS_SUPPORT_DIAL_SWITCH)) {
//							leftRightHand =GlobalVariable.RIGHT_HAND_WEAR;
//							mWriteCommand.controlDialSwitchAandLeftRightHand(leftRightHand, dialType);
//							}
                        }
//						else if (position==2) {
//							if (GetFunctionList.isSupportFunction(mContext,
//									GlobalVariable.IS_SUPPORT_DIAL_SWITCH)) {
//							leftRightHand =GlobalVariable.NOT_SET_UP;
//							mWriteCommand.controlDialSwitchAandLeftRightHand(leftRightHand, dialType);
//							}
//						}
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // TODO Auto-generated method stub

                    }
                });
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


    /**
     * 发送七天天气接口
     */
    private void testSendSevenDayWeather() {
        // TODO Auto-generated method stub
        // SevenDayWeatherInfo info =new SevenDayWeatherInfo(cityName,
        // todayWeatherCode, todayTmpCurrent, todayTmpMax, todayTmpMin,
        // todayPm25, todayAqi,
        // secondDayWeatherCode, secondDayTmpMax, secondDayTmpMin,
        // thirdDayWeatherCode, thirdDayTmpMax, thirdDayTmpMin,
        // fourthDayWeatherCode, fourthDayTmpMax, fourthDayTmpMin,
        // fifthDayWeatherCode, fifthDayTmpMax, fifthDayTmpMin,
        // sixthDayWeatherCode, sixthDayTmpMax, sixthDayTmpMin,
        // seventhDayWeatherCode, seventhDayTmpMax, seventhDayTmpMin);

        if (GetFunctionList.isSupportFunction(mContext,
                GlobalVariable.IS_SUPPORT_SEVEN_DAYS_WEATHER)) {
            SevenDayWeatherInfo info = new SevenDayWeatherInfo("深圳市", "308",
                    25, 30, 20, 155, 50, "311", 32, 12, "312", 33, 13, "313",
                    34, 14, "314", 35, 15, "315", 36, 16, "316", 37, 17);

            mWriteCommand.syncWeatherToBLEForXiaoYang(info);
        } else {
            Toast.makeText(mContext, "不支持七天天气", Toast.LENGTH_SHORT).show();
        }
    }

    private void upDateTodaySkipData() {
        // TODO Auto-generated method stub
        SkipDayInfo mSkipInfo = mySQLOperate.querySkipDayInfo(CalendarUtils
                .getCalendar(0));// 传入日期，0为今天，-1为昨天，-2为前天。。。。
        if (mSkipInfo != null) {
            skip_time.setText(mSkipInfo.getUseTime() + "");
            skip_count.setText(mSkipInfo.getCount() + "");
            skip_calorie.setText(mSkipInfo.getCalories() + "");
        }
    }

    private void upDateTodayStepData() {
        // TODO Auto-generated method stub
        int steps = mySQLOperate.queryStepDate(CalendarUtils
                .getCalendar(-1));// 传入日期，0为今天，-1为昨天，-2为前天。。。。
        webView.loadUrl("javascript:updateTodaySteps('" + steps + "')");
    }

    @Override
    public void onSportsTimeCallback(boolean result, String calendar, int sportsTime,
                                     int timeType) {

        if (timeType == GlobalVariable.SPORTS_TIME_TODAY) {

            Log.d(TAG, "今天的运动时间  calendar =" + calendar + ",sportsTime ="
                    + sportsTime);
            resultBuilder.append("\n" + calendar + "," + sportsTime
                    + getResources().getString(R.string.fminute));
            mHandler.sendEmptyMessage(UPDATE_SPORTS_TIME_DETAILS_MSG);

        } else if (timeType == GlobalVariable.SPORTS_TIME_HISTORY_DAY) {// 7天的运动时间
            Log.d(TAG, "7天的运动时间  calendar =" + calendar
                    + ",sportsTime =" + sportsTime);
            resultBuilder.append("\n" + calendar + "," + sportsTime
                    + getResources().getString(R.string.fminute));
            mHandler.sendEmptyMessage(UPDATE_SPORTS_TIME_DETAILS_MSG);
        }
    }


    public void setWebView(Activity context, WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setUseWideViewPort(true);//关键点
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptEnabled(true); // 设置支持javascript脚本
        settings.setAllowFileAccess(false); // 允许访问文件
        settings.setBuiltInZoomControls(true); // 设置显示缩放按钮
        settings.setSupportZoom(true); // 支持缩放
        settings.setLoadWithOverviewMode(true);
        settings.setDomStorageEnabled(true);
//        settings.setUserAgentString(settings.getUserAgentString() + ";Aiyi/" + CoolPublicMethod.getVersionName(context));
        settings.setBlockNetworkImage(false);
        settings.setDefaultTextEncodingName("utf-8");
//        webView.setLayerType(View.LAYER_TYPE_HARDWARE,null);//开启硬件加速
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
//        DisplayMetrics metrics = new DisplayMetrics();
//        //参数2：Java对象名
//        context.getWindowManager().getDefaultDisplay().getMetrics(metrics);
//        int mDensity = metrics.densityDpi;
//        Log.d("aiyi", "densityDpi = " + mDensity);
//        if (mDensity == 240) {
//            settings.setDefaultZoom(WebSettings.ZoomDensity.FAR);
//        } else if (mDensity == 160) {
//            settings.setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
//        } else if (mDensity == 120) {
//            settings.setDefaultZoom(WebSettings.ZoomDensity.CLOSE);
//        } else if (mDensity == DisplayMetrics.DENSITY_XHIGH) {
//            settings.setDefaultZoom(WebSettings.ZoomDensity.FAR);
//        } else if (mDensity == DisplayMetrics.DENSITY_TV) {
//            settings.setDefaultZoom(WebSettings.ZoomDensity.FAR);
//        } else {
//            settings.setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
//        }

        // 点击链接继续在当前browser中响应
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                CoolPublicMethod.showpProgressDialog("loadding", CoolWebViewActivity.this);
//                android.support.v4.view.loadUrl(url);
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                //注意：super句话一定要删除，或者注释掉，否则又走handler.cancel()默认的不支持https的了。
                //super.onReceivedSslError(view, handler, error);
                //handler.cancel(); // Android默认的处理方式
                //handler.handleMessage(Message msg); // 进行其他处理
                handler.proceed(); // 接受所有网站的证书
            }
        });
    }
}