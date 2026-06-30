package com.opsbrain.postmortem.service;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class PDFGenerationService {

    public byte[] generatePostmortemPDF(String title, String content, LocalDateTime generatedAt, String generatedBy) {
        log.info("Generating PDF for postmortem: {}", title);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Title
            PdfFont titleFont = PdfFontFactory.createFont();
            Paragraph titlePara = new Paragraph(title)
                    .setFont(titleFont)
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(titlePara);

            // Metadata
            PdfFont metadataFont = PdfFontFactory.createFont();
            String metadata = String.format("Generated: %s | By: %s",
                    generatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    generatedBy != null ? generatedBy : "System"
            );
            Paragraph metadataPara = new Paragraph(metadata)
                    .setFont(metadataFont)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10)
                    .setMarginBottom(20);
            document.add(metadataPara);

            // Content
            PdfFont contentFont = PdfFontFactory.createFont();
            String[] lines = content.split("\n");
            for (String line : lines) {
                Paragraph contentPara = new Paragraph(line)
                        .setFont(contentFont)
                        .setFontSize(11)
                        .setMarginBottom(5);
                document.add(contentPara);
            }

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}
