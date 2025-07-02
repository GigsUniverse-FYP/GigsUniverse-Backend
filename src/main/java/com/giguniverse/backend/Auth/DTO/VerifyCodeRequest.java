package com.giguniverse.backend.Auth.DTO;
import lombok.Data;

@Data
public class VerifyCodeRequest { 
    public String email;
    public String code; 
}