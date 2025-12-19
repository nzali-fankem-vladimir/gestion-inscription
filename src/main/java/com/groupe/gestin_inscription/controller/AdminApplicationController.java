package com.groupe.gestin_inscription.controller;

import com.groupe.gestin_inscription.dto.response.ApplicationStatusResponseDto;
import com.groupe.gestin_inscription.dto.response.DocumentResponseDTO;
import com.groupe.gestin_inscription.model.Application;
import com.groupe.gestin_inscription.model.Document;
import com.groupe.gestin_inscription.model.User;
import com.groupe.gestin_inscription.model.Enums.ApplicationStatus;
import com.groupe.gestin_inscription.services.serviceImpl.ApplicationServiceImpl;
import com.groupe.gestin_inscription.services.serviceImpl.DocumentServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/applications")
@Tag(name = "Admin Application Management", description = "Endpoints for administrators to manage applications and documents")
@PreAuthorize("hasRole('AGENT') or hasRole('SUPER_ADMIN')")
public class AdminApplicationController {

    @Autowired
    private ApplicationServiceImpl applicationService;
    
    @Autowired
    private DocumentServiceImpl documentService;

    @Operation(summary = "Get all applications with documents for review")
    @GetMapping("/review")
    public ResponseEntity<?> getApplicationsForReview() {
        try {
            List<Application> applications = applicationService.getApplicationsByStatus(ApplicationStatus.UNDER_REVIEW);
            applications.addAll(applicationService.getApplicationsByStatus(ApplicationStatus.MANUAL_REVIEW));
            
            List<Map<String, Object>> applicationsWithDocuments = applications.stream()
                .map(this::convertToDetailedDto)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applications", applicationsWithDocuments);
            response.put("count", applicationsWithDocuments.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Erreur lors de la récupération des candidatures");
            return ResponseEntity.status(500).body(error);
        }
    }

    @Operation(summary = "Get application details with documents")
    @GetMapping("/{applicationId}/details")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN') or (hasRole('CANDIDATE') and @applicationServiceImpl.isOwner(#applicationId, authentication.name))")
    public ResponseEntity<?> getApplicationDetails(@PathVariable Long applicationId) {
        try {
            Application application = applicationService.getApplicationById(applicationId);
            List<Document> documents = documentService.getDocumentsByApplicationId(applicationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("application", convertToDetailedDto(application));
            response.put("documents", documents.stream().map(this::convertDocumentToDto).collect(Collectors.toList()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Candidature non trouvée");
            return ResponseEntity.status(404).body(error);
        }
    }

    @Operation(summary = "Get applications dashboard statistics")
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        try {
            List<Application> allApplications = applicationService.getAllApplications();
            
            long pending = allApplications.stream().filter(a -> a.getStatus() == ApplicationStatus.PENDING).count();
            long underReview = allApplications.stream().filter(a -> a.getStatus() == ApplicationStatus.UNDER_REVIEW).count();
            long manualReview = allApplications.stream().filter(a -> a.getStatus() == ApplicationStatus.MANUAL_REVIEW).count();
            long approved = allApplications.stream().filter(a -> a.getStatus() == ApplicationStatus.APPROVED).count();
            long rejected = allApplications.stream().filter(a -> a.getStatus() == ApplicationStatus.REJECTED).count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", allApplications.size());
            stats.put("pending", pending);
            stats.put("underReview", underReview);
            stats.put("manualReview", manualReview);
            stats.put("approved", approved);
            stats.put("rejected", rejected);
            stats.put("needsAttention", underReview + manualReview);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Erreur lors du calcul des statistiques");
            return ResponseEntity.status(500).body(error);
        }
    }

    private Map<String, Object> convertToDetailedDto(Application application) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", application.getId());
        dto.put("status", application.getStatus() != null ? application.getStatus().name() : "UNKNOWN");
        dto.put("submissionDate", application.getSubmissionDate());
        dto.put("lastUpdated", application.getLastUpdated());
        dto.put("completionRate", application.getCompletionRate());
        dto.put("targetInstitution", application.getTargetInstitution());
        dto.put("specialization", application.getSpecialization());
        
        // Safe user extraction without circular references
        User applicant = application.getApplicantName();
        if (applicant != null) {
            Map<String, Object> candidateInfo = new HashMap<>();
            candidateInfo.put("id", applicant.getId());
            candidateInfo.put("firstName", applicant.getFirstName());
            candidateInfo.put("lastName", applicant.getLastName());
            candidateInfo.put("email", applicant.getEmail());
            candidateInfo.put("phone", applicant.getPhoneNumber());
            candidateInfo.put("nationality", applicant.getNationality());
            candidateInfo.put("address", applicant.getAddress());
            dto.put("candidate", candidateInfo);
        }
        
        // Safe documents summary without loading full entities
        try {
            List<Document> documents = documentService.getDocumentsByApplicationId(application.getId());
            Map<String, Object> docsSummary = new HashMap<>();
            docsSummary.put("total", documents.size());
            docsSummary.put("validated", documents.stream().filter(d -> "VALIDATED".equals(d.getValidationStatus().name())).count());
            docsSummary.put("rejected", documents.stream().filter(d -> "REJECTED".equals(d.getValidationStatus().name())).count());
            docsSummary.put("pending", documents.stream().filter(d -> "PENDING".equals(d.getValidationStatus().name())).count());
            dto.put("documentsSummary", docsSummary);
        } catch (Exception e) {
            // Fallback if documents can't be loaded
            Map<String, Object> docsSummary = new HashMap<>();
            docsSummary.put("total", 0);
            docsSummary.put("validated", 0);
            docsSummary.put("rejected", 0);
            docsSummary.put("pending", 0);
            dto.put("documentsSummary", docsSummary);
        }
        
        return dto;
    }
    
    private Map<String, Object> convertDocumentToDto(Document document) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", document.getId());
        dto.put("name", document.getName());
        dto.put("fileType", document.getFileType());
        dto.put("validationStatus", document.getValidationStatus() != null ? document.getValidationStatus().name() : "PENDING");
        dto.put("ocrNotes", document.getOcrNotes());
        dto.put("fileSizeMB", document.getFileSizeMB());
        // Don't include application reference to avoid circular dependency
        return dto;
    }
}