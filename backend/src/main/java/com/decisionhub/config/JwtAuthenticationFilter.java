package com.decisionhub.config;

import java.io.IOException;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.decisionhub.entity.authentication.User;
import com.decisionhub.enums.authentication.UserStatus;
import com.decisionhub.repository.authentication.UserRepository;
import com.decisionhub.service.impl.authentication.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private UserRepository userRepository;
    private com.decisionhub.service.interfaces.authentication.BlockedEmailService blockedEmailService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setBlockedEmailService(com.decisionhub.service.interfaces.authentication.BlockedEmailService blockedEmailService) {
        this.blockedEmailService = blockedEmailService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = authHeader.substring(7);
            String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null) {
                if (blockedEmailService != null && blockedEmailService.isBlocked(userEmail)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":403,\"error\":\"This email address has been permanently blocked from the platform\",\"path\":\"" + request.getRequestURI() + "\"}");
                    return;
                }

                if (userRepository != null) {
                    Optional<User> userOpt = userRepository.findByEmailIgnoreCase(userEmail);
                    if (userOpt.isPresent() && userOpt.get().getStatus() == UserStatus.BLOCKED) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"status\":403,\"error\":\"Your account has been blocked by an administrator\",\"path\":\"" + request.getRequestURI() + "\"}");
                        return;
                    }
                }

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                    if (jwtService.isTokenValid(jwt, userDetails) && userDetails.isEnabled() && userDetails.isAccountNonLocked()) {

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            // Token is malformed, expired, or invalid. 
            // We catch it here so the filter chain continues and Spring Security cleanly rejects it.
        }

        filterChain.doFilter(request, response);
    }
}
