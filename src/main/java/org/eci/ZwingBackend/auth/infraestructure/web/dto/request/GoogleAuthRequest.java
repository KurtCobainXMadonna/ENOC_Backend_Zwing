package org.eci.ZwingBackend.auth.infraestructure.web.dto.request;

import lombok.Data;

@Data
public class GoogleAuthRequest {
    private String idToken;
}