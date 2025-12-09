package com.vote.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "vote_record", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"voter_id", "candidate_id", "round"})
})
public class VoteRecord implements Persistable<String> {

    @Id
    private String id;
    
    @Transient
    private boolean newRecord = true;
    
    @Override
    public boolean isNew() {
        return newRecord;
    }
    
    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newRecord = false;
    }

    @Column(name = "voter_id", nullable = false, length = 100)
    private String voterId;

    @Column(name = "candidate_id", nullable = false, length = 64)
    private String candidateId;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Byte round;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false)
    private VoteType voteType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum VoteType {
        approve, oppose, abstain
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
    }
}
