package KarmaTute.KarmaTute.config;

import KarmaTute.KarmaTute.entity.*;
import KarmaTute.KarmaTute.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

@Component
public class DataInitializer implements ApplicationRunner {

    @Value("${karmatute.demo.mode:false}")
    private boolean demoMode;

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final CompetencyRepository competencyRepository;
    private final UserCompetencyRepository userCompetencyRepository;
    private final CompetencyGapRepository competencyGapRepository;
    private final NextBestActionRepository nextBestActionRepository;
    private final EvidenceItemRepository evidenceItemRepository;

    public DataInitializer(
        UserRepository userRepository,
        CompetencyRepository competencyRepository,
        UserCompetencyRepository userCompetencyRepository,
        CompetencyGapRepository competencyGapRepository,
        NextBestActionRepository nextBestActionRepository,
        EvidenceItemRepository evidenceItemRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.competencyRepository = competencyRepository;
        this.userCompetencyRepository = userCompetencyRepository;
        this.competencyGapRepository = competencyGapRepository;
        this.nextBestActionRepository = nextBestActionRepository;
        this.evidenceItemRepository = evidenceItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!demoMode) {
            System.out.println("Normal mode: skipping demo data seeding.");
            return;
        }


        if (userRepository.count() > 0) {
            return;
        }
        System.out.println("DEMO MODE ENABLED: Seeding Rajesh Kumar demo user.");

        // 1. Seed User
        User learner = new User(
            "rajesh.kumar",
            passwordEncoder.encode("demo123"),
            "Rajesh Kumar",
            "ROLE_LEARNER"
        );
        UserProfile profile = new UserProfile(learner);
        profile.setDepartment("Department of Administrative Reforms & Public Grievances");
        profile.setDesignation("Section Officer");
        profile.setTargetRole("Under Secretary - Digital Governance");
        learner.setProfile(profile);
        
        learner = userRepository.save(learner);

        // 2. Seed Competencies
        Competency policyComp = new Competency(
            "COMP-GOV-01",
            "Public Policy & Circular Analysis",
            "GOVERNANCE",
            "Rigorous analysis and contextual synthesis of gazette notifications and policy circulars.",
            85
        );
        policyComp = competencyRepository.save(policyComp);

        Competency dpdpComp = new Competency(
            "COMP-TECH-02",
            "DPDP Act & Digital Privacy Compliance",
            "TECHNICAL",
            "Implementation of Digital Personal Data Protection Act compliance protocols and consent registries.",
            90
        );
        dpdpComp = competencyRepository.save(dpdpComp);

        
        Competency dataQualityComp = competencyRepository.findByCode("COMP-STAT-03").orElseGet(() -> {
            return null;
        });


        Competency ethicsComp = new Competency(
            "COMP-OPS-04",
            "Mission Karmayogi Digital Workplace Ethics",
            "OPERATIONAL",
            "Civil service behavioral integrity and transparency in digital administration.",
            95
        );
        ethicsComp = competencyRepository.save(ethicsComp);

        // 3. Seed User Competencies
        userCompetencyRepository.save(new UserCompetency(learner, policyComp, 45, true, LocalDateTime.now().minusDays(2)));
        userCompetencyRepository.save(new UserCompetency(learner, dpdpComp, 55, false, LocalDateTime.now().minusDays(5)));
        userCompetencyRepository.save(new UserCompetency(learner, dataQualityComp, 82, true, LocalDateTime.now().minusDays(10)));
        userCompetencyRepository.save(new UserCompetency(learner, ethicsComp, 95, true, LocalDateTime.now().minusDays(15)));

        // 4. Seed Competency Gaps
        competencyGapRepository.save(new CompetencyGap(learner, policyComp, 45, 85, 40, "HIGH", "OPEN"));
        competencyGapRepository.save(new CompetencyGap(learner, dpdpComp, 55, 90, 35, "HIGH", "OPEN"));
        competencyGapRepository.save(new CompetencyGap(learner, dataQualityComp, 82, 80, 0, "LOW", "RESOLVED"));
        competencyGapRepository.save(new CompetencyGap(learner, ethicsComp, 95, 95, 0, "LOW", "RESOLVED"));

        // 5. Seed Next Best Action
        nextBestActionRepository.save(new NextBestAction(
            learner,
            policyComp,
            "Executive Briefing: Public Policy Analysis on Mission Karmayogi Guidelines",
            "Engage in an 8-minute interactive voice simulation analyzing Gazette Notification on Civil Services Capacity Building.",
            "Your target role (Under Secretary) requires Level 4 circular analysis proficiency. Resolving this priority gap will bridge 40% of your benchmark deficit and upgrade your verified ledger.",
            "VOICE_SIMULATION",
            8,
            "execution-lab",
            "RECOMMENDED"
        ));

        // 6. Seed Evidence Items
        evidenceItemRepository.save(new EvidenceItem(
            learner,
            "iGOT Module 4: Digital India Governance Standards",
            "CERTIFICATE",
            "iGOT Karmayogi Portal",
            0.98,
            "VERIFIED",
            LocalDateTime.now().minusDays(3)
        ));
        evidenceItemRepository.save(new EvidenceItem(
            learner,
            "TPAC Verified Assessment: Level 2 Administrative Reasoning",
            "TPAC",
            "NSSTA Framework",
            0.94,
            "VERIFIED",
            LocalDateTime.now().minusDays(7)
        ));
        evidenceItemRepository.save(new EvidenceItem(
            learner,
            "Departmental Policy Synthesis Paper - Q3.pdf",
            "DOCUMENT",
            "DARPG Internal Document Store",
            0.72,
            "PENDING_REVIEW",
            LocalDateTime.now().minusDays(1)
        ));
    }
}

