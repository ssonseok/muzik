package son.suck.muzik.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import son.suck.muzik.dto.ChatMessageDto;
import son.suck.muzik.domain.GameRoom;
import son.suck.muzik.domain.GameParticipant;
import son.suck.muzik.domain.Users;
import son.suck.muzik.domain.Music;
import son.suck.muzik.repository.GameRoomRepository;
import son.suck.muzik.repository.GameParticipantRepository;
import son.suck.muzik.repository.UsersRepository;
import son.suck.muzik.repository.MusicRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GamePlayServiceImpl implements GamePlayService {

    private final GameRoomRepository gameRoomRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final UsersRepository usersRepository;
    private final MusicRepository musicRepository;

    private final Map<Long, GameSession> activeGames = new ConcurrentHashMap<>();

    @Getter
    @Setter
    public static class GameSession {
        private List<Music> playlist;
        private int currentRound = 0;

        public GameSession(List<Music> playlist) {
            this.playlist = playlist;
        }

        public Music getCurrentMusic() {
            return playlist.get(currentRound);
        }

        public boolean hasNextRound() {
            return currentRound + 1 < playlist.size();
        }

        public void nextRound() {
            this.currentRound++;
        }
    }

    /**
     * 유저 입장
     */
    @Override
    @Transactional
    public ChatMessageDto processEnter(ChatMessageDto message) {
        GameRoom gameRoom = gameRoomRepository.findById(message.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        if (gameRoom.getParticipants().size() >= gameRoom.getMaxPlayers()) {
            throw new IllegalStateException("방이 꽉 차서 입장할 수 없습니다.");
        }

        Users user = usersRepository.findByNickname(message.getSender())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저닉네임입니다: " + message.getSender()));

        boolean isAlreadyJoined = gameRoom.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(user.getId()));

        if (!isAlreadyJoined) {
            boolean isHost = gameRoom.getParticipants().isEmpty();

            GameParticipant participant = GameParticipant.builder()
                    .user(user)
                    .gameRoom(gameRoom)
                    .isHost(isHost)
                    .build();

            gameParticipantRepository.save(participant);
            gameRoom.getParticipants().add(participant);
        }

        int currentCount = gameRoom.getParticipants().size();
        message.setMessage(user.getNickname() + "님이 입장하셨습니다. [" + currentCount + "/" + gameRoom.getMaxPlayers() + "]");

        return message;
    }

    /**
     * 게임 시작
     */
    @Override
    @Transactional
    public ChatMessageDto processStart(ChatMessageDto message) {
        GameRoom gameRoom = gameRoomRepository.findById(message.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

        gameRoom.updateStatus("PLAYING");

        List<Music> quizSet = musicRepository.findRandomQuizSet(
                gameRoom.getGenre(),
                gameRoom.getStartYear(),
                gameRoom.getEndYear(),
                gameRoom.getMusicCount()
        );

        if (quizSet.isEmpty()) {
            throw new IllegalStateException("선택한 조건에 맞는 음악이 DB에 없습니다.");
        }

        gameRoom.getParticipants().forEach(GameParticipant::resetScore);

        GameSession gameSession = new GameSession(quizSet);
        activeGames.put(gameRoom.getId(), gameSession);

        Music firstMusic = gameSession.getCurrentMusic();

        message.setType("START");
        message.setMessage(firstMusic.getYoutubeId());

        return message;
    }

    /**
     * 실시간 채팅 정답 검증 및 자동 다음 라운드/종료 처리
     */
    @Override
    @Transactional
    public ChatMessageDto checkAnswer(ChatMessageDto message) {
        GameRoom gameRoom = gameRoomRepository.findById(message.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

        if (!"PLAYING".equals(gameRoom.getRoomStatus())) {
            message.setType("TALK");
            return message;
        }

        GameSession session = activeGames.get(gameRoom.getId());
        if (session == null) {
            message.setType("TALK");
            return message;
        }

        Music currentMusic = session.getCurrentMusic();
        String correctAnswer = currentMusic.getTitle();
        String userSubmit = message.getMessage().trim().replaceAll(" ", "");

        if (correctAnswer.replaceAll(" ", "").equalsIgnoreCase(userSubmit)) {

            for (GameParticipant participant : gameRoom.getParticipants()) {
                if (participant.getUser().getNickname().equals(message.getSender())) {
                    participant.addScore(10);
                    break;
                }
            }

            if (session.hasNextRound()) {
                session.nextRound();
                Music nextMusic = session.getCurrentMusic();
                message.setType("ANSWER_AND_NEXT");
                message.setMessage("[" + message.getSender() + "] 정답! '" + correctAnswer + "' (" + currentMusic.getArtist() + ")\n"
                        + "다음 라운드를 시작합니다! 새로운 곡의 유튜브 ID: " + nextMusic.getYoutubeId());
            } else {
                gameRoom.updateStatus("WAITING");
                activeGames.remove(gameRoom.getId());

                String scoreBoard = gameRoom.getParticipants().stream()
                        .sorted((p1, p2) -> Integer.compare(p2.getCurrentScore(), p1.getCurrentScore()))
                        .map(p -> "[" + p.getUser().getNickname() + ": " + p.getCurrentScore() + "점]")
                        .collect(Collectors.joining(", "));

                message.setType("GAME_END");
                message.setMessage("게임이 완전히 끝났습니다! 최종 순위 -> " + scoreBoard);
            }
        } else {
            message.setType("TALK");
        }

        return message;
    }

    /**
     * 라운드 Skip 로직
     */
    @Override
    @Transactional
    public ChatMessageDto processSkip(ChatMessageDto message) {
        GameRoom gameRoom = gameRoomRepository.findById(message.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

        GameSession session = activeGames.get(gameRoom.getId());
        if (session == null || !"PLAYING".equals(gameRoom.getRoomStatus())) {
            message.setType("TALK");
            message.setMessage("현재 진행 중인 게임이 없습니다.");
            return message;
        }

        Music skippedMusic = session.getCurrentMusic();

        if (session.hasNextRound()) {
            session.nextRound();
            Music nextMusic = session.getCurrentMusic();

            message.setType("SKIP_AND_NEXT");
            message.setMessage("현재 곡을 스킵했습니다! 원곡은 '" + skippedMusic.getTitle() + "' (" + skippedMusic.getArtist() + ") 이었습니다.\n"
                    + "다음 라운드 시작! 새로운 유튜브 ID: " + nextMusic.getYoutubeId());
        } else {
            // 마지막 곡에서 스킵이 눌린 경우 게임 종료
            gameRoom.updateStatus("WAITING");
            activeGames.remove(gameRoom.getId());

            String scoreBoard = gameRoom.getParticipants().stream()
                    .sorted((p1, p2) -> Integer.compare(p2.getCurrentScore(), p1.getCurrentScore()))
                    .map(p -> "[" + p.getUser().getNickname() + ": " + p.getCurrentScore() + "점]")
                    .collect(Collectors.joining(", "));

            message.setType("GAME_END");
            message.setMessage("마지막 곡이 스킵되어 게임이 끝났습니다! 최종 순위 -> " + scoreBoard);
        }

        return message;
    }
}