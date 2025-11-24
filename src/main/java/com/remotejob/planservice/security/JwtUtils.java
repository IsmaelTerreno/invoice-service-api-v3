package com.remotejob.planservice.security;

import io.jsonwebtoken.Claims;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for handling operations related to JSON Web Tokens (JWT).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JwtUtils {
    /**
     * Generates a JwtAuthentication object using the provided JWT claims.
     *
     * @param claims the JWT claims from which to generate the JwtAuthentication object
     * @return a JwtAuthentication object containing the roles and username extracted from the claims
     */
    public static JwtAuthentication generate(Claims claims) {
        // Create a new JwtAuthentication object and set the roles and username
        final JwtAuthentication jwtInfoToken = new JwtAuthentication();
        jwtInfoToken.setRoles(getRoles(claims));
        jwtInfoToken.setUsername(claims.get("email", String.class));
        // Return the JwtAuthentication object
        return jwtInfoToken;
    }

    /**
     * Extracts and returns a set of roles from the provided JWT claims.
     *
     * @param claims the JWT claims from which to extract the roles
     * @return a set of roles extracted from the JWT claims
     */
    private static Set<Role> getRoles(Claims claims) {
        // Extract the roles from the claims and convert them to a set of Role objects
        final List<String> roles = claims.get("roles", List.class);
        // Return the set of roles
        return roles.stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());
    }

}
