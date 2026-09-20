package KarmaTute.KarmaTute.service;

import KarmaTute.KarmaTute.aigateway.AIGatewayRequest;
import KarmaTute.KarmaTute.aigateway.AIGatewayService;
import KarmaTute.KarmaTute.aigateway.AITaskType;
import KarmaTute.KarmaTute.entity.AssessmentMaterial;
import KarmaTute.KarmaTute.entity.User;
import KarmaTute.KarmaTute.repository.AssessmentMaterialRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AssessmentMaterialService {
    private static final Logger log = LoggerFactory.getLogger(AssessmentMaterialService.class);
    private static final long MAX_SIZE_BYTES = 50L * 1024 * 1024;
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};

    private final AssessmentMaterialRepository materialRepository;
    private final AIGatewayService aiGatewayService;
    private final ObjectMapper objectMapper;

    public AssessmentMaterialService(AssessmentMaterialRepository materialRepository, AIGatewayService aiGatewayService) {
        this.materialRepository = materialRepository;
        this.aiGatewayService = aiGatewayService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public AssessmentMaterial uploadAndProcess(MultipartFile file, User user) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf"))
            throw new IllegalArgumentException("Only PDF files are accepted. Received: " + originalName);
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf"))
            throw new IllegalArgumentException("Invalid content type: " + contentType + ". Only application/pdf accepted.");
        if (file.getSize() > MAX_SIZE_BYTES)
            throw new IllegalArgumentException("File exceeds 50 MB limit.");

        byte[] bytes = file.getBytes();
        if (bytes.length < 4 || bytes[0] != PDF_MAGIC[0] || bytes[1] != PDF_MAGIC[1] || bytes[2] != PDF_MAGIC[2] || bytes[3] != PDF_MAGIC[3])
            throw new IllegalArgumentException("File is not a valid PDF (magic bytes check failed).");

        String fileHash = computeSHA256(bytes);
        Optional<AssessmentMaterial> existing = materialRepository.findByFileHash(fileHash);
        if (existing.isPresent() && existing.get().getUser().getId().equals(user.getId()))
            return existing.get();

        AssessmentMaterial material = new AssessmentMaterial();
        material.setUser(user);
        material.setFilename(originalName);
        material.setFileHash(fileHash);
        material.setFileSizeBytes(file.getSize());
        material.setUploadedAt(LocalDateTime.now());
        material.setProcessingStatus("PENDING");
        material = materialRepository.save(material);

        try {
            Map<Integer, String> pageTextMap = extractTextWithPDFBox(bytes);
            if (pageTextMap.isEmpty()) {
                material.setProcessingStatus("FAILED");
                material.setExtractionError("Material could not be reliably processed. No readable text found. Please upload a clearer/readable PDF.");
                return materialRepository.save(material);
            }
            material.setPageCount(pageTextMap.size());
            StringBuilder fullText = new StringBuilder();
            List<Map<String, Object>> chunks = new ArrayList<>();
            for (Map.Entry<Integer, String> entry : pageTextMap.entrySet()) {
                fullText.append(entry.getValue()).append("\n\n");
                Map<String, Object> chunk = new LinkedHashMap<>();
                chunk.put("page", entry.getKey());
                chunk.put("text", entry.getValue());
                chunks.add(chunk);
            }
            String fullTextStr = fullText.toString().trim();
            material.setFullExtractedText(fullTextStr);
            material.setChunkedTextJson(objectMapper.writeValueAsString(chunks));

            String topicContext = fullTextStr.length() > 8000 ? fullTextStr.substring(0, 8000) : fullTextStr;
            try {
                var topicResponse = aiGatewayService.executeTask(
                    AIGatewayRequest.builder()
                        .taskType(AITaskType.DOCUMENT_UNDERSTANDING)
                        .systemPrompt("You are a document analysis expert. Analyze the provided text and extract the primary topic and secondary topics. Return a valid JSON object with keys: primaryTopic (string - be specific, e.g. 'Survey Sampling and Stratification Methods'), secondaryTopics (array of strings, max 5). Return ONLY the JSON object, no other text.")
                        .userPrompt("Extract topics from this document:\n\n" + topicContext)
                        .build());
                String topicJson = ((String) topicResponse.getData()).replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                Map<String, Object> topicData = objectMapper.readValue(topicJson, Map.class);
                material.setDetectedTopic((String) topicData.get("primaryTopic"));
                material.setDetectedTopicsJson(objectMapper.writeValueAsString(topicData));
            } catch (Exception e) {
                log.warn("AI topic detection failed: {}", e.getMessage());
                String firstLine = fullTextStr.lines().filter(l -> !l.isBlank()).findFirst().orElse("Document");
                material.setDetectedTopic(firstLine.length() > 100 ? firstLine.substring(0, 100) : firstLine);
            }
            material.setProcessingStatus("READY");
        } catch (IllegalArgumentException e) {
            material.setProcessingStatus("FAILED");
            material.setExtractionError(e.getMessage());
        } catch (Exception e) {
            log.error("PDF extraction failed: {}", e.getMessage());
            material.setProcessingStatus("FAILED");
            material.setExtractionError("Material could not be reliably processed. Error: " + e.getMessage());
        }
        return materialRepository.save(material);
    }

    private Map<Integer, String> extractTextWithPDFBox(byte[] bytes) throws IOException {
        Map<Integer, String> pageTexts = new LinkedHashMap<>();
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            if (doc.isEncrypted())
                throw new IllegalArgumentException("Material could not be reliably processed. PDF is password-protected.");
            if (doc.getNumberOfPages() == 0)
                throw new IllegalArgumentException("Material could not be reliably processed. PDF has no pages.");
            PDFTextStripper stripper = new PDFTextStripper();
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(doc).trim();
                if (!text.isBlank()) pageTexts.put(i, text);
            }
        }
        return pageTexts;
    }

    private String computeSHA256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    public double calculateTextConfidence(String text) {
        if (text == null || text.isBlank()) return 0.0;
        int total = text.length();
        long letters = text.chars().filter(Character::isLetter).count();
        long words = Arrays.stream(text.split("\\s+")).filter(w -> !w.isBlank()).count();
        long sentences = text.chars().filter(c -> c == '.' || c == '!' || c == '?').count();
        double letterRatio = (double) letters / total;
        double avgWordLen = words > 0 ? (double) letters / words : 0;
        double sentenceRatio = words > 0 ? (double) sentences / words : 0;
        double confidence = Math.min(letterRatio * 0.5, 0.5)
            + Math.min((avgWordLen / 8.0) * 0.25, 0.25)
            + Math.min(sentenceRatio * 5 * 0.15, 0.15)
            + Math.min((Math.log(words + 1) / Math.log(500)) * 0.10, 0.10);
        return Math.min(0.99, Math.max(0.0, confidence));
    }

    public List<AssessmentMaterial> getMaterialsForUser(Long userId) {
        return materialRepository.findByUserId(userId);
    }

    public Optional<AssessmentMaterial> getMaterialById(Long materialId, Long userId) {
        return materialRepository.findById(materialId).filter(m -> m.getUser().getId().equals(userId));
    }

    public AssessmentMaterial save(AssessmentMaterial material) {
        return materialRepository.save(material);
    }
}
