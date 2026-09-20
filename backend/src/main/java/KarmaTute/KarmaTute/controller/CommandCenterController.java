package KarmaTute.KarmaTute.controller;

import KarmaTute.KarmaTute.dto.CommandCenterResponse;
import KarmaTute.KarmaTute.service.CommandCenterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import KarmaTute.KarmaTute.entity.User;
import KarmaTute.KarmaTute.repository.UserRepository;

@RestController
@CrossOrigin(origins = "*")
public class CommandCenterController {

    private final CommandCenterService commandCenterService;
    private final UserRepository userRepository;

    public CommandCenterController(CommandCenterService commandCenterService, UserRepository userRepository) {
        this.commandCenterService = commandCenterService;
        this.userRepository = userRepository;
    }

    @GetMapping({"/me/command-center", "/api/v1/me/command-center"})
    public ResponseEntity<CommandCenterResponse> getCommandCenter(
        Authentication authentication,
        @RequestParam(required = false) String state
    ) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        CommandCenterResponse response = commandCenterService.getCommandCenter(user.getId(), state);
        return ResponseEntity.ok(response);
    }
}
