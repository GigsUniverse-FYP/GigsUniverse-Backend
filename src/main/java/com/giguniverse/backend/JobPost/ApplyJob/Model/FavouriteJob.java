package com.giguniverse.backend.JobPost.ApplyJob.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import jakarta.persistence.*;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Builder
@Entity
@Table(name = "favourite_job")
public class FavouriteJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int favouriteJobId;

    private String jobId; // id for the job

    private String freelancerId; // id of the freelancer who favorited the job
}
