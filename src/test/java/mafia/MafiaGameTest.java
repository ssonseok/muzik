//package mafia;
//
//import jakarta.persistence.EntityManager;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.test.annotation.Rollback;
//import org.springframework.transaction.annotation.Transactional;
//import son.suck.muzik.domain.*;
//import son.suck.muzik.dto.MafiaCreateRoomRequestDto;
//import son.suck.muzik.dto.MafiaRoomResponse;
//import son.suck.muzik.repository.GameParticipantRepository;
//import son.suck.muzik.repository.GameRoomRepository;
//import son.suck.muzik.repository.UsersRepository;
//import son.suck.muzik.service.MafiaRoomService;
//
//import java.util.List;
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest(classes = son.suck.muzik.MuzikApplication.class)
//@Transactional
//class MafiaGameTest {
//
//    private static final Logger log = LoggerFactory.getLogger(MafiaGameTest.class);
//    @Autowired
//    private MafiaRoomService mafiaRoomService;
//
//    @Autowired
//    private UsersRepository usersRepository;
//
//    @Autowired
//    private GameRoomRepository gameRoomRepository;
//    @Autowired
//    private EntityManager em;
//
//    @Test
//    @DisplayName("12명 맥시멈 인원 게임 시작 및 직업 배정 밸런스 테스트")
//    @Rollback(false)
//    void testStartGameWith12Players() {
//        // given: 1. 첫 번째 유저(방장) 생성 및 방 생성 (createRoom 활용!)
//        Users hostUser = usersRepository.save(
//                Users.builder()
//                        .loginId("testhost3")
//                        .password("password3")
//                        .nickname("방장유저3")
//                        .build()
//        );
//
//        MafiaCreateRoomRequestDto requestDto = new MafiaCreateRoomRequestDto();
//        requestDto.setRoomName("마피아 테스트방3");
//        requestDto.setMaxPlayers(12);
//
//        MafiaRoomResponse roomResponse = mafiaRoomService.createRoom(requestDto, hostUser.getId());
//        Long roomId = roomResponse.getRoomId(); // 생성된 방 ID
//
//        // given: 2. 나머지 11명의 유저를 만들고 joinRoom으로 방에 입장시키기!
//        for (int i = 2; i <= 12; i++) {
//            Users user = usersRepository.save(
//                    Users.builder()
//                            .loginId("testuser3" + i)
//                            .password("password3" + i)
//                            .nickname("테스트유저3" + i)
//                            .build()
//            );
//
//            // 이미 만들어진 joinRoom 기능 활용! (컬렉션 동기화도 서비스 내부에서 알아서 다 됨)
//            mafiaRoomService.joinRoom(roomId, user.getId());
//        }
//        em.flush();
//        em.clear();
//
//        // when: 방장이 게임 시작 API 호출
//        mafiaRoomService.startGame(roomId, hostUser.getId());
//
//        // then: 상태 변화 및 12명 기준 직업 밸런스 검증
//        GameRoom updatedRoom = gameRoomRepository.findById(roomId).get();
//
//        // 1. 방 상태 및 페이즈 검증
//        assertThat(updatedRoom.getRoomStatus()).isEqualTo("PLAYING");
//        assertThat(updatedRoom.getGamePhase()).isEqualTo(GamePhase.NIGHT);
//
//        // 2. 직업별 카운트 검증 (12명 기준: 마피아 3, 경찰 1, 의사 1, 군인 1, 시민 7)
//        List<GameParticipant> participants = updatedRoom.getParticipants();
//        assertThat(participants).hasSize(12);
//
//        long mafiaCount = participants.stream().filter(p -> p.getMafiaRole() == Mafia_Role.MAFIA).count();
//        long policeCount = participants.stream().filter(p -> p.getMafiaRole() == Mafia_Role.POLICE).count();
//        long doctorCount = participants.stream().filter(p -> p.getMafiaRole() == Mafia_Role.DOCTOR).count();
//        long soldierCount = participants.stream().filter(p -> p.getMafiaRole() == Mafia_Role.SOLDIER).count();
//        long citizenCount = participants.stream().filter(p -> p.getMafiaRole() == Mafia_Role.CITIZEN).count();
//
//        assertThat(mafiaCount).isEqualTo(3);
//        assertThat(policeCount).isEqualTo(1);
//        assertThat(doctorCount).isEqualTo(1);
//        assertThat(soldierCount).isEqualTo(1);
//        assertThat(citizenCount).isEqualTo(6);
//    }
//}
