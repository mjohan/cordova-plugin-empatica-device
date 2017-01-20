package au.edu.sydney.poscomp;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

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

  private final long SCANNING_PERIOD = 10000;

  private EmpaDeviceManager mDeviceManager = null;
  private CallbackContext mCallbackSetup = null;
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

  @Override
  public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
    Log.d("didReceiveAcceleration", "x: " + x + " y: " + y + " z: " + z);
    // TODO: implement later
  }

  @Override
  public void didReceiveBVP(float bvp, double timestamp) {
    Log.d("didReceiveBVP", "bvp: " + bvp);
    // TODO: implement later
  }

  @Override
  public void didReceiveBatteryLevel(float battery, double timestamp) {
    Log.d("didReceiveBatteryLevel", "battery: " + battery);
    // TODO: implement later
  }

  @Override
  public void didReceiveGSR(float gsr, double timestamp) {
    Log.d("didReceiveGSR", "gsr: " + gsr);
    // TODO: implement later
  }

  @Override
  public void didReceiveIBI(float ibi, double timestamp) {
    Log.d("didReceiveIBI", "ibi: " + ibi);
    // TODO: implement later
  }

  @Override
  public void didReceiveTemperature(float temp, double timestamp) {
    Log.d("didReceiveTemperature", "temp: " + temp);
    // TODO: implement later
  }
}
