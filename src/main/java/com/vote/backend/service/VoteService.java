package com.vote.backend.service;

import com.vote.backend.dto.VoteResultDTO;
import com.vote.backend.dto.VoteSubmitDTO;
import com.vote.backend.entity.*;
import com.vote.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final CandidateRepository candidateRepository;
    private final VoteConfigRepository voteConfigRepository;
    private final VoteRecordRepository voteRecordRepository;

    /**
     * 获取当前投票配置和状态
     */
    public VoteConfig getVoteConfig() {
        return voteConfigRepository.getConfig();
    }

    /**
     * 获取当前轮次的候选人列表
     */
    public List<Candidate> getCandidates(Integer round) {
        if (round == 1) {
            return candidateRepository.findAll();
        } else {
            // 第二轮只返回晋级的候选人
            return candidateRepository.findByIsRound2QualifiedTrue();
        }
    }

    /**
     * 检查用户是否已在该轮投票
     */
    public boolean hasVoted(String voterId, Integer round) {
        return voteRecordRepository.existsByVoterIdAndRound(voterId, round.byteValue());
    }

    /**
     * 提交投票
     */
    @Transactional
    public void submitVotes(VoteSubmitDTO dto) {
        VoteConfig config = getVoteConfig();
        
        // 检查投票是否开放
        if (dto.getRound() == 1 && config.getCurrentStatus() != VoteConfig.Status.round1_voting) {
            throw new RuntimeException("第一轮投票未开放");
        }
        if (dto.getRound() == 2 && config.getCurrentStatus() != VoteConfig.Status.round2_voting) {
            throw new RuntimeException("第二轮投票未开放");
        }

        // 检查是否已投票
        if (hasVoted(dto.getVoterId(), dto.getRound())) {
            throw new RuntimeException("您已经在本轮投过票了");
        }

        // 检查赞成票数量限制
        int maxApprove = dto.getRound() == 1 ? config.getRound1MaxApprove() : config.getRound2MaxApprove();
        long approveCount = dto.getVotes().values().stream()
                .filter(v -> "approve".equals(v))
                .count();
        if (approveCount > maxApprove) {
            throw new RuntimeException("赞成票数量超过限制（最多" + maxApprove + "票）");
        }

        // 保存投票记录
        List<VoteRecord> records = new ArrayList<>();
        for (Map.Entry<String, String> entry : dto.getVotes().entrySet()) {
            VoteRecord record = new VoteRecord();
            record.setVoterId(dto.getVoterId());
            record.setCandidateId(entry.getKey());
            record.setRound(dto.getRound().byteValue());
            record.setVoteType(VoteRecord.VoteType.valueOf(entry.getValue()));
            records.add(record);
        }
        voteRecordRepository.saveAll(records);
    }

    /**
     * 获取投票结果
     */
    public List<VoteResultDTO> getVoteResults(Integer round) {
        List<Candidate> candidates = (round == 1) 
            ? candidateRepository.findAll() 
            : candidateRepository.findByIsRound2QualifiedTrue();
        
        Byte roundByte = round.byteValue();
        List<VoteResultDTO> results = new ArrayList<>();
        for (Candidate c : candidates) {
            VoteResultDTO dto = new VoteResultDTO();
            dto.setCandidateId(c.getId());
            dto.setCandidateName(c.getName());
            dto.setCategory(c.getCategory().name());
            dto.setApproveCount(voteRecordRepository.countApproveVotes(c.getId(), roundByte));
            dto.setOpposeCount(voteRecordRepository.countOpposeVotes(c.getId(), roundByte));
            dto.setAbstainCount(voteRecordRepository.countAbstainVotes(c.getId(), roundByte));
            results.add(dto);
        }
        
        // 按赞成票排序
        results.sort((a, b) -> b.getApproveCount().compareTo(a.getApproveCount()));
        return results;
    }
}
