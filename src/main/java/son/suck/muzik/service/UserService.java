package son.suck.muzik.service;

import son.suck.muzik.dto.LoginRequestDto;
import son.suck.muzik.dto.SignupRequestDto;

public interface UserService {
    void signup(SignupRequestDto request);
    Long login(LoginRequestDto request);
}
