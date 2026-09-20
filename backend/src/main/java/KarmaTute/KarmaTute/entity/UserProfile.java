package KarmaTute.KarmaTute.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    private String designation;
    private String department;
    private String organisation;
    private String currentAssignment;
    private Integer yearsOfExperience;
    private String highestEducation;
    
    @Column(length = 500)
    private String educationDetails;
    
    private String primaryDomain;
    private String secondaryDomains;

    @Column(length = 5000)
    private String currentResponsibilities;

    @Column(length = 1000)
    private String keySkills;

    @Column(length = 1000)
    private String toolsTechnologies;

    @Column(length = 1000)
    private String certifications;

    private String targetRole;

    @Column(length = 2000)
    private String careerGoals;

    public UserProfile() {}

    public UserProfile(User user) {
        this.user = user;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getOrganisation() { return organisation; }
    public void setOrganisation(String organisation) { this.organisation = organisation; }
    
    public String getCurrentAssignment() { return currentAssignment; }
    public void setCurrentAssignment(String currentAssignment) { this.currentAssignment = currentAssignment; }
    
    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }
    
    public String getHighestEducation() { return highestEducation; }
    public void setHighestEducation(String highestEducation) { this.highestEducation = highestEducation; }
    
    public String getEducationDetails() { return educationDetails; }
    public void setEducationDetails(String educationDetails) { this.educationDetails = educationDetails; }
    
    public String getPrimaryDomain() { return primaryDomain; }
    public void setPrimaryDomain(String primaryDomain) { this.primaryDomain = primaryDomain; }
    
    public String getSecondaryDomains() { return secondaryDomains; }
    public void setSecondaryDomains(String secondaryDomains) { this.secondaryDomains = secondaryDomains; }
    
    public String getCurrentResponsibilities() { return currentResponsibilities; }
    public void setCurrentResponsibilities(String currentResponsibilities) { this.currentResponsibilities = currentResponsibilities; }
    
    public String getKeySkills() { return keySkills; }
    public void setKeySkills(String keySkills) { this.keySkills = keySkills; }
    
    public String getToolsTechnologies() { return toolsTechnologies; }
    public void setToolsTechnologies(String toolsTechnologies) { this.toolsTechnologies = toolsTechnologies; }
    
    public String getCertifications() { return certifications; }
    public void setCertifications(String certifications) { this.certifications = certifications; }
    
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    
    public String getCareerGoals() { return careerGoals; }
    public void setCareerGoals(String careerGoals) { this.careerGoals = careerGoals; }
}
