package son.suck.muzik.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import son.suck.muzik.dto.LoginRequestDto;
import son.suck.muzik.dto.SignupRequestDto;
import son.suck.muzik.repository.UsersRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UsersRepository usersRepository;

    @Transactional
    @Override
    public void signup(SignupRequestDto request) {

    }

    @Override
    public void login(LoginRequestDto request) {

    }
}
