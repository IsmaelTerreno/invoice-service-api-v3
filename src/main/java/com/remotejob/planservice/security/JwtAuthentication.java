package com.remotejob.planservice.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;

/**
 * JwtAuthentication class represents the authentication object used
 * for JWT (JSON Web Token) based authentication.
 * <p>
 * This class implements the Authentication interface and provides the
 * necessary methods to extract the authentication details from a JWT.
 * <p>
 * The class includes methods to get the authorities, credentials, details,
 * principal, and name of the authenticated user as well as to determine
 * if the user is authenticated.
 * <p>
 * The roles field stores the set of roles assigned to the authenticated user.
 */
@Getter
@Setter
public class JwtAuthentication implements Authentication {

    private boolean authenticated;
    private String username;
    private Set<Role> roles;

    /**
     * Retrieves the collection of roles assigned to the authenticated user.
     *
     * @return a collection of roles that represent the authorities granted to the user.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    /**
     * Retrieves the credentials associated with the authenticated user.
     *
     * @return the credentials of the authenticated user, or null if no credentials are available.
     */
    @Override
    public Object getCredentials() {
        return null;
    }

    /**
     * Retrieves additional details about the authenticated user.
     **/
    @Override
    public Object getDetails() {
        return null;
    }

    /**
     * Retrieves the principal associated with the authenticated user.
     *
     * @return the username of the authenticated
     */
    @Override
    public Object getPrincipal() {
        return username;
    }

    /**
     * Determines whether the user is authenticated.
     *
     * @return true if the user is authenticated, false otherwise
     */
    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Sets the authentication status of the current user.
     *
     * @param isAuthenticated the authentication status to be set
     * @throws IllegalArgumentException if the provided authentication status is invalid
     */
    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    /**
     * Retrieves the name of the authenticated user.
     *
     * @return the username of the authenticated user.
     */
    @Override
    public String getName() {
        return username;
    }

}
