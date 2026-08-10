package son.suck.muzik.service;

import son.suck.muzik.dto.ChatMessageDto;
import son.suck.muzik.dto.GameResultSaveDto;

import java.util.List;

public interface GamePlayService {
    // 유저가 웹소켓으로 입장했을 때 DB 처리 및 메시지 가공
    ChatMessageDto processEnter(ChatMessageDto message);
    // 유저가 입력한 채팅이 정답인지 검증 처리
    ChatMessageDto checkAnswer(ChatMessageDto message);
    ChatMessageDto processStart(ChatMessageDto message);
    ChatMessageDto processSkip(ChatMessageDto message);
    void saveGameHistory(Long roomId, List<GameResultSaveDto> resultsDto);
}