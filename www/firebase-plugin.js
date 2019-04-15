var exec = require('cordova/exec');
var __PLUGIN__ = "FirebaseAuthPlugin";
var __ANALYTICS__ = "FirebaseAnalyticsPlugin";

exports.login = function (provider) {
  return new Promise(function(res, err) {
    switch (provider) {
      case 'google':
        return exec(res, err, __PLUGIN__, "googleLogin", []);
      default:
        return err(["Provider", provider, "is not available"].join(" "));
    }
  });
};

exports.logout = function () {
  return new Promise(function(res, err) {
    exec(res, err, __PLUGIN__, "logout", []);
  });
};

exports.setUserId = function (userId) {
  return new Promise(function (res, err) {
    exec(res, err, __ANALYTICS__, "setUserId", [ userId ]);
  });
};

exports.logEvent = function(name, params) {
  if (!params) params = {};
  return new Promise(function(res, err) {
    exec(res, err, __ANALYTICS__, "logEvent", [ name, params ]);
  });
};

exports.setScreen = function(name) {
  return new Promise(function(res, err) {
    exec(res, err, __ANALYTICS__, "setCurrentScreen", [ name ]);
  });
};

exports.setUserProperty = function(name, value) {
  return new Promise(function(res, err) {
    exec(res, err, __ANALYTICS__, "setUserProperty", [name, value])
  });
};

exports.getRemoteValue = function(key) {
  return new Promise(function(res, err) {
    exec(res, err, __ANALYTICS__, "getValue", [key])
  });
};
