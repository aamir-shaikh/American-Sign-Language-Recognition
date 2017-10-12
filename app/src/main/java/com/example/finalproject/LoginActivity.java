package com.example.finalproject;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private String emailText, passwordText;
    private EditText emailBox, passwordBox;
    private Button signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_screen);

        emailBox = (EditText)findViewById(R.id.email);
        passwordBox = (EditText)findViewById(R.id.password);
        signInButton = (Button) findViewById(R.id.email_sign_in_button);

        emailText = emailBox.getText().toString();
        passwordText = passwordBox.getText().toString();

        //Check database for email and password
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean success = checkCredentials(emailText, passwordText);
                sendToTutorialScreen(success);
            }
        });
    }

    private void sendToTutorialScreen(boolean success) {
        if(success == false){
            Toast.makeText(this, "Email or password wrong", Toast.LENGTH_SHORT);
            return;
        }
        else{
            Intent mainMenu = new Intent(this, MainMenu.class);
            startActivity(mainMenu);
        }
    }

    private boolean checkCredentials(String emailText, String passwordText) {
        //connect with database
        return true;
    }
}
