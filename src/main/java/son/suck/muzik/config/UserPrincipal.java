package son.suck.muzik.config;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;
@Getter
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String loginId;

    public UserPrincipal(Long userId, String loginId) {
        this.userId = userId;
        this.loginId = loginId;
    }


    @Override
    public String getUsername() {
        return loginId;
    }

    @Override public String getPassword() { return null; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return Collections.emptyList(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
