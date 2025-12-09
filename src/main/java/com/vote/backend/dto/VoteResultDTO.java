package com.vote.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoteResultDTO {
    
    private String candidateId;
    private String candidateName;
    private String category;
    private Long approveCount;
    private Long opposeCount;
    private Long abstainCount;
}
