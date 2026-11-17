
package com.example.messloc.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.messloc.data.model.MessCreateResponse;
import com.example.messloc.data.model.MessUpdateRequest;
import com.example.messloc.data.model.owner.OwnerMess;
import com.example.messloc.data.repository.OwnerMessRepository;

import java.util.ArrayList;
import java.util.List;

public class EditMessViewModel extends AndroidViewModel {

    private final OwnerMessRepository repository;

    private String messId;
    private String messStatus = "ACTIVE";

    // Form fields
    public final MutableLiveData<String> name = new MutableLiveData<>("");
    public final MutableLiveData<String> description = new MutableLiveData<>("");
    public final MutableLiveData<String> address = new MutableLiveData<>("");
    public final MutableLiveData<String> city = new MutableLiveData<>("");
    public final MutableLiveData<String> state = new MutableLiveData<>("");
    public final MutableLiveData<String> pincode = new MutableLiveData<>("");
    public final MutableLiveData<String> latitude = new MutableLiveData<>("");
    public final MutableLiveData<String> longitude = new MutableLiveData<>("");
    public final MutableLiveData<String> contactNumber = new MutableLiveData<>("");
    public final MutableLiveData<String> monthlyPrice = new MutableLiveData<>("");
    public final MutableLiveData<String> openingTime = new MutableLiveData<>("07:30");
    public final MutableLiveData<String> closingTime = new MutableLiveData<>("22:00");

    // Food type
    public final MutableLiveData<String> foodType =
            new MutableLiveData<>("VEG");

    // Facilities
    public final MutableLiveData<Boolean> roWater =
            new MutableLiveData<>(false);

    public final MutableLiveData<Boolean> wifi =
            new MutableLiveData<>(false);

    public final MutableLiveData<Boolean> tiffinDelivery =
            new MutableLiveData<>(false);

    public final MutableLiveData<Boolean> sundaySpecial =
            new MutableLiveData<>(false);

    // UI state
    public final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    public final MutableLiveData<String> errorMessage =
            new MutableLiveData<>();

    public final MutableLiveData<String> successMessage =
            new MutableLiveData<>();

    public final MutableLiveData<MessCreateResponse> updateResult =
            new MutableLiveData<>();

    public EditMessViewModel(@NonNull Application application) {
        super(application);
        repository = new OwnerMessRepository(application);
    }

    public void setMessId(String id) {
        messId = id;
    }

    public void setFoodType(String type) {
        foodType.setValue(type);
    }

    // Load existing mess details into the form
    public void loadMess(String id) {

        messId = id;

        if (messId == null || messId.trim().isEmpty()) {
            errorMessage.setValue("Invalid mess ID.");
            return;
        }

        loading.setValue(true);
        errorMessage.setValue(null);

        repository.getMyMessById(
                messId,
                new OwnerMessRepository.RepositoryCallback<OwnerMess>() {

                    @Override
                    public void onSuccess(OwnerMess mess, String message) {
                        loading.postValue(false);

                        if (mess == null) {
                            errorMessage.postValue("Mess details not found.");
                            return;
                        }

                        name.postValue(value(mess.getName()));
                        description.postValue(value(mess.getDescription()));
                        address.postValue(value(mess.getAddress()));
                        city.postValue(value(mess.getCity()));
                        state.postValue(value(mess.getState()));
                        pincode.postValue(value(mess.getPincode()));

                        latitude.postValue(number(mess.getLatitude()));
                        longitude.postValue(number(mess.getLongitude()));

                        contactNumber.postValue(value(mess.getContactNumber()));
                        monthlyPrice.postValue(number(mess.getMonthlyPrice()));

                        openingTime.postValue(value(mess.getOpeningTime()));
                        closingTime.postValue(value(mess.getClosingTime()));

                        foodType.postValue(value(mess.getFoodType()));

                        messStatus = value(mess.getStatus());
                        if (messStatus.isEmpty()) {
                            messStatus = "ACTIVE";
                        }

                        List<String> facilities = mess.getFacilities();

                        roWater.postValue(
                                facilities != null && facilities.contains("RO Water")
                        );

                        wifi.postValue(
                                facilities != null && facilities.contains("WiFi")
                        );

                        tiffinDelivery.postValue(
                                facilities != null && facilities.contains("Tiffin Delivery")
                        );

                        sundaySpecial.postValue(
                                facilities != null && facilities.contains("Sunday Special")
                        );
                    }

                    @Override
                    public void onError(String error) {
                        loading.postValue(false);
                        errorMessage.postValue(error);
                    }
                }
        );
    }

