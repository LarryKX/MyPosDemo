package com.example.larry.myposdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.example.larry.myposdemo.utils.POSHttpConnector;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity  {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private View verificationForm;
    private Boolean isVerificationCode = true;
    private EditText mVerification;

    private String username = "";
    private String mobile_email = "";

    private Button mSendCode;

    private long lastSMSReq = 0L;

    private Handler handler;

    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        //populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        verificationForm = (View) findViewById(R.id.verification_code_form);
        mVerification = (EditText)findViewById(R.id.verification_code);

        mSendCode = (Button) findViewById(R.id.send_code);
        mSendCode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSMSCode();
                timer = new CountDownTimer(60000, 1000) {

                    @Override

                    public void onTick(long millisUntilFinished) {

                        mSendCode.setEnabled(false);

                        mSendCode.setText(millisUntilFinished / 1000 + "秒");

                    }

                    @Override

                    public void onFinish() {

                        mSendCode.setEnabled(true);

                        mSendCode.setText(R.string.send_code);

                    }

                }.start();
            }
        });

        Switch isUseSMSCode = (Switch) findViewById(R.id.use_sms_code);
        mEmailView.setHint(R.string.passwordless_label);
        isUseSMSCode.setChecked(isVerificationCode);
        isUseSMSCode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked){
                    mobile_email = mEmailView.getText().toString();
                    verificationForm.setVisibility(View.VISIBLE);
                    mEmailView.setHint(R.string.passwordless_label);
                    mEmailView.setText(username);
                } else {
                    username = mEmailView.getText().toString();
                    verificationForm.setVisibility(View.GONE);
                    mEmailView.setHint(R.string.mobile);
                    mEmailView.setText(mobile_email);
                }
                isVerificationCode = isChecked;
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        handler = new Handler();
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(LoginActivity.this.getCurrentFocus().getWindowToken(), 0);
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mVerification.setError(null);

        boolean cancel = false;
        View focusView = null;

        String username = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String code = isVerificationCode ? mVerification.getText().toString() : null;

        if(TextUtils.isEmpty(username)){
            mEmailView.setError(getString(R.string.error_empty));
            cancel = true;
            focusView = mEmailView;
            if (cancel) {
                focusView.requestFocus();
                return;
            }
        }

        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_empty));
            focusView = mPasswordView;
            cancel = true;
            if (cancel) {
                focusView.requestFocus();
                return;
            }
        }

        if (isVerificationCode&&TextUtils.isEmpty(code)) {
            mVerification.setError(getString(R.string.error_empty));
            focusView = mVerification;
            cancel = true;
            if (cancel) {
                focusView.requestFocus();
                return;
            }
        }

        mAuthTask = new UserLoginTask(username, password, code);


        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true);
        mAuthTask.execute((Void) null);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public void sendCodeFail() {
        if(timer != null) timer.cancel();
        mSendCode.setText(R.string.send_code);
        mSendCode.setClickable(true);
        mSendCode.setEnabled(true);
    }

    public void sendCodeSuccess() { }

    public boolean sendSMSCode() {
        JsonObject obj = new JsonObject();
        obj.addProperty("channel","MOBILE");
        obj.addProperty("existed",true);
        obj.addProperty("method","SMS");
        obj.addProperty("username",mEmailView.getText().toString());

        POSHttpConnector.getInstance().post(getString(R.string.api_sms_code), obj, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(new Runnable(){
                    public void run(){
                        sendCodeFail();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()) {
                    handler.post(new Runnable() {
                        public void run() {
                            sendCodeSuccess();
                        }
                    });
                } else {
                    this.onFailure(call, null);
                }
            }
        });
        return false;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public void loginSuccess(){
        Intent intent = new Intent();
        intent.setClass(LoginActivity.this, ListActivity.class);
        startActivity(intent);
    }

    public void loginFail(){

    }

    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;
        private final String mCode;

        UserLoginTask(String email, String password, String code) {
            mEmail = email;
            mPassword = password;
            mCode = code;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            JsonObject obj = new JsonObject();
            JsonObject metadata = new JsonObject();
            if(isVerificationCode){
                metadata.addProperty("mechanism","password_and_code");
                obj.addProperty("$code", mCode);
                obj.addProperty("username", mEmail);
            } else {
                metadata.addProperty("mechanism","password");
                if(Pattern.matches("\\d+", mEmail)){
                    obj.addProperty("mobile", mEmail);
                } else {
                    obj.addProperty("email", mEmail);
                }
                //POSHttpConnector.getInstance()
            }
            obj.add("metadata", metadata);
            obj.addProperty("password", mPassword);

            POSHttpConnector.getInstance().post(getString(R.string.api_login), obj, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    handler.post(new Runnable() {
                        public void run() {
                            mAuthTask = null;
                            showProgress(false);
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String body = response.body().string();
                    if(response.isSuccessful()){
                        JsonObject result = new JsonParser().parse(body).getAsJsonObject();
                        POSHttpConnector.getInstance().setToken(result.get("access_token").getAsString());
                        POSHttpConnector.getInstance().setRefresh_token(result.get("refresh_token").getAsString());
                        handler.post(new Runnable() {
                            public void run() {
                                mAuthTask = null;
                                showProgress(false);
                                loginSuccess();
                            }
                        });
                    } else {
                        this.onFailure(call, null);
                    }
                }
            });

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

