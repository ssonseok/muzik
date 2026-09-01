package son.suck.muzik.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;
import son.suck.muzik.dto.CreateRoomRequest;
import son.suck.muzik.dto.GameRoomDetailResponse;
import son.suck.muzik.dto.GameRoomResponse;
import son.suck.muzik.service.GameRoomService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class GameRoomController {

    private final GameRoomService gameRoomService;
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * 1. 게임방 생성 API
     * POST http://localhost:8080/api/rooms
     */
    @PostMapping
    public ResponseEntity<GameRoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        // DB에 방을 저장하고 방장 입장 처리
        GameRoomResponse response = gameRoomService.createRoom(request);
        List<GameRoomResponse> updatedRooms = gameRoomService.getWaitingRooms();
        messagingTemplate.convertAndSend("/topic/lobby", updatedRooms);

        return ResponseEntity.ok(response);
    }

    /**
     * 2. 대기실 게임방 목록 조회 API (최초 진입용)
     * GET http://localhost:8080/api/rooms
     */
    @GetMapping
    public ResponseEntity<List<GameRoomResponse>> getWaitingRooms() {
        List<GameRoomResponse> rooms = gameRoomService.getWaitingRooms();
        return ResponseEntity.ok(rooms);
    }

    /**
     * 3. 게임방 입장 API
     * POST http://localhost:8080/api/rooms/{roomId}/enter
     */
    @PostMapping("/{roomId}/enter")
    public ResponseEntity<String> enterRoom(
            @PathVariable("roomId") Long roomId,
            @RequestParam("userId") Long userId) {
        gameRoomService.enterRoom(roomId, userId);
        List<GameRoomResponse> updatedRooms = gameRoomService.getWaitingRooms();
        messagingTemplate.convertAndSend("/topic/lobby", updatedRooms);

        return ResponseEntity.ok("방 입장에 성공했습니다. (실시간 로비 갱신 완료)");
    }

    /**
     * 4. 방 상세 정보 및 참여자 목록 조회 API
     * GET http://localhost:8080/api/rooms/{roomId}
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<GameRoomDetailResponse> getRoomDetail(@PathVariable("roomId") Long roomId) {
        GameRoomDetailResponse response = gameRoomService.getRoomDetail(roomId);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. 게임방 퇴장 API
     * POST http://localhost:8080/api/rooms/{roomId}/leave?userId=2
     */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<String> leaveRoom(
            @PathVariable("roomId") Long roomId,
            @RequestParam("userId") Long userId) {

        System.out.println("퇴장 API 호출됨 roomId: " + roomId + ", userId: " + userId);

        gameRoomService.leaveRoom(roomId, userId);

        // 퇴장 후 로비의 인원수/방목록 갱신을 위해 웹소켓 실시간 방송
        List<GameRoomResponse> updatedRooms = gameRoomService.getWaitingRooms();
        messagingTemplate.convertAndSend("/topic/lobby", updatedRooms);

        return ResponseEntity.ok("방에서 퇴장했습니다.");
    }


}