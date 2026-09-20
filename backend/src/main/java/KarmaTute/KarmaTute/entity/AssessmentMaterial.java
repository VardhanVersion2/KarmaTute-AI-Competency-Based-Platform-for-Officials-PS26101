package KarmaTute.KarmaTute.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "assessment_materials")
public class AssessmentMaterial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false)
    private String filename;
    @Column(nullable = false, unique = true)
    private String fileHash;
    private Long fileSizeBytes;
    private Integer pageCount;
    private LocalDateTime uploadedAt;
    private String processingStatus = "PENDING";
    @Column(columnDefinition = "TEXT")
    private String extractionError;
    private String detectedTopic;
    @Column(columnDefinition = "TEXT")
    private String detectedTopicsJson;
    @Column(columnDefinition = "TEXT")
    private String chunkedTextJson;
    @Column(columnDefinition = "TEXT")
    private String fullExtractedText;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public User getUser() { return user; } public void setUser(User user) { this.user = user; }
    public String getFilename() { return filename; } public void setFilename(String filename) { this.filename = filename; }
    public String getFileHash() { return fileHash; } public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public Long getFileSizeBytes() { return fileSizeBytes; } public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public Integer getPageCount() { return pageCount; } public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public LocalDateTime getUploadedAt() { return uploadedAt; } public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getProcessingStatus() { return processingStatus; } public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }
    public String getExtractionError() { return extractionError; } public void setExtractionError(String extractionError) { this.extractionError = extractionError; }
    public String getDetectedTopic() { return detectedTopic; } public void setDetectedTopic(String detectedTopic) { this.detectedTopic = detectedTopic; }
    public String getDetectedTopicsJson() { return detectedTopicsJson; } public void setDetectedTopicsJson(String detectedTopicsJson) { this.detectedTopicsJson = detectedTopicsJson; }
    public String getChunkedTextJson() { return chunkedTextJson; } public void setChunkedTextJson(String chunkedTextJson) { this.chunkedTextJson = chunkedTextJson; }
    public String getFullExtractedText() { return fullExtractedText; } public void setFullExtractedText(String fullExtractedText) { this.fullExtractedText = fullExtractedText; }
}
