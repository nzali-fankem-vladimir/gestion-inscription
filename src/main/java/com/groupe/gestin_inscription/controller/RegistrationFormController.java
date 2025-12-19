package com.groupe.gestin_inscription.controller;

import com.groupe.gestin_inscription.model.Application;
import com.groupe.gestin_inscription.model.User;
import com.groupe.gestin_inscription.repository.ApplicationRepository;
import com.groupe.gestin_inscription.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/registration-form")
public class RegistrationFormController {

    @Autowired
    private ApplicationRepository applicationRepository;
    
    @Autowired
    private DocumentRepository documentRepository;

    @PutMapping("/status/{applicationId}")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Long applicationId, 
            @RequestParam String status,
            @RequestParam(required = false) String comment) {
        try {
            Optional<Application> appOpt = applicationRepository.findById(applicationId);
            
            if (appOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Application application = appOpt.get();
            
            // Mettre à jour le statut
            com.groupe.gestin_inscription.model.Enums.ApplicationStatus newStatus = 
                com.groupe.gestin_inscription.model.Enums.ApplicationStatus.valueOf(status.toUpperCase());
            
            application.setStatus(newStatus);
            application.setLastUpdated(java.time.LocalDateTime.now());
            applicationRepository.save(application);
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Statut mis à jour avec succès");
            response.put("newStatus", newStatus.name());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            java.util.Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Erreur lors de la mise à jour du statut");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/status/{applicationId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CANDIDATE', 'ROLE_AGENT', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> getFormStatus(@PathVariable Long applicationId) {
        try {
            Optional<Application> appOpt = applicationRepository.findById(applicationId);
            
            if (appOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Application application = appOpt.get();
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("applicationId", applicationId);
            response.put("status", application.getStatus().name());
            response.put("formAvailable", 
                application.getStatus() == com.groupe.gestin_inscription.model.Enums.ApplicationStatus.APPROVED ||
                application.getStatus() == com.groupe.gestin_inscription.model.Enums.ApplicationStatus.AGENT_VALIDATED);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            java.util.Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Erreur lors de la vérification du statut");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{applicationId}/generate")
    @PreAuthorize("hasAnyAuthority('ROLE_AGENT', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<byte[]> generateRegistrationForm(@PathVariable Long applicationId) {
        try {
            Optional<Application> appOpt = applicationRepository.findById(applicationId);
            
            if (appOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Application application = appOpt.get();
            User user = application.getApplicantName();
            
            if (user == null) {
                return ResponseEntity.badRequest().build();
            }
            
            // Générer le PDF de la fiche d'inscription
            byte[] pdfContent = generatePDF(application, user);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(pdfContent.length);
            headers.add("Content-Disposition", "attachment; filename=\"fiche_inscription_" + applicationId + ".pdf\"");
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            
            return ResponseEntity.ok().headers(headers).body(pdfContent);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private byte[] generatePDF(Application application, User user) throws IOException, DocumentException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            com.itextpdf.text.Document document = new com.itextpdf.text.Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Polices
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
            
            // Titre
            Paragraph title = new Paragraph("FICHE D'INSCRIPTION", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);
            
            Paragraph subtitle = new Paragraph("SIGEC - Système Intégré de Gestion des Candidatures", headerFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);
            
            // Informations personnelles
            Paragraph personalHeader = new Paragraph("INFORMATIONS PERSONNELLES", headerFont);
            personalHeader.setSpacingAfter(10);
            document.add(personalHeader);
            
            document.add(new Paragraph("Nom : " + (user.getLastName() != null ? user.getLastName() : "N/A"), normalFont));
            document.add(new Paragraph("Prénom(s) : " + (user.getFirstName() != null ? user.getFirstName() : "N/A"), normalFont));
            document.add(new Paragraph("Email : " + (user.getEmail() != null ? user.getEmail() : "N/A"), normalFont));
            document.add(new Paragraph("Téléphone : " + (user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A"), normalFont));
            document.add(new Paragraph("Nationalité : " + (user.getNationality() != null ? user.getNationality() : "N/A"), normalFont));
            
            if (user.getDateOfBirth() != null) {
                document.add(new Paragraph("Date de naissance : " + user.getDateOfBirth().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalFont));
            }
            
            document.add(new Paragraph(" ", normalFont)); // Espace
            
            // Informations candidature
            Paragraph candidatureHeader = new Paragraph("CANDIDATURE", headerFont);
            candidatureHeader.setSpacingAfter(10);
            document.add(candidatureHeader);
            
            document.add(new Paragraph("Numéro de candidature : " + application.getId(), normalFont));
            document.add(new Paragraph("Institution cible : " + (application.getTargetInstitution() != null ? application.getTargetInstitution() : "N/A"), normalFont));
            document.add(new Paragraph("Spécialisation : " + (application.getSpecialization() != null ? application.getSpecialization() : "N/A"), normalFont));
            document.add(new Paragraph("Statut : " + (application.getStatus() != null ? application.getStatus().name() : "N/A"), normalFont));
            
            if (application.getSubmissionDate() != null) {
                document.add(new Paragraph("Date de soumission : " + application.getSubmissionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normalFont));
            }
            
            // Historique académique
            if (user.getAcademicHistory() != null) {
                document.add(new Paragraph(" ", normalFont)); // Espace
                
                Paragraph academicHeader = new Paragraph("PARCOURS ACADÉMIQUE", headerFont);
                academicHeader.setSpacingAfter(10);
                document.add(academicHeader);
                
                document.add(new Paragraph("Dernier établissement : " + (user.getAcademicHistory().getLastInstitution() != null ? user.getAcademicHistory().getLastInstitution() : "N/A"), normalFont));
                document.add(new Paragraph("Spécialisation : " + (user.getAcademicHistory().getSpecialization() != null ? user.getAcademicHistory().getSpecialization() : "N/A"), normalFont));
                document.add(new Paragraph("Niveau d'éducation : " + (user.getAcademicHistory().getEducationLevel() != null ? user.getAcademicHistory().getEducationLevel() : "N/A"), normalFont));
                
                if (user.getAcademicHistory().getGpa() != null) {
                    document.add(new Paragraph("Moyenne générale : " + user.getAcademicHistory().getGpa(), normalFont));
                }
            }
            
            // Pied de page
            document.add(new Paragraph(" ", normalFont)); // Espace
            document.add(new Paragraph(" ", normalFont)); // Espace
            
            Paragraph footer = new Paragraph("Document généré automatiquement le " + 
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")), 
                new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            
            document.close();
            return baos.toByteArray();
            
        } catch (DocumentException e) {
            throw new IOException("Erreur lors de la génération du PDF", e);
        }
    }

    private String generateHTMLContent(Application application, User user) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Fiche d'Inscription</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append(".header { text-align: center; border-bottom: 2px solid #000; padding-bottom: 10px; }");
        html.append(".section { margin: 20px 0; }");
        html.append(".field { margin: 5px 0; }");
        html.append(".label { font-weight: bold; display: inline-block; width: 200px; }");
        html.append(".photo { float: right; width: 120px; height: 160px; border: 1px solid #000; }");
        html.append("</style>");
        html.append("</head><body>");
        
        // En-tête
        html.append("<div class='header'>");
        html.append("<h1>FICHE D'INSCRIPTION</h1>");
        html.append("<h2>SYSTÈME INTÉGRÉ DE GESTION DES CANDIDATURES</h2>");
        html.append("</div>");
        
        // Photo d'identité
        String photoPath = getIdentityPhotoPath(application);
        if (photoPath != null) {
            html.append("<div class='photo'>");
            html.append("<img src='data:image/jpeg;base64,").append(encodeImageToBase64(photoPath)).append("' ");
            html.append("style='width: 100%; height: 100%; object-fit: cover;' alt='Photo d\\'identité' />");
            html.append("</div>");
        }
        
        // Informations personnelles
        html.append("<div class='section'>");
        html.append("<h3>INFORMATIONS PERSONNELLES</h3>");
        html.append("<div class='field'><span class='label'>Nom :</span>").append(user.getLastName() != null ? user.getLastName() : "N/A").append("</div>");
        html.append("<div class='field'><span class='label'>Prénom(s) :</span>").append(user.getFirstName() != null ? user.getFirstName() : "N/A").append("</div>");
        html.append("<div class='field'><span class='label'>Sexe :</span>").append(user.getGender() != null ? user.getGender() : "N/A").append("</div>");
        html.append("<div class='field'><span class='label'>Date de naissance :</span>").append(user.getDateOfBirth() != null ? user.getDateOfBirth().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A").append("</div>");
        html.append("<div class='field'><span class='label'>Nationalité :</span>").append(user.getNationality() != null ? user.getNationality() : "N/A").append("</div>");
        html.append("<div class='field'><span class='label'>Numéro d'identité :</span>").append(user.getUserIdNum() != null ? user.getUserIdNum() : "N/A").append("</div>");
        html.append("</div>");
        
        // Coordonnées
        html.append("<div class='section'>");
        html.append("<h3>COORDONNÉES</h3>");
        html.append("<div class='field'><span class='label'>Email :</span>").append(user.getEmail() != null ? user.getEmail() : "N/A").append("</div>");
        html.append("<div class='field'><span class='label'>Téléphone :</span>").append(user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A").append("</div>");
        html.append("<div class='field'><span class='label'>Adresse :</span>").append(user.getAddress() != null ? user.getAddress() : "N/A").append("</div>");
        html.append("<div class='field'><span class='label'>Contact d'urgence :</span>").append(user.getEmergencyContact() != null ? user.getEmergencyContact() : "N/A").append("</div>");
        html.append("</div>");
        
        // Parcours académique
        if (user.getAcademicHistory() != null) {
            html.append("<div class='section'>");
            html.append("<h3>PARCOURS ACADÉMIQUE</h3>");
            html.append("<div class='field'><span class='label'>Dernier établissement :</span>").append(user.getAcademicHistory().getLastInstitution() != null ? user.getAcademicHistory().getLastInstitution() : "N/A").append("</div>");
            html.append("<div class='field'><span class='label'>Spécialisation :</span>").append(user.getAcademicHistory().getSpecialization() != null ? user.getAcademicHistory().getSpecialization() : "N/A").append("</div>");
            html.append("<div class='field'><span class='label'>Sous-spécialisation :</span>").append(user.getAcademicHistory().getSubSpecialization() != null ? user.getAcademicHistory().getSubSpecialization() : "N/A").append("</div>");
            html.append("<div class='field'><span class='label'>Niveau d'éducation :</span>").append(user.getAcademicHistory().getEducationLevel() != null ? user.getAcademicHistory().getEducationLevel() : "N/A").append("</div>");
            html.append("<div class='field'><span class='label'>Moyenne générale :</span>").append(user.getAcademicHistory().getGpa() != null ? user.getAcademicHistory().getGpa().toString() : "N/A").append("</div>");
            html.append("<div class='field'><span class='label'>Mentions :</span>").append(user.getAcademicHistory().getHonors() != null ? user.getAcademicHistory().getHonors() : "N/A").append("</div>");
            html.append("</div>");
        }
        
        // Informations candidature
        html.append("<div class='section'>");
        html.append("<h3>CANDIDATURE</h3>");
        html.append("<div class='field'><span class='label'>Numéro de candidature :</span>").append(application.getId()).append("</div>");
        html.append("<div class='field'><span class='label'>Institution cible :</span>").append(application.getTargetInstitution() != null ? application.getTargetInstitution() : "N/A").append("</div>");
        html.append("<div class='field'><span class='label'>Spécialisation demandée :</span>").append(application.getSpecialization() != null ? application.getSpecialization() : "N/A").append("</div>");
        html.append("<div class='field'><span class='label'>Date de soumission :</span>").append(application.getSubmissionDate() != null ? application.getSubmissionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A").append("</div>");
        html.append("<div class='field'><span class='label'>Statut :</span>").append(application.getStatus() != null ? application.getStatus().name() : "N/A").append("</div>");
        html.append("</div>");
        
        // Pied de page
        html.append("<div class='section' style='margin-top: 50px; text-align: center; font-size: 12px;'>");
        html.append("<p>Document généré automatiquement le ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))).append("</p>");
        html.append("<p>SIGEC - Système Intégré de Gestion des Candidatures</p>");
        html.append("</div>");
        
        html.append("</body></html>");
        
        return html.toString();
    }

    private String getIdentityPhotoPath(Application application) {
        try {
            List<com.groupe.gestin_inscription.model.Document> documents = documentRepository.findByApplicationId(application.getId());
            for (com.groupe.gestin_inscription.model.Document doc : documents) {
                if (doc.getName() != null && doc.getName().contains("IDENTITY_PHOTO")) {
                    return doc.getFilePath();
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de la photo: " + e.getMessage());
        }
        return null;
    }

    private String encodeImageToBase64(String imagePath) {
        try {
            Path path = Paths.get(imagePath);
            if (Files.exists(path)) {
                byte[] imageBytes = Files.readAllBytes(path);
                return java.util.Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'encodage de l'image: " + e.getMessage());
        }
        return "";
    }
}