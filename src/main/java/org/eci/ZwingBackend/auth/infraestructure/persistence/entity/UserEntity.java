package org.eci.ZwingBackend.auth.infraestructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table (name = "Users")
@Data
public class UserEntity {
    @Id
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;
}