    // Update existing mess
    public void updateMess() {

        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }

        if (messId == null || messId.trim().isEmpty()) {
            errorMessage.setValue("Mess ID is missing.");
            return;
        }

        String messName = getValue(name);
        String messDescription = getValue(description);
        String messAddress = getValue(address);
        String messCity = getValue(city);
        String messState = getValue(state);
        String messPincode = getValue(pincode);
        String messLatitude = getValue(latitude);
        String messLongitude = getValue(longitude);
        String messContact = getValue(contactNumber);
        String messPrice = getValue(monthlyPrice);
        String startTime = getValue(openingTime);
        String endTime = getValue(closingTime);
        String selectedFoodType = getValue(foodType);

        if (!selectedFoodType.equals("VEG")
                && !selectedFoodType.equals("NON_VEG")
                && !selectedFoodType.equals("BOTH")) {
            errorMessage.setValue("Please select a valid food type.");
            return;
        }

        if (messName.isEmpty() || messDescription.isEmpty()
                || messAddress.isEmpty() || messCity.isEmpty()
                || messState.isEmpty() || messPincode.isEmpty()
                || messLatitude.isEmpty() || messLongitude.isEmpty()
                || messContact.isEmpty() || messPrice.isEmpty()
                || startTime.isEmpty() || endTime.isEmpty()) {

            errorMessage.setValue("Please fill all required fields.");
            return;
        }

        if (!messPincode.matches("\\d{6}")) {
            errorMessage.setValue("Enter a valid 6-digit pincode.");
            return;
        }

        if (!messContact.matches("\\d{10}")) {
            errorMessage.setValue("Enter a valid 10-digit contact number.");
            return;
        }

        if (!startTime.matches("([01]\\d|2[0-3]):[0-5]\\d")
                || !endTime.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
            errorMessage.setValue("Enter time in HH:mm format.");
            return;
        }

        double lat;
        double lng;
        double price;

        try {
            lat = Double.parseDouble(messLatitude);
            lng = Double.parseDouble(messLongitude);
            price = Double.parseDouble(messPrice);

            if (lat < -90 || lat > 90
                    || lng < -180 || lng > 180
                    || price <= 0) {
                errorMessage.setValue("Enter valid coordinates and price.");
                return;
            }

        } catch (NumberFormatException e) {
            errorMessage.setValue(
                    "Latitude, longitude and price must be valid numbers."
            );
            return;
        }

        List<String> facilities = new ArrayList<>();

        if (Boolean.TRUE.equals(roWater.getValue())) {
            facilities.add("RO Water");
        }

        if (Boolean.TRUE.equals(wifi.getValue())) {
            facilities.add("WiFi");
        }

        if (Boolean.TRUE.equals(tiffinDelivery.getValue())) {
            facilities.add("Tiffin Delivery");
        }

        if (Boolean.TRUE.equals(sundaySpecial.getValue())) {
            facilities.add("Sunday Special");
        }

        MessUpdateRequest request = new MessUpdateRequest(
                messName,
                messDescription,
                messAddress,
                messCity,
                messState,
                messPincode,
                lat,
                lng,
                messContact,
                price,
                selectedFoodType,
                startTime,
                endTime,
                facilities,
                messStatus
        );

        loading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);

        repository.updateMess(
                messId,
                request,
                new OwnerMessRepository.RepositoryCallback<MessCreateResponse>() {

                    @Override
                    public void onSuccess(
                            MessCreateResponse response,
                            String message
                    ) {
                        loading.postValue(false);

                        String resultMessage = message != null
                                ? message
                                : "Mess updated successfully.";

                        successMessage.postValue(resultMessage);
                        updateResult.postValue(response);
                    }

                    @Override
                    public void onError(String error) {
                        loading.postValue(false);
                        errorMessage.postValue(error);
                    }
                }
        );
    }

    private String getValue(MutableLiveData<String> field) {
        String value = field.getValue();
        return value == null ? "" : value.trim();
    }

    private String value(String input) {
        return input == null ? "" : input;
    }

    private String number(Number input) {
        return input == null ? "" : String.valueOf(input);
    }
}
