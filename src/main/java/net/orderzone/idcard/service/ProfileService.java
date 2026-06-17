package net.orderzone.idcard.service;

import net.orderzone.idcard.model.Profile;
import net.orderzone.idcard.model.ProfileType;
import net.orderzone.idcard.model.Template;
import net.orderzone.idcard.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final TemplateService templateService;

    public ProfileService(ProfileRepository profileRepository, TemplateService templateService) {
        this.profileRepository = profileRepository;
        this.templateService = templateService;
    }

    public List<Profile> getAllProfiles() {
        return profileRepository.findAll();
    }

    public Optional<Profile> getProfileById(Long id) {
        return profileRepository.findById(id);
    }

    public Optional<Profile> getProfileByUuid(String uuid) {
        return profileRepository.findByUuid(uuid);
    }

    public List<Profile> searchProfiles(String query) {
        if (query == null || query.isBlank()) {
            return profileRepository.findAll();
        }
        return profileRepository.searchProfiles(query);
    }

    /**
     * Main profile creation logic with automated sequence generation.
     */
    public Profile createProfile(Profile profile) {
        // 1. Assign stable unique tracking UUID
        profile.setUuid(UUID.randomUUID().toString());

        // 2. Generate custom human-friendly registration number if not explicitly set
        if (profile.getRegistrationNumber() == null || profile.getRegistrationNumber().isBlank()) {
            profile.setRegistrationNumber(generateRegistrationNumber(profile));
        }

        // 3. Guarantee registration uniqueness
        if (profileRepository.existsByRegistrationNumber(profile.getRegistrationNumber())) {
            throw new IllegalArgumentException("Registration number already exists: " + profile.getRegistrationNumber());
        }

        // 4. Assign fallback template if omitted
        if (profile.getTemplate() == null) {
            Template defaultTemplate = templateService.getOrCreateDefaultTemplate();
            profile.setTemplate(defaultTemplate);
        }

        return profileRepository.save(profile);
    }

    public Profile updateProfile(Long id, Profile details) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found with ID: " + id));

        profile.setFullName(details.getFullName());
        profile.setDepartment(details.getDepartment());
        profile.setTitle(details.getTitle());
        profile.setEmail(details.getEmail());
        profile.setPhone(details.getPhone());
        profile.setBloodGroup(details.getBloodGroup());
        profile.setDateOfBirth(details.getDateOfBirth());
        profile.setExpiryDate(details.getExpiryDate());
        profile.setBarcodeType(details.getBarcodeType());
        
        if (details.getTemplate() != null) {
            profile.setTemplate(details.getTemplate());
        }

        return profileRepository.save(profile);
    }

    public void deleteProfile(Long id) {
        if (!profileRepository.existsById(id)) {
            throw new IllegalArgumentException("Profile not found with ID: " + id);
        }
        profileRepository.deleteById(id);
    }

    /**
     * Generates custom registration format: YEAR-DEPT-RANDOM_SEQUENCE
     * e.g., 2026-ENG-4821
     */
    private String generateRegistrationNumber(Profile profile) {
        int currentYear = LocalDate.now().getYear();
        String dept = (profile.getDepartment() != null && profile.getDepartment().length() >= 3)
                ? profile.getDepartment().substring(0, 3).toUpperCase()
                : "GEN";
        
        // Appending a small timestamp sequence to ensure fast uniqueness without collisions
        long sequence = System.currentTimeMillis() % 10000;
        return String.format("%d-%s-%04d", currentYear, dept, sequence);
    }

    public void validatePhoto(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        // Size check: Limit to 2MB Max
        long maxSizeBytes = 2 * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("Photo size exceeds maximum allowed limit of 2MB.");
        }

        // Content Type check: Accept only JPEG or PNG formats
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new IllegalArgumentException("Invalid file format. Only JPEG and PNG images are supported.");
        }
    }
}