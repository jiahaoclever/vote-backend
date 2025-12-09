package com.vote.backend.repository;

import com.vote.backend.entity.VoteConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteConfigRepository extends JpaRepository<VoteConfig, Integer> {
    
    // 获取第一条配置
    java.util.Optional<VoteConfig> findTopByOrderByIdAsc();
    
    // 获取唯一的配置（id=1）
    default VoteConfig getConfig() {
        return findById(1).orElse(new VoteConfig());
    }
}
