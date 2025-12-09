package com.vote.backend.repository;

import com.vote.backend.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, String> {
    
    // 查找第二轮晋级的候选人
    List<Candidate> findByIsRound2QualifiedTrue();
    
    // 按类别查找
    List<Candidate> findByCategory(Candidate.Category category);
}
