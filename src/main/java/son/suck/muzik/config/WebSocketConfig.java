package son.suck.muzik.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final MafiaWebSocketInterceptor mafiaWebSocketInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/sub");
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration) {

        // 기존 JWT CONNECT 처리
        registration.interceptors(
                new ChannelInterceptor() {

                    @Override
                    public Message<?> preSend(
                            Message<?> message,
                            MessageChannel channel) {

                        StompHeaderAccessor accessor =
                                MessageHeaderAccessor.getAccessor(
                                        message,
                                        StompHeaderAccessor.class
                                );

                        if (accessor != null &&
                                StompCommand.CONNECT.equals(
                                        accessor.getCommand())) {

                            String bearerToken =
                                    accessor.getFirstNativeHeader(
                                            "Authorization"
                                    );

                            if (bearerToken != null &&
                                    bearerToken.startsWith("Bearer ")) {

                                String token =
                                        bearerToken.substring(7);

                                Long userId =
                                        jwtTokenProvider.getUserId(token);

                                if (accessor.getSessionAttributes()
                                        != null) {

                                    accessor.getSessionAttributes()
                                            .put("userId", userId);
                                }
                            }
                        }

                        return message;
                    }
                },

                // 마피아 전용 SUBSCRIBE 검사
                mafiaWebSocketInterceptor
        );
    }
}
