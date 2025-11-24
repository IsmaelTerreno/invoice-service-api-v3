package com.remotejob.planservice.security;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;


@Getter
@Setter
public class UserClient {
    private UUID id;
    private String email;
    private String password;
    private String username;
    private String token;
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;
    private boolean enabled;
    private boolean credentialsNonExpired;
    private boolean accountNonExpired;
    private boolean accountNonLocked;

    public UserClient() {
    }
}
