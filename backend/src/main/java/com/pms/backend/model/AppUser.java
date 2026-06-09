package com.pms.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    private Integer age;
    private String gender;
    private Double heightCm;
    private Double weightKg;
    private String allergies;
    private String chronicConditions;
    private String lifestyle;
    private String medications;
    private String familyHistory;
    private String mentalHealthHistory;
    private String sleepQuality;
    private String dateOfBirth;
    private String sexAtBirth;
    private String genderIdentity;
    private String preferredLanguage;
    private String phone;
    @Column(length = 500)
    private String address;
    private String bloodType;
    private String pregnancyStatus;
    private String emergencyContactName;
    private String emergencyContactRelationship;
    private String emergencyContactPhone;
    private String preferredHospital;
    @Column(length = 500)
    private String surgeries;
    @Column(length = 500)
    private String immunizations;
    private String primaryDoctor;
    @Column(length = 500)
    private String specialistNames;
    private String hospitalClinic;
    private String insuranceProvider;
    private String insuranceMemberId;
    private String baselineHeartRate;
    private String baselineBloodPressure;
    @Column(length = 500)
    private String tobaccoAlcoholUse;
    @Column(length = 500)
    private String dietNotes;
    private Boolean connectedDataConsent = false;
    private String notificationPreference;
    private String exportFormatPreference;
    private String dataSharingPreference;
    @Lob
    @Column(length = 500000)
    private String profilePhotoDataUrl;
    private LocalDateTime profileSetupCompletedAt;
    private String privacyNoticeVersion;
    private String termsVersion;
    private LocalDateTime privacyNoticeAcceptedAt;
    private LocalDateTime termsAcceptedAt;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public String getChronicConditions() { return chronicConditions; }
    public void setChronicConditions(String chronicConditions) { this.chronicConditions = chronicConditions; }
    public String getLifestyle() { return lifestyle; }
    public void setLifestyle(String lifestyle) { this.lifestyle = lifestyle; }
    public String getMedications() { return medications; }
    public void setMedications(String medications) { this.medications = medications; }
    public String getFamilyHistory() { return familyHistory; }
    public void setFamilyHistory(String familyHistory) { this.familyHistory = familyHistory; }
    public String getMentalHealthHistory() { return mentalHealthHistory; }
    public void setMentalHealthHistory(String mentalHealthHistory) { this.mentalHealthHistory = mentalHealthHistory; }
    public String getSleepQuality() { return sleepQuality; }
    public void setSleepQuality(String sleepQuality) { this.sleepQuality = sleepQuality; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getSexAtBirth() { return sexAtBirth; }
    public void setSexAtBirth(String sexAtBirth) { this.sexAtBirth = sexAtBirth; }
    public String getGenderIdentity() { return genderIdentity; }
    public void setGenderIdentity(String genderIdentity) { this.genderIdentity = genderIdentity; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }
    public String getPregnancyStatus() { return pregnancyStatus; }
    public void setPregnancyStatus(String pregnancyStatus) { this.pregnancyStatus = pregnancyStatus; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactRelationship() { return emergencyContactRelationship; }
    public void setEmergencyContactRelationship(String emergencyContactRelationship) { this.emergencyContactRelationship = emergencyContactRelationship; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    public String getPreferredHospital() { return preferredHospital; }
    public void setPreferredHospital(String preferredHospital) { this.preferredHospital = preferredHospital; }
    public String getSurgeries() { return surgeries; }
    public void setSurgeries(String surgeries) { this.surgeries = surgeries; }
    public String getImmunizations() { return immunizations; }
    public void setImmunizations(String immunizations) { this.immunizations = immunizations; }
    public String getPrimaryDoctor() { return primaryDoctor; }
    public void setPrimaryDoctor(String primaryDoctor) { this.primaryDoctor = primaryDoctor; }
    public String getSpecialistNames() { return specialistNames; }
    public void setSpecialistNames(String specialistNames) { this.specialistNames = specialistNames; }
    public String getHospitalClinic() { return hospitalClinic; }
    public void setHospitalClinic(String hospitalClinic) { this.hospitalClinic = hospitalClinic; }
    public String getInsuranceProvider() { return insuranceProvider; }
    public void setInsuranceProvider(String insuranceProvider) { this.insuranceProvider = insuranceProvider; }
    public String getInsuranceMemberId() { return insuranceMemberId; }
    public void setInsuranceMemberId(String insuranceMemberId) { this.insuranceMemberId = insuranceMemberId; }
    public String getBaselineHeartRate() { return baselineHeartRate; }
    public void setBaselineHeartRate(String baselineHeartRate) { this.baselineHeartRate = baselineHeartRate; }
    public String getBaselineBloodPressure() { return baselineBloodPressure; }
    public void setBaselineBloodPressure(String baselineBloodPressure) { this.baselineBloodPressure = baselineBloodPressure; }
    public String getTobaccoAlcoholUse() { return tobaccoAlcoholUse; }
    public void setTobaccoAlcoholUse(String tobaccoAlcoholUse) { this.tobaccoAlcoholUse = tobaccoAlcoholUse; }
    public String getDietNotes() { return dietNotes; }
    public void setDietNotes(String dietNotes) { this.dietNotes = dietNotes; }
    public Boolean getConnectedDataConsent() { return connectedDataConsent; }
    public void setConnectedDataConsent(Boolean connectedDataConsent) { this.connectedDataConsent = connectedDataConsent; }
    public String getNotificationPreference() { return notificationPreference; }
    public void setNotificationPreference(String notificationPreference) { this.notificationPreference = notificationPreference; }
    public String getExportFormatPreference() { return exportFormatPreference; }
    public void setExportFormatPreference(String exportFormatPreference) { this.exportFormatPreference = exportFormatPreference; }
    public String getDataSharingPreference() { return dataSharingPreference; }
    public void setDataSharingPreference(String dataSharingPreference) { this.dataSharingPreference = dataSharingPreference; }
    public String getProfilePhotoDataUrl() { return profilePhotoDataUrl; }
    public void setProfilePhotoDataUrl(String profilePhotoDataUrl) { this.profilePhotoDataUrl = profilePhotoDataUrl; }
    public LocalDateTime getProfileSetupCompletedAt() { return profileSetupCompletedAt; }
    public void setProfileSetupCompletedAt(LocalDateTime profileSetupCompletedAt) { this.profileSetupCompletedAt = profileSetupCompletedAt; }
    public String getPrivacyNoticeVersion() { return privacyNoticeVersion; }
    public void setPrivacyNoticeVersion(String privacyNoticeVersion) { this.privacyNoticeVersion = privacyNoticeVersion; }
    public String getTermsVersion() { return termsVersion; }
    public void setTermsVersion(String termsVersion) { this.termsVersion = termsVersion; }
    public LocalDateTime getPrivacyNoticeAcceptedAt() { return privacyNoticeAcceptedAt; }
    public void setPrivacyNoticeAcceptedAt(LocalDateTime privacyNoticeAcceptedAt) { this.privacyNoticeAcceptedAt = privacyNoticeAcceptedAt; }
    public LocalDateTime getTermsAcceptedAt() { return termsAcceptedAt; }
    public void setTermsAcceptedAt(LocalDateTime termsAcceptedAt) { this.termsAcceptedAt = termsAcceptedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
