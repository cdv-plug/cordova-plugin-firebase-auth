package ru.reldev.firebase;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.android.gms.auth.api.Auth;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class FirebasePlugin extends BaseCordovaPlugin implements OnCompleteListener, FirebaseAuth.AuthStateListener {

    private static final int RC_SIGN_IN = 100;
    private static final String propertyPrefix = "ru.reldev.firebase";

    private FirebaseAuth mAuth;
    private boolean isInitialized = false;
    private GoogleApiClient mGoogleApiClient;
    private HashMap<String, CallbackContext> subscribers = new HashMap<String, CallbackContext>();
    private CallbackContext signinCallback;

    private String idToken = "";
    private String serverAuthCode = "";


    private static String getProperty(String key,Context context) throws IOException {
        Properties properties = new Properties();;
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open("firebase.properties");
        properties.load(inputStream);
        return properties.getProperty(propertyPrefix + "." + key);

    }

    @Override
    public void onStart() {
        super.onStart();

        if(!isInitialized) {
            this.isInitialized = true;
            log("plugin started");
        }
    }

    private void log(String message) {
        String TAG = "FirebasePlugin";
        Log.d(TAG, message);
    }

    private void configureGoogleSignin() {
        final Context context = this.cordova.getActivity().getApplicationContext();
        String serverClientId = "";

        try {
            serverClientId = getProperty("google_serverClientId", this.cordova.getActivity().getApplicationContext());
        } catch (IOException e) {
            log("unable to retreive google_serverClientId property");
        }

        GoogleSignInOptions.Builder b = new GoogleSignInOptions.Builder();

        GoogleSignInOptions mGso = b
                .requestIdToken(serverClientId)
                .requestServerAuthCode(serverClientId)
                .requestId()
                .requestProfile()
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                //.enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mGso)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    protected void pluginInitialize() {
        final Context context = this.cordova.getActivity().getApplicationContext();
        this.cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                log("initializing plugin");

                FirebaseApp.initializeApp(context);
                mAuth = FirebaseAuth.getInstance();

                log("plugin initialized");

                configureGoogleSignin();
            }
        });
    }

    @SuppressWarnings("unused")
    @CordovaMethod
    public void googleLogin(final CallbackContext context) {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        this.cordova.startActivityForResult(this, signInIntent, RC_SIGN_IN);

        idToken = "";
        serverAuthCode = "";

        signinCallback = context;
    }


    @SuppressWarnings("unused")
    @CordovaMethod
    public void subscribe(final String subscriberId, final CallbackContext context) {
        subscribers.put(subscriberId, context);

        if(subscribers.size() == 1) {
            mAuth.addAuthStateListener(this);
        }
        log("subscribed by id'" + subscriberId + "'");
    }

    @SuppressWarnings("unused")
    @CordovaMethod
    public void unsubscribe(final String subscriberId, final CallbackContext context) {
        CallbackContext deletedSubscriber = subscribers.remove(subscriberId);

        if (subscribers.size() == 0) {
            mAuth.removeAuthStateListener(this);
        }

        if (deletedSubscriber != null) {
            log("Unable to unsubscribe by id '" + subscriberId + "'; because it doesn't exist!");
        } else {
            log("unsubscribed by id'" + subscriberId + "'");
        }

    }

    private void notifySubscribers(final PluginResult pluginResult) {
        log("trying to notify subscribers w/ " + pluginResult.getStrMessage());

        Iterator<Map.Entry<String, CallbackContext>> it = subscribers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CallbackContext> pair = it.next();
            CallbackContext subscriber = pair.getValue();

            subscriber.sendPluginResult(pluginResult);
            it.remove();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if( requestCode == RC_SIGN_IN )
        {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();

                if (account == null) {
                    this.signinCallback.error(result.getStatus().getStatusMessage());
                    this.signinCallback = null;
                    return;
                }

                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                idToken = account.getIdToken();
                serverAuthCode = account.getServerAuthCode();
                mAuth
                        .signInWithCredential(credential)
                        .addOnCompleteListener(cordova.getActivity(), this);
            } else {
                this.signinCallback.error(result.getStatus().getStatusMessage());
                this.signinCallback = null;
            }
        }
    }

    @Override
    public void onComplete(@NonNull Task task) {
        if (this.signinCallback != null) {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    this.signinCallback.success(buildUserResponse(user));
                } else {
                    this.signinCallback.error("Sign-in success, but user object is empty");
                }
            } else {
                Exception ex = task.getException();
                String message = "<no message provided>";
                if (ex != null) {
                    message = ex.getMessage();
                }
                this.signinCallback.error(message);
            }

            this.signinCallback = null;
        }
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        PluginResult pluginResult;
        FirebaseUser user = firebaseAuth.getCurrentUser();
        PluginResult.Status status = PluginResult.Status.OK;

        if (user != null) {
            pluginResult = new PluginResult(status, buildUserResponse(user));
        } else {
            pluginResult = new PluginResult(status, "");
        }
        pluginResult.setKeepCallback(true); // avoid callback removal

        this.notifySubscribers(pluginResult);
    }

    private JSONObject buildUserResponse(final FirebaseUser user) {
        JSONObject result = new JSONObject();

        try {
            result.put("uid", user.getUid());
            result.put("displayName", user.getDisplayName());
            result.put("email", user.getEmail());
            result.put("phoneNumber", user.getPhoneNumber());
            result.put("photoURL", user.getPhotoUrl());
            result.put("providerId", user.getProviderId());

            result.put("idToken", idToken);
            result.put("serverAuthCode", serverAuthCode);
        } catch (JSONException e) {
            log("fail building user: " + e.getMessage());
        }

        return result;
    }
}
