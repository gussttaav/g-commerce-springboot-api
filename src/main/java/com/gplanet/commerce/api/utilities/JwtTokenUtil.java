package com.gplanet.commerce.api.utilities;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.gplanet.commerce.api.configs.app.props.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

/**
 * Utility class for handling JWT (JSON Web Token) operations such as token generation,
 * validation, and extraction of claims.
 * <p>
 * This class uses the {@link JwtProperties} configuration to sign and validate tokens.
 * </p>
 * 
 * @author Gustavo
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class JwtTokenUtil {
    
    /**
     * The JWT properties containing secret, expiration, and issuer information.
     */
    private final JwtProperties jwtProperties;
    
    /**
     * Retrieves the signing key used for JWT token creation and validation.
     *
     * @return the {@link SecretKey} derived from the configured secret.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }
    
    /**
     * Generates a JWT token for the specified user details.
     * The token includes the username as the subject and the user's roles as claims.
     *
     * @param userDetails the user details for which the token is generated.
     * @return a signed JWT token as a String.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        
        // Add roles to claims
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        claims.put("roles", roles);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Validates the JWT token against the provided user details.
     * Checks if the username matches and if the token is not expired.
     *
     * @param token        the JWT token to validate.
     * @param userDetails  the user details to compare with the token's subject.
     * @return {@code true} if the token is valid; {@code false} otherwise.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
    
    /**
     * Extracts the username (subject) from the JWT token.
     *
     * @param token the JWT token.
     * @return the username contained in the token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extracts the roles claim from the JWT token.
     *
     * @param token the JWT token.
     * @return a list of roles contained in the token.
     */
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObject = claims.get("roles");
        
        //Ensure the roles object is a list, filter the string type
        //elements and do a proper cast to match the return type
        if (rolesObject instanceof List<?> rolesList) {
            return rolesList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        
        return List.of();
    }
    
    /**
     * Extracts the expiration date from the JWT token.
     *
     * @param token the JWT token.
     * @return the expiration date of the token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Extracts a specific claim from the JWT token using the provided resolver function.
     *
     * @param <T>            the type of the claim to extract.
     * @param token          the JWT token.
     * @param claimsResolver a function to extract the desired claim from the token's claims.
     * @return the extracted claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Extracts all claims from the JWT token.
     *
     * @param token the JWT token.
     * @return the {@link Claims} object containing all claims.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * Checks if the JWT token is expired.
     *
     * @param token the JWT token.
     * @return {@code true} if the token is expired; {@code false} otherwise.
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
