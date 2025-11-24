package com.remotejob.planservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Provides methods for generating, validating, and parsing JWT (JSON Web Token) tokens.
 * Utilizes secret keys for both access and refresh tokens to ensure secure token operations.
 * This class is crucial for managing authentication and authorization within the application.
 */
@Slf4j
@Component
public class JwtProvider {
    /**
     * Secret key used for signing and verifying JSON Web Tokens (JWT) for access control.
     * This key is critical for ensuring the integrity and authenticity of the JWT provided
     * to clients and should be kept secure.
     */
    private final SecretKey jwtAccessSecret;
    /**
     * This field stores the secret key used for signing and verifying JWT (JSON Web Token) refresh tokens.
     * The secret key is utilized in cryptographic operations to ensure the integrity and authenticity of refresh tokens.
     * It is critical to keep this key secure and private, as it prevents unauthorized access and tampering of refresh tokens.
     */
    private final SecretKey jwtRefreshSecret;

    public JwtProvider(
            @Value("${jwt.secret.access}") String jwtAccessSecret,
            @Value("${jwt.secret.refresh}") String jwtRefreshSecret
    ) {
        // Decode the secret key from the base64 encoded string
        this.jwtAccessSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtAccessSecret));
        this.jwtRefreshSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtRefreshSecret));
    }

    /**
     * Initializes the security settings for JWT secrets.
     * <p>
     * This method is called automatically after the bean's properties have been set.
     * It performs validation to ensure that the necessary JWT secrets are neither null nor empty.
     * If either the JWT access secret or the JWT refresh secret is found to be invalid,
     * an error is logged and an IllegalArgumentException is thrown.
     *
     * @throws IllegalArgumentException if either the JWT access secret or the JWT refresh secret is null or empty.
     */
    @PostConstruct
    private void init() {
        // Ensure that the secret keys are not null or empty
        if (jwtAccessSecret == null || jwtAccessSecret.getEncoded().length == 0) {
            String message = "JWT secret cannot be null or empty";
            log.error(message);
            throw new IllegalArgumentException(message);
        }
        // Ensure that the refresh secret key is not null or empty
        if (jwtRefreshSecret == null || jwtRefreshSecret.getEncoded().length == 0) {
            String message = "JWT refresh secret cannot be null or empty";
            log.error(message);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Generates a JWT token for the given UserClient.
     *
     * @param userClient the client for whom the token is being generated. Must not be null and must have non-empty roles.
     * @return a JWT token as a String for the provided user.
     * @throws IllegalArgumentException if the userClient's roles are null or empty.
     */
    public String generateToken(@NotNull UserClient userClient) {
        // Get the current date and time
        final LocalDateTime now = LocalDateTime.now();
        // Set the expiration date for the token to 5 minutes from the current time
        final Instant accessExpirationInstant = now.plusMinutes(5).atZone(ZoneId.systemDefault()).toInstant();
        // Convert the expiration date to a Date object
        final Date accessExpiration = Date.from(accessExpirationInstant);
        // Ensure that the user has roles assigned
        if (userClient.getRoles() == null || userClient.getRoles().isEmpty()) {
            throw new IllegalArgumentException("User roles cannot be null or empty");
        }
        // Generate the JWT token with the user's email, expiration date, and roles
        return Jwts.builder()
                .setSubject(userClient.getEmail())
                .setExpiration(accessExpiration)
                .signWith(jwtAccessSecret)
                .claim("roles", userClient.getRoles())
                .claim("email", userClient.getEmail())
                .compact();
    }

    public String generateRefreshToken(@NonNull UserClient user) {
        // Get the current date and time
        final LocalDateTime now = LocalDateTime.now();
        // Set the expiration date for the refresh token to 30 days from the current time
        final Instant refreshExpirationInstant = now.plusDays(30).atZone(ZoneId.systemDefault()).toInstant();
        // Convert the expiration date to a Date object
        final Date refreshExpiration = Date.from(refreshExpirationInstant);
        // Generate the JWT refresh token with the user's email and expiration date
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setExpiration(refreshExpiration)
                .signWith(jwtRefreshSecret)
                .compact();
    }

    /**
     * Parses the provided JWT token and extracts the claims.
     *
     * @param token the JWT token to be parsed
     * @return the claims extracted from the provided JWT token
     */
    public Claims getAccessClaims(String token) {
        // Parse the token using the access secret key and return the claims
        return Jwts.parserBuilder()
                .setSigningKey(jwtAccessSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validates the provided JWT token.
     *
     * @param token the JWT token to be validated
     * @return Boolean indicating whether the token is valid (true) or invalid (false)
     */
    public Boolean validateToken(String token) {
        try {
            // Parse the token using the access secret key
            Jwts.parserBuilder()
                    .setSigningKey(jwtAccessSecret)
                    .build()
                    .parseClaimsJws(token);
            // If the token is successfully parsed, return true
            return true;
        } catch (ExpiredJwtException expEx) {
            log.error("Token expired", expEx);
        } catch (UnsupportedJwtException unsEx) {
            log.error("Unsupported jwt", unsEx);
        } catch (MalformedJwtException mjEx) {
            log.error("Malformed jwt", mjEx);
        } catch (SignatureException sEx) {
            log.error("Invalid signature", sEx);
        } catch (Exception e) {
            log.error("invalid token", e);
        }
        // If the token is invalid, return false
        return false;
    }
}