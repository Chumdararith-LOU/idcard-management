package net.orderzone.idcard.controller;

import net.orderzone.idcard.model.Profile;
import net.orderzone.idcard.model.Template;
import net.orderzone.idcard.service.BarcodeService;
import net.orderzone.idcard.service.ProfileService;
import net.orderzone.idcard.service.TemplateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ProfileViewController {

    private final ProfileService profileService;
    private final BarcodeService barcodeService;
    private final TemplateService templateService;

    public ProfileViewController(ProfileService profileService, BarcodeService barcodeService, TemplateService templateService) {
        this.profileService = profileService;
        this.barcodeService = barcodeService;
        this.templateService = templateService;
    }

    @GetMapping("/")
    public String showDashboard(@RequestParam(value = "search", required = false) String search, Model model) {
        List<Profile> profiles = profileService.searchProfiles(search);
        model.addAttribute("profiles", profiles);
        model.addAttribute("searchKeyword", search);
        return "dashboard";
    }

    @GetMapping("/profiles/new")
    public String showNewProfileForm(Model model) {
        Template defaultTheme = templateService.getOrCreateDefaultTemplate();
        List<Template> templates = templateService.getAllTemplates();
        
        model.addAttribute("templates", templates);
        model.addAttribute("defaultTemplate", defaultTheme);
        return "new-profile";
    }

    @GetMapping("/profiles/batch")
    public String showBatchExportForm(Model model) {
        model.addAttribute("profiles", profileService.getAllProfiles());
        return "batch-export";
    }

    @GetMapping("/profiles/{id}/preview")
    public String renderLiveIdCardPreview(@PathVariable Long id, Model model) {
        Profile profile = profileService.getProfileById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profile ID " + id + " does not exist."));

        String trackingUrl = "https://orderzone.net/verify/idcard/" + profile.getUuid();
        String qrBase64 = barcodeService.generateQrCodeBase64(trackingUrl, 200, 200);
        String barcodeBase64 = barcodeService.generateBarcodeBase64(
                profile.getRegistrationNumber(), 
                profile.getBarcodeType(), 
                250, 60
        );

        model.addAttribute("profile", profile);
        model.addAttribute("qrCodeBase64", qrBase64);
        model.addAttribute("barcodeBase64", barcodeBase64);

        return "idcard-preview";
    }
}