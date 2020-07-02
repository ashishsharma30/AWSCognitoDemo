package com.aks.awscognitodemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoServiceConstants;
import com.amazonaws.services.cognitoidentityprovider.model.SignUpResult;

import java.util.HashMap;
//import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private String userName, userPassword;
    private CognitoUserPool cognitoUserPool;
    // private String mfaCode; // multi factor authentication
    // private CountDownLatch latch;
    private EditText etUserName, etUserPassword;
    private Button btnAction;
    private boolean signUp = false;
    CognitoUserAttributes cognitoUserAttributes;
    SignUpHandler signUpHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Context context = getApplicationContext();
        cognitoUserPool = new CognitoUserPool(context, new AWSConfiguration(context));
        cognitoUserPool.getCurrentUser().signOut();
        cognitoUserAttributes = new CognitoUserAttributes();
        etUserName = findViewById(R.id.etUserName);
        etUserPassword = findViewById(R.id.etUserPassword);
        btnAction = findViewById(R.id.btnAction);
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (signUp) {
                    customSignIn();
                } else {
                    customSignUp();
                }
            }
        });

        signUpHandler = new SignUpHandler() {
            @Override
            public void onSuccess(CognitoUser user, SignUpResult signUpResult) {
                Log.e(TAG, "onSuccess: " + user.getUserId());
                Log.e(TAG, "onSuccess: " + signUpResult.isUserConfirmed());
                signUp = true;
                btnAction.setText("Sign In");
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "onFailure: " + exception.toString());
            }
        };
    }

    private void customSignUp() {
        userName = etUserName.getText().toString().trim();
        userPassword = etUserPassword.getText().toString().trim();
        cognitoUserAttributes.addAttribute("userName", userName);
        cognitoUserAttributes.addAttribute("userPassword", userPassword);
        cognitoUserPool.signUpInBackground(userName, userPassword, cognitoUserAttributes, null, signUpHandler);
    }

    private void customSignIn() {
        userName = etUserName.getText().toString().trim();
        userPassword = etUserPassword.getText().toString().trim();
        new Thread(new Runnable() {
            @Override
            public void run() {
                cognitoUserPool.getUser(userName).getSession(new AuthenticationHandler() {
                    @Override
                    public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
                        Log.d(TAG, "onSuccess: " + userSession.getAccessToken());
                    }

                    @Override
                    public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
                        Log.d(TAG, "getAuthenticationDetails: ");
                        final HashMap<String, String> authParameters = new HashMap<>();
                        AuthenticationDetails authenticationDetails = new AuthenticationDetails(userId, userPassword, authParameters, null);
                        authenticationContinuation.setAuthenticationDetails(authenticationDetails);
                        authenticationContinuation.continueTask();
                    }

                    @Override
                    public void getMFACode(MultiFactorAuthenticationContinuation continuation) {
                        Log.d(TAG, "getMFACode: ");
                       /* latch = new CountDownLatch(1);
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continuation.setMfaCode(MainActivity.this.mfaCode);
                        continuation.continueTask();*/
                    }

                    @Override
                    public void authenticationChallenge(ChallengeContinuation continuation) {
                        Log.d(TAG, "authenticationChallenge: " + continuation.getChallengeName());
                        continuation.setChallengeResponse(CognitoServiceConstants.CHLG_RESP_ANSWER, "5");
                        continuation.continueTask();
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        Log.e(TAG, "onFailure: ", exception);
                    }
                });
            }
        }).start();
    }

    /*public void setMFA(View view) {
        this.mfaCode = ((EditText) findViewById(R.id.mfaCodeEditText)).getText().toString();
        if (latch != null) {
            latch.countDown();
        }
    }*/
}