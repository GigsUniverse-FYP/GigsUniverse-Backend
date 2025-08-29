package com.giguniverse.backend.JobPost.ApplyJob.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.JobPost.ApplyJob.Model.FavouriteJob;
import com.giguniverse.backend.JobPost.ApplyJob.Repository.FavouriteJobRepository;

import jakarta.transaction.Transactional;

@Service
public class FavouriteJobService {

    @Autowired
    private FavouriteJobRepository favouriteJobRepository;

    // Add favourite
    @Transactional
    public FavouriteJob addFavourite(String freelancerId, String jobId) {
        boolean exists = favouriteJobRepository.findByFreelancerIdAndJobId(freelancerId, jobId).isPresent();
        if (exists) {
            throw new IllegalStateException("Already favourited");
        }

        FavouriteJob fav = new FavouriteJob();
        fav.setFreelancerId(freelancerId);
        fav.setJobId(jobId);
        return favouriteJobRepository.save(fav);
    }

    // Remove favourite
    @Transactional
    public void removeFavourite(String freelancerId, String jobId) {
        favouriteJobRepository.deleteByFreelancerIdAndJobId(freelancerId, jobId);
    }
}
