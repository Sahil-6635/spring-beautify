package com.example.beautify.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.apache.commons.text.StringEscapeUtils;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;


import java.io.ByteArrayOutputStream;
import java.util.Map;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Element;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;



@Service
public class BeautifyService {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public String beautifyJson(String jsonInput) {
        try {
            String cleaned = jsonInput.trim();

            // Detect and clean escaped JSON (e.g. {\"key\":\"value\"})
            if (cleaned.startsWith("\"{") && cleaned.endsWith("}\"")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }

            // Replace escaped quotes
            cleaned = cleaned.replace("\\\"", "\"");

            // Remove redundant escaping (\\n, \\t, etc.)
            cleaned = cleaned.replace("\\n", "").replace("\\t", "");

            return GSON.toJson(JsonParser.parseString(cleaned));
        } catch (Exception e) {
            return "❌ Error while beautifying JSON: " + e.getMessage() + "\nInput: " + jsonInput;
        }
    }

    public String beautifyXml(String xmlInput) {
        try {
            if (xmlInput == null || xmlInput.trim().isEmpty()) {
                return "❌ Empty XML input!";
            }

            String cleanXml = StringEscapeUtils.unescapeJava(xmlInput.trim());
            if (cleanXml.startsWith("\"") && cleanXml.endsWith("\"")) {
                cleanXml = cleanXml.substring(1, cleanXml.length() - 1);
            }

            // Step 1: Unescape special characters (handles &lt;, &gt;, etc.)
            String unescaped = cleanXml
                    .replaceAll("^\"|\"$", "")
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");

            // Step 2: Remove extra nested XML declarations
            unescaped = StringEscapeUtils.unescapeXml(unescaped.trim());
            unescaped = unescaped.replaceAll("<\\?xml[^>]*\\?>", "");

            // Step 3: Detect XML version
            String version = detectXmlVersion(xmlInput);

            // Step 4: Prepend proper XML declaration
            String declaration = "<?xml version=\"" + version + "\" encoding=\"UTF-8\"?>\n";
            String cleanedXml = declaration + unescaped;

            // Step 5: Beautify output
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StreamSource source = new StreamSource(new StringReader(cleanedXml));
            StringWriter writer = new StringWriter();
            transformer.transform(source, new StreamResult(writer));

            return writer.toString();
        } catch (Exception e) {
            return "❌ Error while beautifying XML: " + e.getMessage();
        }
    }

    private String detectXmlVersion(String xml) {
        if (xml == null) return "1.0";
        if (xml.contains("version=\"1.2\"")) return "1.2";
        if (xml.contains("version=\"1.1\"")) return "1.1";
        return "1.0";
    }

    public ResponseEntity<byte[]> generateFile(Map<String, String> data) {
        // Default to PDF if not provided
        String fileType = data.getOrDefault("fileType", "pdf").toLowerCase();

        if (fileType.equals("text")) {
            return generateTextFile(data);
        } else {
            return generatePdf(data);
        }
    }


    public ResponseEntity<byte[]> generatePdf(Map<String, String> data) {
        try {
            String requestBody = data.getOrDefault("requestBody", "N/A");
            String responseBody = data.getOrDefault("responseBody", "N/A");
            String url = data.getOrDefault("url", "N/A");
            String fileName = data.getOrDefault("fileName", "api-details");

            // Beautify JSON/XML automatically
            requestBody = autoBeautify(requestBody);
            responseBody = autoBeautify(responseBody);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document pdfDoc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(pdfDoc, baos);
            pdfDoc.open();

            // ====== Title ======
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(0, 102, 204));
            Paragraph title = new Paragraph("API Transaction Summary", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12f);
            pdfDoc.add(title);

            // ====== Timestamp ======
            Font dateFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY);
            String timestamp = "Generated on: " + java.time.LocalDateTime.now()
                    .toString().replace("T", " ").substring(0, 19);
            Paragraph datePara = new Paragraph(timestamp, dateFont);
            datePara.setAlignment(Element.ALIGN_RIGHT);
            datePara.setSpacingAfter(15f);
            pdfDoc.add(datePara);

