package son.suck.muzik.service;

import son.suck.muzik.dto.MafiaChatMessageDto;

public interface MafiaChatService {
    void sendMafiaChat(Long roomId, Long userId, MafiaChatMessageDto message);
}
