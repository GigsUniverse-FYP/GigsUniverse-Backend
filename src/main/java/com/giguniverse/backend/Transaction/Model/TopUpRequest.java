package com.giguniverse.backend.Transaction.Model;

import lombok.Data;

@Data
public class TopUpRequest {
    private Long amount; 
    private String method; 
}
