package KarmaTute.KarmaTute.karmadna;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

import org.springframework.security.core.Authentication;
import KarmaTute.KarmaTute.entity.User;
import KarmaTute.KarmaTute.repository.UserRepository;
import KarmaTute.KarmaTute.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/karmadna")
@CrossOrigin(origins = "*")
public class KarmaDNAController {
    private final KarmaDNAService karmaDNAService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public KarmaDNAController(KarmaDNAService karmaDNAService, UserRepository userRepository, JwtUtil jwtUtil) {
        this.karmaDNAService = karmaDNAService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/export/me")
    public ResponseEntity<KarmaDNAExportResult> exportData(Authentication auth) throws Exception {
        User user = getAuthenticatedUser(auth);
        return ResponseEntity.ok(karmaDNAService.exportDNA(user.getId()));
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importData(@RequestBody Map<String, String> payload) throws Exception {
        String data = payload.get("karmaData");
        String key = payload.get("secretKey");
        return ResponseEntity.ok(karmaDNAService.importDNA(data, key));
    }

    @PostMapping("/gati-login")
    public ResponseEntity<?> gatiLogin(@RequestBody Map<String, String> payload) {
        try {
            String data = payload.get("karmaData");
            String key = payload.get("secretKey");
            
            Map<String, Object> decrypted = karmaDNAService.importDNA(data, key);
            Map<String, Object> userMap = (Map<String, Object>) decrypted.get("user");
            
            if (userMap == null || !userMap.containsKey("username")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid KarmaDNA format"));
            }
            
            String username = (String) userMap.get("username");
            User user = userRepository.findByUsername(username).orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found in system. Please use standard login first to create profile."));
            }
            
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole(), user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", user.getRole());
            response.put("profilePreview", decrypted.get("userProfile"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Decryption failed. Invalid file or secret key."));
        }
    }
}
