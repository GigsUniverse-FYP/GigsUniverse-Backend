package com.giguniverse.backend.Chat.Websocket;

import java.security.Principal;

public class JwtHandshakePrincipal implements Principal {
    private final String name; // store userId here

    public JwtHandshakePrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
