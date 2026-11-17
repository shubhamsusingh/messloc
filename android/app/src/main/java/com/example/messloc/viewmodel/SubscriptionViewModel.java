
package com.example.messloc.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.messloc.data.model.subscription.CreateSubscriptionRequest;
import com.example.messloc.data.model.subscription.Subscription;
import com.example.messloc.data.repository.SubscriptionRepository;

import java.util.List;

public class SubscriptionViewModel extends AndroidViewModel {

    private final SubscriptionRepository repository;

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    private final MutableLiveData<String> message =
            new MutableLiveData<>();

    private final MutableLiveData<Subscription> selectedSubscription =
            new MutableLiveData<>();

    private final MutableLiveData<List<Subscription>> subscriptions =
            new MutableLiveData<>();

    public SubscriptionViewModel(@NonNull Application application) {
        super(application);

        repository = new SubscriptionRepository(application);
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

    public LiveData<Subscription> getSelectedSubscription() {
        return selectedSubscription;
    }

    public LiveData<List<Subscription>> getSubscriptions() {
        return subscriptions;
    }

    // Create subscription
    public void createSubscription(
            String messId,
            String planId
    ) {
        if (messId == null || messId.trim().isEmpty()) {
            error.setValue("Invalid mess ID.");
            return;
        }

        loading.setValue(true);
        error.setValue(null);
        message.setValue(null);

        CreateSubscriptionRequest request =
                new CreateSubscriptionRequest(messId, planId);

        repository.createSubscription(
                request,
                new SubscriptionRepository.RepositoryCallback<Subscription>() {

                    @Override
                    public void onSuccess(
                            Subscription data,
                            String responseMessage
                    ) {
                        loading.postValue(false);
                        selectedSubscription.postValue(data);
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

    // Load current user's subscriptions
    public void loadMySubscriptions() {
        loading.setValue(true);
        error.setValue(null);

        repository.getMySubscriptions(
                new SubscriptionRepository.RepositoryCallback<List<Subscription>>() {

                    @Override
                    public void onSuccess(
                            List<Subscription> data,
                            String responseMessage
                    ) {
                        loading.postValue(false);
                        subscriptions.postValue(data);
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

    // Cancel subscription
    public void cancelSubscription(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.trim().isEmpty()) {
            error.setValue("Invalid subscription ID.");
            return;
        }

        loading.setValue(true);
        error.setValue(null);

        repository.cancelSubscription(
                subscriptionId,
                new SubscriptionRepository.RepositoryCallback<Subscription>() {

                    @Override
                    public void onSuccess(
                            Subscription data,
                            String responseMessage
                    ) {
                        loading.postValue(false);
                        selectedSubscription.postValue(data);
                        message.postValue(responseMessage);

                        // Refresh the user's subscription list.
                        loadMySubscriptions();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        loading.postValue(false);
                        error.postValue(errorMessage);
                    }
                }
        );
    }
}
