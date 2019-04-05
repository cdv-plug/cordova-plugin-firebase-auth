# cordova-plugin-firebase-auth
A cordova plugin provides Google Authentication (Not G+) and Analytics to your app, providing profile data and backend tokens (idToken, serverAuthCode). More providers - wip.

## Installation
The latest, from the master repo:
```
$ cordova plugin add https://github.com/stck/cordova-plugin-firebase-auth
$ cordova prepare
```
or in `config.xml`
```
<plugin name="cordova-plugin-firebase-auth" spec="https://github.com/stck/cordova-plugin-firebase-auth" />
```
and it's required to post file references in `config.xml > platforms section`
```
<platform name="android">
    <resource-file src="google-services.json" target="google-services.json" />
</platform>
<platform name="ios">
    <resource-file src="GoogleService-Info.plist" />
</platform>
```


### Requirements

#### android
- `google-services.json` from your firebase console
- gradle >= 4.0

#### ios
- `GoogleService-Info.plist` from your firebase console
- cocoapods >= 1.4.0

## Usage

Make sure you check if the plugin is installed
```
if (window.plugins.FirebasePlugin) {

}
```

**{Promise} login(String provider)** will singin into given provider (only 'google' is accepted for now)

**{Promise} logout()** will logout from firebase (If you won't call for logout when user actually logs out - you will not see Account Selection Dialog (auto sign up))

**{Promise} setUserId(String userId)** will set Analytics internal user id

**{Promise} logEvent(String name, Array[] params)** will set Analytics internal user id

**{Promise} setScreen(String name)** virtual screen name (there are only MainActivity in cordova, so it's required for complex analytic events)

**{Promise} setUserProperty(String name, String value)** set analytics value for parameter

Examples:
```js
// pure Promise
window.FirebasePlugin.login('google')
  .then(function(data) {
    console.log(data);
  })
  .catch(function(err) {
    console.error(err)
  })
// or async-await
await window.FirebasePlugin.logout();
```
