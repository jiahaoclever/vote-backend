package com.vote.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class VoteSubmitDTO {
    
    @NotBlank(message = "投票人ID不能为空")
    private String voterId;
    
    @NotNull(message = "轮次不能为空")
    private Integer round;
    
    // key: candidateId, value: voteType (approve/oppose/abstain)
    @NotNull(message = "投票记录不能为空")
    private Map<String, String> votes;
}
