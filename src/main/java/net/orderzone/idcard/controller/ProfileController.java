package net.orderzone.idcard.controller;

import net.orderzone.idcard.model.Profile;
import net.orderzone.idcard.service.ProfileService;
import net.orderzone.idcard.service.BarcodeService;
import net.orderzone.idcard.service.PdfExportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final BarcodeService barcodeService;    
    private final PdfExportService pdfExportService;    
    private final String UPLOAD_DIR = "uploads/photos/";

    
    public ProfileController(ProfileService profileService, BarcodeService barcodeService, PdfExportService pdfExportService) {
        this.profileService = profileService;
        this.barcodeService = barcodeService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping
    public ResponseEntity<List<Profile>> getAllProfiles(@RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(profileService.searchProfiles(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Profile> getProfileById(@PathVariable Long id) {
        return profileService.getProfileById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/uuid/{uuid}")
    public ResponseEntity<Profile> getProfileByUuid(@PathVariable String uuid) {
        return profileService.getProfileByUuid(uuid)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createProfile(@RequestBody Profile profile) {
        try {
            Profile created = profileService.createProfile(profile);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody Profile profileDetails) {
        try {
            Profile updated = profileService.updateProfile(id, profileDetails);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long id) {
        try {
            profileService.deleteProfile(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Endpoint to upload a profile photo. Handles filesystem allocation and business validations.
     */
    @PostMapping("/{id}/photo")
    public ResponseEntity<String> uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            // 1. Run profile existence check
            Profile profile = profileService.getProfileById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Profile matching ID " + id + " does not exist."));

            // 2. Enforce file constraint rules (size & format checking)
            profileService.validatePhoto(file);

            // 3. Prepare unique file storage destination
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(uniqueFileName);

            // 4. Save file payload to the disk local workspace
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 5. Commit record structural fields back to the model database
            profile.setPhotoFileName(uniqueFileName);
            profile.setPhotoContentType(file.getContentType());
            profileService.updateProfile(id, profile);

            return ResponseEntity.ok("Photo uploaded successfully. Filename stored: " + uniqueFileName);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to store file on filesystem: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/qrcode")
    public ResponseEntity<String> getProfileQrCode(@PathVariable Long id) {
        return profileService.getProfileById(id)
                .map(profile -> {
                    String verificationUrl = "https://orderzone.net/verify/idcard/" + profile.getUuid();
                    String base64Image = barcodeService.generateQrCodeBase64(verificationUrl, 250, 250);
                    return ResponseEntity.ok("data:image/png;base64," + base64Image);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/barcode")
    public ResponseEntity<String> getProfileBarcode(@PathVariable Long id) {
        return profileService.getProfileById(id)
                .map(profile -> {
                    String base64Image = barcodeService.generateBarcodeBase64(
                            profile.getRegistrationNumber(), 
                            profile.getBarcodeType(), 
                            300, 80
                    );
                    return ResponseEntity.ok("data:image/png;base64," + base64Image);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportSingleIdCardPdf(@PathVariable Long id) {
        Profile profile = profileService.getProfileById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found."));

        byte[] pdfContents = pdfExportService.generateBatchIdCardsPdf(List.of(profile));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("inline", "idcard-" + profile.getRegistrationNumber() + ".pdf");
        
        return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/batch/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportBatchIdCardsPdf(@RequestParam(value = "search", required = false) String search) {
        List<Profile> targetingGroup = profileService.searchProfiles(search);
        
        if (targetingGroup.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        byte[] pdfContents = pdfExportService.generateBatchIdCardsPdf(targetingGroup);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "batch-idcards-export.pdf");
        
        return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
    }
}