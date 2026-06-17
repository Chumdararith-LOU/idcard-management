package net.orderzone.idcard.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import net.orderzone.idcard.model.BarcodeType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
public class BarcodeService {

    /**
     * Generates a QR Code image encoded as a Base64 PNG string.
     * Ideal for embedding verification URLs or profile UUIDs directly onto cards.
     */
    public String generateQrCodeBase64(String text, int width, int height) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR Code: " + e.getMessage(), e);
        }
    }

    public String generateBarcodeBase64(String text, BarcodeType type, int width, int height) {
        try {
            BarcodeFormat format;
            if (type != null && type == BarcodeType.EAN_13) {
                format = BarcodeFormat.EAN_13;
                if (!text.matches("\\d{12,13}")) {
                    text = "1234567890128"; 
                }
            } else {
                format = BarcodeFormat.CODE_128; 
            }

            BitMatrix bitMatrix = new MultiFormatWriter().encode(text, format, width, height);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Barcode (" + type + "): " + e.getMessage(), e);
        }
    }
}