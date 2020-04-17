package com.tsa.imagerecognition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private Button btnSignup, btnSignin;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();

        inputEmail = (EditText) findViewById(R.id.email);
        inputPassword = (EditText) findViewById(R.id.password);
        btnSignup = (Button) findViewById(R.id.sign_up_button);
        btnSignin = (Button) findViewById(R.id.sign_in_button);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        btnSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            }
        });

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String email = inputEmail.getText().toString();
                String password = inputPassword.getText().toString();

                if (!checkEmailAndPassword(email, password)) {
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);

                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Toast.makeText(SignupActivity.this, "createUserWithEmail:onComplete:" + task.isSuccessful(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);

                        if (!task.isSuccessful()) {
                            Toast.makeText(SignupActivity.this, "Authentication failed." + task.getException(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                            intent.putExtra("email", inputEmail.getText().toString());
                            startActivity(intent);
                            //finish();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
    }

    public void logOut(View view) {
        auth.signOut();
    }



    //Create test data and push it to Firebase Database
    public void generateData(View view) {
//        String image1 = "https://www.google.com/url?sa=i&url=https%3A%2F%2Fwww.forbes.com%2Fsites%2Fmarkgreene%2F2019%2F10%2F11%2Fhow-to-buy-a-house-with-10000%2F&psig=AOvVaw21g7PCnoitEJ4uGWGesgSt&ust=1581243678106000&source=images&cd=vfe&ved=0CAIQjRxqFwoTCLCNmfDdwecCFQAAAAAdAAAAABAD";
//        String image2 = "https://www.google.com/url?sa=i&url=http%3A%2F%2Fmediabitch.ru%2Fevent-details%2F&psig=AOvVaw1HpesCLwac2NcLOuTAL8FA&ust=1581246229426000&source=images&cd=vfe&ved=0CAIQjRxqFwoTCJDEjLHnwecCFQAAAAAdAAAAABAJ";
//
//        Event event1 = new Event(auth.getCurrentUser().getEmail(),"Party in John's house", "Really big night party at John", image1, "Nijniy Novgorod", new GregorianCalendar(2020,2, 7));
//        Event event2 = new Event(auth.getCurrentUser().getEmail(),"Music concert", "All favourites here! Lets go!", image2, "Nijniy Novgorod", new GregorianCalendar(2020,2, 17));
//
//
//        final FirebaseDatabase database = FirebaseDatabase.getInstance();
//
//        DatabaseReference ref = database.getReference();
//        DatabaseReference usersRef = ref.child("events");
//
//        //Need to move from the main thread‼️
//        usersRef.push().setValue(event1);
//        usersRef.push().setValue(event2);

    }

    private boolean checkEmailAndPassword(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), "Please enter email", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), "Please enter password", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < 6) {
            Toast.makeText(getApplicationContext(), "Password too short, enter minimum 6 characters!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!email.contains("@")) {
            Toast.makeText(getApplicationContext(), "Email incorrect", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
