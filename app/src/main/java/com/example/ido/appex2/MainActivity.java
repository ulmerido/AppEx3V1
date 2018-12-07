package com.example.ido.appex2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;



public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Main Activity:";
    private FirebaseAuth        m_Auth;
    private GoogleSignInClient  m_GoogleSignInClient;
    private SignInButton        m_GoogleSignInButton;
    private CallbackManager     m_CallbackManager;
    private AccessTokenTracker  m_AccessTokenTracker;
    private LoginButton         m_FacebookLogin_btn;
    private TextView            m_SignUp_tv;
    private Button              m_BtnSignin;
    private TextView            m_EtUserEmail;
    private TextView            m_tvRecoverPassword;
    private TextView            m_EtUserPassword;
    private TextView            m_TvAnonymous;
    private String              m_EmailReset;
    private FirebaseRemoteConfig m_RemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        m_RemoteConfig = FirebaseRemoteConfig.getInstance();
        m_Auth = FirebaseAuth.getInstance();
        ifLogedInGoToUserActivity();
        m_FacebookLogin_btn = (LoginButton) findViewById(R.id.login_button);
        m_CallbackManager = CallbackManager.Factory.create();
        m_GoogleSignInButton = (SignInButton) findViewById(R.id.googleSignInButton);
        m_SignUp_tv = (TextView) findViewById(R.id.tvSignUp);
        m_BtnSignin = (Button) findViewById(R.id.btn_SignIn);
        m_tvRecoverPassword = (TextView) findViewById(R.id.tvForgetPass);
        m_EtUserEmail = (TextView) findViewById(R.id.etEmail);
        m_EtUserPassword = (TextView) findViewById(R.id.et_UserPassword);
        m_TvAnonymous = (TextView) findViewById(R.id.tvAnonymous);
        ValidationChecker.CheckPassword(m_EtUserPassword);
        ValidationChecker.CheckEmail(m_EtUserEmail);
        googleSignInBuilder();
        facebookLoginInit();
        setRemoteConfig();
        showAnonymousLogin();

        m_GoogleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignIn();
            }
        });
        m_SignUp_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickSignup();
            }
        });
        m_tvRecoverPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openResetPasswordDialog();
            }
        });

        m_BtnSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickSignin();
            }
        });

        m_TvAnonymous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickAnonymos();
            }
        });


    }

    private void onClickAnonymos()
    {
        signAnonymosly();
        ifLogedInGoToUserActivity();
    }

    private void onClickSignup()
    {
        Intent intent_SignUp = new Intent(getApplicationContext(), SignUpActivity.class);
        startActivity(intent_SignUp);
        slideUpToNewActivity();
    }

    private void setRemoteConfig()
    {
        m_RemoteConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(true)
                .build());
        HashMap<String, Object> defaults = new HashMap<>();
        defaults.put("show_anonymous", true);
        m_RemoteConfig.setDefaults(defaults);
        Task<Void> fetch = m_RemoteConfig.fetch(0);
        fetch.addOnSuccessListener(this, new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                m_RemoteConfig.activateFetched();

            }
        });
    }

    private void signAnonymosly() {

        Log.e(TAG, "signAnonymosly >>");
        m_Auth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInAnonymously:success");
                            FirebaseUser user = m_Auth.getCurrentUser();
                        }
                        else
                            {
                            Log.w(TAG, "signInAnonymously:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });
        Log.e(TAG, "signAnonymosly <<");
    }

    public void openResetPasswordDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);

        final EditText emailToReset = new EditText(this);
        emailToReset.setHint("youremail@somthing.som");
        builder.setView(emailToReset);
        builder.setMessage("Please enter the email you signed up with");
        builder.setTitle("Forgot Password");
        builder.setIcon(android.R.drawable.ic_menu_revert);
        builder.setCancelable(true);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_EmailReset = emailToReset.getText().toString();
                recoverPassword();
            }
        });

        builder.show();
    }

    private void recoverPassword() {
        try {
            if (m_Auth.getCurrentUser() == null) {
                m_Auth.sendPasswordResetEmail(m_EmailReset)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "Password reset request sent", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Failed to reset password", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } else {
                Toast.makeText(MainActivity.this, "log out first", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }

    }

    private void showAnonymousLogin() {
        boolean show_anonymos = (boolean) m_RemoteConfig.getBoolean("show_anonymous");
        if (show_anonymos) {
            m_TvAnonymous.setVisibility(View.VISIBLE);
        } else {
            m_TvAnonymous.setVisibility(View.GONE);
        }

    }

    private void onClickSignin()
    {
        if (ValidationChecker.CheckValidEmail(m_EtUserEmail) && ValidationChecker.CheckValidPassword(m_EtUserPassword))
        {
            Log.e(TAG, "onClickSignin >>");
            final String passString = m_EtUserPassword.getText().toString().trim();
            final String emailString = m_EtUserEmail.getText().toString().trim();
            m_Auth.signInWithEmailAndPassword(emailString, passString)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful())
                            {
                                if (m_Auth.getCurrentUser().isEmailVerified()) {
                                    Log.d(TAG, "signInWithEmail:success");
                                    FirebaseUser user = m_Auth.getCurrentUser();
                                    Toast.makeText(MainActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                                    ifLogedInGoToUserActivity();
                                }
                                else
                                    {
                                    m_Auth.signOut();
                                    Toast.makeText(MainActivity.this, "unverified email",
                                            Toast.LENGTH_SHORT).show();
                                }

                                Log.e(TAG, "signin <<");
                            }
                            else
                            {
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
        else if (!ValidationChecker.CheckValidPassword(m_EtUserPassword) && !ValidationChecker.CheckValidEmail(m_EtUserEmail))
        {
            Toast.makeText(MainActivity.this, "Please fill in email and password to continue",
                    Toast.LENGTH_SHORT).show();
        }
        else if (!ValidationChecker.CheckValidEmail(m_EtUserEmail))
        {
            Toast.makeText(MainActivity.this, "Please fill in email to continue",
                    Toast.LENGTH_SHORT).show();
        }
        else if (!ValidationChecker.CheckValidPassword(m_EtUserPassword))
        {
            Toast.makeText(MainActivity.this, "Please fill in password to continue",
                    Toast.LENGTH_SHORT).show();
        }
    }
    private void ifLogedInGoToUserActivity()
    {
        Log.e(TAG, "ifLogedInGoToUserActivity >>");

        if(m_Auth.getCurrentUser()!=null)
        {
            Intent intent_UserActivity = new Intent(getApplicationContext(), UserActivity.class);
            startActivity(intent_UserActivity);
            finish();
        }
        Log.e(TAG, "ifLogedInGoToUserActivity <<");

    }
    private void googleSignInBuilder()
    {
        Log.e(TAG, "googleSignInBuilder >>");
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        m_GoogleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso);
        Log.e(TAG, "googleSignInBuilder <<");
    }

    private void googleSignIn() {
        Log.e(TAG, "googleSignIn >>");
        Intent signInIntent = m_GoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, 101);
        Log.e(TAG, "googleSignIn <<");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "onActivityResult >>");
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == 101) {
            Task<GoogleSignInAccount> task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("Sign IN:", "Google sign in failed", e);
                // ...
            }

        }

        m_CallbackManager.onActivityResult(requestCode, resultCode, data);

        Log.e(TAG, "onActivityResult <<");
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount i_Account) {
        Log.e(TAG, "firebaseAuthWithGoogle >>");
        Log.d(TAG, "firebaseAuthWithGoogle:" + i_Account.getId());
        firebaseAuthWithGoogleAndFacebook(GoogleAuthProvider.getCredential(i_Account.getIdToken(), null));
        Log.e(TAG, "firebaseAuthWithGoogle <<");
    }

    private void facebookLoginInit() {
        Log.e(TAG, "facebookLoginInit() >>");

        m_FacebookLogin_btn.setReadPermissions("email", "public_profile");
        m_FacebookLogin_btn.registerCallback(m_CallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.e(TAG, "facebook:onSuccess () >>" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
                Log.e(TAG, "facebook:onSuccess () <<");
            }

            @Override
            public void onCancel() {
                Log.e(TAG, "facebook:onCancel() >>");
                Log.e(TAG, "facebook:onCancel() <<");
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(TAG, "facebook:onError () >>" + error.getMessage());
                Log.e(TAG, "facebook:onError <<");
            }
        });

        m_AccessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {
                if (currentAccessToken == null) {
                    m_Auth.signOut();
                }

                Log.e(TAG, "onCurrentAccessTokenChanged() >> currentAccessToken=" + (currentAccessToken != null ? currentAccessToken.getToken() : "Null") + " ,oldAccessToken=" + (oldAccessToken != null ? oldAccessToken.getToken() : "Null"));
            }
        };

        Log.e(TAG, "facebookLoginInit() <<");
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.e(TAG, "handleFacebookAccessToken () >>" + token.getToken());
        firebaseAuthWithGoogleAndFacebook(FacebookAuthProvider.getCredential(token.getToken()));
        Log.e(TAG, "handleFacebookAccessToken () <<");
    }

    private void firebaseAuthTaskCheck(Task<AuthResult> i_Task) {
        Log.e(TAG, "firebaseAuthTaskCheck() >>");

        if (i_Task.isSuccessful()) {
            Log.d(TAG, "signInWithCredential:success");
            ifLogedInGoToUserActivity();
            Toast.makeText(getApplicationContext(), "User logged in successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), i_Task.getException().toString(), Toast.LENGTH_SHORT).show();
            Log.w(TAG, "signInWithCredential:failure", i_Task.getException());
        }
        Log.e(TAG, "firebaseAuthTaskCheck() <<");
    }

    private void firebaseAuthWithGoogleAndFacebook(AuthCredential i_Credential) {
        Log.e(TAG, "firebaseAuthWithGoogleAndFacebook() >>");
        m_Auth.signInWithCredential(i_Credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        firebaseAuthTaskCheck(task);
                    }
                });
        Log.e(TAG, "firebaseAuthWithGoogleAndFacebook() <<");
    }

    private void slideUpToNewActivity() {
        finish();
        overridePendingTransition(R.anim.slide_up, R.anim.no_animation);
    }
}