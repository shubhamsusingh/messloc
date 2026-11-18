package com.example.messloc.viewmodel;

import android.app.Application;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.messloc.data.model.OwnerSignupRequest;
import com.example.messloc.data.model.OwnerSignupResponse;
import com.example.messloc.data.repository.AuthRepository;

public class OwnerSignupViewModel extends AndroidViewModel {

    private final AuthRepository repository;

    public MutableLiveData<String> name = new MutableLiveData<>("");
    public MutableLiveData<String> email = new MutableLiveData<>("");
    public MutableLiveData<String> phone = new MutableLiveData<>("");
    public MutableLiveData<String> password = new MutableLiveData<>("");

    public MutableLiveData<String> businessName =
            new MutableLiveData<>("");

    public MutableLiveData<String> businessDescription =
            new MutableLiveData<>("");

    public MutableLiveData<String> businessAddress =
            new MutableLiveData<>("");

    public MutableLiveData<String> fssaiLicenseNumber =
            new MutableLiveData<>("");

    public MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    public MutableLiveData<String> loadingMessage =
            new MutableLiveData<>("Registering your business...");

    public MutableLiveData<String> errorMessage =
            new MutableLiveData<>();

    public MutableLiveData<OwnerSignupResponse> signupResult =
            new MutableLiveData<>();

    public OwnerSignupViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
    }

    public void registerOwner() {

        String ownerName = getValue(name);
        String ownerEmail = getValue(email);
        String ownerPhone = getValue(phone);
        String ownerPassword = getValue(password);

        String business = getValue(businessName);
        String description = getValue(businessDescription);
        String address = getValue(businessAddress);
        String fssai = getValue(fssaiLicenseNumber);

        // Personal information validation

        if (ownerName.length() < 2 || ownerName.length() > 50) {
            errorMessage.setValue(
                    "Name must contain 2 to 50 characters."
            );
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(ownerEmail).matches()) {
            errorMessage.setValue("Enter a valid email address.");
            return;
        }

        if (!ownerPhone.matches("\\d{10}")) {
            errorMessage.setValue(
                    "Phone number must contain exactly 10 digits."
            );
            return;
        }

        if (ownerPassword.length() < 8) {
            errorMessage.setValue(
                    "Password must contain at least 8 characters."
            );
            return;
        }

        // Business information validation

        if (business.length() < 3 || business.length() > 100) {
            errorMessage.setValue(
                    "Business name must contain 3 to 100 characters."
            );
            return;
        }

        if (description.length() < 10 || description.length() > 500) {
            errorMessage.setValue(
                    "Description must contain 10 to 500 characters."
            );
            return;
        }

        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }

        errorMessage.setValue(null);
        loadingMessage.setValue("Registering your business...");
        loading.setValue(true);

        OwnerSignupRequest request = new OwnerSignupRequest(
                ownerName,
                ownerEmail,
                ownerPassword,
                ownerPhone,
                business,
                description,
                address.isEmpty() ? null : address,
                fssai.isEmpty() ? null : fssai
        );

        repository.registerOwner(
                request,
                new AuthRepository.OwnerSignupCallback() {

                    @Override
                    public void onSuccess(OwnerSignupResponse response) {
                        loading.postValue(false);
                        signupResult.postValue(response);
                    }

                    @Override
                    public void onError(String message) {
                        loading.postValue(false);
                        errorMessage.postValue(message);
                    }
                }
        );
    }

    private String getValue(MutableLiveData<String> liveData) {
        String value = liveData.getValue();
        return value == null ? "" : value.trim();
    }
}