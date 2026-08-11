package son.suck.muzik.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import son.suck.muzik.dto.FriendRequestDto;
import son.suck.muzik.service.FriendService;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {
    private final FriendService friendService;

    @PostMapping("/request")
    public ResponseEntity<String> sendFriendRequest(
            @RequestParam("userId") Long currentUserId,
            @RequestBody FriendRequestDto requestDto) {

        friendService.sendFriendRequest(currentUserId, requestDto.getTargetNickname());
        return ResponseEntity.ok("친구 요청을 성공적으로 보냈습니다.");
    }
}
