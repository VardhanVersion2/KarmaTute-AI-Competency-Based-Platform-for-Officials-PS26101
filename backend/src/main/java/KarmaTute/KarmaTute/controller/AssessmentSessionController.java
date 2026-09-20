package KarmaTute.KarmaTute.controller;
import KarmaTute.KarmaTute.entity.User;
import KarmaTute.KarmaTute.repository.UserRepository;
import KarmaTute.KarmaTute.service.AssessmentSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
@RestController
@RequestMapping("/api/assessment-session")
@CrossOrigin(origins = "*")
public class AssessmentSessionController {
    private final AssessmentSessionService sessionService;
    private final UserRepository userRepository;
    public AssessmentSessionController(AssessmentSessionService sessionService, UserRepository userRepository) {
        this.sessionService = sessionService; this.userRepository = userRepository;
    }
    private User getAuthenticatedUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));
    }
    @PostMapping("/start")
    public ResponseEntity<?> startLevel1(@RequestBody Map<String, Long> body, Authentication auth) {
        try {
            User user = getAuthenticatedUser(auth);
            Long materialId = body.get("materialId");
            if (materialId == null) return ResponseEntity.badRequest().body(Map.of("error", "materialId required"));
            return ResponseEntity.ok(sessionService.startLevel1(materialId, user));
        } catch (IllegalArgumentException | IllegalStateException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (SecurityException e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())); }
    }
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentSession(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        return sessionService.getCurrentSession(user).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable Long sessionId, Authentication auth) {
        try { return ResponseEntity.ok(sessionService.getSessionStatus(sessionId, getAuthenticatedUser(auth))); }
        catch (SecurityException e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return ResponseEntity.notFound().build(); }
    }
    @PostMapping("/{sessionId}/l1/submit")
    public ResponseEntity<?> submitL1(@PathVariable Long sessionId, @RequestBody Map<Long, String> answers, Authentication auth) {
        try { return ResponseEntity.ok(sessionService.submitLevel1(sessionId, answers, getAuthenticatedUser(auth))); }
        catch (IllegalStateException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (SecurityException e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())); }
    }
    @PostMapping("/{sessionId}/l2/start")
    public ResponseEntity<?> startL2(@PathVariable Long sessionId, Authentication auth) {
        try { return ResponseEntity.ok(sessionService.startLevel2(sessionId, getAuthenticatedUser(auth))); }
        catch (IllegalStateException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (SecurityException e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())); }
    }


    @PostMapping("/{sessionId}/l2/demo-submit")
    public ResponseEntity<?> demoSubmitL2(@PathVariable Long sessionId, Authentication auth) {
        try { return ResponseEntity.ok(sessionService.demoSubmitLevel2(sessionId, getAuthenticatedUser(auth))); }
        catch (IllegalArgumentException | IllegalStateException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (SecurityException e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{sessionId}/l2/submit")
    public ResponseEntity<?> submitL2(@PathVariable Long sessionId, @RequestParam("answerFile") MultipartFile answerFile, Authentication auth) {
        try { return ResponseEntity.ok(sessionService.submitLevel2(sessionId, answerFile, getAuthenticatedUser(auth))); }
        catch (IllegalArgumentException | IllegalStateException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (SecurityException e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())); }
    }
    @PostMapping("/{sessionId}/l3/start")
    public ResponseEntity<?> startL3(@PathVariable Long sessionId, Authentication auth) {
        try { return ResponseEntity.ok(sessionService.startLevel3(sessionId, getAuthenticatedUser(auth))); }
        catch (IllegalStateException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (SecurityException e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())); }
    }
    @PostMapping("/{sessionId}/l3/submit")
    public ResponseEntity<?> submitL3(@PathVariable Long sessionId, @RequestBody Map<String, String> body, Authentication auth) {
        try {
            String answerText = body.get("answerText");
            if (answerText == null || answerText.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "answerText required"));
            return ResponseEntity.ok(sessionService.submitLevel3(sessionId, answerText, getAuthenticatedUser(auth)));
        } catch (IllegalArgumentException | IllegalStateException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (SecurityException e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{sessionId}/demo-pass")
    public ResponseEntity<?> demoPass(@PathVariable Long sessionId, Authentication auth) {
        try {
            return ResponseEntity.ok(sessionService.demoPassSession(sessionId, getAuthenticatedUser(auth)));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}

