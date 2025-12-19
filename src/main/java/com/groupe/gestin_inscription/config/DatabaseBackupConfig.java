package com.groupe.gestin_inscription.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Configuration pour les backups automatiques de la base de données PostgreSQL
 * Exécute un backup quotidien à 2h du matin
 */
@Configuration
@EnableScheduling
@Profile("prod")
public class DatabaseBackupConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupConfig.class);

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.backup.directory:/var/backups/sigec}")
    private String backupDirectory;

    @Value("${app.backup.retention-days:30}")
    private int retentionDays;

    /**
     * Backup automatique quotidien à 2h du matin
     * Cron: 0 0 2 * * * = Tous les jours à 2h00
     */
    @Scheduled(cron = "${app.backup.cron:0 0 2 * * *}")
    public void performDatabaseBackup() {
        logger.info("Starting automatic database backup...");
        
        try {
            // Créer le répertoire de backup s'il n'existe pas
            File backupDir = new File(backupDirectory);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            // Extraire le nom de la base de données de l'URL
            String dbName = extractDatabaseName(dbUrl);
            
            // Générer le nom du fichier de backup avec timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String backupFileName = String.format("%s/backup_%s_%s.sql", 
                backupDirectory, dbName, timestamp);

            // Construire la commande pg_dump
            ProcessBuilder processBuilder = new ProcessBuilder(
                "pg_dump",
                "-h", extractHost(dbUrl),
                "-p", extractPort(dbUrl),
                "-U", dbUsername,
                "-F", "c",  // Format custom (compressé)
                "-b",       // Include blobs
                "-v",       // Verbose
                "-f", backupFileName,
                dbName
            );

            // Définir le mot de passe via variable d'environnement
            processBuilder.environment().put("PGPASSWORD", dbPassword);
            
            // Exécuter la commande
            Process process = processBuilder.start();
            
            // Lire la sortie
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("pg_dump: {}", line);
            }

            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                logger.info("Database backup completed successfully: {}", backupFileName);
                
                // Nettoyer les anciens backups
                cleanOldBackups();
            } else {
                logger.error("Database backup failed with exit code: {}", exitCode);
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Error during database backup", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Nettoie les backups plus anciens que le nombre de jours de rétention
     */
    private void cleanOldBackups() {
        File backupDir = new File(backupDirectory);
        File[] files = backupDir.listFiles((dir, name) -> name.startsWith("backup_") && name.endsWith(".sql"));
        
        if (files != null) {
            long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60L * 60L * 1000L);
            
            for (File file : files) {
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        logger.info("Deleted old backup: {}", file.getName());
                    } else {
                        logger.warn("Failed to delete old backup: {}", file.getName());
                    }
                }
            }
        }
    }

    /**
     * Extrait le nom de la base de données de l'URL JDBC
     */
    private String extractDatabaseName(String url) {
        // Format: jdbc:postgresql://host:port/database
        String[] parts = url.split("/");
        String dbNameWithParams = parts[parts.length - 1];
        return dbNameWithParams.split("\\?")[0];
    }

    /**
     * Extrait le host de l'URL JDBC
     */
    private String extractHost(String url) {
        // Format: jdbc:postgresql://host:port/database
        String withoutProtocol = url.substring(url.indexOf("//") + 2);
        String hostPort = withoutProtocol.split("/")[0];
        return hostPort.split(":")[0];
    }

    /**
     * Extrait le port de l'URL JDBC
     */
    private String extractPort(String url) {
        // Format: jdbc:postgresql://host:port/database
        String withoutProtocol = url.substring(url.indexOf("//") + 2);
        String hostPort = withoutProtocol.split("/")[0];
        String[] parts = hostPort.split(":");
        return parts.length > 1 ? parts[1] : "5432";
    }
}
