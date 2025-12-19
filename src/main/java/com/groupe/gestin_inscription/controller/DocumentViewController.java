package com.groupe.gestin_inscription.controller;

import com.groupe.gestin_inscription.model.Document;
import com.groupe.gestin_inscription.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentViewController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentViewController.class);

    @Autowired
    private DocumentRepository documentRepository;

    @GetMapping("/{documentId}/preview")
    @PreAuthorize("hasAnyAuthority('ROLE_AGENT', 'ROLE_SUPER_ADMIN', 'ROLE_CANDIDATE')")
    public ResponseEntity<?> previewDocument(@PathVariable Long documentId) {
        try {
            logger.info("Preview request for document ID: {}", documentId);
            
            Optional<Document> docOpt = documentRepository.findById(documentId);
            
            if (docOpt.isEmpty()) {
                logger.warn("Document not found with ID: {}", documentId);
                return ResponseEntity.notFound().build();
            }
            
            Document document = docOpt.get();
            logger.info("Found document: {} at path: {}", document.getName(), document.getFilePath());
            logger.info("Document details - ID: {}, Name: {}, Type: {}, Size: {} MB, Hash: {}, Application ID: {}", 
                document.getId(), document.getName(), document.getFileType(), 
                document.getFileSizeMB(), document.getHash(), 
                document.getApplication() != null ? document.getApplication().getId() : "null");
            
            if (document.getFilePath() == null || document.getFilePath().trim().isEmpty()) {
                logger.error("Document {} has no file path", documentId);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Chemin de fichier manquant");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
            
            Path filePath = Paths.get(document.getFilePath());
            
            if (!Files.exists(filePath)) {
                logger.error("File does not exist at path: {}", document.getFilePath());
                Map<String, String> error = new HashMap<>();
                error.put("error", "Fichier non trouvé sur le disque");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            byte[] fileContent = Files.readAllBytes(filePath);
            logger.info("Successfully read {} bytes from file", fileContent.length);
            
            // Vérifier l'intégrité du fichier avec le hash stocké
            if (document.getHash() != null && !document.getHash().isEmpty()) {
                String currentHash = calculateFileHash(fileContent);
                if (!document.getHash().equals(currentHash)) {
                    logger.warn("File integrity check failed! Stored hash: {}, Current hash: {}", 
                        document.getHash(), currentHash);
                } else {
                    logger.info("File integrity verified - hash matches: {}", currentHash);
                }
            }
            
            HttpHeaders headers = new HttpHeaders();
            
            String contentType = document.getFileType();
            if (contentType == null || contentType.trim().isEmpty()) {
                // Détecter le type à partir de l'extension du fichier
                contentType = detectContentTypeFromFilename(document.getFilePath());
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                logger.info("Detected content type from file path: {}", contentType);
            }
            
            try {
                headers.setContentType(MediaType.parseMediaType(contentType));
            } catch (Exception e) {
                logger.warn("Invalid content type: {}, detecting from filename", contentType);
                String detectedType = detectContentTypeFromFilename(document.getName());
                if (detectedType != null) {
                    headers.setContentType(MediaType.parseMediaType(detectedType));
                } else {
                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                }
            }
            
            String filename = getProperFilename(document);
            headers.add("Content-Disposition", "inline; filename=\"" + filename + "\"");
            headers.setContentLength(fileContent.length);
            
            return ResponseEntity.ok().headers(headers).body(fileContent);
            
        } catch (Exception e) {
            logger.error("Error previewing document {}: {}", documentId, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur lors de la prévisualisation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    private String getProperFilename(Document document) {
        String name = document.getName();
        
        // Si le nom stocké est un type MIME ou invalide, utiliser le chemin du fichier
        if (name == null || name.trim().isEmpty() || name.contains("/") || name.startsWith("image/") || name.startsWith("application/")) {
            // Extraire le nom du fichier depuis le chemin
            if (document.getFilePath() != null) {
                String filePath = document.getFilePath();
                String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
                fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
                
                // Si le fichier a une extension, l'utiliser
                if (fileName.contains(".")) {
                    return "document_" + document.getId() + fileName.substring(fileName.lastIndexOf("."));
                }
            }
            
            // Fallback: générer un nom avec l'extension détectée
            name = "document_" + document.getId();
        }
        
        // Vérifier si le nom a déjà une extension
        if (name.contains(".")) {
            return name;
        }
        
        // Ajouter l'extension basée sur le type de fichier ou le chemin
        String extension = null;
        
        // Essayer d'abord avec le type MIME
        String fileType = document.getFileType();
        if (fileType != null && !fileType.trim().isEmpty()) {
            extension = getExtensionFromMimeType(fileType);
        }
        
        // Si pas d'extension trouvée, détecter depuis le chemin
        if (extension == null && document.getFilePath() != null) {
            String detectedType = detectContentTypeFromFilename(document.getFilePath());
            if (detectedType != null) {
                extension = getExtensionFromMimeType(detectedType);
            }
        }
        
        if (extension != null) {
            return name + "." + extension;
        }
        
        return name;
    }
    
    private String getExtensionFromMimeType(String mimeType) {
        switch (mimeType.toLowerCase()) {
            case "application/pdf":
                return "pdf";
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "application/msword":
                return "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return "docx";
            case "text/plain":
                return "txt";
            case "application/vnd.ms-excel":
                return "xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                return "xlsx";
            default:
                return null;
        }
    }

    @GetMapping("/{documentId}/download")
    @PreAuthorize("hasAnyAuthority('ROLE_AGENT', 'ROLE_SUPER_ADMIN', 'ROLE_CANDIDATE')")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long documentId) {
        try {
            Optional<Document> docOpt = documentRepository.findById(documentId);
            
            if (docOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Document document = docOpt.get();
            Path filePath = Paths.get(document.getFilePath());
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] fileContent = Files.readAllBytes(filePath);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String filename = getProperFilename(document);
            headers.add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            
            return ResponseEntity.ok().headers(headers).body(fileContent);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private String calculateFileHash(byte[] fileContent) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(fileContent);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("Error calculating file hash", e);
            return null;
        }
    }
    
    private String detectContentTypeFromFilename(String filename) {
        if (filename == null) return null;
        
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".pdf")) return "application/pdf";
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".jfif")) return "image/jpeg";
        if (lowerName.endsWith(".png")) return "image/png";
        if (lowerName.endsWith(".gif")) return "image/gif";
        if (lowerName.endsWith(".webp")) return "image/webp";
        if (lowerName.endsWith(".bmp")) return "image/bmp";
        if (lowerName.endsWith(".doc")) return "application/msword";
        if (lowerName.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lowerName.endsWith(".txt")) return "text/plain";
        if (lowerName.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lowerName.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        
        return null;
    }
}