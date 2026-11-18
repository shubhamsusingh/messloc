
package com.example.messloc.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.messloc.data.model.UserProfile;
import com.example.messloc.data.model.UserProfileResponse;
import com.example.messloc.data.repository.UserRepository;

public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository;

    private final MutableLiveData<UserProfile> profile =
            new MutableLiveData<>();

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> errorMessage =
            new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
    }

    public LiveData<UserProfile> getProfile() {
        return profile;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadProfile() {

        loading.setValue(true);
        errorMessage.setValue(null);

        userRepository.getMyProfile(
                new UserRepository.ProfileCallback() {

                    @Override
                    public void onSuccess(UserProfileResponse response) {

                        loading.postValue(false);
                        profile.postValue(response.getData());
                    }

                    @Override
                    public void onError(String message) {

                        loading.postValue(false);
                        errorMessage.postValue(message);
                    }
                }
        );
    }
}
