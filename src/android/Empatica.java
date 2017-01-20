package au.edu.sydney.poscomp;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;

/**
 * This class performs as a cordova plugin for Empatica Device
 */
public class Empatica extends CordovaPlugin implements EmpaDataDelegate, EmpaStatusDelegate {

  public static final String ACC_SENSOR = "acc_sensor";
  public static final String BVP_SENSOR = "bvp_sensor";
  public static final String BATTERY_SENSOR = "battery_sensor";
  public static final String GSR_SENSOR = "gsr_sensor";
  public static final String IBI_SENSOR = "ibi_sensor";
  public static final String TEMP_SENSOR = "temp_sensor";

  private final long SCANNING_PERIOD = 15000; // 15 seconds

  private EmpaDeviceManager mDeviceManager = null;

  private CallbackContext mCallbackSetup = null;
  private CallbackContext mCallbackAcc = null;
  private CallbackContext mCallbackBVP = null;
  private CallbackContext mCallbackBattery = null;
  private CallbackContext mCallbackGSR = null;
  private CallbackContext mCallbackIBI = null;
  private CallbackContext mCallbackTemp = null;

  private boolean mDisconnectAttempt = false;
  private boolean mDeviceConnected = false;
  private boolean mStreamingData = false;

  @Override
  public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (action.equals("initialize")) {
      final String apiKey = args.getString(0);
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          initialize(apiKey, callbackContext);
        }
      });
      return true;
    } else if (action.equals("connect")) {
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          connect(callbackContext);
        }
      });
      return true;
    } else if (action.equals("disconnect")) {
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          disconnect(callbackContext);
        }
      });
      return true;
    } else if (action.equals("subscribe")) {
      mStreamingData = true;
      final String sensor = args.getString(0);
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          subscribe(sensor, callbackContext);
        }
      });
      return true;
    } else if (action.equals("unsubscribe")) {
      final String sensor = args.getString(0);
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          unsubscribe(sensor, callbackContext);
        }
      });
      return true;
    }
    return false;
  }

  /** Initializes the device manager with context and api key, then waits for didUpdateStatus callback */
  private void initialize(String apiKey, CallbackContext callbackContext) {
    mCallbackSetup = callbackContext;
    Looper.prepare();
    mDeviceManager = new EmpaDeviceManager(cordova.getActivity().getApplicationContext(), this, this);
    mDeviceManager.authenticateWithAPIKey(apiKey);
    Looper.loop();
  }

  /** connects to the device then waits for didUpdateStatus callback */
  private void connect(CallbackContext callbackContext) {
    if (mDeviceManager == null) {
      callbackContext.error("Cannot connect, should be initialized first.");
    }
    mCallbackSetup = callbackContext;
    mDeviceManager.startScanning();

    // put timer to stop the scanning process, stop scanning after a certain period of time
    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
      @Override
      public void run() {
        if (mCallbackSetup != null && !mDeviceConnected) {
          mDeviceManager.stopScanning();
          mCallbackSetup.error("Cannot find empatica device.");
        }
      }
    }, SCANNING_PERIOD);
  }

  /** disconnects to the device then waits for didUpdateStatus callback */
  private void disconnect(CallbackContext callbackContext) {
    if (mDeviceManager == null) {
      callbackContext.error("Cannot connect, should be initialized first.");
    }
    mDisconnectAttempt = true;
    mCallbackSetup = callbackContext;
    mDeviceManager.disconnect();
  }

  /** subscribes to one of sensors provided by empatica device */
  private void subscribe(String sensor, CallbackContext callbackContext) {
    if (!mDeviceConnected) {
      callbackContext.error("Cannot subscribe, should be initialized and connected first.");
    }

    if (sensor.equals(BVP_SENSOR)) {
      mCallbackBVP = callbackContext;
    } else if (sensor.equals(IBI_SENSOR)) {
      mCallbackIBI = callbackContext;
    } else if (sensor.equals(GSR_SENSOR)) {
      mCallbackGSR = callbackContext;
    } else if (sensor.equals(ACC_SENSOR)) {
      mCallbackAcc = callbackContext;
    } else if (sensor.equals(TEMP_SENSOR)) {
      mCallbackTemp = callbackContext;
    } else if (sensor.equals(BATTERY_SENSOR)) {
      mCallbackBattery = callbackContext;
    }
  }

  /** unsubscribe to one of sensors provided by empatica device */
  private void unsubscribe(String sensor, CallbackContext callbackContext) {
    if (!mDeviceConnected) {
      callbackContext.error("Cannot unsubscribe, should be initialized and connected first.");
    }

    if (sensor.equals(BVP_SENSOR)) {
      mCallbackBVP = null;
    } else if (sensor.equals(IBI_SENSOR)) {
      mCallbackIBI = null;
    } else if (sensor.equals(GSR_SENSOR)) {
      mCallbackGSR = null;
    } else if (sensor.equals(ACC_SENSOR)) {
      mCallbackAcc = null;
    } else if (sensor.equals(TEMP_SENSOR)) {
      mCallbackTemp = null;
    } else if (sensor.equals(BATTERY_SENSOR)) {
      mCallbackBattery = null;
    }
    callbackContext.success("Successfully unsubscribe sensor");
  }

  @Override
  public void didUpdateStatus(EmpaStatus status) {
    if (mCallbackSetup != null) {
      if (status == EmpaStatus.READY) { // this will be called after initializing process finishes
        mCallbackSetup.success("Device is ready");
      } else if (status == EmpaStatus.CONNECTED) {
        mDeviceConnected = true;
        mCallbackSetup.success("Device is connected");
      } else if (status == EmpaStatus.DISCONNECTED) {
        if (mDisconnectAttempt) {
          mDisconnectAttempt = false;
          mCallbackSetup.success("Device is disconnected");
        } else if (mStreamingData) {
          mCallbackSetup.error("The device is disconnected.");
        }
        mStreamingData = false;
        mDeviceConnected = false;
      }
    }
  }

  @Override
  public void didUpdateSensorStatus(EmpaSensorStatus status, EmpaSensorType type) {
    // implement later if we need it in the future
  }

  @Override
  public void didDiscoverDevice(BluetoothDevice bluetoothDevice, String deviceName, int rssi, boolean allowed) {
    if (allowed) {
      mDeviceManager.stopScanning();
      try {
        mDeviceManager.connectDevice(bluetoothDevice);
      } catch (ConnectionNotAllowedException e) {
        if (mCallbackSetup != null) mCallbackSetup.error("Cannot connect to the device.");
      }
    }
  }

  @Override
  public void didRequestEnableBluetooth() {
    if (mCallbackSetup != null) mCallbackSetup.error("Bluetooth is not enabled.");
  }

  public void sendCallbackData(CallbackContext callbackContext, JSONObject data) {
    PluginResult result = new PluginResult(PluginResult.Status.OK, data);
    result.setKeepCallback(true);
    callbackContext.sendPluginResult(result);
  }

  @Override
  public void didReceiveBVP(float bvp, double timestamp) {
    if (mCallbackBVP != null) {
      try {
        JSONObject data = new JSONObject();
        data.put("bvp", bvp);
        data.put("timestamp", timestamp);
        sendCallbackData(mCallbackBVP, data);
      } catch (JSONException e) {
        mCallbackBVP.error("Error when preparing JSONObject");
      }
    }
  }

  @Override
  public void didReceiveIBI(float ibi, double timestamp) {
    if (mCallbackIBI != null) {
      try {
        JSONObject data = new JSONObject();
        data.put("ibi", ibi);
        data.put("timestamp", timestamp);
        sendCallbackData(mCallbackIBI, data);
      } catch (JSONException e) {
        mCallbackBVP.error("Error when preparing JSONObject");
      }
    }
  }

  @Override
  public void didReceiveGSR(float gsr, double timestamp) {
    if (mCallbackGSR != null) {
      try {
        JSONObject data = new JSONObject();
        data.put("gsr", gsr);
        data.put("timestamp", timestamp);
        sendCallbackData(mCallbackGSR, data);
      } catch (JSONException e) {
        mCallbackBVP.error("Error when preparing JSONObject");
      }
    }
  }

  @Override
  public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
    if (mCallbackAcc != null) {
      try {
        JSONObject data = new JSONObject();
        data.put("x", x);
        data.put("y", y);
        data.put("z", z);
        data.put("timestamp", timestamp);
        sendCallbackData(mCallbackAcc, data);
      } catch (JSONException e) {
        mCallbackBVP.error("Error when preparing JSONObject");
      }
    }
  }

  @Override
  public void didReceiveTemperature(float temp, double timestamp) {
    if (mCallbackTemp != null) {
      try {
        JSONObject data = new JSONObject();
        data.put("temp", temp);
        data.put("timestamp", timestamp);
        sendCallbackData(mCallbackTemp, data);
      } catch (JSONException e) {
        mCallbackBVP.error("Error when preparing JSONObject");
      }
    }
  }

  @Override
  public void didReceiveBatteryLevel(float battery, double timestamp) {
    if (mCallbackBattery != null) {
      try {
        JSONObject data = new JSONObject();
        data.put("battery", battery);
        data.put("timestamp", timestamp);
        sendCallbackData(mCallbackBattery, data);
      } catch (JSONException e) {
        mCallbackBVP.error("Error when preparing JSONObject");
      }
    }
  }
}
