package ru.reldev.firebase;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class FirebaseAnalyticsPlugin extends BaseCordovaPlugin {
    private boolean isInitialized = false;
    private FirebaseAnalytics firebaseAnalytics;

    @Override
    public void onStart() {
        super.onStart();

        if(!isInitialized) {
            this.isInitialized = true;
            log("plugin started");
        }
    }

    private void log(String message) {
        String TAG = "FirebaseAuthPlugin";
        Log.d(TAG, message);
    }

    @Override
    protected void pluginInitialize() {
        final Context context = this.cordova.getActivity().getApplicationContext();
        this.cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                log("initializing plugin");
                firebaseAnalytics = FirebaseAnalytics.getInstance(context);
                log("initialized");
            }
        });
    }

    @CordovaMethod
    private void setUserId(String userId, CallbackContext callbackContext) {
        this.firebaseAnalytics.setUserId(userId);

        callbackContext.success();
    }

    @CordovaMethod
    private void setUserProperty(String name, String value, CallbackContext callbackContext) {
        this.firebaseAnalytics.setUserProperty(name, value);

        callbackContext.success();
    }

    @CordovaMethod
    private void logEvent(String name, JSONObject params, CallbackContext callbackContext) throws JSONException {
        Bundle bundle = new Bundle();
        Iterator<String> it = params.keys();

        while (it.hasNext()) {
            String key = it.next();
            Object value = params.get(key);

            if (value instanceof String) {
                bundle.putString(key, (String)value);
            } else if (value instanceof Integer) {
                bundle.putInt(key, (Integer)value);
            } else if (value instanceof Double) {
                bundle.putDouble(key, (Double)value);
            } else if (value instanceof Long) {
                bundle.putLong(key, (Long)value);
            } else {
                log("invalid type given for key :\""+key+"\"");
            }
        }

        this.firebaseAnalytics.logEvent(name, bundle);

        callbackContext.success();
    }

    @CordovaMethod(ExecutionThread.UI)
    private void setCurrentScreen(String screenName, CallbackContext callbackContext) {
        firebaseAnalytics.setCurrentScreen(
                cordova.getActivity(),
                screenName,
                null
        );

        callbackContext.success();
    }
}