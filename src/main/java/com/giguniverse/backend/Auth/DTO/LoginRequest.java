package com.giguniverse.backend.Auth.DTO;

import lombok.Data;


@Data
public class LoginRequest {
    String email;
    String password;
}
