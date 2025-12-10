package com.vote.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "vote_config")
public class VoteConfig {

    @Id
    private Integer id = 1;

    // 第一轮限票数（分类别）
    @Column(name = "round1_director_max_approve")
    private Integer round1DirectorMaxApprove = 60;  // 第一轮常务理事最多可投赞成票数

    @Column(name = "round1_manager_max_approve")
    private Integer round1ManagerMaxApprove = 15;   // 第一轮负责人最多可投赞成票数

    // 第二轮限票数（分类别）
    @Column(name = "round2_director_max_approve")
    private Integer round2DirectorMaxApprove = 40;  // 第二轮常务理事最多可投赞成票数

    @Column(name = "round2_manager_max_approve")
    private Integer round2ManagerMaxApprove = 10;   // 第二轮负责人最多可投赞成票数

    // 晋级名额（从第一轮到第二轮）
    @Column(name = "director_qualify_count")
    private Integer directorQualifyCount = 60;      // 常务理事晋级到第二轮的名额

    @Column(name = "manager_qualify_count")
    private Integer managerQualifyCount = 15;       // 负责人晋级到第二轮的名额

    // 最终当选名额
    @Column(name = "director_elect_count")
    private Integer directorElectCount = 40;        // 常务理事最终当选名额

    @Column(name = "manager_elect_count")
    private Integer managerElectCount = 10;         // 负责人最终当选名额

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
