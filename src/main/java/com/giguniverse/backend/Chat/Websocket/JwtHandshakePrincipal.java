package com.giguniverse.backend.Chat.Websocket;

import org.springframework.security.core.Authentication;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public class JwtHandshakePrincipal implements Authentication {

    private final String userId;
    private boolean authenticated = true;

    public JwtHandshakePrincipal(String userId) {
        this.userId = userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null; // or empty list if needed
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return userId;
    }
}
