package com.groupe.gestin_inscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugDataController {

    private static final Logger logger = LoggerFactory.getLogger(DebugDataController.class);

    @PostMapping(value = "/check-data", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> checkReceivedData(
            @RequestParam(required = false) String applicationData,
            @RequestPart(required = false) List<MultipartFile> files) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Log raw data
            logger.info("=== DEBUG DATA RECEPTION ===");
            logger.info("Raw applicationData: {}", applicationData);
            logger.info("Files count: {}", files != null ? files.size() : 0);
            
            // Parse JSON data
            if (applicationData != null && !applicationData.trim().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                Map<String, Object> parsedData = mapper.readValue(applicationData, Map.class);
                
                logger.info("Parsed data keys: {}", parsedData.keySet());
                
                // Check each section
                if (parsedData.containsKey("personalInfo")) {
                    Map<String, Object> personalInfo = (Map<String, Object>) parsedData.get("personalInfo");
                    logger.info("PersonalInfo keys: {}", personalInfo.keySet());
                    logger.info("PersonalInfo values: {}", personalInfo);
                }
                
                if (parsedData.containsKey("contactInfo")) {
                    Map<String, Object> contactInfo = (Map<String, Object>) parsedData.get("contactInfo");
                    logger.info("ContactInfo keys: {}", contactInfo.keySet());
                }
                
                if (parsedData.containsKey("academicHistory")) {
                    Map<String, Object> academicHistory = (Map<String, Object>) parsedData.get("academicHistory");
                    logger.info("AcademicHistory keys: {}", academicHistory.keySet());
                }
                
                response.put("parsedData", parsedData);
            }
            
            response.put("success", true);
            response.put("rawData", applicationData);
            response.put("filesCount", files != null ? files.size() : 0);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error in debug endpoint", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}