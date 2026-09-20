package KarmaTute.KarmaTute.entity;
import jakarta.persistence.*;
@Entity
@Table(name = "generated_questions")
public class GeneratedQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AssessmentSession session;
    private Integer level;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;
    @Column(columnDefinition = "TEXT")
    private String optionsJson;
    @Column(nullable = false)
    private String correctAnswer;
    private String topic;
    private String subtopic;
    private String difficulty;
    private Integer sourcePage;
    @Column(columnDefinition = "TEXT")
    private String sourceChunk;
    private String validationStatus;
    private String questionType;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public AssessmentSession getSession() { return session; } public void setSession(AssessmentSession session) { this.session = session; }
    public Integer getLevel() { return level; } public void setLevel(Integer level) { this.level = level; }
    public String getQuestionText() { return questionText; } public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getOptionsJson() { return optionsJson; } public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
    public String getCorrectAnswer() { return correctAnswer; } public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public String getTopic() { return topic; } public void setTopic(String topic) { this.topic = topic; }
    public String getSubtopic() { return subtopic; } public void setSubtopic(String subtopic) { this.subtopic = subtopic; }
    public String getDifficulty() { return difficulty; } public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public Integer getSourcePage() { return sourcePage; } public void setSourcePage(Integer sourcePage) { this.sourcePage = sourcePage; }
    public String getSourceChunk() { return sourceChunk; } public void setSourceChunk(String sourceChunk) { this.sourceChunk = sourceChunk; }
    public String getValidationStatus() { return validationStatus; } public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public String getQuestionType() { return questionType; } public void setQuestionType(String questionType) { this.questionType = questionType; }
}
