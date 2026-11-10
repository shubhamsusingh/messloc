package com.MessLoc.service;

import com.MessLoc.dto.mess.MessDetailsResponse;
import com.MessLoc.dto.mess.PublicMessResponse;
import com.MessLoc.entity.Mess;
import com.MessLoc.enums.FoodType;
import com.MessLoc.enums.MessStatus;
import com.MessLoc.enums.VerificationStatus;
import com.MessLoc.exception.MessNotFoundException;
import com.MessLoc.repository.MessRepository;
import com.MessLoc.repository.UserRepository;
import com.MessLoc.service.impl.MessServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessDiscoveryServiceTest {

    @Mock
    private MessRepository messRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessServiceImpl messService;

    private Mess vegMess;
    private Mess nonVegMess;
    private Mess bothMess;
    private Mess pendingMess;
    private Mess inactiveMess;
    private Mess rejectedMess;

    @BeforeEach
    void setUp() {
        vegMess = Mess.builder()
                .id("m-veg")
                .name("Shree Ganesh Pure Veg")
                .description("Homely sattvic meals")
                .address("Paltan Bazaar")
                .city("Dehradun")
                .state("Uttarakhand")
                .pincode("248001")
                .latitude(30.3200)
                .longitude(78.0350)
                .contactNumber("9876543210")
                .monthlyPrice(2800.0)
                .foodType(FoodType.VEG)
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(22, 0))
                .facilities(List.of("WiFi", "RO Water"))
                .verificationStatus(VerificationStatus.APPROVED)
                .status(MessStatus.ACTIVE)
                .build();

        nonVegMess = Mess.builder()
                .id("m-nonveg")
                .name("Doonger Non-Veg Delight")
                .description("Delicious non-veg daily meal")
                .address("EC Road")
                .city("Dehradun")
                .state("Uttarakhand")
                .pincode("248001")
                .latitude(30.3250)
                .longitude(78.0450)
                .contactNumber("9876543211")
                .monthlyPrice(3800.0)
                .foodType(FoodType.NON_VEG)
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(23, 0))
                .facilities(List.of("AC Dining"))
                .verificationStatus(VerificationStatus.APPROVED)
                .status(MessStatus.ACTIVE)
                .build();

        bothMess = Mess.builder()
                .id("m-both")
                .name("Himalayan Royal Mess")
                .description("Veg and non-veg options both available")
                .address("Rajpur Road")
                .city("Dehradun")
                .state("Uttarakhand")
                .pincode("248001")
                .latitude(30.3300)
                .longitude(78.0500)
                .contactNumber("9876543212")
                .monthlyPrice(3500.0)
                .foodType(FoodType.BOTH)
                .openingTime(LocalTime.of(7, 30))
                .closingTime(LocalTime.of(22, 30))
                .facilities(List.of("WiFi", "RO Water", "Parking"))
                .verificationStatus(VerificationStatus.APPROVED)
                .status(MessStatus.ACTIVE)
                .build();

        pendingMess = Mess.builder()
                .id("m-pending")
                .name("Unapproved Mess")
                .verificationStatus(VerificationStatus.PENDING)
                .status(MessStatus.ACTIVE)
                .build();

        inactiveMess = Mess.builder()
                .id("m-inactive")
                .name("Inactive Mess")
                .verificationStatus(VerificationStatus.APPROVED)
                .status(MessStatus.INACTIVE)
                .build();

        rejectedMess = Mess.builder()
                .id("m-rejected")
                .name("Rejected Mess")
                .verificationStatus(VerificationStatus.REJECTED)
                .status(MessStatus.ACTIVE)
                .build();
    }

    // ==========================================
    // FEATURE 1: GET ALL APPROVED ACTIVE MESSES
    // ==========================================

    @Test
    @DisplayName("Should return all approved and active messes without sensitive fields")
    void testGetPublicMesses_AllApprovedAndActive() {
        when(messRepository.findByVerificationStatusAndStatus(VerificationStatus.APPROVED, MessStatus.ACTIVE))
                .thenReturn(List.of(vegMess, nonVegMess, bothMess));

        List<PublicMessResponse> results = messService.getPublicMesses();

        assertEquals(3, results.size());
        assertEquals("Shree Ganesh Pure Veg", results.get(0).getName());
        assertEquals(FoodType.VEG, results.get(0).getFoodType());
        assertNull(results.get(0).getDistanceInKm()); // null when not a nearby query
    }

    @Test
    @DisplayName("Should return empty list when no approved active messes exist")
    void testGetPublicMesses_EmptyWhenNoneApproved() {
        when(messRepository.findByVerificationStatusAndStatus(VerificationStatus.APPROVED, MessStatus.ACTIVE))
                .thenReturn(List.of());

        List<PublicMessResponse> results = messService.getPublicMesses();

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ==========================================
    // FEATURE 2: SEARCH BY CITY
    // ==========================================

    @Test
    @DisplayName("Should search messes by city trimming whitespace and case-insensitively")
    void testGetPublicMesses_ByCity() {
        when(messRepository.findByVerificationStatusAndStatusAndCityIgnoreCase(
                VerificationStatus.APPROVED, MessStatus.ACTIVE, "Dehradun"))
                .thenReturn(List.of(vegMess, bothMess));

        List<PublicMessResponse> results = messService.getPublicMesses("  Dehradun  ");

        assertEquals(2, results.size());
        verify(messRepository).findByVerificationStatusAndStatusAndCityIgnoreCase(
                VerificationStatus.APPROVED, MessStatus.ACTIVE, "Dehradun");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when city parameter is blank")
    void testGetPublicMesses_BlankCity_ThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                messService.getPublicMesses("    "));

        assertEquals("City search parameter cannot be blank", ex.getMessage());
        verify(messRepository, never()).findByVerificationStatusAndStatusAndCityIgnoreCase(any(), any(), any());
    }

    // ==========================================
    // FEATURE 3: FILTER BY FOOD TYPE
    // ==========================================

    @Test
    @DisplayName("When user selects VEG, should query for VEG and BOTH listings")
    void testGetPublicMesses_FilterVeg_IncludesVegAndBoth() {
        when(messRepository.findByVerificationStatusAndStatusAndFoodTypeIn(
                eq(VerificationStatus.APPROVED), eq(MessStatus.ACTIVE), eq(List.of(FoodType.VEG, FoodType.BOTH))))
                .thenReturn(List.of(vegMess, bothMess));

        List<PublicMessResponse> results = messService.getPublicMesses(null, FoodType.VEG);

        assertEquals(2, results.size());
        verify(messRepository).findByVerificationStatusAndStatusAndFoodTypeIn(
                VerificationStatus.APPROVED, MessStatus.ACTIVE, List.of(FoodType.VEG, FoodType.BOTH));
    }

    @Test
    @DisplayName("When user selects NON_VEG, should query for NON_VEG and BOTH listings")
    void testGetPublicMesses_FilterNonVeg_IncludesNonVegAndBoth() {
        when(messRepository.findByVerificationStatusAndStatusAndFoodTypeIn(
                eq(VerificationStatus.APPROVED), eq(MessStatus.ACTIVE), eq(List.of(FoodType.NON_VEG, FoodType.BOTH))))
                .thenReturn(List.of(nonVegMess, bothMess));

        List<PublicMessResponse> results = messService.getPublicMesses(null, FoodType.NON_VEG);

        assertEquals(2, results.size());
        verify(messRepository).findByVerificationStatusAndStatusAndFoodTypeIn(
                VerificationStatus.APPROVED, MessStatus.ACTIVE, List.of(FoodType.NON_VEG, FoodType.BOTH));
    }

    @Test
    @DisplayName("When user selects BOTH, should query explicitly for BOTH listings only")
    void testGetPublicMesses_FilterBoth_IncludesOnlyBoth() {
        when(messRepository.findByVerificationStatusAndStatusAndFoodTypeIn(
                eq(VerificationStatus.APPROVED), eq(MessStatus.ACTIVE), eq(List.of(FoodType.BOTH))))
                .thenReturn(List.of(bothMess));

        List<PublicMessResponse> results = messService.getPublicMesses(null, FoodType.BOTH);

        assertEquals(1, results.size());
        assertEquals("Himalayan Royal Mess", results.get(0).getName());
        verify(messRepository).findByVerificationStatusAndStatusAndFoodTypeIn(
                VerificationStatus.APPROVED, MessStatus.ACTIVE, List.of(FoodType.BOTH));
    }

    // ==========================================
    // FEATURE 4: COMBINED CITY & FOOD TYPE
    // ==========================================

    @Test
    @DisplayName("Should query with both city and foodType combination")
    void testGetPublicMesses_CityAndFoodTypeCombined() {
        when(messRepository.findByVerificationStatusAndStatusAndCityIgnoreCaseAndFoodTypeIn(
                eq(VerificationStatus.APPROVED), eq(MessStatus.ACTIVE), eq("Dehradun"), eq(List.of(FoodType.VEG, FoodType.BOTH))))
                .thenReturn(List.of(vegMess, bothMess));

        List<PublicMessResponse> results = messService.getPublicMesses("Dehradun", FoodType.VEG);

        assertEquals(2, results.size());
        verify(messRepository).findByVerificationStatusAndStatusAndCityIgnoreCaseAndFoodTypeIn(
                VerificationStatus.APPROVED, MessStatus.ACTIVE, "Dehradun", List.of(FoodType.VEG, FoodType.BOTH));
    }

    // ==========================================
    // FEATURE 5: GET MESS DETAILS BY ID
    // ==========================================

    @Test
    @DisplayName("Should return MessDetailsResponse for valid approved and active mess")
    void testGetPublicMess_ValidApprovedActive_ReturnsDetails() {
        when(messRepository.findById("m-veg")).thenReturn(Optional.of(vegMess));

        MessDetailsResponse response = messService.getPublicMess("m-veg");

        assertNotNull(response);
        assertEquals("m-veg", response.getId());
        assertEquals("Shree Ganesh Pure Veg", response.getName());
        assertEquals("Paltan Bazaar", response.getAddress());
        assertEquals("Dehradun", response.getCity());
        assertEquals("248001", response.getPincode());
        assertEquals(FoodType.VEG, response.getFoodType());
        assertEquals(2800.0, response.getMonthlyPrice());
    }

    @Test
    @DisplayName("Should throw MessNotFoundException when mess does not exist")
    void testGetPublicMess_NotFound_Throws404() {
        when(messRepository.findById("unknown-id")).thenReturn(Optional.empty());

        assertThrows(MessNotFoundException.class, () -> messService.getPublicMess("unknown-id"));
    }

    @Test
    @DisplayName("Should throw MessNotFoundException when mess is pending verification")
    void testGetPublicMess_Pending_Throws404() {
        when(messRepository.findById("m-pending")).thenReturn(Optional.of(pendingMess));

        assertThrows(MessNotFoundException.class, () -> messService.getPublicMess("m-pending"));
    }

    @Test
    @DisplayName("Should throw MessNotFoundException when mess is inactive")
    void testGetPublicMess_Inactive_Throws404() {
        when(messRepository.findById("m-inactive")).thenReturn(Optional.of(inactiveMess));

        assertThrows(MessNotFoundException.class, () -> messService.getPublicMess("m-inactive"));
    }

    @Test
    @DisplayName("Should throw MessNotFoundException when mess is rejected")
    void testGetPublicMess_Rejected_Throws404() {
        when(messRepository.findById("m-rejected")).thenReturn(Optional.of(rejectedMess));

        assertThrows(MessNotFoundException.class, () -> messService.getPublicMess("m-rejected"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when ID is blank")
    void testGetPublicMess_BlankId_ThrowsBadRequest() {
        assertThrows(IllegalArgumentException.class, () -> messService.getPublicMess("   "));
    }

    // ==========================================
    // FEATURE 6: NEARBY MESS DISCOVERY
    // ==========================================

    @Test
    @DisplayName("Should calculate Haversine distance, filter by radius, and sort nearest first")
    void testGetNearbyMesses_CalculatesDistanceAndSorts() {
        // Query location: (30.3200, 78.0350) - exactly at vegMess
        // vegMess is at 0.0 km
        // nonVegMess is at ~1.1 km
        // bothMess is at ~1.8 km
        when(messRepository.findByVerificationStatusAndStatus(VerificationStatus.APPROVED, MessStatus.ACTIVE))
                .thenReturn(List.of(bothMess, vegMess, nonVegMess));

        List<PublicMessResponse> nearby = messService.getNearbyMesses(30.3200, 78.0350, 5.0);

        assertEquals(3, nearby.size());
        // Ordered nearest first: vegMess (0.0km), then nonVegMess, then bothMess
        assertEquals("m-veg", nearby.get(0).getId());
        assertEquals(0.0, nearby.get(0).getDistanceInKm());

        assertEquals("m-nonveg", nearby.get(1).getId());
        assertTrue(nearby.get(1).getDistanceInKm() > 0.0);
        assertTrue(nearby.get(1).getDistanceInKm() < nearby.get(2).getDistanceInKm());

        assertEquals("m-both", nearby.get(2).getId());
    }

    @Test
    @DisplayName("Should exclude messes exceeding search radius")
    void testGetNearbyMesses_ExcludesOutsideRadius() {
        // Search radius: 0.5 km. Only vegMess (0.0 km) should be included.
        when(messRepository.findByVerificationStatusAndStatus(VerificationStatus.APPROVED, MessStatus.ACTIVE))
                .thenReturn(List.of(vegMess, nonVegMess, bothMess));

        List<PublicMessResponse> nearby = messService.getNearbyMesses(30.3200, 78.0350, 0.5);

        assertEquals(1, nearby.size());
        assertEquals("m-veg", nearby.get(0).getId());
    }

    @Test
    @DisplayName("Should support nearby search combined with foodType filter")
    void testGetNearbyMesses_WithFoodTypeFilter() {
        when(messRepository.findByVerificationStatusAndStatusAndFoodTypeIn(
                eq(VerificationStatus.APPROVED), eq(MessStatus.ACTIVE), eq(List.of(FoodType.NON_VEG, FoodType.BOTH))))
                .thenReturn(List.of(nonVegMess, bothMess));

        List<PublicMessResponse> nearby = messService.getNearbyMesses(30.3200, 78.0350, 5.0, FoodType.NON_VEG);

        assertEquals(2, nearby.size());
        assertEquals("m-nonveg", nearby.get(0).getId());
        assertEquals("m-both", nearby.get(1).getId());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when latitude is out of bounds")
    void testGetNearbyMesses_InvalidLatitude() {
        assertThrows(IllegalArgumentException.class, () ->
                messService.getNearbyMesses(95.0, 78.0, 5.0));

        assertThrows(IllegalArgumentException.class, () ->
                messService.getNearbyMesses(-91.0, 78.0, 5.0));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when longitude is out of bounds")
    void testGetNearbyMesses_InvalidLongitude() {
        assertThrows(IllegalArgumentException.class, () ->
                messService.getNearbyMesses(30.0, 185.0, 5.0));

        assertThrows(IllegalArgumentException.class, () ->
                messService.getNearbyMesses(30.0, -181.0, 5.0));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when radius is zero or negative")
    void testGetNearbyMesses_InvalidRadius_ZeroOrNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                messService.getNearbyMesses(30.0, 78.0, 0.0));

        assertThrows(IllegalArgumentException.class, () ->
                messService.getNearbyMesses(30.0, 78.0, -5.0));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when radius exceeds maximum 100 km")
    void testGetNearbyMesses_InvalidRadius_ExceedsMax() {
        assertThrows(IllegalArgumentException.class, () ->
                messService.getNearbyMesses(30.0, 78.0, 150.0));
    }
}

