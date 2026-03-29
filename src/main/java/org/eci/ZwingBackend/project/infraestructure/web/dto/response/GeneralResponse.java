package org.eci.ZwingBackend.project.infraestructure.web.dto.response;

import lombok.Data;

@Data
public class GeneralResponse <T>{
    private String status;
    private String message;
    private T data;

    public GeneralResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static <T> GeneralResponse<T> success(T data, String message) {
        return new GeneralResponse<>("Success", message, data);
    }

    public static <T> GeneralResponse<T> error(String message) {
        return new GeneralResponse<>("Error", message, null);
    }

}