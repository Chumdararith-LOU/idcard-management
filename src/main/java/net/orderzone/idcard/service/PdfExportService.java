package net.orderzone.idcard.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import net.orderzone.idcard.model.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

@Service
public class PdfExportService {

    private final BarcodeService barcodeService;
    private final String UPLOAD_DIR = "uploads/photos/";

    public PdfExportService(BarcodeService barcodeService) {
        this.barcodeService = barcodeService;
    }

    public byte[] generateBatchIdCardsPdf(List<Profile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            throw new IllegalArgumentException("Profile collection for batch export cannot be empty.");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        boolean isHorizontal = profiles.get(0).getTemplate() != null && "HORIZONTAL".equalsIgnoreCase(profiles.get(0).getTemplate().getLayout());
        Rectangle pageSize = isHorizontal ? new Rectangle(243f, 153f) : new Rectangle(153f, 243f);

        Document document = new Document(pageSize, 0f, 0f, 0f, 0f);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            for (int i = 0; i < profiles.size(); i++) {
                Profile profile = profiles.get(i);
                
                if (i > 0) {
                    boolean currentHorizontal = profile.getTemplate() != null && "HORIZONTAL".equalsIgnoreCase(profile.getTemplate().getLayout());
                    document.setPageSize(currentHorizontal ? new Rectangle(243f, 153f) : new Rectangle(153f, 243f));
                    document.newPage();
                }

                renderSingleCardPage(writer, profile);
            }

            document.close();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Error rendering batch iText structural export: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    private void renderSingleCardPage(PdfWriter writer, Profile profile) throws DocumentException, IOException {
        PdfContentByte cb = writer.getDirectContent();
        float width = writer.getPageSize().getWidth();
        float height = writer.getPageSize().getHeight();

        // 1. Establish robust style parameters with safe default fallbacks
        String primaryHex = (profile.getTemplate() != null && profile.getTemplate().getPrimaryColor() != null) 
                ? profile.getTemplate().getPrimaryColor() : "#1d4ed8";
        String secondaryHex = (profile.getTemplate() != null && profile.getTemplate().getSecondaryColor() != null) 
                ? profile.getTemplate().getSecondaryColor() : "#e0e7ff";
        String textHex = (profile.getTemplate() != null && profile.getTemplate().getTextColor() != null) 
                ? profile.getTemplate().getTextColor() : "#111827";
        String organizationName = (profile.getTemplate() != null && profile.getTemplate().getOrganizationName() != null) 
                ? profile.getTemplate().getOrganizationName() : "ORDERZONE ACADEMY";
        String layout = (profile.getTemplate() != null && profile.getTemplate().getLayout() != null) 
                ? profile.getTemplate().getLayout() : "VERTICAL";

        BaseColor primaryColor = new BaseColor(java.awt.Color.decode(primaryHex).getRGB());
        BaseColor secondaryColor = new BaseColor(java.awt.Color.decode(secondaryHex).getRGB());
        BaseColor textColor = new BaseColor(java.awt.Color.decode(textHex).getRGB());

        // 2. Draw Main Background Base Card Shell Canvas
        cb.setColorFill(BaseColor.WHITE);
        cb.rectangle(0, 0, width, height);
        cb.fill();

        // 3. Draw Header Blocks depending on the orientation format
        boolean isHorizontal = "HORIZONTAL".equalsIgnoreCase(layout);

        if (!isHorizontal) {
            // VERTICAL LAYOUT
            cb.setColorFill(primaryColor);
            cb.rectangle(0, height - 50f, width, 50f);
            cb.fill();

            cb.setColorFill(secondaryColor);
            cb.rectangle(0, 0, width, 15f);
            cb.fill();

            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            
            cb.beginText();
            cb.setFontAndSize(bf, 10f);
            cb.setColorFill(BaseColor.WHITE);
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, organizationName.toUpperCase(), width / 2, height - 25f, 0);
            
            cb.setFontAndSize(bf, 6f);
            if (profile.getTemplate() != null && profile.getTemplate().getTagline() != null) {
                cb.showTextAligned(PdfContentByte.ALIGN_CENTER, profile.getTemplate().getTagline(), width / 2, height - 38f, 0);
            }

            // Cardholder Body Meta Info Context fields
            cb.setColorFill(textColor);
            cb.setFontAndSize(bf, 11f);
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, profile.getFullName(), width / 2, height - 120f, 0);

            cb.setFontAndSize(bf, 7f);
            cb.setColorFill(BaseColor.DARK_GRAY);
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, profile.getTitle(), width / 2, height - 132f, 0);

