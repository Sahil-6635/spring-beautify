package com.example.beautify.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.springframework.stereotype.Service;
import org.apache.commons.text.StringEscapeUtils;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

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
}
