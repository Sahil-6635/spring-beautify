package com.example.beautify.controller;

import com.example.beautify.service.BeautifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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
}
