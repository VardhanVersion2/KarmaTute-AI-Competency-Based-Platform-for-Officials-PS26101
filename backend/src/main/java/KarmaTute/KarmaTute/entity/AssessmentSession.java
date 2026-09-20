package KarmaTute.KarmaTute.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "assessment_sessions")
public class AssessmentSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private AssessmentMaterial material;
    private String state = "L1_READY";
    private Integer l1Score;
    private Integer l2Score;
    private Integer l3Score;
    @Column(columnDefinition = "TEXT")
    private String l2Question;
    @Column(columnDefinition = "TEXT")
    private String l3Question;
    @Column(columnDefinition = "TEXT")
    private String l2OcrText;
    private Double l2OcrConfidence;
    private Boolean l2NeedsReview;
    @Column(columnDefinition = "TEXT")
    private String l2RubricResultJson;
    @Column(columnDefinition = "TEXT")
    private String l3RubricResultJson;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public User getUser() { return user; } public void setUser(User user) { this.user = user; }
    public AssessmentMaterial getMaterial() { return material; } public void setMaterial(AssessmentMaterial material) { this.material = material; }
    public String getState() { return state; } public void setState(String state) { this.state = state; }
    public Integer getL1Score() { return l1Score; } public void setL1Score(Integer l1Score) { this.l1Score = l1Score; }
    public Integer getL2Score() { return l2Score; } public void setL2Score(Integer l2Score) { this.l2Score = l2Score; }
    public Integer getL3Score() { return l3Score; } public void setL3Score(Integer l3Score) { this.l3Score = l3Score; }
    public String getL2Question() { return l2Question; } public void setL2Question(String l2Question) { this.l2Question = l2Question; }
    public String getL3Question() { return l3Question; } public void setL3Question(String l3Question) { this.l3Question = l3Question; }
    public String getL2OcrText() { return l2OcrText; } public void setL2OcrText(String l2OcrText) { this.l2OcrText = l2OcrText; }
    public Double getL2OcrConfidence() { return l2OcrConfidence; } public void setL2OcrConfidence(Double l2OcrConfidence) { this.l2OcrConfidence = l2OcrConfidence; }
    public Boolean getL2NeedsReview() { return l2NeedsReview; } public void setL2NeedsReview(Boolean l2NeedsReview) { this.l2NeedsReview = l2NeedsReview; }
    public String getL2RubricResultJson() { return l2RubricResultJson; } public void setL2RubricResultJson(String l2RubricResultJson) { this.l2RubricResultJson = l2RubricResultJson; }
    public String getL3RubricResultJson() { return l3RubricResultJson; } public void setL3RubricResultJson(String l3RubricResultJson) { this.l3RubricResultJson = l3RubricResultJson; }
    public LocalDateTime getStartedAt() { return startedAt; } public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; } public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
