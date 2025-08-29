package com.giguniverse.backend.Company.Model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvailableUserDTO {
    private String id;
    private String name;
    private String username;
    private String role;
    private String avatar; 
}