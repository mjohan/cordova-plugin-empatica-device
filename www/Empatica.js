module.exports = {
    SENSORS: {
        ACC_SENSOR: "acc_sensor",
        BVP_SENSOR: "bvp_sensor",
        BATTERY_SENSOR: "battery_sensor",
        GSR_SENSOR: "gsr_sensor",
        IBI_SENSOR: "ibi_sensor",
        TEMP_SENSOR: "temp_sensor"
    },
    initialize: function(apiKey, success, error) {
        cordova.exec(success, error, "Empatica", "initialize", [apiKey]);
    },
    connect: function(success, error) {
        cordova.exec(success, error, "Empatica", "connect", []);
    },
    disconnect: function(success, error) {
        cordova.exec(success, error, "Empatica", "disconnect", []);
    },
    subscribe: function(success, error, sensor) {
        cordova.exec(success, error, "Empatica", "subscribe", [sensor]);
    },
    unsubscribe: function(success, error, sensor) {
        cordova.exec(success, error, "Empatica", "unsubscribe", [sensor]);
    }
};