            // ====== Conditional Sections ======
            if (!"N/A".equalsIgnoreCase(url.trim()) && !url.trim().isEmpty()) {
                addSection(pdfDoc, "🔗 URL :", url, new BaseColor(230, 240, 255));
            }

            if (!"N/A".equalsIgnoreCase(requestBody.trim()) && !requestBody.trim().isEmpty()) {
                addSection(pdfDoc, "📤 Request Body :", requestBody, new BaseColor(255, 247, 230));
            }

            if (!"N/A".equalsIgnoreCase(responseBody.trim()) && !responseBody.trim().isEmpty()) {
                addSection(pdfDoc, "📥 Response Body :", responseBody, new BaseColor(230, 255, 230));
            }

            pdfDoc.close();

            // ====== Response ======
            byte[] pdfBytes = baos.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(("Error generating PDF: " + e.getMessage()).getBytes());
        }
    }


    private void addSection(Document document, String title, String content, BaseColor bgColor)
            throws DocumentException {
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, new BaseColor(0, 0, 102));
        Font bodyFont = new Font(Font.FontFamily.COURIER, 10);

        // Paragraph for section label (e.g. "Request Body:")
        Paragraph labelParagraph = new Paragraph(title, labelFont);
        labelParagraph.setSpacingBefore(10f);
        labelParagraph.setSpacingAfter(5f);
        document.add(labelParagraph);

        // Paragraph for section content (JSON/XML)
        Paragraph contentParagraph = new Paragraph(content.trim(), bodyFont);
        contentParagraph.setIndentationLeft(15f);  // left indent for neat look
        contentParagraph.setLeading(0, 1.25f);      // line spacing

        // Wrap content inside a highlighted cell for background
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell(contentParagraph);
        cell.setBackgroundColor(bgColor);
        cell.setPaddingTop(10f);
        cell.setPaddingBottom(10f);
        cell.setPaddingLeft(12f);
        cell.setPaddingRight(10f);
        cell.setBorderColor(BaseColor.LIGHT_GRAY);
        cell.setNoWrap(false);

        // Allow long content to split naturally across pages
        table.setSplitLate(false);
        table.addCell(cell);

        document.add(table);
        document.add(new Paragraph(" ")); // space between sections
    }




    // Helper: auto-beautify JSON/XML
    private String autoBeautify(String input) {
        if (input == null) return "N/A";
        input = input.trim();
        if (input.startsWith("{") || input.startsWith("[")) {
            return beautifyJson(input);
        } else if (input.startsWith("<")) {
            return beautifyXml(input);
        }
        return input;
    }

    public ResponseEntity<byte[]> generateTextFile(Map<String, String> data) {
        try {
            String requestBody = data.getOrDefault("requestBody", "N/A");
            String responseBody = data.getOrDefault("responseBody", "N/A");
            String url = data.getOrDefault("url", "N/A");
            String fileName = data.getOrDefault("fileName", "api-request-response");

            // Beautify JSON/XML automatically for readability
            requestBody = autoBeautify(requestBody);
            responseBody = autoBeautify(responseBody);

            // Build text content
            StringBuilder sb = new StringBuilder();
            sb.append("=====================================\n");
            sb.append("         API TRANSACTION REPORT       \n");
            sb.append("=====================================\n\n");

            if (!"N/A".equalsIgnoreCase(url.trim())) {
                sb.append("🔗 URL:\n");
                sb.append(url).append("\n\n");
            }

            if (!"N/A".equalsIgnoreCase(requestBody.trim())) {
                sb.append("📤 Request Body:\n");
                sb.append(requestBody).append("\n\n");
            }

            if (!"N/A".equalsIgnoreCase(responseBody.trim())) {
                sb.append("📥 Response Body:\n");
                sb.append(responseBody).append("\n");
            }
            String finalContent = sb.toString();

            // Convert to bytes
            byte[] textBytes = finalContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // Set headers for download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", fileName + ".txt");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(textBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(("Error generating TXT file: " + e.getMessage()).getBytes());
        }
    }

}
