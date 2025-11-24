package com.remotejob.planservice.security;

/**
 * Exception thrown when an authentication-related error occurs.
 * <p>
 * This exception extends RuntimeException and is typically thrown
 * to indicate that a user authentication process failed, such as
 * when user credentials are invalid or an email address is not found
 * in the system.
 */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }

}