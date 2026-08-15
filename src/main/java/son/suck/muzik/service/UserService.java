package son.suck.muzik.service;

import son.suck.muzik.dto.LoginRequestDto;
import son.suck.muzik.dto.LoginResponseDto;
import son.suck.muzik.dto.SignupRequestDto;
import son.suck.muzik.dto.UserStatsResponseDto;

public interface UserService {
    void signup(SignupRequestDto request);
    //Long login(LoginRequestDto request);
    LoginResponseDto login(LoginRequestDto request);
    UserStatsResponseDto getUserStats(Long userId);
}
