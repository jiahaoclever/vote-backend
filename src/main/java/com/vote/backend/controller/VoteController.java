package com.vote.backend.controller;

import com.vote.backend.dto.ApiResponse;
import com.vote.backend.dto.VoteSubmitDTO;
import com.vote.backend.entity.Candidate;
import com.vote.backend.entity.VoteConfig;
import com.vote.backend.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 投票端接口（供前端调用）
 */
@RestController
@RequestMapping("/api/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    /**
     * 获取当前投票状态和配置
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        VoteConfig config = voteService.getVoteConfig();
        
        Map<String, Object> data = new HashMap<>();
        data.put("currentStatus", config.getCurrentStatus().name());
        data.put("round1MaxApprove", config.getRound1MaxApprove());
        data.put("round2MaxApprove", config.getRound2MaxApprove());
        data.put("directorCount", config.getDirectorCount());
        data.put("managerCount", config.getManagerCount());
        
        // 判断当前是第几轮
        int currentRound = 0;
        if (config.getCurrentStatus() == VoteConfig.Status.round1_voting) {
            currentRound = 1;
        } else if (config.getCurrentStatus() == VoteConfig.Status.round2_voting) {
            currentRound = 2;
        }
        data.put("currentRound", currentRound);
        
        return ApiResponse.success(data);
    }

    /**
     * 获取候选人列表
     */
    @GetMapping("/candidates")
    public ApiResponse<List<Candidate>> getCandidates(@RequestParam(defaultValue = "1") Integer round) {
        List<Candidate> candidates = voteService.getCandidates(round);
        return ApiResponse.success(candidates);
    }

    /**
     * 检查用户是否已投票
     */
    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> checkVoteStatus(
            @RequestParam String voterId,
            @RequestParam Integer round) {
        boolean hasVoted = voteService.hasVoted(voterId, round);
        
        Map<String, Object> data = new HashMap<>();
        data.put("hasVoted", hasVoted);
        
        return ApiResponse.success(data);
    }

    /**
     * 提交投票
     */
    @PostMapping("/submit")
    public ApiResponse<Void> submitVotes(@Valid @RequestBody VoteSubmitDTO dto) {
        try {
            voteService.submitVotes(dto);
            return ApiResponse.success("投票成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
