package com.vote.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "vote_config")
public class VoteConfig {

    @Id
    private Integer id = 1;

    @Column(name = "round1_max_approve")
    private Integer round1MaxApprove = 100;

    @Column(name = "round2_max_approve")
    private Integer round2MaxApprove = 50;

    @Column(name = "director_count")
    private Integer directorCount = 80;

    @Column(name = "manager_count")
    private Integer managerCount = 20;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status")
    private Status currentStatus = Status.not_started;

    public enum Status {
        not_started,
        round1_voting,
        round1_ended,
        round2_voting,
        round2_ended
    }
}
