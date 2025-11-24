package com.remotejob.planservice.security;


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

/**
 * Enumeration representing various roles within the application.
 * <p>
 * Each role corresponds to a specific authority that can be granted to users.
 * The roles available are:
 * <ul>
 * <li>ROLE_USER - Basic user role with standard privileges.</li>
 * <li>ROLE_API_PROVIDER - Role for API providers with specific permissions.</li>
 * <li>ROLE_ADMIN - Administrative role with elevated privileges.</li>
 * </ul>
 * </p>
 * <p>
 * This enum implements the {@link GrantedAuthority} interface, enabling it to be used
 * within Spring Security for authorization purposes. The getAuthority method is overridden
 * to return the string representation of the role, which can be used for role-based access control.
 * </p>
 */
@RequiredArgsConstructor
public enum Role implements GrantedAuthority {

    ROLE_USER("ROLE_USER"),
    ROLE_API_PROVIDER("ROLE_CLIENT_API"),
    ROLE_ADMIN("ROLE_ADMIN");

    private final String vale;

    @Override
    public String getAuthority() {
        return vale;
    }

}
