package org.eci.ZwingBackend.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class GoogleUserData {
    private String googleId;
    private String email;
    private String name;
    private String avatarUrl;
}
