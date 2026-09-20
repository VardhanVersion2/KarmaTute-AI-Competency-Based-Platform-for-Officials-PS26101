package KarmaTute.KarmaTute.controller;

import KarmaTute.KarmaTute.competency.*;
import KarmaTute.KarmaTute.entity.User;
import KarmaTute.KarmaTute.repository.UserRepository;
import KarmaTute.KarmaTute.certificate.Certificate;
import KarmaTute.KarmaTute.certificate.CertificateRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/dev")
@CrossOrigin(origins = "*")
public class DevController {

    private final UserRepository userRepository;
    private final CompetencySnapshotRepository snapshotRepository;
    private final CertificateRepository certificateRepository;

    public DevController(UserRepository userRepository, CompetencySnapshotRepository snapshotRepository, CertificateRepository certificateRepository) {
        this.userRepository = userRepository;
        this.snapshotRepository = snapshotRepository;
        this.certificateRepository = certificateRepository;
    }

    @GetMapping("/fill-dummy-data")
    @Transactional
    public String fillDummyData() {
        User user = userRepository.findByUsername("rajesh.kumar").orElseThrow();
        Long competencyId = 1L;

        CompetencySnapshot latest = snapshotRepository.findByUserIdAndCompetencyIdOrderByTimestampDesc(user.getId(), competencyId).stream().findFirst().orElse(null);
        
        CompetencySnapshot newSnap = CompetencySnapshot.builder()
            .userId(user.getId())
            .competencyId(competencyId)
            
            .current(100.0)
            .evidenceIds("demo-123")
            .timestamp(java.time.LocalDateTime.now())
            .engineVersion("demo")
            .build();
        snapshotRepository.save(newSnap);

        Certificate cert = Certificate.builder()
            .certificateId("KT-" + user.getId() + "-" + competencyId + "-" + System.currentTimeMillis())
            .userId(user.getId())
            .competencyId(competencyId)
            .achievedScore(100.0)
            .requiredScore(85.0)
            .competencySummary("Digital Literacy [Technology]")
            .evidenceSnapshot("demo-123")
            .integrityHash("demo-hash")
            .status(KarmaTute.KarmaTute.certificate.CertificateStatus.ISSUED)
            .issuedAt(java.time.LocalDateTime.now())
            .build();
        certificateRepository.save(cert);
        
        return "SUCCESS! Dummy data filled for user rajesh.kumar. Go check Karma DNA and Growth Proof pages!";
    }
}
