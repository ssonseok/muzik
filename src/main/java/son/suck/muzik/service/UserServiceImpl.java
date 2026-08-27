package son.suck.muzik.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import son.suck.muzik.config.JwtTokenProvider;
import son.suck.muzik.domain.UserOnlineStatus;
import son.suck.muzik.domain.UserStatus;
import son.suck.muzik.domain.Users;
import son.suck.muzik.dto.LoginRequestDto;
import son.suck.muzik.dto.LoginResponseDto;
import son.suck.muzik.dto.SignupRequestDto;
import son.suck.muzik.dto.UserStatsResponseDto;
import son.suck.muzik.repository.GameHistoryResultRepository;
import son.suck.muzik.repository.GameParticipantRepository;
import son.suck.muzik.repository.UsersRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UsersRepository usersRepository;
    private final GameHistoryResultRepository gameHistoryResultRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    @Override
    public void signup(SignupRequestDto request) {
        if (usersRepository.existsByLoginId(request.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (usersRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        Users user = Users.builder()
                .loginId(request.getLoginId())
                .password(request.getPassword()) //  스프링시큐리티 적용 예정
                .nickname(request.getNickname())
                .build();

        UserStatus status = UserStatus.builder()
                .status(UserOnlineStatus.OFFLINE)
                .build();

        user.setUserStatus(status);

        usersRepository.save(user);
    }

    @Transactional
    @Override
    public LoginResponseDto login(LoginRequestDto request) {
        Users user = usersRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        user.getUserStatus().updateStatus(UserOnlineStatus.ONLINE);
        String token = jwtTokenProvider.createToken(user.getLoginId());

        return new LoginResponseDto(token, user.getId(), user.getNickname());
    }

    @Override
    public UserStatsResponseDto getUserStats(Long userId) {
        long totalGames = gameHistoryResultRepository.countByUserId(userId);
        long winCount = gameHistoryResultRepository.countByUserIdAndRanking(userId, 1);
        Integer maxScore = gameHistoryResultRepository.findMaxScoreByUserId(userId);

        return UserStatsResponseDto.of(totalGames, winCount, maxScore);
    }
}
