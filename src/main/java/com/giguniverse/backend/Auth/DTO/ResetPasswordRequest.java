package com.giguniverse.backend.Auth.DTO;

import lombok.Data;

@Data
public class ResetPasswordRequest { 
    public String email;
    public String code;
    public String newPassword; 
}