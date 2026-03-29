package org.eci.ZwingBackend.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class User {
    private UUID userId;
    private String name;
    private String email;
}
