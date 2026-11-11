package com.eftyg4n.campusmate.activities;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ActivityAuthBinding;
import com.eftyg4n.campusmate.firebase.SyncManager;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

// Sign in / create account with Firebase Authentication (email + password, or Google).
// The app also works WITHOUT an account: this screen is opened from the Profile tab.
public class AuthActivity extends AppCompatActivity {

    public static final String EXTRA_START_REGISTER = "extra_start_register";

    private static final String TAG = "CampusMate_Auth";
    private static final String STATE_REGISTER = "state_register";

    private ActivityAuthBinding binding;
    private FirebaseAuth auth;
    private boolean registerMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return insets;
        });
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        try {
            auth = FirebaseAuth.getInstance();
        } catch (IllegalStateException e) {
            Toast.makeText(this, R.string.auth_error_not_configured, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        binding.buttonSubmit.setOnClickListener(view -> submit());
        binding.buttonSwitchMode.setOnClickListener(view -> setMode(!registerMode));
        binding.buttonForgot.setOnClickListener(view -> sendResetEmail());
        binding.buttonGoogle.setOnClickListener(view -> signInWithGoogle());

        boolean startRegister = savedInstanceState != null
                ? savedInstanceState.getBoolean(STATE_REGISTER)
                : getIntent().getBooleanExtra(EXTRA_START_REGISTER, false);
        setMode(startRegister);
    }

    private void setMode(boolean register) {
        registerMode = register;
        binding.toolbar.setTitle(register ? R.string.auth_title_register : R.string.auth_title_login);
        binding.buttonSubmit.setText(register ? R.string.auth_button_register
                : R.string.auth_button_login);
        binding.buttonSwitchMode.setText(register ? R.string.auth_switch_to_login
                : R.string.auth_switch_to_register);
        binding.layoutConfirm.setVisibility(register ? View.VISIBLE : View.GONE);
        binding.buttonForgot.setVisibility(register ? View.GONE : View.VISIBLE);
        clearErrors();
    }

    private void submit() {
        clearErrors();
        String email = textOf(binding.editEmail);
        String password = binding.editPassword.getText() == null ? ""
                : binding.editPassword.getText().toString();
        String confirm = binding.editConfirm.getText() == null ? ""
                : binding.editConfirm.getText().toString();

        boolean valid = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.setError(getString(R.string.auth_error_email));
            valid = false;
        }
        if (password.length() < 6) { // Firebase's own minimum
            binding.layoutPassword.setError(getString(R.string.auth_error_password));
            valid = false;
        }
        if (registerMode && !password.equals(confirm)) {
            binding.layoutConfirm.setError(getString(R.string.auth_error_confirm));
            valid = false;
        }
        if (!valid) {
            return;
        }

        setBusy(true);
        Task<AuthResult> task = registerMode
                ? auth.createUserWithEmailAndPassword(email, password)
                : auth.signInWithEmailAndPassword(email, password);
        // No Activity passed on purpose: the result must be handled even if the screen
        // was rotated meanwhile (the sign in itself already happened on the server).
        task.addOnCompleteListener(result -> {
            if (result.isSuccessful()) {
                onSuccess(registerMode);
            } else if (!isDestroyed()) {
                setBusy(false);
                showError(result.getException());
            }
        });
    }

    private void onSuccess(boolean newEmailAccount) {
        FirebaseUser user = auth.getCurrentUser();
        if (newEmailAccount && user != null) {
            user.sendEmailVerification(); // best effort, we do not block the student
        }
        // Wait for the first sync, so the account's own profile is already on the phone
        // when the next screen opens (a few seconds at most).
        setBusy(true);
        SyncManager.onSignedIn(getApplicationContext(), () -> {
            if (!isDestroyed()) {
                Toast.makeText(this, newEmailAccount ? R.string.auth_toast_registered
                        : R.string.auth_toast_signed_in, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void sendResetEmail() {
        String email = textOf(binding.editEmail);
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.setError(getString(R.string.auth_error_email));
            return;
        }
        setBusy(true);
        auth.sendPasswordResetEmail(email).addOnCompleteListener(result -> {
            if (isDestroyed()) {
                return;
            }
            setBusy(false);
            if (result.isSuccessful()) {
                // Same text whether the account exists or not (does not leak who has an account).
                Toast.makeText(this, R.string.auth_toast_reset_sent, Toast.LENGTH_LONG).show();
            } else {
                showError(result.getException());
            }
        });
    }

    private void showError(Exception e) {
        int text;
        if (e instanceof FirebaseAuthWeakPasswordException) {
            text = R.string.auth_error_weak_password;
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            text = R.string.auth_error_email_in_use;
        } else if (e instanceof FirebaseAuthInvalidCredentialsException
                || e instanceof FirebaseAuthInvalidUserException) {
            // Wrong password and unknown email look the same on purpose.
            text = R.string.auth_error_wrong_credentials;
        } else if (e instanceof FirebaseNetworkException) {
            text = R.string.auth_error_network;
        } else {
            text = R.string.auth_error_other;
        }
        binding.textError.setText(text);
        binding.textError.setVisibility(View.VISIBLE);
    }

    // ---- Google sign-in ----------------------------------------------------------------

    private void signInWithGoogle() {
        clearErrors();
        // default_web_client_id is generated by the google-services plugin from
        // google-services.json. Looked up by name so the app still compiles (and shows a
        // clear message) when the file has no Google client yet.
        int id = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        if (id == 0) {
            showMessage(R.string.auth_error_google_not_configured);
            return;
        }
        String serverClientId = getString(id);

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(new GetSignInWithGoogleOption.Builder(serverClientId).build())
                .build();

        setBusy(true);
        CredentialManager.create(this).getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        handleGoogleCredential(response.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        setBusy(false);
                        if (e instanceof GetCredentialCancellationException) {
                            return; // the student closed the chooser: not an error
                        }
                        Log.w(TAG, "Google sign-in failed", e);
                        showMessage(e instanceof NoCredentialException
                                ? R.string.auth_error_google_no_account
                                : R.string.auth_error_google_failed);
                    }
                });
    }

    private void handleGoogleCredential(Credential credential) {
        if (credential instanceof CustomCredential
                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                .equals(credential.getType())) {
            try {
                GoogleIdTokenCredential google =
                        GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());
                // Hand the Google ID token to Firebase: same account on every phone.
                auth.signInWithCredential(GoogleAuthProvider.getCredential(google.getIdToken(), null))
                        .addOnCompleteListener(result -> {
                            if (result.isSuccessful()) {
                                onSuccess(false);
                            } else if (!isDestroyed()) {
                                setBusy(false);
                                showError(result.getException());
                            }
                        });
                return;
            } catch (RuntimeException e) {
                Log.w(TAG, "Bad Google token", e);
            }
        }
        setBusy(false);
        showMessage(R.string.auth_error_google_failed);
    }

    private void showMessage(int text) {
        binding.textError.setText(text);
        binding.textError.setVisibility(View.VISIBLE);
    }

    private void setBusy(boolean busy) {
        binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.buttonSubmit.setEnabled(!busy);
        binding.buttonSwitchMode.setEnabled(!busy);
        binding.buttonForgot.setEnabled(!busy);
        binding.buttonGoogle.setEnabled(!busy);
    }

    private void clearErrors() {
        binding.layoutEmail.setError(null);
        binding.layoutPassword.setError(null);
        binding.layoutConfirm.setError(null);
        binding.textError.setVisibility(View.GONE);
    }

    private String textOf(com.google.android.material.textfield.TextInputEditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString().trim();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_REGISTER, registerMode);
    }
}
