package com.giguniverse.backend.JobPost.ApplyJob.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.JobPost.ApplyJob.Model.FavouriteJob;
import com.giguniverse.backend.JobPost.ApplyJob.Service.FavouriteJobService;

@RestController
@RequestMapping("/api/favourites")
public class FavouriteJobController {
    @Autowired
    private FavouriteJobService favouriteJobService;

    @PostMapping("/toggle")
    public ResponseEntity<?> addFavourite(@RequestBody Map<String, String> payload) {
        String jobId = payload.get("jobId");
        String freelancerId = AuthUtil.getUserId();

        try {
            FavouriteJob fav = favouriteJobService.addFavourite(freelancerId, jobId);
            return ResponseEntity.ok(fav);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/toggle")
    public ResponseEntity<?> removeFavourite(@RequestBody Map<String, String> payload) {
        String jobId = payload.get("jobId");
        String freelancerId = AuthUtil.getUserId();

        favouriteJobService.removeFavourite(freelancerId, jobId);
        return ResponseEntity.ok().build();
    }
}


