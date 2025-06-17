package com.giguniverse.backend.Connection;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/check")
public class MongoConnectionCheck {

    private final MongoTemplate mongoTemplate;

    public MongoConnectionCheck(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/mongo")
    public String checkMongo() {
        try {
            mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            return "MongoDB connection is OK";
        } catch (Exception e) {
            return "MongoDB connection error: " + e.getMessage();
        }
    }
}
