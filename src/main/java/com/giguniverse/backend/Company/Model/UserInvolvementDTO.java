package com.giguniverse.backend.Company.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInvolvementDTO {
    private String userId;
    private boolean involved;
    private Integer companyId;
}
