package net.orderzone.idcard.controller;

import net.orderzone.idcard.model.Profile;
import net.orderzone.idcard.service.BarcodeService;
import net.orderzone.idcard.service.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profiles")
public class ProfileViewController {

    private final ProfileService profileService;
    private final BarcodeService barcodeService;

    public ProfileViewController(ProfileService profileService, BarcodeService barcodeService) {
        this.profileService = profileService;
        this.barcodeService = barcodeService;
    }

    /**
     * Renders a live visual view representation of the specified identity cardholder template.
     * URI: http://localhost:8080/profiles/{id}/preview
     */
    @GetMapping("/{id}/preview")
    public String renderLiveIdCardPreview(@PathVariable Long id, Model model) {
        Profile profile = profileService.getProfileById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cardholder matching profile ID " + id + " does not exist."));

        // 1. Generate real-time base64 vector graphic payloads
        String verificationUrl = "https://orderzone.net/verify/idcard/" + profile.getUuid();
        String qrBase64 = barcodeService.generateQrCodeBase64(verificationUrl, 200, 200);
        String barcodeBase64 = barcodeService.generateBarcodeBase64(
                profile.getRegistrationNumber(), 
                profile.getBarcodeType(), 
                250, 60
        );

        // 2. Bind model attributes onto Thymeleaf variable scopes
        model.addAttribute("profile", profile);
        model.addAttribute("qrCodeBase64", qrBase64);
        model.addAttribute("barcodeBase64", barcodeBase64);

        // 3. Resolve rendering back to the idcard-preview.html template file
        return "idcard-preview";
    }
}