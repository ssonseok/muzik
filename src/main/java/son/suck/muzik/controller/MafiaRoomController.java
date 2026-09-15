package son.suck.muzik.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import son.suck.muzik.config.JwtTokenProvider;
import son.suck.muzik.config.UserPrincipal;
import son.suck.muzik.dto.MafiaCreateRoomRequestDto;
import son.suck.muzik.dto.MafiaMyInfoResponse;
import son.suck.muzik.dto.MafiaRoomResponse;
import son.suck.muzik.service.MafiaRoomService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/mafia/rooms")
@RequiredArgsConstructor
public class MafiaRoomController {
    private final MafiaRoomService mafiaRoomService;

    //방생성
    @PostMapping
    public ResponseEntity<MafiaRoomResponse> createRoom(
            @RequestBody MafiaCreateRoomRequestDto requestDto,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Long userId = principal.getUserId();

        MafiaRoomResponse response = mafiaRoomService.createRoom(requestDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //방목록
    @GetMapping
    public ResponseEntity<List<MafiaRoomResponse>> getRoomList() {
        List<MafiaRoomResponse> roomList = mafiaRoomService.getRoomList();
        return ResponseEntity.ok(roomList);
    }

    //방입장
    @PostMapping("/{roomId}/join")
    public ResponseEntity<Void> joinRoom(@PathVariable Long roomId,
                                         @AuthenticationPrincipal(expression = "userId") Long userId) {

        mafiaRoomService.joinRoom(roomId, userId);
        return ResponseEntity.ok().build();
    }
    //방퇴장
    @DeleteMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable Long roomId,
                                          @AuthenticationPrincipal(expression = "userId") Long userId) {
        mafiaRoomService.leaveRoom(roomId, userId);
        return ResponseEntity.ok().build();
    }
    //게임시작
    @PostMapping("/{roomId}/start")
    public ResponseEntity<Void> startGame(
            @PathVariable Long roomId,
            @AuthenticationPrincipal(expression = "userId") Long userId) {

        mafiaRoomService.startGame(roomId, userId);
        return ResponseEntity.ok().build();
    }
    // 내 직업 및 방 상태 조회
    @GetMapping("/{roomId}/my-info")
    public ResponseEntity<MafiaMyInfoResponse> getMyInfo(
            @PathVariable Long roomId,
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        MafiaMyInfoResponse response = mafiaRoomService.getMyInfo(roomId, userId);
        return ResponseEntity.ok(response);
    }

}