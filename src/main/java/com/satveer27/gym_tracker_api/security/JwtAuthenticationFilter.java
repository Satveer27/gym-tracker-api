package com.satveer27.gym_tracker_api.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try{
            String token = null;

            if(request.getCookies() != null){
                for(Cookie cookie : request.getCookies()){
                    if(cookie.getName().equals("access_token")){
                        token = cookie.getValue();
                        break;
                    }
                }
            }

            if(token !=null && jwtService.validateJwtToken(token)
                    && jwtService.isAccessToken(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                Long userId = Long.parseLong(jwtService.getIdFromJwtToken(token));
                String role = jwtService.getRolesFromToken(token);
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
                UsernamePasswordAuthenticationToken authToken = new
                            UsernamePasswordAuthenticationToken(userId, null,
                            authorities);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                MDC.put("user_id", userId.toString());
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException ex) {
            log.warn("action=jwt_filter_invalid_token message={}", ex.getMessage());
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        } catch (Exception ex){
            log.error("action=jwt_filter_error message={}", ex.getMessage(), ex);
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                    "{\"status\": 500, \"error\": \"Internal Error\", \"message\": \"An unexpected error occurred\"}"
            );
        }
    }
}
