package com.example.messloc.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.messloc.data.model.AuthResponse;
import com.example.messloc.data.model.LoginRequest;
import com.example.messloc.data.model.RegisterRequest;
import com.example.messloc.data.repository.AuthRepository;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    public MutableLiveData<String> loadingMessage = new MutableLiveData<>("Please wait...");
    public MutableLiveData<String> name = new MutableLiveData<>("");
    public MutableLiveData<String> email = new MutableLiveData<>("");
    public MutableLiveData<String> phone = new MutableLiveData<>("");
    public MutableLiveData<String> password = new MutableLiveData<>("");
    public MutableLiveData<String> confirmPassword = new MutableLiveData<>("");

    public MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public MutableLiveData<AuthResponse> authResult = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
    }

    public void register() {

        String userName = name.getValue();
        String userEmail = email.getValue();
        String userPhone = phone.getValue();
        String userPassword = password.getValue();

        if (isEmpty(userName) || isEmpty(userEmail)
                || isEmpty(userPhone) || isEmpty(userPassword)) {

            errorMessage.setValue("Please fill all fields");
            return;
        }

        loading.setValue(true);

        RegisterRequest request = new RegisterRequest(
                userName.trim(),
                userEmail.trim(),
                userPhone.trim(),
                userPassword
        );

        repository.register(request, new AuthRepository.AuthCallback() {

            @Override
            public void onSuccess(AuthResponse response) {
                loading.postValue(false);
                authResult.postValue(response);
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void login() {

        String userEmail = email.getValue();
        String userPassword = password.getValue();

        if (isEmpty(userEmail) || isEmpty(userPassword)) {
            errorMessage.setValue("Please enter email and password");
            return;
        }

        loadingMessage.postValue("Logging you in...");
        loading.setValue(true);

        LoginRequest request = new LoginRequest(
                userEmail.trim(),
                userPassword
        );

        repository.login(request, new AuthRepository.AuthCallback() {

            @Override
            public void onSuccess(AuthResponse response) {
                loading.postValue(false);
                authResult.postValue(response);
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}