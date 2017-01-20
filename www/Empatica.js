module.exports = {
    initialize: function(apiKey, success, error) {
        cordova.exec(success, error, "Empatica", "initialize", [apiKey]);
    },
    connect: function(success, error) {
        cordova.exec(success, error, "Empatica", "connect", []);
    },
    disconnect: function(success, error) {
        cordova.exec(success, error, "Empatica", "disconnect", []);
    }
};
