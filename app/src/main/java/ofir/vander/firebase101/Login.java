package ofir.vander.firebase101;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Context;

public class Login extends AppCompatActivity {


    TextInputLayout tilEmailIn, tilPasswordIn ;
    TextInputEditText etEmailIn,  etPasswordIn ;
    Button bSignIn;
    String email="", password="";
    static Context context;
    TextView tvSignUpHere;
    String fullText = "Not signed up yet? Sign up here";
    String clickableText = "Sign up here";
    SpannableString spannableString ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        init();

        // listener for the sign up clickable String
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Intent to navigate to your SignUpActivity
                Intent go2SignUp = new Intent(context, SignUp.class);
                startActivity(go2SignUp);
            }
        };
        // Find the start and end of the clickable text
        int startIndex = fullText.indexOf(clickableText);
        int endIndex = startIndex + clickableText.length();

        if (startIndex != -1) {
            // Apply the clickable span
            spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Apply an underline span
            spannableString.setSpan(new UnderlineSpan(), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        // Sets the Link in the TextView and Makes the link clickable
        tvSignUpHere.setText(spannableString);
        tvSignUpHere.setMovementMethod(LinkMovementMethod.getInstance());

        // Listener for the Sign In Button
        bSignIn.setOnClickListener(v -> {
            Intent go2DBFetchWait = new Intent(context, DBFetchWait.class);
            startActivity(go2DBFetchWait);
        });


    }
    void init() {
        context = this;
        tilEmailIn = findViewById(R.id.tilEmailIn);
        tilPasswordIn = findViewById(R.id.tilPasswordIn);
        etEmailIn = findViewById(R.id.etEmailIn);
        etPasswordIn = findViewById(R.id.etPasswordIn);
        bSignIn = findViewById(R.id.bSignIn);
        tvSignUpHere = findViewById(R.id.tvSignUpHere);
        spannableString = new SpannableString(fullText);


    }


}