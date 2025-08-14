package com.giguniverse.backend.Chat.Model;

import lombok.Data;

@Data
public class FileRequest {
    private String name;
    private long size;
    private String type;
    private String content; 
}