package com.giguniverse.backend.Dashboard.Users.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Dashboard.Users.Model.SuspendUserRequest;
import com.giguniverse.backend.Dashboard.Users.Model.UnsuspendUserRequest;
import com.giguniverse.backend.Dashboard.Users.Model.UserRecordDTO;
import com.giguniverse.backend.Dashboard.Users.Service.UserRecordService;

@RestController
@RequestMapping("/api/user-records")
public class UserRecordController {

    @Autowired
    private UserRecordService userRecordService;

    @GetMapping("/get-records")
    public ResponseEntity<List<UserRecordDTO>> getAllUserRecords() {
        List<UserRecordDTO> records = userRecordService.getAllUserRecords();
        return ResponseEntity.ok(records);
    }

    @PostMapping("/suspend")
    public ResponseEntity<String> suspendUser(@RequestBody SuspendUserRequest request) {
        userRecordService.suspendUser(request);
        return ResponseEntity.ok("User suspended successfully");
    }

    @PostMapping("/unsuspend")
    public ResponseEntity<String> unsuspendUser(@RequestBody UnsuspendUserRequest request) {
        userRecordService.unsuspendUser(request);
        return ResponseEntity.ok("User unsuspended successfully");
    }
}