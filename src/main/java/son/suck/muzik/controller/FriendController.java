package son.suck.muzik.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import son.suck.muzik.dto.FriendListResponseDto;
import son.suck.muzik.dto.FriendRequestDto;
import son.suck.muzik.service.FriendService;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {
    private final FriendService friendService;

    //친추 요청
    @PostMapping("/request")
    public ResponseEntity<String> sendFriendRequest(
            @RequestParam("userId") Long currentUserId,
            @RequestBody FriendRequestDto requestDto) {

        friendService.sendFriendRequest(currentUserId, requestDto.getTargetNickname());
        return ResponseEntity.ok("친구 요청을 성공적으로 보냈습니다.");
    }

    //친추 수락
    @PatchMapping("/accept/{friendshipId}")
    public ResponseEntity<String> acceptFriendRequest(
            @RequestParam("userId") Long currentUserId, // 수락하는 사람(나)의 ID
            @PathVariable("friendshipId") Long friendshipId) {

        friendService.acceptFriendRequest(currentUserId, friendshipId);
        return ResponseEntity.ok("친구 요청을 수락했습니다.");
    }

    //친구목록
    @GetMapping
    public ResponseEntity<List<FriendListResponseDto>> getMyFriendList(
            @RequestParam("userId") Long currentUserId) {

        List<FriendListResponseDto> friendList = friendService.getMyFriendList(currentUserId);
        return ResponseEntity.ok(friendList);
    }
}
