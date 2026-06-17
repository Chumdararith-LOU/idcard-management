package net.orderzone.idcard.model;

import java.time.LocalDate;
import java.util.UUID;

public class ProfileBuilder {

    public static Profile buildDefaultProfile(ProfileType type, Template defaultTemplate) {
        String uniqueRegNum = "TMP-" + (type == null ? "USER" : type.name()) + "-" + System.currentTimeMillis() % 10000;
        
        return Profile.builder()
                .uuid(UUID.randomUUID().toString())
                .registrationNumber(uniqueRegNum)
                .type(type != null ? type : ProfileType.USER)
                .fullName("John Doe")
                .department("General Administration")
                .title(type == ProfileType.STUDENT ? "Undergraduate Student" : "Staff Member")
                .email("johndoe@example.com")
                .phone("+1234567890")
                .bloodGroup("O+")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .issueDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(4)) // Default validity of 4 years
                .barcodeType(BarcodeType.CODE_128)
                .template(defaultTemplate)
                .build();
    }
}