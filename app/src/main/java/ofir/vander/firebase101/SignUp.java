package ofir.vander.firebase101;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUp extends AppCompatActivity {

    // debug TAG
    private static final String TAG = "SignUp";
    // view objects
    TextInputLayout tilEmail, tilPassword, tilUserName;
    TextInputEditText etEmail, etPassword, etUserName;
    TextView tvHiddenRules;
    Button bSignUp;
    //String Holders
    String email="", password="", userName="";
    // Firebase Authentication
    private FirebaseAuth mAuth;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        init();

        //when password text field is pushed
        etPassword.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus)
                tvHiddenRules.setVisibility(View.VISIBLE);
            else
                tvHiddenRules.setVisibility(View.INVISIBLE);
        });


        // when button pushed
        bSignUp.setOnClickListener(view -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();
            String userName = etUserName.getText().toString();

            if(email.isEmpty()){
                tilEmail.setError("Email Required");
                return;
            }
            if(password.isEmpty()){
                tilPassword.setError("Password Required");
                return;
            }
            if(userName.isEmpty()){
                tilUserName.setError("UserName Required");
                return;
            }
            if(!isValidPassword(password)){
                tilPassword.setError("Password Rules Below");
                tvHiddenRules.setVisibility(View.VISIBLE);
                return;
            }

            Toast.makeText(SignUp.this, "Signing Up", Toast.LENGTH_SHORT).show();

            // method from the firebase authentication library to create a new user with email / password
            // it is asynchronic, so we need a callback
            // we place a listener on it with a task object.
            // we implement actions if the task is successful and if not
            mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success");
                    FirebaseUser fbUser = mAuth.getCurrentUser();
                    // optionally, initiate email verification
                    // fbUser.sendEmailVerification().addOnCompleteListener(taskVerify -> {
                    //    if (taskVerify.isSuccessful()) {
                    //        Log.d(TAG, "Email sent.");
                    //        Toast.makeText(SignUp.this, "Verification Email sent to " + email, Toast.LENGTH_SHORT).show();
                    //    }
                    //    else {
                    //        Log.e(TAG, "sendEmailVerification", taskVerify.getException());
                    //        Toast.makeText(SignUp.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                    //    }
                    //
                    // });
                    // in which case we'd also want to add an " I Verified my email" button, that goes to login screen

                    // My method to create a new User object in my app Realtime Database
                    createUserAndNextActivity(fbUser.getUid(), userName);
                }
                else {
                    // check why it failed
                    Exception e = task.getException();
                    Log.w(TAG, "createUserWithEmail:failure", e);
                    String errorMessage = e.getMessage();
                    if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        // invalid credentials. which credential?
                        Log.e(TAG, "Invalid Credentials: "+errorMessage);
                        if(errorMessage.contains("ERROR_INVALID_EMAIL"))
                            tilEmail.setError("Valid email address Please!");
                    }
                    else if (e instanceof FirebaseAuthUserCollisionException) {
                        // user already exists
                        Log.e(TAG, "User already exists: "+errorMessage);
                        tilEmail.setError("This Email is already used!");
                    }
                    //  Firebase also checks for weak password = under 6 characters
                    // here we have our own isValidPassword() method
                    // else if (e instanceof FirebaseAuthWeakPasswordException)
                    //     tilPassword.setError("Password must be at least 6 characters");
                    else {
                        Log.e(TAG, "Unknown error: " + errorMessage);
                        Toast.makeText(SignUp.this, "Unknown error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }

                }
            }); // listener on the task

        }); // listener on the button

    }  // onCreate

    private void createUserAndNextActivity(String uid, String userName){
        // first create a User object
        User currentUser = new User(uid, userName);
        // then put it in the GameGlobalsSingleton - for other activities to access
        GameGlobalsSingleton.getInstance().setCurrentUser(currentUser);
        // Then write it to firebase. first reference the Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        // and reference a new node (leaf) in the tree for the new user
        DatabaseReference userNode = database.getReference("Users").child(uid);
        // This is the Firebase Realtime Database write method
        // we place a listener on it, to see that its successful
        // the setValue method returns a Task<Void> - it doesnt return any object
        userNode.setValue(currentUser).addOnCompleteListener(aVoid -> {
            // if successfull
            Log.d(TAG, "User created successfully with uid " + uid);
            Toast.makeText(SignUp.this, "User created successfully.", Toast.LENGTH_SHORT).show();
            // go to dbFetchWait screen
            Intent go2DBFetchWait = new Intent(this, DBFetchWait.class);
            startActivity(go2DBFetchWait);
        }).addOnFailureListener(e -> {
            // if failed
            Log.e(TAG, "Failed to create user in database", e);
            Toast.makeText(SignUp.this, "Failed to create user in database.", Toast.LENGTH_SHORT).show();
        });
    }

    private boolean isValidPassword(String password) {
        // Password must be at least 6 characters long and contain at least one letter and one digit
        return password.matches(".*[A-Za-z].*") && password.matches(".*\\d.*") && password.length() >= 6;
    }

    private void init(){
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilUserName = findViewById(R.id.tilUserName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etUserName = findViewById(R.id.etUserName);
        bSignUp = findViewById(R.id.bSignUp);
        tvHiddenRules = findViewById(R.id.tvHiddenRules);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

    }
}