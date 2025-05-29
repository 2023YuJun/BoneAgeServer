package com.example.server.service;

import com.example.server.Utils.RCScoreUtils;
import com.example.server.Utils.TRScoreUtils;
import com.example.server.model.RusChnResult;
import com.example.server.model.Tw3CRusResult;
import com.example.server.repository.RusChnResultRepository;
import com.example.server.repository.Tw3CRusResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BoneAgeService {
    private final RusChnResultRepository rusRepo;
    private final Tw3CRusResultRepository tw3Repo;

    @Autowired
    public BoneAgeService(RusChnResultRepository rusRepo, Tw3CRusResultRepository tw3Repo) {
        this.rusRepo = rusRepo;
        this.tw3Repo = tw3Repo;
    }

    public Map<String, Object> processRusChn(boolean isMale, Map<String, Integer> partIndices) {
        // 计算结果（自动处理越界等级）
        double boneAge = RCScoreUtils.calculateBoneAge(isMale, partIndices);
        int total = RCScoreUtils.calculateTotalScore(isMale, partIndices);

        // 构建实体（记录原始输入值）
        RusChnResult result = new RusChnResult();
        populateRusChnResultFields(result, partIndices);
        result.setTotal(total);
        result.setBoneAge(boneAge);
        result.setUpdateTime(LocalDateTime.now());

        // 存储记录
        Long rcResultId = rusRepo.save(result);

        return Map.of(
                "boneAge", boneAge,
                "rcResultId", rcResultId
        );
    }

    public Map<String, Object> processTw3CRus(boolean isMale, Map<String, Integer> partIndices) {
        int total = TRScoreUtils.calculateTotalScore(isMale, partIndices);
        double boneAge = TRScoreUtils.calculateBoneAge(isMale, partIndices);

        Tw3CRusResult result = new Tw3CRusResult();
        populateTw3CRusResultFields(result, partIndices, isMale);
        result.setTotal(total);
        result.setBoneAge(boneAge);
        result.setUpdateTime(LocalDateTime.now());

        // 存储记录
        Long tcrResultId = tw3Repo.save(result);

        return Map.of(
                "boneAge", boneAge,
                "tcrResultId", tcrResultId
        );
    }

    private void populateRusChnResultFields(RusChnResult result, Map<String, Integer> inputs) {
        result.setMcpFirst(validateGrade("MCPFirst", inputs.get("MCPFirst")));
        result.setMcpThird(validateGrade("MCPThird", inputs.get("MCPThird")));
        result.setMcpFifth(validateGrade("MCPFifth", inputs.get("MCPFifth")));
        result.setPipFirst(validateGrade("PIPFirst", inputs.get("PIPFirst")));
        result.setPipThird(validateGrade("PIPThird", inputs.get("PIPThird")));
        result.setPipFifth(validateGrade("PIPFifth", inputs.get("PIPFifth")));
        result.setMipThird(validateGrade("MIPThird", inputs.get("MIPThird")));
        result.setMipFifth(validateGrade("MIPFifth", inputs.get("MIPFifth")));
        result.setDipFirst(validateGrade("DIPFirst", inputs.get("DIPFirst")));
        result.setDipThird(validateGrade("DIPThird", inputs.get("DIPThird")));
        result.setDipFifth(validateGrade("DIPFifth", inputs.get("DIPFifth")));
        result.setRadius(validateGrade("Radius", inputs.get("Radius")));
        result.setUlna(validateGrade("Ulna", inputs.get("Ulna")));
    }

    private Integer validateGrade(String part, Integer grade) {
        if (grade == null) return null;
        try {
            // 获取对应性别的分数表
            Map<String, List<Integer>> scoreTable = RCScoreUtils.SCORE_TABLES.get(true); // 默认男性
            List<Integer> partScores = scoreTable.get(part);
            if (partScores == null) {
                return -1; // 如果是无效部位，返回-1
            }

            int maxGrade = partScores.size();
            if (grade < 1 || grade > maxGrade) {
                return -1; // 越界返回-1
            }
        } catch (Exception e) {
            return -1; // 如果发生异常，返回-1
        }
        return grade;
    }

    private void populateTw3CRusResultFields(Tw3CRusResult result, Map<String, Integer> inputs, boolean isMale) {
        // TR方法记录映射后的等级
        result.setMcpFirst(getMappedGrade("MCPFirst", inputs));
        result.setMcpThird(getMappedGrade("MCPThird", inputs));
        result.setMcpFifth(getMappedGrade("MCPFifth", inputs));
        result.setPipFirst(getMappedGrade("PIPFirst", inputs));
        result.setPipThird(getMappedGrade("PIPThird", inputs));
        result.setPipFifth(getMappedGrade("PIPFifth", inputs));
        result.setMipThird(getMappedGrade("MIPThird", inputs));
        result.setMipFifth(getMappedGrade("MIPFifth", inputs));
        result.setDipFirst(getMappedGrade("DIPFirst", inputs));
        result.setDipThird(getMappedGrade("DIPThird", inputs));
        result.setDipFifth(getMappedGrade("DIPFifth", inputs));
        result.setRadius(getMappedGrade("Radius", inputs));
        result.setUlna(getMappedGrade("Ulna", inputs));
    }

    private Integer getMappedGrade(String part, Map<String, Integer> inputs) {
        Integer original = inputs.get(part);
        if (original == null) return null;
        try {
            return TRScoreUtils.mapGrade(part, original);
        } catch (IllegalArgumentException e) {
            return -1; // 越界时返回-1
        }
    }
}