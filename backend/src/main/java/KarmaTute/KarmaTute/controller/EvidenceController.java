package KarmaTute.KarmaTute.controller;

import KarmaTute.KarmaTute.competency.EvidenceRecord;
import KarmaTute.KarmaTute.dto.CompetencySnapshotDto;
import KarmaTute.KarmaTute.service.EvidenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.security.core.Authentication;
import KarmaTute.KarmaTute.entity.User;
import KarmaTute.KarmaTute.repository.UserRepository;

@RestController
@RequestMapping("/api/evidence")
@CrossOrigin(origins = "*")
public class EvidenceController {

    private final EvidenceService evidenceService;
    private final UserRepository userRepository;

    public EvidenceController(EvidenceService evidenceService, UserRepository userRepository) {
        this.evidenceService = evidenceService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    public ResponseEntity<EvidenceRecord> submitEvidence(@RequestBody EvidenceRecord record, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        record.setUserId(user.getId());
        return ResponseEntity.ok(evidenceService.submitEvidence(record));
    }

    @GetMapping("/me")
    public ResponseEntity<List<EvidenceRecord>> getUserEvidence(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        return ResponseEntity.ok(evidenceService.getUserEvidence(user.getId()));
    }

    @GetMapping("/me/competency-snapshot")
    public ResponseEntity<List<CompetencySnapshotDto>> getCompetencySnapshots(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        return ResponseEntity.ok(evidenceService.getCompetencySnapshots(user.getId()));
    }

    @GetMapping("/me/competency-snapshot/competency/{competencyId}")
    public ResponseEntity<List<CompetencySnapshotDto>> getCompetencyHistory(
        Authentication auth, @PathVariable Long competencyId) {
        User user = getAuthenticatedUser(auth);
        return ResponseEntity.ok(evidenceService.getCompetencyHistory(user.getId(), competencyId));
    }
}
