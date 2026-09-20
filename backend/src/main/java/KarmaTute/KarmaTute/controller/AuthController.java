package KarmaTute.KarmaTute.controller;

import KarmaTute.KarmaTute.entity.User;
import KarmaTute.KarmaTute.repository.UserRepository;
import KarmaTute.KarmaTute.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final KarmaTute.KarmaTute.repository.UserProfileRepository userProfileRepository;
    private final KarmaTute.KarmaTute.repository.UserLearningPreferencesRepository preferencesRepository;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                          KarmaTute.KarmaTute.repository.UserProfileRepository userProfileRepository,
                          KarmaTute.KarmaTute.repository.UserLearningPreferencesRepository preferencesRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userProfileRepository = userProfileRepository;
        this.preferencesRepository = preferencesRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Username and password are required"));
        }

        username = username.trim().toLowerCase();

        java.util.Optional<User> optionalUser = userRepository.findByUsername(username);
        
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(password, user.getPasswordHash())) {
                String token = jwtUtil.generateToken(user.getUsername(), user.getRole(), user.getId());
                return ResponseEntity.ok(Map.of("token", token, "role", user.getRole()));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        } else {
            // Auto-provision new Officer / Learner account on first login
            String role = username.contains("admin") ? "ROLE_ADMIN" : "ROLE_LEARNER";
            String fullName = username.contains("@") ? username.split("@")[0] : username;
            fullName = Character.toUpperCase(fullName.charAt(0)) + fullName.substring(1);

            User newUser = new User(
                username,
                passwordEncoder.encode(password),
                fullName,
                role
            );
            newUser.setProfileStatus("PENDING");
            newUser = userRepository.save(newUser);

            // Auto-provision empty profile and preferences to prevent NullPointerExceptions
            KarmaTute.KarmaTute.entity.UserProfile profile = new KarmaTute.KarmaTute.entity.UserProfile(newUser);
            userProfileRepository.save(profile);
            newUser.setProfile(profile);
            userRepository.save(newUser);

            KarmaTute.KarmaTute.entity.UserLearningPreferences prefs = new KarmaTute.KarmaTute.entity.UserLearningPreferences(newUser);
            preferencesRepository.save(prefs);

            String token = jwtUtil.generateToken(newUser.getUsername(), newUser.getRole(), newUser.getId());
            return ResponseEntity.ok(Map.of("token", token, "role", newUser.getRole(), "isNewUser", true));
        }
    }
}
