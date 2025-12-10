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

        // 获取所有候选人用于分类
        Map<String, Candidate> candidateMap = new HashMap<>();
        candidateRepository.findAll().forEach(c -> candidateMap.put(c.getId(), c));

        // 按类别统计赞成票
        long directorApproveCount = 0;
        long managerApproveCount = 0;
        for (Map.Entry<String, String> entry : dto.getVotes().entrySet()) {
            if ("approve".equals(entry.getValue())) {
                Candidate c = candidateMap.get(entry.getKey());
                if (c != null) {
                    if (c.getCategory() == Candidate.Category.director) {
                        directorApproveCount++;
                    } else if (c.getCategory() == Candidate.Category.manager) {
                        managerApproveCount++;
                    }
                }
            }
        }

        // 检查各类别赞成票数量限制
        int directorMaxApprove, managerMaxApprove;
        if (dto.getRound() == 1) {
            directorMaxApprove = config.getRound1DirectorMaxApprove();
            managerMaxApprove = config.getRound1ManagerMaxApprove();
        } else {
            directorMaxApprove = config.getRound2DirectorMaxApprove();
            managerMaxApprove = config.getRound2ManagerMaxApprove();
        }

        if (directorApproveCount > directorMaxApprove) {
            throw new RuntimeException("常务理事赞成票数量超过限制（最多" + directorMaxApprove + "票，当前" + directorApproveCount + "票）");
        }
        if (managerApproveCount > managerMaxApprove) {
            throw new RuntimeException("负责人赞成票数量超过限制（最多" + managerMaxApprove + "票，当前" + managerApproveCount + "票）");
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

    /**
     * 获取实时投票结果（供大屏展示）
     */
    public Map<String, Object> getLiveResults() {
        VoteConfig config = getVoteConfig();
        Map<String, Object> result = new HashMap<>();
        
        // 当前状态
        result.put("currentStatus", config.getCurrentStatus().name());
        
        // 判断当前轮次
        final int currentRound;
        if (config.getCurrentStatus() == VoteConfig.Status.round1_voting || 
            config.getCurrentStatus() == VoteConfig.Status.round1_ended) {
            currentRound = 1;
        } else if (config.getCurrentStatus() == VoteConfig.Status.round2_voting || 
                   config.getCurrentStatus() == VoteConfig.Status.round2_ended) {
            currentRound = 2;
        } else {
            currentRound = 0;
        }
        result.put("currentRound", currentRound);
        
        // 配置信息
        result.put("directorQualifyCount", config.getDirectorQualifyCount());
        result.put("managerQualifyCount", config.getManagerQualifyCount());
        result.put("directorElectCount", config.getDirectorElectCount());
        result.put("managerElectCount", config.getManagerElectCount());
        
        // 获取所有候选人
        List<Candidate> allCandidates = candidateRepository.findAll();
        
        // 统计已投票人数
        if (currentRound > 0) {
            long voterCount = voteRecordRepository.countDistinctVotersByRound((byte) currentRound);
            result.put("voterCount", voterCount);
        } else {
            result.put("voterCount", 0);
        }
        
        // 分类别获取投票结果
        if (currentRound > 0) {
            Byte roundByte = (byte) currentRound;
            List<Object[]> voteStats = voteRecordRepository.countAllVotesByRound(roundByte);
            
            // 构建 candidateId -> approveCount 映射
            Map<String, Long> approveMap = new HashMap<>();
            for (Object[] row : voteStats) {
                String candidateId = (String) row[0];
                String voteType = row[1].toString();
                Long count = (Long) row[2];
                if ("approve".equals(voteType)) {
                    approveMap.put(candidateId, count);
                }
            }
            
            // 常务理事结果
            List<Map<String, Object>> directorResults = allCandidates.stream()
                    .filter(c -> c.getCategory() == Candidate.Category.director)
                    .filter(c -> currentRound == 1 || c.getIsRound2Qualified())
                    .map(c -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", c.getId());
                        m.put("name", c.getName());
                        m.put("title", c.getTitle());
                        m.put("approveCount", approveMap.getOrDefault(c.getId(), 0L));
                        m.put("isQualified", c.getIsRound2Qualified());
                        return m;
                    })
                    .sorted((a, b) -> Long.compare((Long) b.get("approveCount"), (Long) a.get("approveCount")))
                    .toList();
            
            // 负责人结果
            List<Map<String, Object>> managerResults = allCandidates.stream()
                    .filter(c -> c.getCategory() == Candidate.Category.manager)
                    .filter(c -> currentRound == 1 || c.getIsRound2Qualified())
                    .map(c -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", c.getId());
                        m.put("name", c.getName());
                        m.put("title", c.getTitle());
                        m.put("approveCount", approveMap.getOrDefault(c.getId(), 0L));
                        m.put("isQualified", c.getIsRound2Qualified());
                        return m;
                    })
                    .sorted((a, b) -> Long.compare((Long) b.get("approveCount"), (Long) a.get("approveCount")))
                    .toList();
            
            result.put("directors", directorResults);
            result.put("managers", managerResults);
        } else {
            result.put("directors", List.of());
            result.put("managers", List.of());
        }
        
        return result;
    }
}
