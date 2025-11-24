package com.remotejob.planservice.security;


import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

/**
 * JwtFilter is a servlet filter that intercepts HTTP requests to check for a valid JWT token in the Authorization header.
 * If a valid token is present, it extracts the claims, creates an authentication object, and sets it in the SecurityContextHolder
 * for subsequent security checks within the application.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends GenericFilterBean {

    private static final String AUTHORIZATION = "Authorization";

    private final JwtProvider jwtProvider;

    /**
     * This method filters incoming requests to determine if they contain a valid JWT token.
     * If a valid token is found, it extracts the claims, generates the authentication object,
     * and sets it in the SecurityContextHolder. Finally, it proceeds with the filter chain.
     *
     * @param request  the incoming request to be filtered
     * @param response the response associated with the request
     * @param fc       the filter chain to pass the request and response to the next filter in the chain
     * @throws IOException      if an I/O error occurs during the filtering process
     * @throws ServletException if an error occurs while processing the request or response
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain fc)
            throws IOException, ServletException {
        final String token = getTokenFromRequest((HttpServletRequest) request);
        if (token != null && jwtProvider.validateToken(token)) {
            final Claims claims = jwtProvider.getAccessClaims(token);
            final JwtAuthentication jwtInfoToken = JwtUtils.generate(claims);
            jwtInfoToken.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(jwtInfoToken);
        }
        fc.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the HTTP request's Authorization header.
     * The token is expected to be prefixed with "Bearer ".
     *
     * @param request the HttpServletRequest containing the Authorization header
     * @return the extracted JWT token, or null if the Authorization header is missing or does not start with "Bearer "
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader(AUTHORIZATION);
        return (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
    }

}