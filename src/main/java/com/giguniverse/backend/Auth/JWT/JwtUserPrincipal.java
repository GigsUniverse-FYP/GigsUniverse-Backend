package com.giguniverse.backend.Auth.JWT;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class JwtUserPrincipal implements UserDetails {
    private final String userId;
    private final String email;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    public JwtUserPrincipal(String userId, String email, String role, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.authorities = authorities;
    }

    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    // Ignore this is not used, but inherited from the parent class
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return null; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
