package KarmaTute.KarmaTute.service;

import KarmaTute.KarmaTute.aigateway.AIGatewayRequest;
import KarmaTute.KarmaTute.aigateway.AIGatewayService;
import KarmaTute.KarmaTute.aigateway.AITaskType;
import KarmaTute.KarmaTute.entity.AssessmentMaterial;
import KarmaTute.KarmaTute.entity.AssessmentSession;
import KarmaTute.KarmaTute.entity.GeneratedQuestion;
import KarmaTute.KarmaTute.entity.UserProfile;
import KarmaTute.KarmaTute.repository.GeneratedQuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MCQGenerationService {
    private static final Logger log = LoggerFactory.getLogger(MCQGenerationService.class);
    private static final int REQUIRED_MCQ_COUNT = 15;
    private static final int MAX_ATTEMPTS = 5;

    private final AIGatewayService aiGatewayService;
    private final GeneratedQuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    public MCQGenerationService(AIGatewayService aiGatewayService, GeneratedQuestionRepository questionRepository) {
        this.aiGatewayService = aiGatewayService;
        this.questionRepository = questionRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public List<GeneratedQuestion> generateLevel1MCQs(AssessmentSession session, AssessmentMaterial material, UserProfile userProfile) {
        String designation = userProfile != null ? userProfile.getDesignation() : "Officer";
        String department = userProfile != null ? userProfile.getDepartment() : "Government Department";
        String topic = material.getDetectedTopic();
        String materialContext = material.getFullExtractedText().length() > 12000 ? material.getFullExtractedText().substring(0, 12000) : material.getFullExtractedText();

        List<GeneratedQuestion> allQuestions = new ArrayList<>();
        int attempts = 0;
        while (allQuestions.size() < REQUIRED_MCQ_COUNT && attempts < MAX_ATTEMPTS) {
            int need = REQUIRED_MCQ_COUNT - allQuestions.size();
            int batchSize = Math.min(need + 3, 20);
            try {
                String systemPrompt = String.format(
                    "You are a government assessment expert generating MCQs for a %s in %s." +
                    " Generate questions ONLY from the provided document content." +
                    " Do NOT invent facts not in the material." +
                    " Return a valid JSON array. Each element must have: questionText (string), options (array of exactly 4 strings), correctAnswer (string matching one option exactly), topic (string), subtopic (string), difficulty (EASY/MEDIUM/HARD), sourcePage (integer or 0), sourceChunk (string excerpt from document)." +
                    " Return ONLY the JSON array.", designation, department);
                String userPrompt = String.format(
                    "Document topic: %s\n\nDocument text:\n%s\n\nGenerate EXACTLY %d unique MCQ questions grounded in this document.%s Mix types: factual, application, interpretation, practical decision.",
                    topic, materialContext, batchSize,
                    allQuestions.isEmpty() ? "" : " Already have " + allQuestions.size() + " questions, generate completely different ones.");
                var response = aiGatewayService.executeTask(
                    AIGatewayRequest.builder().taskType(AITaskType.MCQ_GENERATION).systemPrompt(systemPrompt).userPrompt(userPrompt).bypassCache(true).build());
                String rawJson = ((String) response.getData()).replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                List<Map<String, Object>> generated = objectMapper.readValue(rawJson, List.class);
                for (Map<String, Object> q : generated) {
                    if (allQuestions.size() >= REQUIRED_MCQ_COUNT) break;
                    GeneratedQuestion gq = parseQuestion(q, session, material);
                    if (gq != null && !isDuplicate(gq, allQuestions)) allQuestions.add(gq);
                }
            } catch (Exception e) {
                log.error("MCQ batch generation failed attempt {}: {}", attempts + 1, e.getMessage());
            }
            attempts++;
        }
        if (allQuestions.size() < REQUIRED_MCQ_COUNT)
            throw new RuntimeException("Material could not generate " + REQUIRED_MCQ_COUNT + " valid questions (got " + allQuestions.size() + "). Please upload a more comprehensive PDF.");
        return questionRepository.saveAll(allQuestions.subList(0, REQUIRED_MCQ_COUNT));
    }

    @Transactional
    public GeneratedQuestion generateLevel2Question(AssessmentSession session, AssessmentMaterial material, UserProfile userProfile) {
        String designation = userProfile != null ? userProfile.getDesignation() : "Officer";
        String department = userProfile != null ? userProfile.getDepartment() : "Government Department";
        String responsibilities = userProfile != null ? userProfile.getCurrentResponsibilities() : "";
        String topic = material.getDetectedTopic();
        String materialContext = material.getFullExtractedText().length() > 10000 ? material.getFullExtractedText().substring(0, 10000) : material.getFullExtractedText();
        try {
            var response = aiGatewayService.executeTask(AIGatewayRequest.builder()
                .taskType(AITaskType.MCQ_GENERATION)
                .systemPrompt("You are an expert assessment designer. Create exactly ONE practical reasoning question from the provided document. The question must require applying, interpreting, or reasoning with material concepts. Return JSON: {questionText, topic, subtopic, difficulty:'HARD', sourcePage, sourceChunk}. Return ONLY the JSON.")
                .userPrompt(String.format("Officer: %s at %s. Responsibilities: %s.\nMaterial topic: %s\n\nDocument:\n%s\n\nGenerate ONE reasoning question grounded in this material.", designation, department, responsibilities, topic, materialContext))
                .bypassCache(true).build());
            String rawJson = ((String) response.getData()).replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            Map<String, Object> q = objectMapper.readValue(rawJson, Map.class);
            GeneratedQuestion gq = new GeneratedQuestion();
            gq.setSession(session); gq.setLevel(2);
            gq.setQuestionText((String) q.getOrDefault("questionText", ""));
            gq.setTopic((String) q.getOrDefault("topic", topic));
            gq.setSubtopic((String) q.getOrDefault("subtopic", ""));
            gq.setDifficulty("HARD");
            Object sp = q.get("sourcePage"); gq.setSourcePage(sp instanceof Integer ? (Integer) sp : 0);
            gq.setSourceChunk((String) q.getOrDefault("sourceChunk", ""));
            gq.setQuestionType("REASONING"); gq.setValidationStatus("VALID"); gq.setCorrectAnswer("");
            return questionRepository.save(gq);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Level 2 question: " + e.getMessage(), e);
        }
    }

    @Transactional
    public GeneratedQuestion generateLevel3Question(AssessmentSession session, AssessmentMaterial material, UserProfile userProfile) {
        String designation = userProfile != null ? userProfile.getDesignation() : "Officer";
        String department = userProfile != null ? userProfile.getDepartment() : "Government Department";
        String targetRole = userProfile != null ? userProfile.getTargetRole() : "";
        String topic = material.getDetectedTopic();
        String materialContext = material.getFullExtractedText().length() > 10000 ? material.getFullExtractedText().substring(0, 10000) : material.getFullExtractedText();
        try {
            var response = aiGatewayService.executeTask(AIGatewayRequest.builder()
                .taskType(AITaskType.SCENARIO_EVALUATION)
                .systemPrompt("You are a senior assessment expert. Create ONE comprehensive mega reasoning scenario that combines multiple concepts from the document. Requires judgement, decision-making, and deep interpretation. Do not invent facts not in the material. Return JSON: {questionText, topic, subtopic, sourceChunk}. Return ONLY the JSON.")
                .userPrompt(String.format("Officer: %s at %s, targeting: %s.\nMaterial topic: %s\n\nDocument:\n%s\n\nGenerate ONE mega reasoning scenario combining multiple material concepts.", designation, department, targetRole, topic, materialContext))
                .bypassCache(true).build());
            String rawJson = ((String) response.getData()).replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            Map<String, Object> q = objectMapper.readValue(rawJson, Map.class);
            GeneratedQuestion gq = new GeneratedQuestion();
            gq.setSession(session); gq.setLevel(3);
            gq.setQuestionText((String) q.getOrDefault("questionText", ""));
            gq.setTopic((String) q.getOrDefault("topic", topic));
            gq.setSubtopic((String) q.getOrDefault("subtopic", ""));
            gq.setDifficulty("HARD");
            gq.setSourceChunk((String) q.getOrDefault("sourceChunk", ""));
            gq.setQuestionType("MEGA"); gq.setValidationStatus("VALID"); gq.setCorrectAnswer("");
            return questionRepository.save(gq);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Level 3 mega question: " + e.getMessage(), e);
        }
    }

    private GeneratedQuestion parseQuestion(Map<String, Object> q, AssessmentSession session, AssessmentMaterial material) {
        try {
            String questionText = (String) q.get("questionText");
            List<String> options = (List<String>) q.get("options");
            String correctAnswer = (String) q.get("correctAnswer");
            if (questionText == null || questionText.isBlank() || options == null || options.size() != 4 || correctAnswer == null || correctAnswer.isBlank()) return null;
            if (options.stream().noneMatch(o -> o.equalsIgnoreCase(correctAnswer))) return null;
            GeneratedQuestion gq = new GeneratedQuestion();
            gq.setSession(session); gq.setLevel(1);
            gq.setQuestionText(questionText);
            gq.setOptionsJson(objectMapper.writeValueAsString(options));
            gq.setCorrectAnswer(correctAnswer);
            gq.setTopic((String) q.getOrDefault("topic", material.getDetectedTopic()));
            gq.setSubtopic((String) q.getOrDefault("subtopic", ""));
            gq.setDifficulty((String) q.getOrDefault("difficulty", "MEDIUM"));
            Object sp = q.get("sourcePage"); gq.setSourcePage(sp instanceof Integer ? (Integer) sp : 0);
            gq.setSourceChunk((String) q.getOrDefault("sourceChunk", ""));
            gq.setValidationStatus("VALID"); gq.setQuestionType("MCQ");
            return gq;
        } catch (Exception e) { log.warn("Failed to parse question: {}", e.getMessage()); return null; }
    }

    private boolean isDuplicate(GeneratedQuestion newQ, List<GeneratedQuestion> existing) {
        String newText = newQ.getQuestionText().toLowerCase().trim();
        return existing.stream().anyMatch(q -> q.getQuestionText().toLowerCase().trim().equals(newText) || similarity(q.getQuestionText(), newQ.getQuestionText()) > 0.85);
    }

    private double similarity(String a, String b) {
        Set<String> setA = new HashSet<>(Arrays.asList(a.toLowerCase().split("\\W+")));
        Set<String> setB = new HashSet<>(Arrays.asList(b.toLowerCase().split("\\W+")));
        Set<String> intersection = new HashSet<>(setA); intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA); union.addAll(setB);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
}
