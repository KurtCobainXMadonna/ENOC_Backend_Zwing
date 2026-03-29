package org.eci.ZwingBackend.auth.infraestructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    @JsonIgnore
    private String token;
    private String name;
    private String email;
    private boolean isNewUser;
}