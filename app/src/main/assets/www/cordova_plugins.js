cordova.define('cordova/plugin_list', function (require, exports, module) {
  module.exports = [
    {
      "id": "cordova-plugin-camera.Camera",
      "file": "plugins/cordova-plugin-camera/www/CameraConstants.js",
      "pluginId": "cordova-plugin-camera",
      "clobbers": [
        "Camera"
      ]
    },
    {
      "id": "cordova-plugin-camera.CameraPopoverOptions",
      "file": "plugins/cordova-plugin-camera/www/CameraPopoverOptions.js",
      "pluginId": "cordova-plugin-camera",
      "clobbers": [
        "CameraPopoverOptions"
      ]
    },
    {
      "id": "cordova-plugin-camera.camera",
      "file": "plugins/cordova-plugin-camera/www/Camera.js",
      "pluginId": "cordova-plugin-camera",
      "clobbers": [
        "navigator.camera"
      ]
    },
    {
      "id": "cordova-plugin-camera.CameraPopoverHandle",
      "file": "plugins/cordova-plugin-camera/www/CameraPopoverHandle.js",
      "pluginId": "cordova-plugin-camera",
      "clobbers": [
        "CameraPopoverHandle"
      ]
    },
    {
      "id": "cordova-plugin-battery-status.battery",
      "file": "plugins/cordova-plugin-battery-status/www/battery.js",
      "pluginId": "cordova-plugin-battery-status",
      "clobbers": [
        "navigator.battery"
      ]
    },
    {
      "file": "plugins/org.apache.cordova.geolocation/www/Coordinates.js",
      "id": "org.apache.cordova.geolocation.Coordinates",
      "clobbers": [
        "Coordinates"
      ]
    },
    {
      "file": "plugins/org.apache.cordova.geolocation/www/PositionError.js",
      "id": "org.apache.cordova.geolocation.PositionError",
      "clobbers": [
        "PositionError"
      ]
    },
    {
      "file": "plugins/org.apache.cordova.geolocation/www/Position.js",
      "id": "org.apache.cordova.geolocation.Position",
      "clobbers": [
        "Position"
      ]
    },
    {
      "file": "plugins/org.apache.cordova.geolocation/www/android/geolocation.js",
      "id": "org.apache.cordova.geolocation.geolocation",
      "clobbers": [
        "navigator.geolocation"
      ]
    }
  ];
  module.exports.metadata = {
    "cordova-plugin-camera": "6.0.0",
    "cordova-plugin-battery-status": "2.0.3",
    "org.apache.cordova.geolocation": "4.1.0"
  };
});