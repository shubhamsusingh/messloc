
package com.example.messloc.viewmodel.admin;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.messloc.data.model.admin.AdminHealth;
import com.example.messloc.data.model.admin.AdminMess;
import com.example.messloc.data.model.admin.AdminOwner;
import com.example.messloc.data.model.admin.CreateAdminRequest;
import com.example.messloc.data.repository.AdminRepository;

import java.util.List;

public class AdminViewModel extends AndroidViewModel {

    private final AdminRepository repository;

    private final MutableLiveData<List<AdminMess>> allMesses =
            new MutableLiveData<>();

    private final MutableLiveData<List<AdminMess>> pendingMesses =
            new MutableLiveData<>();

    private final MutableLiveData<List<AdminOwner>> pendingOwners =
            new MutableLiveData<>();

    private final MutableLiveData<AdminHealth> adminHealth =
            new MutableLiveData<>();

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    private final MutableLiveData<String> message =
            new MutableLiveData<>();

    public AdminViewModel(@NonNull Application application) {
        super(application);
        repository = new AdminRepository(application);
    }

    // LiveData getters

    public MutableLiveData<List<AdminMess>> getAllMesses() {
        return allMesses;
    }

    public MutableLiveData<List<AdminMess>> getPendingMesses() {
        return pendingMesses;
    }

    public MutableLiveData<List<AdminOwner>> getPendingOwners() {
        return pendingOwners;
    }

    public MutableLiveData<AdminHealth> getAdminHealth() {
        return adminHealth;
    }

    public MutableLiveData<Boolean> getLoading() {
        return loading;
    }

    public MutableLiveData<String> getError() {
        return error;
    }

    public MutableLiveData<String> getMessage() {
        return message;
    }

    // Load all messes

    public void loadAllMesses() {
        loading.setValue(true);
        error.setValue(null);

        repository.getAllMesses(new AdminRepository.RepositoryCallback<List<AdminMess>>() {
            @Override
            public void onSuccess(List<AdminMess> data, String msg) {
                allMesses.postValue(data);
                loading.postValue(false);
                message.postValue(msg);
            }

            @Override
            public void onError(String errorMessage) {
                loading.postValue(false);
                error.postValue(errorMessage);
            }
        });
    }

    // Load pending messes

    public void loadPendingMesses() {
        loading.setValue(true);
        error.setValue(null);

        repository.getPendingMesses(new AdminRepository.RepositoryCallback<List<AdminMess>>() {
            @Override
            public void onSuccess(List<AdminMess> data, String msg) {
                pendingMesses.postValue(data);
                loading.postValue(false);
                message.postValue(msg);
            }

            @Override
            public void onError(String errorMessage) {
                loading.postValue(false);
                error.postValue(errorMessage);
            }
        });
    }

    // Load pending owners

    public void loadPendingOwners() {
        loading.setValue(true);
        error.setValue(null);

        repository.getPendingOwners(new AdminRepository.RepositoryCallback<List<AdminOwner>>() {
            @Override
            public void onSuccess(List<AdminOwner> data, String msg) {
                pendingOwners.postValue(data);
                loading.postValue(false);
                message.postValue(msg);
            }

            @Override
            public void onError(String errorMessage) {
                loading.postValue(false);
                error.postValue(errorMessage);
            }
        });
    }

    // Check admin access

    public void checkAdminAccess() {
        loading.setValue(true);
        error.setValue(null);

        repository.checkAdminAccess(new AdminRepository.RepositoryCallback<AdminHealth>() {
            @Override
            public void onSuccess(AdminHealth data, String msg) {
                adminHealth.postValue(data);
                loading.postValue(false);
            }

            @Override
            public void onError(String errorMessage) {
                loading.postValue(false);
                error.postValue(errorMessage);
            }
        });
    }

    // Approve mess

    public void approveMess(String messId) {
        performAction(
                callback -> repository.approveMess(messId, callback),
                this::loadPendingMesses
        );
    }

    // Reject mess

    public void rejectMess(String messId) {
        performAction(
                callback -> repository.rejectMess(messId, callback),
                this::loadPendingMesses
        );
    }

    // Verify owner

    public void verifyOwner(String userId) {
        performAction(
                callback -> repository.verifyOwner(userId, callback),
                this::loadPendingOwners
        );
    }

    // Block owner

    public void blockOwner(String userId) {
        performAction(
                callback -> repository.blockOwner(userId, callback),
                this::loadPendingOwners
        );
    }

    // Create admin

    public void createAdmin(CreateAdminRequest request) {
        loading.setValue(true);
        error.setValue(null);

        repository.createAdmin(request,
                new AdminRepository.RepositoryCallback<com.example.messloc.data.model.admin.AdminOwner>() {
                    @Override
                    public void onSuccess(
                            com.example.messloc.data.model.admin.AdminOwner data,
                            String msg
                    ) {
                        loading.postValue(false);
                        message.postValue(msg);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        loading.postValue(false);
                        error.postValue(errorMessage);
                    }
                });
    }

    // Common handler for approve/reject/verify/block actions

    private interface Action {
        void execute(
                AdminRepository.RepositoryCallback<Object> callback
        );
    }

    private interface RefreshAction {
        void refresh();
    }

    private void performAction(Action action, RefreshAction refreshAction) {
        loading.setValue(true);
        error.setValue(null);
        message.setValue(null);

        action.execute(new AdminRepository.RepositoryCallback<Object>() {
            @Override
            public void onSuccess(Object data, String msg) {
                loading.postValue(false);
                message.postValue(msg);
                refreshAction.refresh();
            }

            @Override
            public void onError(String errorMessage) {
                loading.postValue(false);
                error.postValue(errorMessage);
            }
        });
    }

    public void clearMessages() {
        error.setValue(null);
        message.setValue(null);
    }
}
