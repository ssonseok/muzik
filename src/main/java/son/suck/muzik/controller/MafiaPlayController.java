package son.suck.muzik.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import son.suck.muzik.dto.MafiaExecutionVoteRequestDto;
import son.suck.muzik.dto.MafiaNightActionRequestDto;
import son.suck.muzik.dto.MafiaVoteRequestDto;
import son.suck.muzik.service.MafiaPlayService;

@RestController
@RequestMapping("/api/mafia")
@RequiredArgsConstructor
public class MafiaPlayController {

    private final MafiaPlayService mafiaPlayService;

    /**
     * 1. 밤 행동 제출 (마피아 킬, 의사 힐, 경찰 조사)
     */
    @PostMapping("/night/action")
    public ResponseEntity<String> processNightAction(
            @RequestBody MafiaNightActionRequestDto request,
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        mafiaPlayService.processNightAction(userId, request);
        return ResponseEntity.ok("밤 행동이 정상적으로 접수되었습니다.");
    }

    /**
     * 3. 1차 지목 투표 접수 (VOTE 페이즈)
     */
    @PostMapping("/vote/nomination")
    public ResponseEntity<String> processNominationVote(
            @RequestBody MafiaVoteRequestDto request,
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        mafiaPlayService.processNominationVote(userId, request);
        return ResponseEntity.ok("1차 지목 투표가 정상적으로 접수되었습니다.");
    }

    /**
     * 4. 2차 찬반 투표 접수 (DEFENSE 페이즈)
     */
    @PostMapping("/vote/defense")
    public ResponseEntity<String> processDefenseVote(
            @RequestBody MafiaExecutionVoteRequestDto request,
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        mafiaPlayService.processDefenseVote(userId, request);
        return ResponseEntity.ok("찬반 투표가 정상적으로 접수되었습니다.");
    }

    // (나머지 calculate 메서드는 방 ID만 받으므로 수정 불필요)
    @PostMapping("/night/calculate/{roomId}")
    public ResponseEntity<String> calculateNightResult(@PathVariable Long roomId) {
        mafiaPlayService.calculateNightResult(roomId);
        return ResponseEntity.ok("밤 결과 정산이 완료되었습니다.");
    }

    @PostMapping("/day/calculate/{roomId}")
    public ResponseEntity<String> calculateDayResult(@PathVariable Long roomId) {
        mafiaPlayService.calculateDayResult(roomId);
        return ResponseEntity.ok("낮 결과 정산이 완료되었습니다.");
    }
}