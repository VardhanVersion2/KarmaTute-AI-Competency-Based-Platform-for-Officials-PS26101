package KarmaTute.KarmaTute.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "user_learning_preferences")
public class UserLearningPreferences {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    private String learningDuration; // e.g. "30 min"
    private String preferredDays;
    private String preferredTime;
    private String preferredLearningFormat; // e.g. "practical"
    private String preferredLanguage; // e.g. "English"
    private String difficultyPreference; // e.g. "gradual"
    
    @Column(length = 500)
    private String learningBarriers;
    
    @Column(length = 1000)
    private String specialDirections;

    public UserLearningPreferences() {}

    public UserLearningPreferences(User user) {
        this.user = user;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getLearningDuration() { return learningDuration; }
    public void setLearningDuration(String learningDuration) { this.learningDuration = learningDuration; }

    public String getPreferredDays() { return preferredDays; }
    public void setPreferredDays(String preferredDays) { this.preferredDays = preferredDays; }

    public String getPreferredTime() { return preferredTime; }
    public void setPreferredTime(String preferredTime) { this.preferredTime = preferredTime; }

    public String getPreferredLearningFormat() { return preferredLearningFormat; }
    public void setPreferredLearningFormat(String preferredLearningFormat) { this.preferredLearningFormat = preferredLearningFormat; }

    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

    public String getDifficultyPreference() { return difficultyPreference; }
    public void setDifficultyPreference(String difficultyPreference) { this.difficultyPreference = difficultyPreference; }

    public String getLearningBarriers() { return learningBarriers; }
    public void setLearningBarriers(String learningBarriers) { this.learningBarriers = learningBarriers; }

    public String getSpecialDirections() { return specialDirections; }
    public void setSpecialDirections(String specialDirections) { this.specialDirections = specialDirections; }
}
