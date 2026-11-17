package com.example.messloc.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.messloc.data.model.MessCreateResponse;
import com.example.messloc.data.model.owner.OwnerMess;
import com.example.messloc.data.repository.OwnerMessRepository;

import java.util.List;
import com.example.messloc.data.model.MessUpdateRequest;
import com.example.messloc.data.repository.OwnerRepository;

public class OwnerMessViewModel extends AndroidViewModel {

    private final OwnerMessRepository repository;

    // Owner mess listings
    private final MutableLiveData<List<OwnerMess>> myMesses =
            new MutableLiveData<>();

    // Selected mess details
    private final MutableLiveData<OwnerMess> selectedMess =
            new MutableLiveData<>();

    // Loading state
    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    // Error state
    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    // Success message
    private final MutableLiveData<String> message =
            new MutableLiveData<>();

    public OwnerMessViewModel(@NonNull Application application) {
        super(application);

        repository = new OwnerMessRepository(
                application.getApplicationContext()
        );
    }

    // LiveData observers
    public LiveData<List<OwnerMess>> getMyMesses() {
        return myMesses;
    }

    public LiveData<OwnerMess> getSelectedMess() {
        return selectedMess;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    // Fetch all mess listings of the authenticated owner
    public void loadMyMesses() {

        loading.setValue(true);
        error.setValue(null);
        message.setValue(null);

        repository.getMyMesses(
                new OwnerMessRepository.RepositoryCallback<List<OwnerMess>>() {

                    @Override
                    public void onSuccess(
                            List<OwnerMess> data,
                            String responseMessage
                    ) {

                        loading.postValue(false);
                        myMesses.postValue(data);
                        message.postValue(responseMessage);
                    }

                    @Override
                    public void onError(String errorMessage) {

                        loading.postValue(false);
                        error.postValue(errorMessage);
                    }
                }
        );
    }

    // Fetch complete details of a selected mess
    public void loadMyMessById(String messId) {

        if (messId == null || messId.trim().isEmpty()) {
            error.setValue("Invalid mess ID.");
            return;
        }

        loading.setValue(true);
        error.setValue(null);
        message.setValue(null);

        repository.getMyMessById(
                messId,
                new OwnerMessRepository.RepositoryCallback<OwnerMess>() {

                    @Override
                    public void onSuccess(
                            OwnerMess data,
                            String responseMessage
                    ) {

                        loading.postValue(false);
                        selectedMess.postValue(data);
                        message.postValue(responseMessage);
                    }

                    @Override
                    public void onError(String errorMessage) {

                        loading.postValue(false);
                        error.postValue(errorMessage);
                    }
                }
        );
    }

//    Update mess
    public void updateMess(String messId, MessUpdateRequest request) {

    loading.setValue(true);
    error.setValue(null);
    message.setValue(null);

    repository.updateMess(
            messId,
            request,
            new OwnerMessRepository.RepositoryCallback<MessCreateResponse>() {

                @Override
                public void onSuccess(
                        MessCreateResponse data,
                        String responseMessage
                ) {
                    loading.postValue(false);

                    message.postValue(
                            responseMessage != null
                                    ? responseMessage
                                    : "Mess updated successfully."
                    );

                    loadMyMessById(messId);
                    loadMyMesses();
                }

                @Override
                public void onError(String errorMessage) {
                    loading.postValue(false);
                    error.postValue(errorMessage);
                }
            }
    );
}
    // Clear previous error
    public void clearError() {
        error.setValue(null);
    }

    // Clear previous message
    public void clearMessage() {
        message.setValue(null);
    }
}
