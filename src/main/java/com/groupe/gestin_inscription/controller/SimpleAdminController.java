package com.groupe.gestin_inscription.controller;

import com.groupe.gestin_inscription.repository.ApplicationRepository;
import com.groupe.gestin_inscription.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simple-admin")
@PreAuthorize("hasRole('AGENT') or hasRole('SUPER_ADMIN')")
public class SimpleAdminController {

    @Autowired
    private com.groupe.gestin_inscription.repository.AdministratorRepository administratorRepository;

    @GetMapping("/agents")
    public ResponseEntity<?> getAgents() {
        try {
            List<com.groupe.gestin_inscription.model.Administrator> agents = administratorRepository.findByRole(com.groupe.gestin_inscription.model.Enums.AdministratorRole.AGENT);
            List<com.groupe.gestin_inscription.dto.AgentDTO> agentDTOs = agents.stream()
                .map(admin -> com.groupe.gestin_inscription.dto.AgentDTO.builder()
                    .id(admin.getId())
                    .firstName(admin.getFirstName())
                    .lastName(admin.getLastName())
                    .email(admin.getEmail())
                    .role(admin.getRole() != null ? admin.getRole().name() : null)
                    .build())
                .toList();
            return ResponseEntity.ok(agentDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Erreur lors de la récupération des agents");
            error.put("details", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @Autowired
    private ApplicationRepository applicationRepository;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/applications")
    public ResponseEntity<?> getApplicationsSimple() {
        try {
            // Requête SQL simple sans jointures complexes
            List<Object[]> results = applicationRepository.findApplicationsWithUserInfo();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applications", results);
            response.put("count", results.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Erreur lors de la récupération");
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStatsSimple() {
        try {
            long totalApplications = applicationRepository.count();
            long totalUsers = userRepository.count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalApplications", totalApplications);
            stats.put("totalUsers", totalUsers);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Erreur lors du calcul des statistiques");
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/heatmap")
    public ResponseEntity<?> getHeatmapData() {
        try {
            // Données simulées pour la carte thermique des universités camerounaises
            List<Map<String, Object>> heatmapData = new ArrayList<>();
            
            // Universités principales du Cameroun avec coordonnées approximatives
            heatmapData.add(createHeatmapPoint("Université de Yaoundé I", 3.848, 11.502, 15));
            heatmapData.add(createHeatmapPoint("Université de Douala", 4.048, 9.767, 12));
            heatmapData.add(createHeatmapPoint("Université de Buea", 4.154, 9.241, 8));
            heatmapData.add(createHeatmapPoint("Université de Ngaoundéré", 7.322, 13.584, 6));
            heatmapData.add(createHeatmapPoint("Université de Bamenda", 5.959, 10.149, 5));
            heatmapData.add(createHeatmapPoint("Université de Maroua", 10.591, 14.315, 4));
            
            long totalApplications = applicationRepository.count();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", heatmapData);
            response.put("totalApplications", totalApplications);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Erreur lors du chargement de la carte thermique");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    private Map<String, Object> createHeatmapPoint(String name, double lat, double lng, int count) {
        Map<String, Object> point = new HashMap<>();
        point.put("name", name);
        point.put("lat", lat);
        point.put("lng", lng);
        point.put("count", count);
        point.put("intensity", Math.min(1.0, count / 15.0)); // Normaliser l'intensité
        return point;
    }
}