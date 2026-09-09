package son.suck.muzik.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import son.suck.muzik.dto.MafiaNightActionRequestDto;
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
    public ResponseEntity<String> processNightAction(@RequestBody MafiaNightActionRequestDto request) {
        mafiaPlayService.processNightAction(request);
        return ResponseEntity.ok("밤 행동이 정상적으로 접수되었습니다.");
    }

    /**
     * 2. 밤 결과 정산 트리거 (테스트용)
     * - 타이머가 끝났을 때를 가정하여 서버가 결과를 계산하고 사망자를 판정합니다.
     */
    @PostMapping("/night/calculate/{roomId}")
    public ResponseEntity<String> calculateNightResult(@PathVariable Long roomId) {
        mafiaPlayService.calculateNightResult(roomId);
        return ResponseEntity.ok("밤 결과 정산이 완료되었습니다. (사망자 및 생사 판정 완료)");
    }
}
