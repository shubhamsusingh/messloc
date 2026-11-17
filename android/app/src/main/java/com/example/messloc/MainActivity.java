package com.example.messloc;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.splashscreen.SplashScreen;

import com.example.messloc.ui.auth.LoginActivity;
import com.example.messloc.utils.TokenManager;

public class MainActivity extends AppCompatActivity {

    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

       // EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        tokenManager = new TokenManager(this);

        if (tokenManager.hasValidSession()) {

            String role = tokenManager.getUserRole();

            Intent intent;

            if ("ADMIN".equalsIgnoreCase(role)) {

                intent = new Intent(
                        this,
                        com.example.messloc.ui.admin.AdminActivity.class
                );

            } else if ("USER".equalsIgnoreCase(role)
                    || "OWNER".equalsIgnoreCase(role)) {

                intent = new Intent(
                        this,
                        com.example.messloc.ui.main.MainActivity.class
                );

            } else {

                tokenManager.clearSession();

                intent = new Intent(
                        this,
                        LoginActivity.class
                );
            }

            startActivity(intent);

        } else {

            startActivity(new Intent(
                    this,
                    LoginActivity.class
            ));
        }

        finish();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}

