package com.groupe.gestin_inscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
}
