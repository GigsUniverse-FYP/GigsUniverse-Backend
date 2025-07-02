package com.giguniverse.backend.Auth.DTO;

import lombok.Data;

@Data
public class FreelancerRegisterRequest {
    private String freelancerUserId;
    private String email;
    private String password;
}
