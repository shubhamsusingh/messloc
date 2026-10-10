package com.example.messloc.viewmodel;

import android.view.View;

import androidx.databinding.ObservableField;

public class AuthViewModel {

    public ObservableField<String> name =
            new ObservableField<>("");


    public ObservableField<String> email =
            new ObservableField<>("");

    public ObservableField<String> password =
            new ObservableField<>("");

    public ObservableField<String> confirmPassword =
            new ObservableField<>("");

    public void onSignupBtn(View view) {

        String nameValue = name.get();
        String emailValue = email.get();
        String passwordValue = password.get();
        String confirmPasswordValue = confirmPassword.get();

        if (nameValue == null || nameValue.isEmpty()) {
            return;
        }

        if (emailValue == null || emailValue.isEmpty()) {
            return;
        }

        if (passwordValue == null || passwordValue.isEmpty()) {
            return;
        }

        if (!passwordValue.equals(confirmPasswordValue)) {
            return;
        }

        // Signup logic
    }

    public void onLoginBtn(View view){

    }
}