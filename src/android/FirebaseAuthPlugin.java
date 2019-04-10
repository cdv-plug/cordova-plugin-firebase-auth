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
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.android.gms.auth.api.Auth;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
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

public class FirebaseAuthPlugin extends BaseCordovaPlugin implements OnCompleteListener {

    private static final int RC_SIGN_IN = 100;
    private static final String propertyPrefix = "ru.reldev.firebase";

    private FirebaseAuth mAuth;
    private boolean isInitialized = false;
    private GoogleApiClient mGoogleApiClient;
    private CallbackContext signinCallback;

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
        String TAG = "FirebaseAuthPlugin";
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
    public void logout(final CallbackContext context) {
        this.cordova.getThreadPool().execute(new Runnable() {
             public void run() {

                 log("Logout");
                 mAuth.signOut();
                 Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                     @Override
                     public void onResult(@NonNull Status status) {
                         PluginResult result;
                         if (status.equals(Status.RESULT_SUCCESS)) {
                             result = new PluginResult(PluginResult.Status.OK, "ok");
                         } else {
                             result = new PluginResult(PluginResult.Status.ERROR, "error");
                         }
                         context.sendPluginResult(result);
                     }
                 });
             }
         }
        );
    }

    @SuppressWarnings("unused")
    @CordovaMethod
    public void googleLogin(final CallbackContext context) {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        this.cordova.startActivityForResult(this, signInIntent, RC_SIGN_IN);

        signinCallback = context;
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
                mAuth
                    .signInWithCredential(credential)
                    .addOnCompleteListener(cordova.getActivity(), this);
            } else {
                this.signinCallback.error(result.getStatus().toString());
                this.signinCallback = null;
            }
        }
    }

    @Override
    public void onComplete(@NonNull Task task) {
        if (this.signinCallback != null) {
            final CallbackContext signinCallback = this.signinCallback;
            if (task.isSuccessful()) {
                final FirebaseUser user = mAuth.getCurrentUser();

                if (user != null) {
                    user.getIdToken(true)
                        .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                            @Override
                            public void onComplete(@NonNull Task<GetTokenResult> task) {
                                if (task.isSuccessful()) {
                                    try {
                                        final String idToken = task.getResult().getToken();
                                        signinCallback.success(buildUserResponse(user, idToken));
                                    } catch (NullPointerException e) {
                                        signinCallback.error("Unable to retrieve idToken");
                                    }
                                } else {
                                    signinCallback.error("Unable to retrieve idToken");
                                }
                            }
                        });
                } else {
                    signinCallback.error("Sign-in success, but user object is empty");
                    return;
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

    private JSONObject buildUserResponse(final FirebaseUser user, final String idToken) {
        JSONObject result = new JSONObject();

        try {
            result.put("idToken", idToken);
        } catch (JSONException e) {
            log("fail building user: " + e.getMessage());
        }

        return result;
    }
}
