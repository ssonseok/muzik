package son.suck.muzik.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import son.suck.muzik.dto.LoginRequestDto;
import son.suck.muzik.dto.LoginResponseDto;
import son.suck.muzik.dto.SignupRequestDto;
import son.suck.muzik.dto.UserStatsResponseDto;
import son.suck.muzik.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /**
     * 1. 회원가입 API
     * POST http://localhost:8080/api/users/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequestDto request) {
        try {
            userService.signup(request);
            return ResponseEntity.ok("회원가입이 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 2. 로그인 API
     * POST http://localhost:8080/api/users/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto request) {
        try {
            LoginResponseDto response = userService.login(request);
            return ResponseEntity.ok(response); ///수정필요 8.15
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<UserStatsResponseDto> getUserStats(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(userService.getUserStats(userId));
    }
}