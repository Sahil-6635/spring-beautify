package com.example.beautify.controller;

import com.example.beautify.service.BeautifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/beautify")
public class BeautifyController {

    @Autowired
    private BeautifyService beautifyService;

    // Beautify JSON
    @PostMapping(value = "/json", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String beautifyJson(@RequestBody String jsonInput) {
        return beautifyService.beautifyJson(jsonInput);
    }

    // Beautify XML
    @PostMapping(value = "/xml", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String beautifyXml(@RequestBody String xmlInput) {
        return beautifyService.beautifyXml(xmlInput);
    }


    // ✅ Generate PDF containing URL, Request Body, and Response Body
    @PostMapping(value = "/file", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> generateFile(
            @RequestHeader(value = "fileType", defaultValue = "pdf") String fileType,
            @RequestHeader(value = "fileName", defaultValue = "api-details") String fileName,
            @RequestBody Map<String, String> data) {

        data.put("fileType", fileType);
        data.put("fileName", fileName);
        return beautifyService.generateFile(data);
    }
}
