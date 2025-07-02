package com.giguniverse.backend.Auth.DTO;

import lombok.Data;

@Data
public class EmployerRegisterRequest {
    private String employerUserId;
    private String email;
    private String password;
}
