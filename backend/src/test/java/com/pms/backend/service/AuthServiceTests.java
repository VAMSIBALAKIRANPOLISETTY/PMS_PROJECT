package com.pms.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pms.backend.dto.AuthDtos.LoginRequest;
import com.pms.backend.dto.AuthDtos.ProfileUpdateRequest;
import com.pms.backend.dto.AuthDtos.RegisterRequest;
import com.pms.backend.model.Role;
import com.pms.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTests {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void publicRegistrationAlwaysCreatesPatientAccount() {
        var response = authService.register(new RegisterRequest(
                "new.patient@example.com",
                "newpatient",
                "New Patient",
                "password123",
                32,
                171.0,
                69.0,
                "Prefer not to say",
                true,
                true
        ));

        assertEquals(Role.USER, response.user().role());
        assertEquals(3, response.token().split("\\.").length);
        assertFalse(response.user().profileSetupComplete());
        assertTrue(response.user().profileCompletion() < 100);
    }

    @Test
    void publicRegistrationRequiresAdultPatient() {
        var error = assertThrows(IllegalArgumentException.class, () -> authService.register(new RegisterRequest(
                "young.patient@example.com",
                "youngpatient",
                "Young Patient",
                "password123",
                17,
                165.0,
                60.0,
                "Prefer not to say",
                true,
                true
        )));

        assertTrue(error.getMessage().contains("18"));
    }

    @Test
    void publicRegistrationRequiresPrivacyAndTermsAcceptance() {
        var error = assertThrows(IllegalArgumentException.class, () -> authService.register(new RegisterRequest(
                "privacy.patient@example.com",
                "privacypatient",
                "Privacy Patient",
                "password123",
                26,
                165.0,
                60.0,
                "Prefer not to say",
                false,
                true
        )));

        assertTrue(error.getMessage().contains("privacy notice"));
    }

    @Test
    void publicRegistrationStoresAcceptanceVersionsAndTimes() {
        authService.register(new RegisterRequest(
                "accepted.patient@example.com",
                "acceptedpatient",
                "Accepted Patient",
                "password123",
                29,
                168.0,
                63.0,
                "Prefer not to say",
                true,
                true
        ));

        var user = userRepository.findByEmailIgnoreCase("accepted.patient@example.com").orElseThrow();
        assertEquals(AuthService.PRIVACY_NOTICE_VERSION, user.getPrivacyNoticeVersion());
        assertEquals(AuthService.TERMS_VERSION, user.getTermsVersion());
        assertTrue(user.getPrivacyNoticeAcceptedAt() != null);
        assertTrue(user.getTermsAcceptedAt() != null);
    }

    @Test
    void patientLoginRejectsStaffAccount() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> authService.login(new LoginRequest("admin@example.com", "password123")));

        assertTrue(error.getMessage().contains("Staff login"));
    }

    @Test
    void staffLoginAcceptsAdministrator() {
        var response = authService.staffLogin(new LoginRequest("admin@example.com", "password123"));

        assertEquals(Role.ADMIN, response.user().role());
        assertEquals(3, response.token().split("\\.").length);
    }

    @Test
    void bearerJwtAuthenticatesUserAfterLogin() {
        var response = authService.login(new LoginRequest("user@example.com", "password123"));
        var user = authService.requireUser("Bearer " + response.token());

        assertEquals(response.user().id(), user.getId());
    }

    @Test
    void staffLoginRejectsPatientAccount() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> authService.staffLogin(new LoginRequest("user@example.com", "password123")));

        assertTrue(error.getMessage().contains("patient login"));
    }

    @Test
    void legacyGeneratedHistoryValuesRequirePatientReview() {
        authService.register(new RegisterRequest(
                "legacy.patient@example.com",
                "legacypatient",
                "Legacy Patient",
                "password123",
                36,
                172.0,
                72.0,
                "Prefer not to say",
                true,
                true
        ));
        var user = userRepository.findByEmailIgnoreCase("legacy.patient@example.com").orElseThrow();
        user.setAllergies("No known allergies");
        user.setChronicConditions("None");
        user.setMedications("None");
        user.setLifestyle("Moderate activity");
        userRepository.save(user);

        var response = authService.toUserResponse(user);
        assertFalse(response.profileSetupComplete());
        assertTrue(response.profileCompletion() < 100);
        assertNull(response.allergies());
        assertNull(response.chronicConditions());
    }

    @Test
    void explicitNoneSelectionsCompleteHealthHistorySetup() {
        authService.register(new RegisterRequest(
                "reviewed.patient@example.com",
                "reviewedpatient",
                "Reviewed Patient",
                "password123",
                28,
                169.0,
                64.0,
                "Prefer not to say",
                true,
                true
        ));
        var user = userRepository.findByEmailIgnoreCase("reviewed.patient@example.com").orElseThrow();

        var response = authService.updateProfile(user, new ProfileUpdateRequest(
                user.getFullName(),
                user.getAge(),
                user.getHeightCm(),
                user.getWeightKg(),
                user.getGender(),
                "No known allergies",
                "None",
                "Moderately active",
                "None",
                "None known",
                "None",
                "Restful"
        ));

        assertTrue(response.profileSetupComplete());
        assertTrue(response.profileCompletion() < 100);
        assertTrue(userRepository.findById(user.getId()).orElseThrow().getProfileSetupCompletedAt() != null);
    }

    @Test
    void fullPatientProfileCompletesAllProfileGroups() {
        authService.register(new RegisterRequest(
                "complete.profile@example.com",
                "completeprofile",
                "Complete Profile",
                "password123",
                34,
                174.0,
                76.0,
                "Female",
                true,
                true
        ));
        var user = userRepository.findByEmailIgnoreCase("complete.profile@example.com").orElseThrow();

        var response = authService.updateProfile(user, new ProfileUpdateRequest(
                user.getFullName(),
                user.getAge(),
                user.getHeightCm(),
                user.getWeightKg(),
                user.getGender(),
                "No known allergies",
                "None",
                "Moderately active",
                "None",
                "None known",
                "None",
                "Restful",
                "1992-04-12",
                "Female",
                "Woman",
                "English",
                "+91 90000 00000",
                "Hyderabad",
                "O+",
                "Not pregnant",
                "Family Contact",
                "Sibling",
                "+91 90000 00001",
                "Preferred Hospital",
                "None",
                "Routine immunizations",
                "Primary Doctor",
                "None",
                "City Clinic",
                "Health Insurance",
                "MEM-123",
                "72 bpm",
                "120/80",
                "No tobacco or alcohol use",
                "Balanced diet",
                true,
                "Email",
                "PDF",
                "Share only with consent"
        ));

        assertEquals(100, response.profileCompletion());
        assertEquals("O+", response.bloodType());
    }
}
