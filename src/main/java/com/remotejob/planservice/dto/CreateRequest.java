package com.remotejob.planservice.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Represents a request to create a new user account.
 * <p>
 * This class contains the necessary information to create a new user, including
 * email, password, and username. The properties included in this class are used
 * for user registration purposes.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class CreateRequest {
    @EqualsAndHashCode.Include
    private String email;
    @EqualsAndHashCode.Include
    private String password;
    @EqualsAndHashCode.Include
    private String username;
}
