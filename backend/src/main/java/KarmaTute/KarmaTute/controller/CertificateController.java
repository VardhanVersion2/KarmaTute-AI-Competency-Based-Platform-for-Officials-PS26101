package KarmaTute.KarmaTute.controller;

import KarmaTute.KarmaTute.certificate.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import org.springframework.security.core.Authentication;
import KarmaTute.KarmaTute.entity.User;
import KarmaTute.KarmaTute.repository.UserRepository;

@RestController
@RequestMapping("/api/certificate")
@CrossOrigin(origins = "*")
public class CertificateController {
    private final CertificateService certificateService;
    private final UserRepository userRepository;

    public CertificateController(CertificateService certificateService, UserRepository userRepository) {
        this.certificateService = certificateService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/me/eligibility/{competencyId}")
    public ResponseEntity<CertificateEligibilityResult> checkEligibility(
            Authentication auth, @PathVariable Long competencyId) {
        User user = getAuthenticatedUser(auth);
        return ResponseEntity.ok(certificateService.checkEligibility(user.getId(), competencyId));
    }

    @PostMapping("/me/issue/{competencyId}")
    public ResponseEntity<Certificate> issueCertificate(
            Authentication auth, @PathVariable Long competencyId) {
        User user = getAuthenticatedUser(auth);
        try {
            return ResponseEntity.ok(certificateService.issueCertificate(user.getId(), competencyId));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<List<Certificate>> getUserCertificates(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        return ResponseEntity.ok(certificateService.getUserCertificates(user.getId()));
    }

    @GetMapping("/verify/{certificateId}")
    public ResponseEntity<Certificate> verifyCertificate(@PathVariable String certificateId) {
        return certificateService.verifyCertificate(certificateId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
