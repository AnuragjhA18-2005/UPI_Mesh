package com.example.UPImesh.Security;

import java.io.IOException;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JWTfilter extends OncePerRequestFilter {//extends OncePerRequestFilter. This is necessary for it to be a valid member of the SpringSecurity filter chain, ensuring it runs exactly once per request.
    @Autowired private JWTutil jwTutil;

    

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String authHeader=request.getHeader("Authorization");
        if (authHeader!=null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwTutil.validateToken(token)) {
                String bridgeId=jwTutil.extractBridgeId(token);
                UsernamePasswordAuthenticationToken auth =new UsernamePasswordAuthenticationToken(bridgeId, null,new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }

}

// The JWTfilter.java file acts as a Security Sentry for your application. Its primary role is to intercept every
//   incoming HTTP request, look for a digital "ID badge" (the JWT token), and verify it before allowing the request to
//   reach your controllers.

//   Here is a breakdown of what the doFilterInternal function does, step-by-step:

//   1. The Interception
//   doFilterInternal is called for every request. Its first job is to look at the HTTP Headers:

//    1 String authHeader = request.getHeader("Authorization");
//   It specifically looks for the Authorization header, where clients typically send the token.

//   2. The Extraction
//   It checks if the header exists and starts with the standard prefix "Bearer ":

//    1 if (authHeader != null && authHeader.startsWith("Bearer ")) {
//    2     String token = authHeader.substring(7); // Removes "Bearer " to get just the JWT
//   If the header is missing or incorrectly formatted, it simply skips the validation logic and passes the request along
//   (where it might later be blocked by Spring Security if the endpoint is protected).

//   3. The Validation (Using JWTutil)
//   It uses the JWTutil bean we just refactored to verify that the token hasn't been tampered with and hasn't expired:

//    1 if (jwTutil.validateToken(token)) {
//    2     String bridgeId = jwTutil.extractBridgeId(token);
//   If validateToken returns true, it extracts the bridgeId (which you stored as the "Subject" during token generation).

//   4. Establishing Identity (The "Security Context")
//   This is the most critical part. Once the token is verified, the filter tells Spring Security: "I trust this user; here
//   is their identity."

//    1 UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(bridgeId, null, new
//      ArrayList<>());
//    2 SecurityContextHolder.getContext().setAuthentication(auth);
//    * UsernamePasswordAuthenticationToken: Creates a trusted "Authentication" object.
//    * SecurityContextHolder: This is a global storage for the current request. By setting the authentication here, other
//      parts of your app (like your Controllers) can now access the bridgeId and know the request is authenticated.

//   5. Continuing the Journey

//    1 chain.doFilter(request, response);
//   Finally, it calls chain.doFilter. This tells the system: "I'm done with my check; pass this request to the next filter
//   or to the Controller."