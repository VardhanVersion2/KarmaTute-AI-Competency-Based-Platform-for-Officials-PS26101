package KarmaTute.KarmaTute.service;

import KarmaTute.KarmaTute.aigateway.AIGatewayRequest;
import KarmaTute.KarmaTute.aigateway.AIGatewayService;
import KarmaTute.KarmaTute.aigateway.AITaskType;
import KarmaTute.KarmaTute.entity.AssessmentMaterial;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class RubricEvaluationService {
    private static final Logger log = LoggerFactory.getLogger(RubricEvaluationService.class);
    private final AIGatewayService aiGatewayService;
    private final ObjectMapper objectMapper;

    public RubricEvaluationService(AIGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
        this.objectMapper = new ObjectMapper();
    }

    public record RubricResult(int score, String rubricJson, String summaryFeedback) {}

    public RubricResult evaluateOpenAnswer(String answerText, AssessmentMaterial material, String question, int level) {
        String materialContext = material.getFullExtractedText().length() > 6000 ? material.getFullExtractedText().substring(0, 6000) : material.getFullExtractedText();
        try {
            var response = aiGatewayService.executeTask(AIGatewayRequest.builder()
                .taskType(AITaskType.RUBRIC_ASSISTANCE)
                .systemPrompt("You are an expert evaluator for government officer assessments. Evaluate the answer against rubric criteria derived ONLY from the provided document. Return JSON: {score (0-100), criteria: [{name, score (0-25), evidence, confidence}], summaryFeedback (2-3 sentences)}. Criteria: 1. Concept Understanding (0-25) 2. Application (0-25) 3. Reasoning Quality (0-25) 4. Material Consistency (0-25). Return ONLY the JSON.")
                .userPrompt(String.format("Document topic: %s\n\nDocument text:\n%s\n\nLevel %d Question:\n%s\n\nOfficer Answer:\n%s\n\nEvaluate.", material.getDetectedTopic(), materialContext, level, question, answerText))
                .bypassCache(true).build());
            String rawJson = ((String) response.getData()).replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            Map<String, Object> result = objectMapper.readValue(rawJson, Map.class);
            int score = ((Number) result.getOrDefault("score", 50)).intValue();
            String feedback = (String) result.getOrDefault("summaryFeedback", "");
            return new RubricResult(score, rawJson, feedback);
        } catch (Exception e) {
            log.error("Rubric evaluation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to evaluate answer: " + e.getMessage(), e);
        }
    }
}
