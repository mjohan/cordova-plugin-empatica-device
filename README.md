# cordova-plugin-empatica-device

This plugin helps cordova/ionic/phonegap projects to communicate with Empatica device (especially E4). 

This plugin implements [Empatica mobile SDK](http://developer.empatica.com/) and needs __SDK library (empalink__) and __API-KEY__ provided by Empatica. These should be available for you if you have empatica devices and an Empatica Connect account. Both of them are available from [Empatica developer page](https://www.empatica.com/connect/developer.php).

Supported function list:
 * [initialize](#initialize)
 * [connect](#connect)
 * [disconnect](#disconnect)
 * [subscribe](#subscribe)
 * [unsubscribe](#unsubscribe)

The details about these functions will be explained in [Methods](#Methods) section. All these functions will be available after `deviceready` event.
```javascript
// ionic
$ionicPlatform.ready(function() {
  // Empatica-plugin calls
});

// cordova
document.addEventListener("deviceready", onDeviceReady, false);
function onDeviceReady() {
  // Empatica-plugin calls
});
```

### Supported Platform

* iOS (8.0 or greater)
* Android (4.4 or greater) as Empatica Android SDK has `minSdkVersion = 19` value in it.

>**Note** You should also change the `minSdkVersion` value on your project to **19 or greater**. Please check whether your android platform has already supported this SDK version. Furthermore, please check also your build-tools version on your Android SDK if you have problems using this plugin.

## Installation

* From outside your project directory, clone this repo

```
git clone -v https://github.com/mjohan/cordova-plugin-empatica-device.git
```

* Copy Empatica android framework file (**empalink-2.1.aar**) into `src/android/` directory
* Copy Empatica ios framework file (**EmpaLink-ios-0.7-full.framework**) into `src/ios/` directory
* Go to your project directory
* Add the plugin from the repo directory in your computer
```
cordova plugin add <path_to_the_cloned_repo>
```

## Methods and Constants

### initialize

Initializes the plugin with provided API-Key to bind the device. This method must be called before other method calls.

```javascript
Empatica.initialize('<API_KEY>', success, failure);
```

#### Parameters

* `<API_KEY>`: String value of API_KEY provided by Empatica
* `success`: Callback function when the initialization process is proceeded
* `failure`: Callback function when the initialization process is failed

### connect

Connects to the Empatica device.

```javascript
Empatica.connect(success, failure);
```

#### Parameters

* `success`: Callback function when the Empatica device is succesfully connected
* `failure`: Callback function when the connection is failed

### disconnect

Disconnects to the Empatica device.

```javascript
Empatica.disconnect(success, failure);
```

#### Parameters

* `success`: Callback function when the Empatica device is succesfully disconnected
* `failure`: Callback function when the disconnection is failed

### subscribe

Subscribes to any sensor's update value. This method needs the third param which indicates the sensor type. This param should be assigned with one of Empatica.SENSORS constants.

```javascript
Empatica.subscribe(success, failure, sensor);
```

>***Warning*** The device is in streaming mode when the connection has been made. This subscribe method is only an interface to get the streamed values. In fact, all sensors values are synched by the device to your app.  

#### Parameters

* `success`: Callback function if the plugin successfully subscribed to the sensor value
* `failure`: Callback function if the subscription process is failed
* `sensor`: Sensor type provided by Empatica.SENSORS constants

### unsubscribe

Stops subscribing to a certain sensor type.

```javascript
Empatica.unsubscribe(success, failure, sensor);
```

#### Parameters

* `success`: Callback function if the plugin successfully unsubscribed to the sensor value
* `failure`: Callback function if the unsubscription process is failed
* `sensor`: Sensor type provided by Empatica.SENSORS constants

### Constants

These are the supported sensors by Empatica E4 device:

* `Empatica.SENSORS.BVP_SENSOR` : Blood Volume Pulse
* `Empatica.SENSORS.IBI_SENSOR` : Inter-Beat Interval
* `Empatica.SENSORS.GSR_SENSOR` : Galvanic Skin Response
* `Empatica.SENSORS.ACC_SENSOR` : Accelerometer
* `Empatica.SENSORS.TEMP_SENSOR` : Temperature
* `Empatica.SENSORS.BATTERY_SENSOR` : Battery level

## Sample

This sample shows how we could subscribe to BVP sensor. After initialization and connect process are succeeded, the progam tries to listen for any BVP update value. The program then stop listening to it after 5 seconds and disconnects the device after 15 seconds.

```javascript
if (typeof Empatica !== 'undefined') {
  Empatica.initialize('<API_KEY>', function(initializeResult) {
    console.log(JSON.stringify(initializeResult));
    
    Empatica.connect(function(connectResult) {
      console.log(JSON.stringify(connectResult));
      
      // subscribes to BVP sensor
      Empatica.subscribe(function(data) {
        console.log(JSON.stringify(data)); // {"bvp": double_value, "timestamp": double_value}
      }, function (error) {
        console.log(JSON.stringify(error));
      }, Empatica.SENSORS.BVP_SENSOR);
      
      // unsubscribes after 5000 ms
      $interval(function () {
        Empatica.unsubscribe(function(data) {
          console.log(JSON.stringify(data));
        }, function (error) {
          console.log(JSON.stringify(error));
        }, Empatica.SENSORS.BVP_SENSOR);
      }, 5000, 1);
      
      // disconnects after 15000 ms
      $interval(function () {
        Empatica.disconnect(function() {
          console.log("Success disconnect");
        }, function(error) {
          console.log(JSON.stringify(error));
        });
      }, 15000, 1);
    }, function (error) {
      console.log(JSON.stringify(error));
    });
  }, function(error) {
    console.log(JSON.stringify(error));
  });
}
```

>**Note** `subscribe` and `unsubscribe` methods do not need to be put inside connect `success-callback`. 

## License

MIT

## Feedback

If you find any problems with this plugin, please create an issue or create a pull request.

## Credits

 * [Empatica Android sample project](https://github.com/empatica/empalink-sample-project-android) which provides an example for building this plugin.
 * [Alberto Guarino](http://developer.empatica.com/#ios) who gives an iOS project example for connecting a single Empatica device
 * [cordova-plugin-empatica](https://github.com/patte/cordova-plugin-empatica) which indicates that we have to include other frameworks for iOS platform to work
 * [cordova-plugin-ble-central](https://github.com/don/cordova-plugin-ble-central) which gives information about `NSBluetoothPeripheralUsageDescription` and `UIBackgroundModes` for iOS platform
 * [cordova-plugin-msband](https://github.com/wshaheer/cordova-plugin-msband) which gives me an idea to use `setKeepCallback` method
