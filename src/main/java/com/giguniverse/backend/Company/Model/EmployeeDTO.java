package com.giguniverse.backend.Company.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeDTO {
    private String id;
    private String name;
    private String email;
    private String role;
    private String avatar;
    private boolean isCreator;
}