package com.vote.backend.controller;

import com.vote.backend.dto.ApiResponse;
import com.vote.backend.dto.VoteResultDTO;
import com.vote.backend.entity.Candidate;
import com.vote.backend.entity.VoteConfig;
import com.vote.backend.repository.CandidateRepository;
import com.vote.backend.repository.VoteConfigRepository;
import com.vote.backend.repository.VoteRecordRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CandidateRepository candidateRepository;
    private final VoteConfigRepository voteConfigRepository;
    private final VoteRecordRepository voteRecordRepository;

    // ==================== 候选人管理 ====================

    @GetMapping("/candidates")
    public ApiResponse<List<Candidate>> getAllCandidates() {
        return ApiResponse.success(candidateRepository.findAll());
    }

    @GetMapping("/candidates/{id}")
    public ApiResponse<Candidate> getCandidate(@PathVariable String id) {
        return candidateRepository.findById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("候选人不存在"));
    }

    @PostMapping("/candidates")
    public ApiResponse<Candidate> addCandidate(@RequestBody Candidate candidate) {
        candidate.setId(UUID.randomUUID().toString());
        candidate.setCreatedAt(LocalDateTime.now());
        candidate.setUpdatedAt(LocalDateTime.now());
        candidate.setIsRound2Qualified(false);
        return ApiResponse.success(candidateRepository.save(candidate));
    }

    @PutMapping("/candidates/{id}")
    public ApiResponse<Candidate> updateCandidate(@PathVariable String id, @RequestBody Candidate candidate) {
        return candidateRepository.findById(id)
                .map(existing -> {
                    existing.setName(candidate.getName());
                    existing.setTitle(candidate.getTitle());
                    existing.setDescription(candidate.getDescription());
                    existing.setPdfUrl(candidate.getPdfUrl());
                    existing.setCategory(candidate.getCategory());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return ApiResponse.success(candidateRepository.save(existing));
                })
                .orElse(ApiResponse.error("候选人不存在"));
    }

    @DeleteMapping("/candidates/{id}")
    public ApiResponse<Void> deleteCandidate(@PathVariable String id) {
        if (candidateRepository.existsById(id)) {
            candidateRepository.deleteById(id);
            return ApiResponse.success(null);
        }
        return ApiResponse.error("候选人不存在");
    }

    // ==================== 投票配置 ====================

    @GetMapping("/config")
    public ApiResponse<VoteConfig> getConfig() {
        return voteConfigRepository.findTopByOrderByIdAsc()
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("配置不存在"));
    }

    @PutMapping("/config")
    public ApiResponse<VoteConfig> updateConfig(@RequestBody VoteConfig config) {
        return voteConfigRepository.findTopByOrderByIdAsc()
                .map(existing -> {
                    if (config.getRound1MaxApprove() != null) {
                        existing.setRound1MaxApprove(config.getRound1MaxApprove());
                    }
                    if (config.getRound2MaxApprove() != null) {
                        existing.setRound2MaxApprove(config.getRound2MaxApprove());
                    }
                    if (config.getDirectorCount() != null) {
                        existing.setDirectorCount(config.getDirectorCount());
                    }
                    if (config.getManagerCount() != null) {
                        existing.setManagerCount(config.getManagerCount());
                    }
                    return ApiResponse.success(voteConfigRepository.save(existing));
                })
                .orElse(ApiResponse.error("配置不存在"));
    }

    // ==================== 轮次控制 ====================

    @PostMapping("/round/start-round1")
    public ApiResponse<Void> startRound1() {
        return voteConfigRepository.findTopByOrderByIdAsc()
                .map(config -> {
                    if (config.getCurrentStatus() != VoteConfig.Status.not_started) {
                        return ApiResponse.<Void>error("当前状态不允许开启第一轮");
                    }
                    config.setCurrentStatus(VoteConfig.Status.round1_voting);
                    voteConfigRepository.save(config);
                    return ApiResponse.<Void>success(null);
                })
                .orElse(ApiResponse.error("配置不存在"));
    }

    @PostMapping("/round/end-round1")
    public ApiResponse<Void> endRound1() {
        return voteConfigRepository.findTopByOrderByIdAsc()
                .map(config -> {
                    if (config.getCurrentStatus() != VoteConfig.Status.round1_voting) {
                        return ApiResponse.<Void>error("当前状态不允许结束第一轮");
                    }
                    config.setCurrentStatus(VoteConfig.Status.round1_ended);
                    voteConfigRepository.save(config);
                    return ApiResponse.<Void>success(null);
                })
                .orElse(ApiResponse.error("配置不存在"));
    }

    @PutMapping("/round2/qualified")
    public ApiResponse<Void> setRound2Qualified(@RequestBody List<String> candidateIds) {
        // 先清除所有候选人的晋级状态
        List<Candidate> allCandidates = candidateRepository.findAll();
        for (Candidate c : allCandidates) {
            c.setIsRound2Qualified(false);
        }
        candidateRepository.saveAll(allCandidates);

        // 设置晋级的候选人
        List<Candidate> qualifiedCandidates = candidateRepository.findAllById(candidateIds);
        for (Candidate c : qualifiedCandidates) {
            c.setIsRound2Qualified(true);
        }
        candidateRepository.saveAll(qualifiedCandidates);

        return ApiResponse.success(null);
    }

    @PostMapping("/round/start-round2")
    public ApiResponse<Void> startRound2() {
        return voteConfigRepository.findTopByOrderByIdAsc()
                .map(config -> {
                    if (config.getCurrentStatus() != VoteConfig.Status.round1_ended) {
                        return ApiResponse.<Void>error("当前状态不允许开启第二轮");
                    }
                    config.setCurrentStatus(VoteConfig.Status.round2_voting);
                    voteConfigRepository.save(config);
                    return ApiResponse.<Void>success(null);
                })
                .orElse(ApiResponse.error("配置不存在"));
    }

    @PostMapping("/round/end-round2")
    public ApiResponse<Void> endRound2() {
        return voteConfigRepository.findTopByOrderByIdAsc()
                .map(config -> {
                    if (config.getCurrentStatus() != VoteConfig.Status.round2_voting) {
                        return ApiResponse.<Void>error("当前状态不允许结束第二轮");
                    }
                    config.setCurrentStatus(VoteConfig.Status.round2_ended);
                    voteConfigRepository.save(config);
                    return ApiResponse.<Void>success(null);
                })
                .orElse(ApiResponse.error("配置不存在"));
    }

    // ==================== 投票结果 ====================

    @GetMapping("/results/{round}")
    public ApiResponse<List<VoteResultDTO>> getResults(@PathVariable Integer round) {
        Byte roundByte = round.byteValue();
        List<Candidate> candidates = candidateRepository.findAll();
        
        // 一次性查询所有投票统计（优化N+1问题）
        List<Object[]> voteStats = voteRecordRepository.countAllVotesByRound(roundByte);
        
        // 构建 candidateId -> {voteType -> count} 的映射
        Map<String, Map<String, Long>> statsMap = new HashMap<>();
        for (Object[] row : voteStats) {
            String candidateId = (String) row[0];
            // voteType 是枚举，需要转成字符串
            String voteType = row[1] != null ? row[1].toString() : "";
            Long count = (Long) row[2];
            statsMap.computeIfAbsent(candidateId, k -> new HashMap<>()).put(voteType, count);
        }

        List<VoteResultDTO> results = candidates.stream().map(c -> {
            VoteResultDTO dto = new VoteResultDTO();
            dto.setCandidateId(c.getId());
            dto.setCandidateName(c.getName());
            dto.setCategory(c.getCategory() != null ? c.getCategory().name() : null);
            
            Map<String, Long> counts = statsMap.getOrDefault(c.getId(), Map.of());
            dto.setApproveCount(counts.getOrDefault("approve", 0L));
            dto.setOpposeCount(counts.getOrDefault("oppose", 0L));
            dto.setAbstainCount(counts.getOrDefault("abstain", 0L));
            return dto;
        }).sorted((a, b) -> Long.compare(b.getApproveCount(), a.getApproveCount()))
          .collect(Collectors.toList());

        return ApiResponse.success(results);
    }

    // ==================== Excel 模板下载与导入 ====================

    @GetMapping("/candidates/template")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=candidate_template.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("候选人");

            // 表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"姓名", "头衔", "简介", "类别(director/manager)", "履历PDF链接"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // 添加示例数据
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("张三");
            exampleRow.createCell(1).setCellValue("XX部门主任");
            exampleRow.createCell(2).setCellValue("从事XX工作20年...");
            exampleRow.createCell(3).setCellValue("director");
            exampleRow.createCell(4).setCellValue("https://example.com/resume.pdf");

            workbook.write(response.getOutputStream());
        }
    }

    @PostMapping("/candidates/import")
    public ApiResponse<Map<String, Object>> importCandidates(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error("请选择文件");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // 获取已存在的候选人姓名（用于去重）
            Set<String> existingNames = candidateRepository.findAll().stream()
                    .map(Candidate::getName)
                    .collect(Collectors.toSet());

            List<Candidate> toImport = new ArrayList<>();
            List<String> skipped = new ArrayList<>();
            int successCount = 0;

            // 从第二行开始（跳过表头）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getCellStringValue(row.getCell(0));
                if (name == null || name.trim().isEmpty()) continue;

                // 去重检查
                if (existingNames.contains(name.trim())) {
                    skipped.add(name.trim());
                    continue;
                }

                Candidate candidate = new Candidate();
                candidate.setId(UUID.randomUUID().toString());
                candidate.setName(name.trim());
                candidate.setTitle(getCellStringValue(row.getCell(1)));
                candidate.setDescription(getCellStringValue(row.getCell(2)));

                String category = getCellStringValue(row.getCell(3));
                candidate.setCategory("manager".equalsIgnoreCase(category) ? Candidate.Category.manager : Candidate.Category.director);

                candidate.setPdfUrl(getCellStringValue(row.getCell(4)));
                candidate.setIsRound2Qualified(false);
                candidate.setCreatedAt(LocalDateTime.now());
                candidate.setUpdatedAt(LocalDateTime.now());

                toImport.add(candidate);
                existingNames.add(name.trim()); // 防止文件内重复
                successCount++;
            }

            // 批量保存
            if (!toImport.isEmpty()) {
                candidateRepository.saveAll(toImport);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("skippedCount", skipped.size());
            result.put("skippedNames", skipped);

            return ApiResponse.success(result);

        } catch (Exception e) {
            return ApiResponse.error("导入失败: " + e.getMessage());
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }
}
