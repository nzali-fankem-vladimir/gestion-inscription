package com.groupe.gestin_inscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.groupe.gestin_inscription.dto.request.ApplicationSubmissionRequestDTO;
import com.groupe.gestin_inscription.dto.request.DocumentUploadRequestDTO;
import com.groupe.gestin_inscription.dto.response.ApplicationStatusResponseDto;
import com.groupe.gestin_inscription.model.Application;
import com.groupe.gestin_inscription.model.User;
import com.groupe.gestin_inscription.model.AcademicHistory;
import com.groupe.gestin_inscription.model.Enums.ApplicationStatus;
import com.groupe.gestin_inscription.model.Enums.Gender;
import com.groupe.gestin_inscription.repository.UserRepository;
import com.groupe.gestin_inscription.repository.AcademicHistoryRepository;
import com.groupe.gestin_inscription.repository.ApplicationRepository;
import com.groupe.gestin_inscription.services.serviceImpl.ApplicationServiceImpl;
import com.groupe.gestin_inscription.services.serviceImpl.DocumentServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Application Management", description = "Endpoints for managing the application submission workflow")
public class ApplicationController {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationController.class);

    @Autowired
    private ApplicationServiceImpl applicationServiceImpl;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AcademicHistoryRepository academicHistoryRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private DocumentServiceImpl documentService;

    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Test endpoint works");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all-simple")
    @PreAuthorize("hasAnyAuthority('ROLE_AGENT', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllApplicationsSimple() {
        try {
            logger.info("Starting getAllApplicationsSimple");
            List<Application> applications = applicationRepository.findAll();
            logger.info("Found {} applications", applications.size());
            
            List<Map<String, Object>> applicationList = new ArrayList<>();
            for (Application app : applications) {
                try {
                    Map<String, Object> dto = convertApplicationToDto(app);
                    applicationList.add(dto);
                    logger.debug("Successfully converted application {} to DTO", app.getId());
                } catch (Exception e) {
                    logger.error("Error converting application {} to DTO: {}", app.getId(), e.getMessage(), e);
                    // Continue with other applications instead of failing completely
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applications", applicationList);
            response.put("count", applicationList.size());
            
            logger.info("Successfully processed {} applications", applicationList.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error in getAllApplicationsSimple", e);
            Map<String, Object> errorResponse = createErrorResponse("INTERNAL_ERROR", "Une erreur interne s'est produite");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/submit-simple")
    public ResponseEntity<?> submitSimpleApplication(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("MISSING_USERNAME", "Nom d'utilisateur requis"));
            }
            
            Application newApplication = applicationServiceImpl.createFromExistingUser(username, new ArrayList<>());
            return ResponseEntity.ok(convertToDto(newApplication));
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(createErrorResponse("ALREADY_SUBMITTED", "Candidature déjà soumise"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(createErrorResponse("USER_NOT_FOUND", "Utilisateur non trouvé"));
        } catch (Exception e) {
            logger.error("Server error during application submission", e);
            return ResponseEntity.status(500).body(createErrorResponse("SERVER_ERROR", "Erreur du serveur"));
        }
    }
    
    @PostMapping(value = "/submit", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> submitApplication(
            @RequestParam(required = false) String applicationData,
            @RequestPart(required = false) List<MultipartFile> files) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            
            logger.info("Processing application submission for user: {}", currentUsername);
            
            // Find or create user
            User user = findOrCreateUser(currentUsername);
            
            // Parse application data from frontend format
            Map<String, Object> frontendData = null;
            if (applicationData != null && !applicationData.trim().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                frontendData = mapper.readValue(applicationData, Map.class);
                logger.info("Parsed frontend application data for user: {}. Data keys: {}", currentUsername, frontendData.keySet());
                logger.info("Raw applicationData received: {}", applicationData);
            } else {
                logger.warn("No application data received for user: {}", currentUsername);
            }
            
            // Update user with form data
            if (frontendData != null) {
                logger.info("Before update - User data: firstName={}, lastName={}, email={}", 
                    user.getFirstName(), user.getLastName(), user.getEmail());
                updateUserFromFrontendData(user, frontendData);
                user = userRepository.save(user);
                logger.info("After update - User data: firstName={}, lastName={}, email={}", 
                    user.getFirstName(), user.getLastName(), user.getEmail());
                logger.info("Updated user information for: {}", currentUsername);
            }
            
            // Process documents
            List<DocumentUploadRequestDTO> documentRequests = new ArrayList<>();
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        DocumentUploadRequestDTO docRequest = new DocumentUploadRequestDTO();
                        docRequest.setName(file.getOriginalFilename());
                        docRequest.setFileContent(file);
                        docRequest.setDocumentType(file.getContentType());
                        documentRequests.add(docRequest);
                        logger.info("Added document: {} for processing", file.getOriginalFilename());
                    }
                }
            }
            
            // Create application
            Application application = createApplicationFromFrontendData(user, frontendData, documentRequests);
            
            // Save documents
            if (!documentRequests.isEmpty()) {
                for (DocumentUploadRequestDTO docRequest : documentRequests) {
                    try {
                        documentService.uploadDocument(application.getId(), docRequest);
                        logger.info("Saved document: {} for application: {}", docRequest.getName(), application.getId());
                    } catch (Exception e) {
                        logger.error("Failed to save document: {}", docRequest.getName(), e);
                    }
                }
            }
            
            logger.info("Successfully created application with ID: {} for user: {}", application.getId(), currentUsername);
            return ResponseEntity.ok(convertToDto(application));
            
        } catch (IllegalStateException e) {
            logger.warn("Application already submitted for user");
            return ResponseEntity.status(409).body(createErrorResponse("ALREADY_SUBMITTED", "Candidature déjà soumise"));
        } catch (Exception e) {
            logger.error("Error during application submission", e);
            return ResponseEntity.status(500).body(createErrorResponse("SERVER_ERROR", "Erreur lors de la soumission"));
        }
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ROLE_CANDIDATE', 'ROLE_AGENT', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> getApplicationsByStatus(@PathVariable ApplicationStatus status) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            boolean isAgent = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_AGENT") || auth.getAuthority().equals("ROLE_SUPER_ADMIN"));

            List<Application> applications = applicationServiceImpl.getApplicationsByStatus(status);
            
            if (!isAgent) {
                // Pour les candidats, filtrer seulement leurs propres candidatures
                applications = applications.stream()
                        .filter(app -> app.getApplicantName() != null &&
                                currentUsername.equals(app.getApplicantName().getUsername()))
                        .collect(Collectors.toList());
            }

            // Pour les agents, retourner toutes les données complètes
            List<Map<String, Object>> responseDtos = applications.stream()
                    .map(isAgent ? this::convertApplicationToDto : app -> {
                        Map<String, Object> basicDto = new HashMap<>();
                        ApplicationStatusResponseDto dto = convertToDto(app);
                        basicDto.put("applicationId", dto.getApplicationId());
                        basicDto.put("status", dto.getStatus());
                        basicDto.put("completionRate", dto.getCompletionRate());
                        basicDto.put("submissionDate", dto.getSubmissionDate());
                        basicDto.put("applicantName", dto.getApplicantName());
                        return basicDto;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applications", responseDtos);
            response.put("status", status.name());
            response.put("count", responseDtos.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching applications by status", e);
            return ResponseEntity.status(500).body(createErrorResponse("STATUS_FETCH_ERROR", "Erreur lors de la recherche par statut"));
        }
    }

    @PutMapping("/review/{applicationId}")
    @PreAuthorize("hasAuthority('ROLE_AGENT')")
    public ResponseEntity<?> reviewApplication(@PathVariable Long applicationId, @RequestParam("decision") String reviewDecision) {
        try {
            applicationServiceImpl.reviewDossier(applicationId, reviewDecision);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Candidature traitée avec succès");
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(createErrorResponse("APPLICATION_NOT_FOUND", "Candidature non trouvée"));
        } catch (Exception e) {
            logger.error("Error during application review", e);
            return ResponseEntity.status(500).body(createErrorResponse("REVIEW_ERROR", "Erreur lors du traitement"));
        }
    }

    @GetMapping("/my-applications")
    public ResponseEntity<?> getMyApplications(@RequestParam(required = false) String email) {
        try {
            String currentUsername = getCurrentUsername(email);
            
            List<Application> allApplications = applicationServiceImpl.getAllApplications();
            List<Application> userApplications = allApplications.stream()
                    .filter(app -> app.getApplicantName() != null &&
                            (currentUsername.equals(app.getApplicantName().getUsername()) ||
                             currentUsername.equals(app.getApplicantName().getEmail())))
                    .collect(Collectors.toList());

            List<Map<String, Object>> responseDtos = userApplications.stream()
                    .map(app -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("applicationId", app.getId());
                        dto.put("status", app.getStatus().name());
                        dto.put("completionRate", app.getCompletionRate());
                        dto.put("submissionDate", app.getSubmissionDate());
                        dto.put("lastUpdated", app.getLastUpdated());
                        dto.put("targetInstitution", app.getTargetInstitution());
                        dto.put("specialization", app.getSpecialization());
                        
                        // Ajouter les documents
                        try {
                            List<com.groupe.gestin_inscription.model.Document> documents = documentService.getDocumentsByApplicationId(app.getId());
                            List<Map<String, Object>> docList = documents.stream()
                                .map(doc -> {
                                    Map<String, Object> docDto = new HashMap<>();
                                    docDto.put("id", doc.getId());
                                    docDto.put("name", doc.getName());
                                    docDto.put("fileType", doc.getFileType());
                                    docDto.put("validationStatus", doc.getValidationStatus().name());
                                    docDto.put("fileSizeMB", doc.getFileSizeMB());
                                    return docDto;
                                })
                                .collect(Collectors.toList());
                            dto.put("documents", docList);
                        } catch (Exception e) {
                            logger.warn("Could not load documents for application {}", app.getId());
                            dto.put("documents", new ArrayList<>());
                        }
                        
                        return dto;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applications", responseDtos);
            response.put("count", responseDtos.size());
            response.put("username", currentUsername);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching user applications", e);
            return ResponseEntity.status(500).body(createErrorResponse("FETCH_ERROR", "Erreur lors de la récupération des candidatures"));
        }
    }

    @GetMapping("/can-submit")
    public ResponseEntity<?> canSubmitApplication() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            
            List<Application> allApplications = applicationServiceImpl.getAllApplications();
            boolean hasExistingApplication = allApplications.stream()
                    .anyMatch(app -> app.getApplicantName() != null &&
                            currentUsername.equals(app.getApplicantName().getUsername()));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("canSubmit", !hasExistingApplication);
            response.put("username", currentUsername);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking application eligibility", e);
            return ResponseEntity.status(500).body(createErrorResponse("CHECK_ERROR", "Erreur de vérification"));
        }
    }

    @GetMapping("/debug-user/{username}")
    @PreAuthorize("hasAnyAuthority('ROLE_AGENT', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> debugUserData(@PathVariable String username) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username)
                    .or(() -> userRepository.findByEmail(username));
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(createErrorResponse("USER_NOT_FOUND", "Utilisateur non trouvé"));
            }
            
            User user = userOpt.get();
            Map<String, Object> userData = new HashMap<>();
            
            // Données personnelles
            userData.put("username", user.getUsername());
            userData.put("firstName", user.getFirstName());
            userData.put("lastName", user.getLastName());
            userData.put("email", user.getEmail());
            userData.put("phoneNumber", user.getPhoneNumber());
            userData.put("address", user.getAddress());
            userData.put("nationality", user.getNationality());
            userData.put("dateOfBirth", user.getDateOfBirth());
            userData.put("gender", user.getGender());
            userData.put("userIdNum", user.getUserIdNum());
            userData.put("emergencyContact", user.getEmergencyContact());
            
            // Historique académique
            if (user.getAcademicHistory() != null) {
                Map<String, Object> academic = new HashMap<>();
                AcademicHistory ah = user.getAcademicHistory();
                academic.put("lastInstitution", ah.getLastInstitution());
                academic.put("specialization", ah.getSpecialization());
                academic.put("subSpecialization", ah.getSubSpecialization());
                academic.put("educationLevel", ah.getEducationLevel());
                academic.put("gpa", ah.getGpa());
                academic.put("honors", ah.getHonors());
                academic.put("startDate", ah.getStartDate());
                academic.put("endDate", ah.getEndDate());
                userData.put("academicHistory", academic);
            }
            
            // Applications
            List<Application> userApplications = applicationRepository.findAll().stream()
                    .filter(app -> app.getApplicantName() != null && 
                            user.getUsername().equals(app.getApplicantName().getUsername()))
                    .collect(Collectors.toList());
            
            userData.put("applicationsCount", userApplications.size());
            userData.put("applications", userApplications.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList()));
            
            return ResponseEntity.ok(userData);
            
        } catch (Exception e) {
            logger.error("Error in debug user data", e);
            return ResponseEntity.status(500).body(createErrorResponse("DEBUG_ERROR", "Erreur de debug"));
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllApplications() {
        try {
            List<Application> applications = applicationServiceImpl.getAllApplications();
            List<Map<String, Object>> detailedApplications = applications.stream()
                .map(this::convertApplicationToDto)
                .collect(Collectors.toList());
            return ResponseEntity.ok(detailedApplications);
        } catch (Exception e) {
            logger.error("Error fetching all applications", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_AGENT', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getApplicationById(@PathVariable Long id) {
        try {
            logger.info("Fetching application with ID: {}", id);
            
            Optional<Application> applicationOpt = applicationRepository.findById(id);
            if (applicationOpt.isEmpty()) {
                logger.warn("Application not found with ID: {}", id);
                return ResponseEntity.status(404).body(createErrorResponse("APPLICATION_NOT_FOUND", "Candidature non trouvée"));
            }
            
            Application application = applicationOpt.get();
            logger.info("Found application with ID: {}, converting to DTO", id);
            
            Map<String, Object> dto = convertApplicationToDto(application);
            logger.info("Successfully converted application {} to DTO", id);
            
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Error fetching application by ID: {}", id, e);
            return ResponseEntity.status(500).body(createErrorResponse("INTERNAL_ERROR", "Erreur interne du serveur"));
        }
    }

    private User findOrCreateUser(String username) {
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new NoSuchElementException("Utilisateur non trouvé: " + username));
    }

    @SuppressWarnings("unchecked")
    private void updateUserFromFrontendData(User user, Map<String, Object> frontendData) {
        logger.info("Updating user from frontend data. Data keys: {}", frontendData.keySet());
        
        // Update personal info from frontend format
        Map<String, Object> personalInfo = (Map<String, Object>) frontendData.get("personalInfo");
        if (personalInfo != null) {
            logger.info("Processing personalInfo: {}", personalInfo.keySet());
            
            if (personalInfo.get("lastName") != null) {
                user.setLastName((String) personalInfo.get("lastName"));
                logger.info("Set lastName: {}", personalInfo.get("lastName"));
            }
            
            List<String> firstNames = (List<String>) personalInfo.get("firstNames");
            if (firstNames != null && !firstNames.isEmpty()) {
                user.setFirstName(String.join(" ", firstNames));
                logger.info("Set firstName: {}", String.join(" ", firstNames));
            }
            
            if (personalInfo.get("birthDate") != null) {
                try {
                    user.setDateOfBirth(LocalDate.parse((String) personalInfo.get("birthDate")));
                    logger.info("Set birthDate: {}", personalInfo.get("birthDate"));
                } catch (Exception e) {
                    logger.warn("Invalid birth date format: {}", personalInfo.get("birthDate"));
                }
            }
            
            if (personalInfo.get("nationality") != null) {
                user.setNationality((String) personalInfo.get("nationality"));
                logger.info("Set nationality: {}", personalInfo.get("nationality"));
            }
            
            if (personalInfo.get("gender") != null) {
                try {
                    user.setGender(Gender.valueOf(((String) personalInfo.get("gender")).toUpperCase()));
                    logger.info("Set gender: {}", personalInfo.get("gender"));
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid gender value: {}", personalInfo.get("gender"));
                }
            }
            
            if (personalInfo.get("idType") != null) {
                user.setUserIdNum((String) personalInfo.get("idType"));
                logger.info("Set userIdNum: {}", personalInfo.get("idType"));
            }
        } else {
            logger.warn("No personalInfo found in frontend data");
        }

        // Update contact info from frontend format
        Map<String, Object> contactInfo = (Map<String, Object>) frontendData.get("contactInfo");
        if (contactInfo != null) {
            logger.info("Processing contactInfo: {}", contactInfo.keySet());
            
            if (contactInfo.get("email") != null) {
                user.setEmail((String) contactInfo.get("email"));
                logger.info("Set email: {}", contactInfo.get("email"));
            }
            
            if (contactInfo.get("phone") != null) {
                user.setPhoneNumber((String) contactInfo.get("phone"));
                logger.info("Set phone: {}", contactInfo.get("phone"));
            }
            
            Map<String, Object> address = (Map<String, Object>) contactInfo.get("address");
            if (address != null) {
                String fullAddress = String.join(", ", 
                    (String) address.getOrDefault("street", ""),
                    (String) address.getOrDefault("city", ""),
                    (String) address.getOrDefault("postalCode", ""),
                    (String) address.getOrDefault("country", "")
                ).replaceAll(", ,", ",").replaceAll("^,|,$", "");
                user.setAddress(fullAddress);
                logger.info("Set address: {}", fullAddress);
            }
            
            Map<String, Object> emergencyContact = (Map<String, Object>) contactInfo.get("emergencyContact");
            if (emergencyContact != null) {
                String emergencyInfo = String.format("%s (%s) - %s", 
                    (String) emergencyContact.getOrDefault("name", ""),
                    (String) emergencyContact.getOrDefault("relationship", ""),
                    (String) emergencyContact.getOrDefault("phone", "")
                );
                user.setEmergencyContact(emergencyInfo);
                logger.info("Set emergencyContact: {}", emergencyInfo);
            }
        } else {
            logger.warn("No contactInfo found in frontend data");
        }

        // Update or create academic history from frontend format
        Map<String, Object> academicHistory = (Map<String, Object>) frontendData.get("academicHistory");
        if (academicHistory != null) {
            logger.info("Processing academicHistory: {}", academicHistory.keySet());
            
            AcademicHistory academic = user.getAcademicHistory();
            if (academic == null) {
                academic = new AcademicHistory();
                academic.setUser(user);
                logger.info("Created new AcademicHistory for user");
            }
            
            if (academicHistory.get("lastInstitution") != null) {
                academic.setLastInstitution((String) academicHistory.get("lastInstitution"));
                logger.info("Set lastInstitution: {}", academicHistory.get("lastInstitution"));
            }
            if (academicHistory.get("specialization") != null) {
                academic.setSpecialization((String) academicHistory.get("specialization"));
                logger.info("Set specialization: {}", academicHistory.get("specialization"));
            }
            if (academicHistory.get("subSpecialization") != null) {
                academic.setSubSpecialization((String) academicHistory.get("subSpecialization"));
                logger.info("Set subSpecialization: {}", academicHistory.get("subSpecialization"));
            }
            if (academicHistory.get("educationLevel") != null) {
                academic.setEducationLevel((String) academicHistory.get("educationLevel"));
                logger.info("Set educationLevel: {}", academicHistory.get("educationLevel"));
            }
            if (academicHistory.get("gpa") != null) {
                academic.setGpa(((Number) academicHistory.get("gpa")).doubleValue());
                logger.info("Set gpa: {}", academicHistory.get("gpa"));
            }
            
            List<String> honors = (List<String>) academicHistory.get("honors");
            if (honors != null && !honors.isEmpty()) {
                academic.setHonors(String.join(", ", honors));
                logger.info("Set honors: {}", String.join(", ", honors));
            }
            
            if (academicHistory.get("startDate") != null) {
                try {
                    academic.setStartDate(LocalDate.parse((String) academicHistory.get("startDate")));
                    logger.info("Set startDate: {}", academicHistory.get("startDate"));
                } catch (Exception e) {
                    logger.warn("Invalid start date format: {}", academicHistory.get("startDate"));
                }
            }
            
            if (academicHistory.get("endDate") != null) {
                try {
                    academic.setEndDate(LocalDate.parse((String) academicHistory.get("endDate")));
                    logger.info("Set endDate: {}", academicHistory.get("endDate"));
                } catch (Exception e) {
                    logger.warn("Invalid end date format: {}", academicHistory.get("endDate"));
                }
            }
            
            academic = academicHistoryRepository.save(academic);
            user.setAcademicHistory(academic);
            logger.info("Saved AcademicHistory with ID: {}", academic.getId());
        } else {
            logger.warn("No academicHistory found in frontend data");
        }
    }

    private Application createApplicationFromFrontendData(User user, Map<String, Object> frontendData, List<DocumentUploadRequestDTO> documents) {
        Application application = new Application();
        application.setApplicantName(user);
        application.setSubmissionDate(LocalDateTime.now());
        application.setLastUpdated(LocalDateTime.now());
        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        
        // Set target institution and specialization from frontend data
        String targetInstitution = "DEFAULT_INSTITUTION";
        String specialization = "DEFAULT_SPECIALIZATION";
        
        if (frontendData != null) {
            if (frontendData.get("targetInstitution") != null) {
                targetInstitution = (String) frontendData.get("targetInstitution");
            }
            if (frontendData.get("specialization") != null) {
                specialization = (String) frontendData.get("specialization");
            }
            
            // Fallback to academic history data
            @SuppressWarnings("unchecked")
            Map<String, Object> academicHistory = (Map<String, Object>) frontendData.get("academicHistory");
            if (academicHistory != null) {
                if ("DEFAULT_INSTITUTION".equals(targetInstitution) && academicHistory.get("lastInstitution") != null) {
                    targetInstitution = (String) academicHistory.get("lastInstitution");
                }
                if ("DEFAULT_SPECIALIZATION".equals(specialization) && academicHistory.get("specialization") != null) {
                    specialization = (String) academicHistory.get("specialization");
                }
            }
        }
        
        // Final fallback to user's academic history
        if (user.getAcademicHistory() != null) {
            if ("DEFAULT_INSTITUTION".equals(targetInstitution) && user.getAcademicHistory().getLastInstitution() != null) {
                targetInstitution = user.getAcademicHistory().getLastInstitution();
            }
            if ("DEFAULT_SPECIALIZATION".equals(specialization) && user.getAcademicHistory().getSpecialization() != null) {
                specialization = user.getAcademicHistory().getSpecialization();
            }
        }
        
        application.setTargetInstitution(targetInstitution);
        application.setSpecialization(specialization);
        
        // Calculate completion rate
        double completionRate = calculateCompletionRate(user, documents);
        application.setCompletionRate(completionRate);
        
        return applicationRepository.save(application);
    }

    private double calculateCompletionRate(User user, List<DocumentUploadRequestDTO> documents) {
        int totalFields = 12;
        int completedFields = 0;
        
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) completedFields++;
        if (user.getLastName() != null && !user.getLastName().isEmpty()) completedFields++;
        if (user.getEmail() != null && !user.getEmail().isEmpty()) completedFields++;
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) completedFields++;
        if (user.getAddress() != null && !user.getAddress().isEmpty()) completedFields++;
        if (user.getNationality() != null && !user.getNationality().isEmpty()) completedFields++;
        if (user.getDateOfBirth() != null) completedFields++;
        if (user.getGender() != null) completedFields++;
        if (user.getEmergencyContact() != null && !user.getEmergencyContact().isEmpty()) completedFields++;
        if (user.getUserIdNum() != null && !user.getUserIdNum().isEmpty()) completedFields++;
        
        if (user.getAcademicHistory() != null && 
            user.getAcademicHistory().getLastInstitution() != null && 
            !user.getAcademicHistory().getLastInstitution().isEmpty()) completedFields++;
        
        if (documents != null && !documents.isEmpty()) completedFields++;
        
        return ((double) completedFields / totalFields) * 100;
    }

    private String getCurrentUsername(String email) {
        if (email != null && !email.trim().isEmpty()) {
            return email;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentification requise");
        }
        
        return authentication.getName();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createErrorResponse(String code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("code", code);
        error.put("message", message);
        return error;
    }

    private ApplicationStatusResponseDto convertToDto(Application application) {
        ApplicationStatusResponseDto dto = new ApplicationStatusResponseDto();
        dto.setApplicationId(application.getId());
        dto.setStatus(application.getStatus().name());
        dto.setCompletionRate(application.getCompletionRate());
        dto.setSubmissionDate(application.getSubmissionDate());
        dto.setCreatedAt(application.getCreatedAt());

        User applicant = application.getApplicantName();
        if (applicant != null) {
            dto.setUserIdNum(applicant.getUserIdNum());
            dto.setUsername(applicant.getUsername());
            
            String firstName = applicant.getFirstName() != null ? applicant.getFirstName() : "";
            String lastName = applicant.getLastName() != null ? applicant.getLastName() : "";
            String fullName = (firstName + " " + lastName).trim();
            dto.setApplicantName(fullName.isEmpty() ? "N/A" : fullName);
        }

        return dto;
    }

    @GetMapping("/heatmap-data")
    @PreAuthorize("hasAuthority('ROLE_AGENT') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getHeatmapData() {
        try {
            List<Application> applications = applicationRepository.findAll();
            
            Map<String, Long> institutionCounts = applications.stream()
                .filter(app -> app.getTargetInstitution() != null)
                .collect(Collectors.groupingBy(
                    Application::getTargetInstitution,
                    Collectors.counting()
                ));
            
            List<Map<String, Object>> heatmapData = institutionCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("institution", entry.getKey());
                    data.put("count", entry.getValue());
                    return data;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(heatmapData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Méthode complète pour les agents - retourne TOUTES les données du candidat
    private Map<String, Object> convertApplicationToDto(Application application) {
        Map<String, Object> dto = new HashMap<>();
        
        try {
            // Informations de base de l'application
            dto.put("applicationId", application.getId());
            dto.put("status", application.getStatus() != null ? application.getStatus().name() : "UNKNOWN");
            dto.put("completionRate", application.getCompletionRate() != null ? application.getCompletionRate() : Double.valueOf(0.0));
            dto.put("submissionDate", application.getSubmissionDate());
            dto.put("lastUpdated", application.getLastUpdated());
            dto.put("targetInstitution", application.getTargetInstitution() != null ? application.getTargetInstitution() : "N/A");
            dto.put("specialization", application.getSpecialization() != null ? application.getSpecialization() : "N/A");
            
            User applicant = application.getApplicantName();
            if (applicant != null) {
                // Informations personnelles complètes
                Map<String, Object> personalInfo = new HashMap<>();
                personalInfo.put("firstName", applicant.getFirstName() != null ? applicant.getFirstName() : "");
                personalInfo.put("lastName", applicant.getLastName() != null ? applicant.getLastName() : "");
                personalInfo.put("email", applicant.getEmail() != null ? applicant.getEmail() : "");
                personalInfo.put("phoneNumber", applicant.getPhoneNumber() != null ? applicant.getPhoneNumber() : "");
                personalInfo.put("address", applicant.getAddress() != null ? applicant.getAddress() : "");
                personalInfo.put("nationality", applicant.getNationality() != null ? applicant.getNationality() : "");
                personalInfo.put("dateOfBirth", applicant.getDateOfBirth());
                personalInfo.put("gender", applicant.getGender() != null ? applicant.getGender().name() : null);
                personalInfo.put("userIdNum", applicant.getUserIdNum() != null ? applicant.getUserIdNum() : "");
                personalInfo.put("emergencyContact", applicant.getEmergencyContact() != null ? applicant.getEmergencyContact() : "");
                personalInfo.put("username", applicant.getUsername() != null ? applicant.getUsername() : "");
                dto.put("personalInfo", personalInfo);
                
                // Nom complet pour affichage
                String fullName = (personalInfo.get("firstName") + " " + personalInfo.get("lastName")).trim();
                dto.put("applicantName", fullName.isEmpty() ? "N/A" : fullName);
                
                // Historique académique complet
                if (applicant.getAcademicHistory() != null) {
                    Map<String, Object> academicInfo = new HashMap<>();
                    AcademicHistory academic = applicant.getAcademicHistory();
                    academicInfo.put("lastInstitution", academic.getLastInstitution() != null ? academic.getLastInstitution() : "");
                    academicInfo.put("specialization", academic.getSpecialization() != null ? academic.getSpecialization() : "");
                    academicInfo.put("subSpecialization", academic.getSubSpecialization() != null ? academic.getSubSpecialization() : "");
                    academicInfo.put("educationLevel", academic.getEducationLevel() != null ? academic.getEducationLevel() : "");
                    academicInfo.put("gpa", academic.getGpa() != null ? academic.getGpa() : Double.valueOf(0.0));
                    academicInfo.put("honors", academic.getHonors() != null ? academic.getHonors() : "");
                    academicInfo.put("startDate", academic.getStartDate());
                    academicInfo.put("endDate", academic.getEndDate());
                    dto.put("academicHistory", academicInfo);
                } else {
                    dto.put("academicHistory", new HashMap<>());
                }
            } else {
                dto.put("personalInfo", new HashMap<>());
                dto.put("academicHistory", new HashMap<>());
                dto.put("applicantName", "N/A");
            }
            
            // Documents soumis - utiliser le service pour éviter les problèmes de lazy loading
            try {
                List<com.groupe.gestin_inscription.model.Document> documents = documentService.getDocumentsByApplicationId(application.getId());
                List<Map<String, Object>> documentList = documents.stream()
                    .map(doc -> {
                        Map<String, Object> docInfo = new HashMap<>();
                        docInfo.put("id", doc.getId());
                        docInfo.put("name", doc.getName() != null ? doc.getName() : "Document");
                        docInfo.put("fileType", doc.getFileType() != null ? doc.getFileType() : "unknown");
                        docInfo.put("filePath", doc.getFilePath() != null ? doc.getFilePath() : "");
                        docInfo.put("fileSizeMB", doc.getFileSizeMB());
                        docInfo.put("validationStatus", doc.getValidationStatus() != null ? doc.getValidationStatus().name() : "PENDING");
                        return docInfo;
                    })
                    .collect(Collectors.toList());
                dto.put("documents", documentList);
                dto.put("documentCount", documentList.size());
            } catch (Exception e) {
                logger.warn("Could not load documents for application {}: {}", application.getId(), e.getMessage());
                dto.put("documents", new ArrayList<>());
                dto.put("documentCount", 0);
            }
            
            logger.debug("Successfully converted application {} to DTO with {} documents", 
                application.getId(), ((List<?>) dto.get("documents")).size());
            
        } catch (Exception e) {
            logger.error("Error converting application {} to DTO: {}", application.getId(), e.getMessage(), e);
            throw e;
        }
        
        return dto;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ROLE_AGENT') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            List<Application> applications = applicationRepository.findAll();
            List<User> users = userRepository.findAll();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", users.size());
            stats.put("totalApplications", applications.size());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("STATS_ERROR", "Erreur lors de la récupération des statistiques"));
        }
    }

    @PutMapping("/status/{applicationId}")
    @PreAuthorize("hasAnyAuthority('ROLE_AGENT', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Long applicationId, 
            @RequestParam String status) {
        try {
            Optional<Application> applicationOpt = applicationRepository.findById(applicationId);
            if (applicationOpt.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(createErrorResponse("APPLICATION_NOT_FOUND", "Candidature non trouvée"));
            }
            
            Application application = applicationOpt.get();
            ApplicationStatus newStatus = ApplicationStatus.valueOf(status.toUpperCase());
            application.setStatus(newStatus);
            application.setLastUpdated(LocalDateTime.now());
            
            applicationRepository.save(application);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Statut mis à jour avec succès");
            response.put("newStatus", newStatus.name());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("INVALID_STATUS", "Statut invalide: " + status));
        } catch (Exception e) {
            logger.error("Error updating application status", e);
            return ResponseEntity.status(500)
                .body(createErrorResponse("UPDATE_ERROR", "Erreur lors de la mise à jour du statut"));
        }
    }
}