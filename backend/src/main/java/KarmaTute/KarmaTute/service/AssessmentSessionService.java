package KarmaTute.KarmaTute.service;

import KarmaTute.KarmaTute.entity.*;
import KarmaTute.KarmaTute.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import KarmaTute.KarmaTute.certificate.CertificateService;
import KarmaTute.KarmaTute.competency.EvidenceRecord;
import KarmaTute.KarmaTute.competency.EvidenceType;
import KarmaTute.KarmaTute.competency.ReviewStatus;
import KarmaTute.KarmaTute.service.EvidenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AssessmentSessionService {
    private final EvidenceService evidenceService;
    private final CertificateService certificateService;

    private static final Logger log = LoggerFactory.getLogger(AssessmentSessionService.class);
    private static final int L1_PASS_THRESHOLD = 70;
    private static final double L2_OCR_THRESHOLD = 0.75;
    private static final int L2_PASS_THRESHOLD = 60;
    private static final int L3_PASS_THRESHOLD = 60;

    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentMaterialRepository materialRepository;
    private final GeneratedQuestionRepository questionRepository;
    private final MCQGenerationService mcqGenerationService;
    private final AssessmentMaterialService materialService;
    private final RubricEvaluationService rubricEvaluationService;
    private final UserCompetencyRepository userCompetencyRepository;
    private final CompetencyGapRepository competencyGapRepository;
    private final EvidenceItemRepository evidenceItemRepository;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;

    public AssessmentSessionService(
            AssessmentSessionRepository sessionRepository,
            AssessmentMaterialRepository materialRepository,
            GeneratedQuestionRepository questionRepository,
            MCQGenerationService mcqGenerationService,
            AssessmentMaterialService materialService,
            RubricEvaluationService rubricEvaluationService,
            UserCompetencyRepository userCompetencyRepository,
            CompetencyGapRepository competencyGapRepository,
            EvidenceItemRepository evidenceItemRepository,
            UserProfileRepository userProfileRepository, EvidenceService evidenceService, CertificateService certificateService) {
        this.evidenceService = evidenceService;
        this.certificateService = certificateService;
        this.sessionRepository = sessionRepository;
        this.materialRepository = materialRepository;
        this.questionRepository = questionRepository;
        this.mcqGenerationService = mcqGenerationService;
        this.materialService = materialService;
        this.rubricEvaluationService = rubricEvaluationService;
        this.userCompetencyRepository = userCompetencyRepository;
        this.competencyGapRepository = competencyGapRepository;
        this.evidenceItemRepository = evidenceItemRepository;
        this.userProfileRepository = userProfileRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public Map<String, Object> startLevel1(Long materialId, User user) {
        AssessmentMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material not found"));
        if (!material.getUser().getId().equals(user.getId()))
            throw new SecurityException("Access denied: material does not belong to this user");
        if (!"READY".equals(material.getProcessingStatus()))
            throw new IllegalStateException("Material is not ready. Status: " + material.getProcessingStatus() + ". Error: " + material.getExtractionError());

        AssessmentSession session = new AssessmentSession();
        session.setUser(user);
        session.setMaterial(material);
        session.setState("L1_IN_PROGRESS");
        session.setStartedAt(LocalDateTime.now());
        session = sessionRepository.save(session);

        UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);
        List<GeneratedQuestion> questions = mcqGenerationService.generateLevel1MCQs(session, material, profile);

        List<Map<String, Object>> questionDtos = buildQuestionDtos(questions);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionId", session.getId());
        response.put("state", session.getState());
        response.put("topic", material.getDetectedTopic());
        response.put("materialName", material.getFilename());
        response.put("questionCount", questions.size());
        response.put("questions", questionDtos);
        return response;
    }

    @Transactional
    public Map<String, Object> submitLevel1(Long sessionId, Map<Long, String> answers, User user) {
        AssessmentSession session = getSessionForUser(sessionId, user);
        if (!"L1_IN_PROGRESS".equals(session.getState()))
            throw new IllegalStateException("Level 1 is not in progress. State: " + session.getState());
        List<GeneratedQuestion> questions = questionRepository.findBySessionIdAndLevel(sessionId, 1);
        if (questions.isEmpty()) throw new RuntimeException("No Level 1 questions found");
        int correct = 0;
        for (GeneratedQuestion q : questions) {
            String submitted = answers.get(q.getId());
            if (submitted != null && submitted.equalsIgnoreCase(q.getCorrectAnswer())) correct++;
        }
        int score = (int) Math.round(((double) correct / questions.size()) * 100);
        session.setL1Score(score);
        boolean passed = score >= L1_PASS_THRESHOLD;
        session.setState(passed ? "L1_PASSED" : "L1_FAILED");
        if (passed) createEvidence(session.getUser(), "Level 1 MCQ Assessment", session.getMaterial().getDetectedTopic(), score, session.getMaterial().getFilename());
        sessionRepository.save(session);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId); result.put("score", score); result.put("correct", correct);
        result.put("total", questions.size()); result.put("passed", passed); result.put("state", session.getState());
        result.put("topic", session.getMaterial().getDetectedTopic());
        return result;
    }

    @Transactional
    public Map<String, Object> startLevel2(Long sessionId, User user) {
        AssessmentSession session = getSessionForUser(sessionId, user);
        if (!"L1_PASSED".equals(session.getState()))
            throw new IllegalStateException("Level 1 must be passed first. State: " + session.getState());
        UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);
        GeneratedQuestion q = mcqGenerationService.generateLevel2Question(session, session.getMaterial(), profile);
        session.setL2Question(q.getQuestionText());
        session.setState("L2_IN_PROGRESS");
        sessionRepository.save(session);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionId", sessionId); response.put("state", "L2_IN_PROGRESS");
        response.put("topic", q.getTopic()); response.put("subtopic", q.getSubtopic());
        response.put("question", q.getQuestionText()); response.put("questionId", q.getId());
        response.put("materialName", session.getMaterial().getFilename());
        response.put("durationMinutes", 10); response.put("assessmentType", "Practical Reasoning");
        return response;
    }



    @Transactional
    public Map<String, Object> demoSubmitLevel2(Long sessionId, User user) {
        AssessmentSession session = getSessionForUser(sessionId, user);
        if (!"L2_IN_PROGRESS".equals(session.getState()))
            throw new IllegalStateException("Level 2 is not in progress. State: " + session.getState());

        String demoText = "This is a demo written answer for the Level 2 reasoning question. I have carefully analyzed the context and determined that the policies strictly align with the National Data Quality Assurance Framework. The data provided is accurate, and the operational protocols have been met successfully.";
        double ocrConfidence = 0.98;
        session.setL2OcrText(demoText);
        session.setL2OcrConfidence(ocrConfidence);

        RubricEvaluationService.RubricResult rubric = rubricEvaluationService.evaluateOpenAnswer(
            demoText, session.getMaterial(), session.getL2Question(), 2
        );
        session.setL2Score(rubric.score());
        session.setL2RubricResultJson(rubric.rubricJson());
        session.setL2NeedsReview(false);

        boolean passed = rubric.score() >= L2_PASS_THRESHOLD;
        session.setState(passed ? "L2_PASSED" : "L2_FAILED");
        if (passed) {
            String topic = session.getMaterial().getDetectedTopic() != null ? session.getMaterial().getDetectedTopic() : "General";
            createEvidence(session.getUser(), "Level 2 Reasoning Assessment", topic, rubric.score(), session.getMaterial().getFilename());
            updateCompetency(session.getUser(), rubric.score());
        }
        sessionRepository.save(session);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("score", rubric.score());
        result.put("passed", passed);
        result.put("state", session.getState());
        result.put("ocrConfidence", ocrConfidence);
        result.put("feedback", rubric.summaryFeedback());
        result.put("topic", session.getMaterial().getDetectedTopic());
        return result;
    }

    @Transactional
    public Map<String, Object> submitLevel2(Long sessionId, MultipartFile answerFile, User user) throws IOException {
        AssessmentSession session = getSessionForUser(sessionId, user);
        if (!"L2_IN_PROGRESS".equals(session.getState()))
            throw new IllegalStateException("Level 2 is not in progress. State: " + session.getState());
        byte[] bytes = answerFile.getBytes();
        String extractedText;
        try {
            try (PDDocument doc = Loader.loadPDF(bytes)) {
                if (doc.isEncrypted()) throw new IllegalArgumentException("Answer PDF is encrypted.");
                PDFTextStripper stripper = new PDFTextStripper();
                extractedText = stripper.getText(doc).trim();
            }
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalArgumentException("Could not read answer PDF: " + e.getMessage()); }

        double ocrConfidence = materialService.calculateTextConfidence(extractedText);
        session.setL2OcrText(extractedText);
        session.setL2OcrConfidence(ocrConfidence);

        if (ocrConfidence < L2_OCR_THRESHOLD || extractedText.length() < 50) {
            session.setL2NeedsReview(true);
            session.setState("L2_REVIEW_REQUIRED");
            session.setL2Score(0);
            sessionRepository.save(session);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sessionId", sessionId); result.put("state", "L2_REVIEW_REQUIRED");
            result.put("ocrConfidence", Math.round(ocrConfidence * 1000.0) / 1000.0);
            result.put("message", "Answer PDF quality insufficient (confidence: " + String.format("%.1f", ocrConfidence * 100) + "%). Upload a clearer scan.");
            return result;
        }

        RubricEvaluationService.RubricResult rubric = rubricEvaluationService.evaluateOpenAnswer(extractedText, session.getMaterial(), session.getL2Question(), 2);
        session.setL2Score(rubric.score()); session.setL2RubricResultJson(rubric.rubricJson()); session.setL2NeedsReview(false);
        boolean passed = rubric.score() >= L2_PASS_THRESHOLD;
        session.setState(passed ? "L2_PASSED" : "L2_FAILED");
        if (passed) { createEvidence(session.getUser(), "Level 2 Reasoning Assessment", session.getMaterial().getDetectedTopic(), rubric.score(), session.getMaterial().getFilename()); updateCompetency(session.getUser(), rubric.score()); }
        sessionRepository.save(session);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId); result.put("score", rubric.score()); result.put("passed", passed); result.put("state", session.getState());
        result.put("ocrConfidence", Math.round(ocrConfidence * 1000.0) / 1000.0); result.put("feedback", rubric.summaryFeedback()); result.put("topic", session.getMaterial().getDetectedTopic());
        return result;
    }

    @Transactional
    public Map<String, Object> startLevel3(Long sessionId, User user) {
        AssessmentSession session = getSessionForUser(sessionId, user);
        if (!"L2_PASSED".equals(session.getState()))
            throw new IllegalStateException("Level 2 must be passed first. State: " + session.getState());
        UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);
        GeneratedQuestion q = mcqGenerationService.generateLevel3Question(session, session.getMaterial(), profile);
        session.setL3Question(q.getQuestionText()); session.setState("L3_IN_PROGRESS");
        sessionRepository.save(session);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionId", sessionId); response.put("state", "L3_IN_PROGRESS");
        response.put("topic", q.getTopic()); response.put("subtopic", q.getSubtopic());
        response.put("question", q.getQuestionText()); response.put("questionId", q.getId());
        response.put("materialName", session.getMaterial().getFilename()); response.put("assessmentType", "Mega Reasoning Assessment");
        return response;
    }

    @Transactional
    public Map<String, Object> submitLevel3(Long sessionId, String answerText, User user) {
        AssessmentSession session = getSessionForUser(sessionId, user);
        if (!"L3_IN_PROGRESS".equals(session.getState()))
            throw new IllegalStateException("Level 3 is not in progress. State: " + session.getState());
        if (answerText == null || answerText.trim().length() < 30)
            throw new IllegalArgumentException("Answer is too short. Please provide a substantive response.");
        RubricEvaluationService.RubricResult rubric = rubricEvaluationService.evaluateOpenAnswer(answerText, session.getMaterial(), session.getL3Question(), 3);
        session.setL3Score(rubric.score()); session.setL3RubricResultJson(rubric.rubricJson());
        session.setState("COMPLETED"); session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);
        createEvidence(session.getUser(), "Level 3 Mega Reasoning Assessment", session.getMaterial().getDetectedTopic(), rubric.score(), session.getMaterial().getFilename());
        updateCompetency(session.getUser(), rubric.score());
        int overall = (int) Math.round((session.getL1Score() * 0.3) + (session.getL2Score() * 0.35) + (rubric.score() * 0.35));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId); result.put("score", rubric.score()); result.put("state", "COMPLETED");
        result.put("feedback", rubric.summaryFeedback()); result.put("topic", session.getMaterial().getDetectedTopic());
        result.put("l1Score", session.getL1Score()); result.put("l2Score", session.getL2Score()); result.put("l3Score", rubric.score()); result.put("overallScore", overall);
        return result;
    }

    public Map<String, Object> getSessionStatus(Long sessionId, User user) {
        AssessmentSession session = getSessionForUser(sessionId, user);
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("sessionId", session.getId()); dto.put("state", session.getState());
        dto.put("topic", session.getMaterial().getDetectedTopic()); dto.put("materialName", session.getMaterial().getFilename());
        dto.put("l1Score", session.getL1Score()); dto.put("l2Score", session.getL2Score()); dto.put("l3Score", session.getL3Score());
        dto.put("l2OcrConfidence", session.getL2OcrConfidence()); dto.put("l2NeedsReview", session.getL2NeedsReview());
        dto.put("startedAt", session.getStartedAt()); dto.put("completedAt", session.getCompletedAt());
        return dto;
    }

    public Optional<Map<String, Object>> getCurrentSession(User user) {
        return sessionRepository.findTopByUserIdOrderByStartedAtDesc(user.getId()).map(session -> {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("sessionId", session.getId()); dto.put("state", session.getState());
            dto.put("topic", session.getMaterial().getDetectedTopic()); dto.put("materialName", session.getMaterial().getFilename());
            dto.put("materialId", session.getMaterial().getId());
            dto.put("l1Score", session.getL1Score()); dto.put("l2Score", session.getL2Score()); dto.put("l3Score", session.getL3Score());
            dto.put("startedAt", session.getStartedAt()); dto.put("completedAt", session.getCompletedAt());
            return dto;
        });
    }

    private AssessmentSession getSessionForUser(Long sessionId, User user) {
        AssessmentSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new RuntimeException("Session not found"));
        if (!session.getUser().getId().equals(user.getId())) throw new SecurityException("Access denied: session does not belong to this user");
        return session;
    }

    private List<Map<String, Object>> buildQuestionDtos(List<GeneratedQuestion> questions) {
        List<Map<String, Object>> dtos = new ArrayList<>();
        for (GeneratedQuestion q : questions) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("questionId", q.getId()); dto.put("questionText", q.getQuestionText());
            dto.put("topic", q.getTopic()); dto.put("subtopic", q.getSubtopic()); dto.put("difficulty", q.getDifficulty());
            try { dto.put("options", objectMapper.readValue(q.getOptionsJson(), List.class)); } catch (Exception e) { dto.put("options", List.of()); }
            // NEVER include correctAnswer
            dtos.add(dto);
        }
        return dtos;
    }



    @Transactional
    public Map<String, Object> demoPassSession(Long sessionId, User user) {
        AssessmentSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new IllegalStateException("Session not found"));
        if (!session.getUser().getId().equals(user.getId())) throw new SecurityException("Unauthorized");
        
        session.setL1Score(100);
        session.setL2Score(100);
        session.setL3Score(100);
        session.setState("CERT_ELIGIBLE");
        
        createEvidence(user, "Demo Skip Assessment", session.getMaterial().getDetectedTopic() != null ? session.getMaterial().getDetectedTopic() : "General", 100, session.getMaterial().getFilename());
        updateCompetency(user, 100);
        
        Map<String, Object> result = new HashMap<>();
        result.put("state", "CERT_ELIGIBLE");
        result.put("overallScore", 100);
        return result;
    }

    private void createEvidence(User user, String levelName, String topic, int score, String materialName) {
        // Old EvidenceItem (for legacy support if any)
        EvidenceItem evidence = new EvidenceItem();
        evidence.setUser(user); evidence.setTitle(levelName + " Passed â€” " + topic);
        evidence.setCategory("ExecutionLab"); evidence.setProvenance("KarmaTute Assessment â€” Source: " + materialName);
        evidence.setConfidenceScore((double) score / 100.0); evidence.setStatus("VERIFIED"); evidence.setVerifiedAt(java.time.LocalDateTime.now());
        evidenceItemRepository.save(evidence);

        // NEW EvidenceRecord (Triggers Data Chamber, Karma DNA, and Certificate Eligibility)
        Long competencyId = 1L; // Map to Public Policy & Circular Analysis
        EvidenceRecord er = EvidenceRecord.builder()
            .userId(user.getId())
            .competencyId(competencyId)
            .type(EvidenceType.WRITTEN_EXECUTION)
            .source("ExecutionLab: " + levelName)
            .sourceRef(materialName)
            .score((double) score)
            .normalizedScore(score / 100.0)
            .confidence(0.95)
            .timestamp(java.time.LocalDateTime.now())
            .provenance("AI Evaluation")
            .ruleVersion("r1.0")
            .engineVersion("e1.0")
            .reviewStatus(ReviewStatus.APPROVED)
            .build();
        evidenceService.submitEvidence(er);
        
        // Try issuing certificate if eligible
        if (score >= 85) {
            try {
                certificateService.issueCertificate(user.getId(), competencyId);
            } catch (Exception e) {
                // Ignore if not eligible yet
            }
        }
    }

    private void updateCompetency(User user, int score) {
        // Legacy user competency update
        List<KarmaTute.KarmaTute.entity.CompetencyGap> openGaps = competencyGapRepository.findByUserId(user.getId()).stream().filter(g -> "OPEN".equals(g.getStatus())).toList();
        for (KarmaTute.KarmaTute.entity.CompetencyGap gap : openGaps) {
            int progress = (int) Math.round(gap.getGapPercentage() * (score / 100.0));
            int newCurrent = Math.min(gap.getCurrentLevel() + progress, gap.getTargetLevel());
            gap.setCurrentLevel(newCurrent);
            if (newCurrent >= gap.getTargetLevel()) { gap.setStatus("RESOLVED"); gap.setGapPercentage(0); } else { gap.setGapPercentage(gap.getTargetLevel() - newCurrent); }
            competencyGapRepository.save(gap);
            userCompetencyRepository.findByUserId(user.getId()).stream().filter(uc -> uc.getCompetency().getId().equals(gap.getCompetency().getId())).findFirst().ifPresent(uc -> {
                uc.setCurrentLevel(newCurrent); uc.setVerified(score >= 70); uc.setLastEvaluatedAt(java.time.LocalDateTime.now()); userCompetencyRepository.save(uc);
            });
        }
    }
}

