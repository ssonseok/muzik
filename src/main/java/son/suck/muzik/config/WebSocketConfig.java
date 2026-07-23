package son.suck.muzik.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 프론트엔드가 최초로 웹소켓 연결을 시도할 때 접속할 주소(Endpoint)를 설정
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 메시지를 보낼 때(정답 제출, 채팅 등) 주소 앞에 붙일 접두사(Prefix) 설정
        // 예: 프론트엔드가 /app/room/1/answer 로 메시지를 던지면 @MessageMapping이 낚아챕니다.
        registry.setApplicationDestinationPrefixes("/app");

        // 서버가 클라이언트에게 실시간으로 데이터를 뿌려줄 때(브로드캐스팅) 사용할 접두사 설정
        // 예: 방에 있는 사람들에게 실시간 점수를 뿌릴 때 /topic/room/1 주소를 사용
        registry.enableSimpleBroker("/topic");
    }
}
