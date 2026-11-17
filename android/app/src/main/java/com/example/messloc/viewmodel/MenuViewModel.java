
package com.example.messloc.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.messloc.data.model.menu.CreateMenuRequest;
import com.example.messloc.data.model.menu.DailyMenu;
import com.example.messloc.data.model.menu.MenuItemRequest;
import com.example.messloc.data.model.menu.UpdateMenuRequest;
import com.example.messloc.data.model.menu.WeeklyMenuResponse;
import com.example.messloc.data.repository.MenuRepository;

public class MenuViewModel extends AndroidViewModel {

    private final MenuRepository repository;

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    private final MutableLiveData<String> message =
            new MutableLiveData<>();

    private final MutableLiveData<DailyMenu> selectedMenu =
            new MutableLiveData<>();

    private final MutableLiveData<WeeklyMenuResponse> weeklyMenu =
            new MutableLiveData<>();

    public MenuViewModel(@NonNull Application application) {
        super(application);
        repository = new MenuRepository(application);
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

    public LiveData<DailyMenu> getSelectedMenu() {
        return selectedMenu;
    }

    public LiveData<WeeklyMenuResponse> getWeeklyMenu() {
        return weeklyMenu;
    }

    // Create menu
    public void createMenu(CreateMenuRequest request) {
        startLoading();

        repository.createMenu(request, new MenuRepository.RepositoryCallback<DailyMenu>() {
            @Override
            public void onSuccess(DailyMenu data, String serverMessage) {
                loading.setValue(false);
                selectedMenu.setValue(data);
                message.setValue(serverMessage);
            }

            @Override
            public void onError(String errorMessage) {
                finishWithError(errorMessage);
            }
        });
    }

    // Update complete menu
    public void updateMenu(String menuId, UpdateMenuRequest request) {
        startLoading();

        repository.updateMenu(menuId, request, new MenuRepository.RepositoryCallback<DailyMenu>() {
            @Override
            public void onSuccess(DailyMenu data, String serverMessage) {
                loading.setValue(false);
                selectedMenu.setValue(data);
                message.setValue(serverMessage);
            }

            @Override
            public void onError(String errorMessage) {
                finishWithError(errorMessage);
            }
        });
    }

    // Add menu item
    public void addMenuItem(String menuId, MenuItemRequest request) {
        startLoading();

        repository.addMenuItem(menuId, request, new MenuRepository.RepositoryCallback<DailyMenu>() {
            @Override
            public void onSuccess(DailyMenu data, String serverMessage) {
                loading.setValue(false);
                selectedMenu.setValue(data);
                message.setValue(serverMessage);
            }

            @Override
            public void onError(String errorMessage) {
                finishWithError(errorMessage);
            }
        });
    }

    // Update menu item
    public void updateMenuItem(
            String menuId,
            String itemId,
            MenuItemRequest request
    ) {
        startLoading();

        repository.updateMenuItem(
                menuId,
                itemId,
                request,
                new MenuRepository.RepositoryCallback<DailyMenu>() {
                    @Override
                    public void onSuccess(DailyMenu data, String serverMessage) {
                        loading.setValue(false);
                        selectedMenu.setValue(data);
                        message.setValue(serverMessage);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        finishWithError(errorMessage);
                    }
                }
        );
    }

    // Delete menu item
    public void deleteMenuItem(String menuId, String itemId) {
        startLoading();

        repository.deleteMenuItem(
                menuId,
                itemId,
                new MenuRepository.RepositoryCallback<DailyMenu>() {
                    @Override
                    public void onSuccess(DailyMenu data, String serverMessage) {
                        loading.setValue(false);
                        selectedMenu.setValue(data);
                        message.setValue(serverMessage);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        finishWithError(errorMessage);
                    }
                }
        );
    }

    // Delete complete menu
    public void deleteMenu(String menuId) {
        startLoading();

        repository.deleteMenu(
                menuId,
                new MenuRepository.RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void data, String serverMessage) {
                        loading.setValue(false);
                        selectedMenu.setValue(null);
                        message.setValue(serverMessage);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        finishWithError(errorMessage);
                    }
                }
        );
    }

    // Load owner's weekly menu
    public void loadOwnerWeeklyMenu(String messId, String startDate) {
        startLoading();

        repository.getOwnerWeeklyMenu(
                messId,
                startDate,
                new MenuRepository.RepositoryCallback<WeeklyMenuResponse>() {
                    @Override
                    public void onSuccess(
                            WeeklyMenuResponse data,
                            String serverMessage
                    ) {
                        loading.setValue(false);
                        weeklyMenu.setValue(data);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        finishWithError(errorMessage);
                    }
                }
        );
    }

    // Load today's public menu
    public void loadTodayMenu(String messId) {
        startLoading();

        repository.getTodayMenu(
                messId,
                new MenuRepository.RepositoryCallback<DailyMenu>() {
                    @Override
                    public void onSuccess(DailyMenu data, String serverMessage) {
                        loading.setValue(false);
                        selectedMenu.setValue(data);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        finishWithError(errorMessage);
                    }
                }
        );
    }

    // Load public weekly menu
    public void loadPublicWeeklyMenu(String messId, String startDate) {
        startLoading();

        repository.getPublicWeeklyMenu(
                messId,
                startDate,
                new MenuRepository.RepositoryCallback<WeeklyMenuResponse>() {
                    @Override
                    public void onSuccess(
                            WeeklyMenuResponse data,
                            String serverMessage
                    ) {
                        loading.setValue(false);
                        weeklyMenu.setValue(data);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        finishWithError(errorMessage);
                    }
                }
        );
    }

    private void startLoading() {
        loading.setValue(true);
        error.setValue(null);
        message.setValue(null);
    }

    private void finishWithError(String errorMessage) {
        loading.setValue(false);
        error.setValue(errorMessage);
    }
}
