package org.eci.ZwingBackend.project.infraestructure.web.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateProjectRequest {
    private String name;
}