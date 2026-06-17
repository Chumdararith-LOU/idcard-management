package net.orderzone.idcard;

import net.orderzone.idcard.model.Profile;
import net.orderzone.idcard.model.ProfileBuilder;
import net.orderzone.idcard.model.ProfileType;
import net.orderzone.idcard.model.Template;
import net.orderzone.idcard.repository.ProfileRepository;
import net.orderzone.idcard.service.ProfileService;
import net.orderzone.idcard.service.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private ProfileService profileService;

    private Template mockTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        mockTemplate = Template.builder()
                .id(1L)
                .code("DEFAULT")
                .name("Default Theme")
                .build();
    }

    @Test
    void testProfileBuilder_ShouldCreateValidDefaultProfile() {
        // Test ProfileBuilder directly
        Profile defaultProfile = ProfileBuilder.buildDefaultProfile(ProfileType.STUDENT, mockTemplate);

        assertNotNull(defaultProfile);
        assertEquals(ProfileType.STUDENT, defaultProfile.getType());
        assertEquals("John Doe", defaultProfile.getFullName());
        assertEquals("Default Theme", defaultProfile.getTemplate().getName());
        assertNotNull(defaultProfile.getUuid());
        assertNotNull(defaultProfile.getRegistrationNumber());
    }

    @Test
    void testCreateProfile_ShouldAutoGenerateRegistrationAndUuid() {
        Profile inputProfile = Profile.builder()
                .fullName("Jane Smith")
                .department("Engineering")
                .type(ProfileType.EMPLOYEE)
                .build();

        when(profileRepository.existsByRegistrationNumber(anyString())).thenReturn(false);
        when(templateService.getOrCreateDefaultTemplate()).thenReturn(mockTemplate);
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Profile savedProfile = profileService.createProfile(inputProfile);

        assertNotNull(savedProfile.getUuid());
        assertNotNull(savedProfile.getRegistrationNumber());
        assertTrue(savedProfile.getRegistrationNumber().contains("ENG")); // Validates custom sequence contains short dept code
        assertEquals(mockTemplate, savedProfile.getTemplate());
        verify(profileRepository, times(1)).save(any(Profile.class));
    }

    @Test
    void testValidatePhoto_ShouldThrowException_WhenFileIsTooLarge() {
        // Creating a 3MB mock file (Max limit is 2MB)
        byte[] largeFileBytes = new byte[3 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile("file", "avatar.png", "image/png", largeFileBytes);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            profileService.validatePhoto(largeFile);
        });

        assertTrue(exception.getMessage().contains("exceeds maximum allowed limit"));
    }

    @Test
    void testValidatePhoto_ShouldThrowException_WhenFormatIsInvalid() {
        byte[] validSizeBytes = new byte[100];
        // Invalid text/plain content type
        MockMultipartFile textFile = new MockMultipartFile("file", "document.txt", "text/plain", validSizeBytes);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            profileService.validatePhoto(textFile);
        });

        assertTrue(exception.getMessage().contains("Only JPEG and PNG images are supported"));
    }
}