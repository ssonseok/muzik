package son.suck.muzik.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import son.suck.muzik.config.JwtTokenProvider;
import son.suck.muzik.config.UserPrincipal;
import son.suck.muzik.dto.MafiaCreateRoomRequestDto;
import son.suck.muzik.dto.MafiaRoomResponse;
import son.suck.muzik.service.MafiaRoomService;

@Slf4j
@RestController
@RequestMapping("/api/mafia/rooms")
@RequiredArgsConstructor
public class MafiaRoomController {
    private final MafiaRoomService mafiaRoomService;

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
}