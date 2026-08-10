package son.suck.muzik.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import son.suck.muzik.domain.*;
import son.suck.muzik.dto.ChatMessageDto;
import son.suck.muzik.dto.GameResultSaveDto;
import son.suck.muzik.repository.*;
import son.suck.muzik.session.GameSession;

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
    private final GameHistoryRepository gameHistoryRepository;

    private final Map<Long, GameSession> activeGames = new ConcurrentHashMap<>();


    /**
     * 유저 입장 ->게임 도중에 참가할지말지 고민중
     */
    @Override
    @Transactional(readOnly = true)
    public ChatMessageDto processEnter(ChatMessageDto message) {
        GameRoom gameRoom = gameRoomRepository.findById(message.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        Users user = usersRepository.findByNickname(message.getSender())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저닉네임입니다: " + message.getSender()));

        // DB 저장은 enterRoom에서 이미 끝났으므로, 현재 최신 인원수만 가져와서 메시지 생성
        int currentCount = gameRoom.getParticipants().size();

        message.setType("ENTER");
        message.setMessage(user.getNickname() + "님이 입장하셨습니다. [" + currentCount + "/" + gameRoom.getMaxPlayers() + "]");

        return message;
    }

    /**
     * 게임 시작 -> 방장만 시작하는거 화면에서 구현할지 여기에 추가할지
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
        message.setRound(1);
        message.setYoutubeId(firstMusic.getYoutubeId());
        message.setMessage("게임이 시작되었습니다! (1/" + quizSet.size() + " 라운드)");

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

        // 게임 진행 중이 아니면 일반 채팅 처리
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
        String correctAnswer = normalizeAnswer(currentMusic.getTitle());
        String userSubmit = normalizeAnswer(message.getMessage());

        // 정답을 맞춘 경우
        if (!userSubmit.isEmpty() && correctAnswer.equals(userSubmit)) {

            for (GameParticipant participant : gameRoom.getParticipants()) {
                if (participant.getUser().getNickname().equals(message.getSender())) {
                    participant.addScore(10);
                    break;
                }
            }

            // 다음 라운드가 남아있는 경우
            if (session.hasNextRound()) {
                session.nextRound();
                Music nextMusic = session.getCurrentMusic();

                message.setType("ANSWER_AND_NEXT");

                message.setRound(session.getCurrentRound() + 1);
                message.setYoutubeId(nextMusic.getYoutubeId());

                message.setMessage("[" + message.getSender() + "] 정답! 정답은 '" + currentMusic.getTitle() + "' (" + currentMusic.getArtist() + ") 이었습니다!\n"
                        + "다음 라운드(" + (session.getCurrentRound() + 1) + "라운드) 시작!");
            }
            //모든 문제를 맞춰서 게임이 완전히 끝난 경우
            else {
                finishGame(gameRoom.getId());
                gameRoom.updateStatus("WAITING");
                activeGames.remove(gameRoom.getId()); // 메모리 세션 삭제로 메모리 누수 방지

                String scoreBoard = gameRoom.getParticipants().stream()
                        .sorted((p1, p2) -> Integer.compare(p2.getCurrentScore(), p1.getCurrentScore()))
                        .map(p -> "[" + p.getUser().getNickname() + ": " + p.getCurrentScore() + "점]")
                        .collect(Collectors.joining(", "));

                message.setType("GAME_END");
                message.setYoutubeId(null);
                message.setMessage("모든 문제를 맞췄습니다! 게임이 종료됩니다.\n[최종 순위] -> " + scoreBoard);
            }
        } else {
            // 정답이 아니면 일반 채팅으로 처리
            message.setType("TALK");
        }

        return message;
    }

    private String normalizeAnswer(String input) {
        if (input == null) return "";
        return input.replaceAll("[^a-zA-Z0-9가-힣]", "").toLowerCase();
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

        String sender = message.getSender();

        // 스킵 투표 등록
        boolean isNewVote = session.addSkipVote(sender);
        if (!isNewVote) {
            message.setType("TALK");
            message.setMessage(sender + "님은 이미 스킵에 투표하셨습니다.");
            return message;
        }

        int currentVotes = session.getSkipVoteCount();
        int totalParticipants = gameRoom.getParticipants().size();

        int requiredVotes = (totalParticipants + 1) / 2;

        if (currentVotes < requiredVotes) {
            message.setType("TALK");
            message.setMessage(sender + "님이 스킵에 투표하셨습니다. (" + currentVotes + "/" + requiredVotes + "명 찬성)");
            return message;
        }

        Music skippedMusic = session.getCurrentMusic();

        if (session.hasNextRound()) {
            session.nextRound();
            Music nextMusic = session.getCurrentMusic();

            message.setType("SKIP_AND_NEXT");

            message.setRound(session.getCurrentRound() + 1);
            message.setYoutubeId(nextMusic.getYoutubeId());

            message.setMessage("과반수 찬성으로 스킵되었습니다! 원곡: '" + skippedMusic.getTitle() + "' (" + skippedMusic.getArtist() + ")\n"
                    + "다음 라운드(" + (session.getCurrentRound() + 1) + "라운드) 시작!");
        } else {
            finishGame(gameRoom.getId());
            // 마지막 곡에서 스킵된 경우 게임 종료 처리
            gameRoom.updateStatus("WAITING");
            activeGames.remove(gameRoom.getId()); // 메모리 세션 삭제

            String scoreBoard = gameRoom.getParticipants().stream()
                    .sorted((p1, p2) -> Integer.compare(p2.getCurrentScore(), p1.getCurrentScore()))
                    .map(p -> "[" + p.getUser().getNickname() + ": " + p.getCurrentScore() + "점]")
                    .collect(Collectors.joining(", "));

            message.setType("GAME_END");
            message.setYoutubeId(null);
            message.setMessage("마지막 곡이 스킵되어 게임이 종료되었습니다!\n[최종 순위] -> " + scoreBoard);
        }

        return message;
    }

    @Transactional
    public void saveGameHistory(Long roomId, List<GameResultSaveDto> resultsDto) {
        // 방 정보 조회 (장르 추출)
        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다. ID: " + roomId));

        // 부모 GameHistory 엔티티 생성
        GameHistory gameHistory = GameHistory.builder()
                .playGenre(gameRoom.getGenre())
                .build();

        resultsDto.sort((a, b) -> Integer.compare(b.getFinalScore(), a.getFinalScore()));

        //순위 계산 및 자식 GameHistoryResult 추가
        int rank = 1;
        for (GameResultSaveDto dto : resultsDto) {
            Users user = usersRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + dto.getUserId()));

            GameHistoryResult result = GameHistoryResult.builder()
                    .user(user)
                    .gameHistory(gameHistory)
                    .finalScore(dto.getFinalScore())
                    .ranking(rank++)
                    .build();

            // 양방향 매핑 리스트에 추가 (CascadeType.ALL에 의해 함께 저장됨)
            gameHistory.getResults().add(result);
        }
        gameHistoryRepository.save(gameHistory);
    }

    // 기존 게임 완료/종료 처리 메서드
    @Transactional
    public void finishGame(Long roomId) {
        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        List<GameResultSaveDto> resultsDto = gameRoom.getParticipants().stream()
                .map(p -> new GameResultSaveDto(p.getUser().getId(), p.getCurrentScore()))
                .collect(Collectors.toList());

        // 방금 추가한 저장 메서드 호출
        saveGameHistory(roomId, resultsDto);

        gameRoom.updateStatus("WAITING");
    }
}