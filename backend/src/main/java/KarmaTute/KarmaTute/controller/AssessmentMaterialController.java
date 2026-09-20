package KarmaTute.KarmaTute.controller;
import KarmaTute.KarmaTute.entity.AssessmentMaterial;
import KarmaTute.KarmaTute.entity.User;
import KarmaTute.KarmaTute.repository.UserRepository;
import KarmaTute.KarmaTute.service.AssessmentMaterialService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/assessment-material")
@CrossOrigin(origins = "*")
public class AssessmentMaterialController {
    private final AssessmentMaterialService materialService;
    private final UserRepository userRepository;
    public AssessmentMaterialController(AssessmentMaterialService materialService, UserRepository userRepository) {
        this.materialService = materialService; this.userRepository = userRepository;
    }
    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
    }
    @PostMapping("/upload")
    public ResponseEntity<?> uploadMaterial(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            AssessmentMaterial material = materialService.uploadAndProcess(file, user);
            return ResponseEntity.ok(Map.of(
                "materialId", material.getId(), "filename", material.getFilename(),
                "status", material.getProcessingStatus(),
                "detectedTopic", material.getDetectedTopic() != null ? material.getDetectedTopic() : "",
                "pageCount", material.getPageCount() != null ? material.getPageCount() : 0,
                "error", material.getExtractionError() != null ? material.getExtractionError() : ""
            ));
        } catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed: " + e.getMessage())); }
    }
    @GetMapping("/my")
    public ResponseEntity<List<AssessmentMaterial>> getMyMaterials(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(materialService.getMaterialsForUser(user.getId()));
    }
    @GetMapping("/{materialId}/status")
    public ResponseEntity<?> getMaterialStatus(@PathVariable Long materialId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return materialService.getMaterialById(materialId, user.getId())
                .map(m -> ResponseEntity.ok(Map.of("materialId", m.getId(), "status", m.getProcessingStatus(), "detectedTopic", m.getDetectedTopic() != null ? m.getDetectedTopic() : "", "error", m.getExtractionError() != null ? m.getExtractionError() : "")))
                .orElse(ResponseEntity.notFound().build());
    }

}
