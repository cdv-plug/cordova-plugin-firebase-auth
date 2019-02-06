var exec = require('cordova/exec');
var __PLUGIN__ = "FirebasePlugin";

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