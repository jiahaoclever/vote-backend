package com.vote.backend.repository;

import com.vote.backend.entity.VoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRecordRepository extends JpaRepository<VoteRecord, Long> {
    
    // 查找某投票人在某轮的所有投票记录
    List<VoteRecord> findByVoterIdAndRound(String voterId, Byte round);
    
    // 检查是否已投票
    boolean existsByVoterIdAndRound(String voterId, Byte round);
    
    // 统计某候选人在某轮的赞成票数
    @Query("SELECT COUNT(v) FROM VoteRecord v WHERE v.candidateId = ?1 AND v.round = ?2 AND v.voteType = 'approve'")
    Long countApproveVotes(String candidateId, Byte round);
    
    // 统计某候选人在某轮的反对票数
    @Query("SELECT COUNT(v) FROM VoteRecord v WHERE v.candidateId = ?1 AND v.round = ?2 AND v.voteType = 'oppose'")
    Long countOpposeVotes(String candidateId, Byte round);
    
    // 统计某候选人在某轮的弃权票数
    @Query("SELECT COUNT(v) FROM VoteRecord v WHERE v.candidateId = ?1 AND v.round = ?2 AND v.voteType = 'abstain'")
    Long countAbstainVotes(String candidateId, Byte round);
    
    // 批量统计某轮所有候选人的投票情况（优化N+1查询）
    @Query("SELECT v.candidateId, v.voteType, COUNT(v) FROM VoteRecord v WHERE v.round = ?1 GROUP BY v.candidateId, v.voteType")
    List<Object[]> countAllVotesByRound(Byte round);
}