            // Left-aligned meta records block
            cb.setColorFill(textColor);
            cb.setFontAndSize(bf, 6.5f);
            float currentY = height - 155f;
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, "Reg Num:  " + profile.getRegistrationNumber(), 15f, currentY, 0);
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, "Dept:         " + (profile.getDepartment() != null ? profile.getDepartment() : "N/A"), 15f, currentY - 10f, 0);
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, "Blood Grp: " + (profile.getBloodGroup() != null ? profile.getBloodGroup() : "N/A"), 15f, currentY - 20f, 0);
            cb.endText();

            // Render Verification QR Codes and Identification barcodes onto the footer zone
            injectEmbedGraphics(cb, profile, width, height, isHorizontal);

        } else {
            // HORIZONTAL LAYOUT
            cb.setColorFill(primaryColor);
            cb.rectangle(0, 0, 85f, height);
            cb.fill();

            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            
            cb.beginText();
            cb.setFontAndSize(bf, 9f);
            cb.setColorFill(BaseColor.WHITE);
            // Rotate string layout 90 degrees upwards for horizontal sidebars
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, organizationName.toUpperCase(), 42f, height / 2, 90f);
            
            cb.setColorFill(textColor);
            cb.setFontAndSize(bf, 12f);
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, profile.getFullName(), 100f, height - 35f, 0);

            cb.setFontAndSize(bf, 8f);
            cb.setColorFill(BaseColor.DARK_GRAY);
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, profile.getTitle(), 100f, height - 48f, 0);

            cb.setColorFill(textColor);
            cb.setFontAndSize(bf, 7f);
            float currentY = height - 75f;
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, "ID Reg No: " + profile.getRegistrationNumber(), 100f, currentY, 0);
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, "Department: " + (profile.getDepartment() != null ? profile.getDepartment() : "N/A"), 100f, currentY - 12f, 0);
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, "Blood Type: " + (profile.getBloodGroup() != null ? profile.getBloodGroup() : "N/A"), 100f, currentY - 24f, 0);
            cb.endText();

            injectEmbedGraphics(cb, profile, width, height, isHorizontal);
        }
    }

    private void injectEmbedGraphics(PdfContentByte cb, Profile profile, float width, float height, boolean isHorizontal) throws DocumentException, IOException {
        // 1. Photo Avatar Placement Parsing logic
        if (profile.hasPhoto()) {
            Path photoPath = Paths.get(UPLOAD_DIR).resolve(profile.getPhotoFileName());
            if (Files.exists(photoPath)) {
                Image img = Image.getInstance(photoPath.toAbsolutePath().toString());
                if (!isHorizontal) {
                    img.scaleAbsolute(45f, 45f);
                    img.setAbsolutePosition(width / 2 - 22.5f, height - 100f);
                } else {
                    // Reposition photo safely to the upper middle-right field area
                    img.scaleAbsolute(45f, 45f);
                    img.setAbsolutePosition(185f, height - 65f);
                }
                cb.addImage(img);
            }
        }

        String trackingUrl = "https://orderzone.net/verify/idcard/" + profile.getUuid();
        
        byte[] qrBytes = Base64.getDecoder().decode(barcodeService.generateQrCodeBase64(trackingUrl, 100, 100));
        Image qrImg = Image.getInstance(qrBytes);
        
        byte[] bcBytes = Base64.getDecoder().decode(barcodeService.generateBarcodeBase64(profile.getRegistrationNumber(), profile.getBarcodeType(), 150, 40));
        Image barcodeImg = Image.getInstance(bcBytes);

        if (!isHorizontal) {
            qrImg.scaleAbsolute(30f, 30f);
            qrImg.setAbsolutePosition(20f, 22f);
            
            barcodeImg.scaleAbsolute(70f, 22f);
            barcodeImg.setAbsolutePosition(68f, 25f);
        } else {
            // Standardized footer alignment parameters for Horizontal layout
            qrImg.scaleAbsolute(32f, 32f);
            qrImg.setAbsolutePosition(195f, 15f);
            
            barcodeImg.scaleAbsolute(80f, 20f);
            barcodeImg.setAbsolutePosition(100f, 20f); // Safely clear of the upper profile photo area
        }

        cb.addImage(qrImg);
        cb.addImage(barcodeImg);
    }
}