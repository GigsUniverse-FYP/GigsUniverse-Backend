package com.giguniverse.backend.Chat.Model;

import java.util.List;

import lombok.Data;

@Data
public class MessageRequest {
    private String textContent;
    private List<FileRequest> files;
}