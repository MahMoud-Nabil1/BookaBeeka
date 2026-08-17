package com.system.booking.modules.admin.internal.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Self-contained JWT service for the admin module.
 *
 * <p>Uses the same {@code security.jwt.*} properties and token format as the
 * security module so that when the security team wires their JWT validation
 * filter, it will automatically recognize and validate SuperAdmin tokens.
 *
 * <p>The generated token carries:
 * <ul>
 *   <li>{@code user_type}: "SUPER_ADMIN" — the filter should grant {@code ROLE_SUPER_ADMIN}</li>
 *   <li>{@code sub}: the SuperAdmin's UUID</li>
 * </ul>
 *
 * <p><b>TODO for security team:</b> When wiring the JWT filter, ensure it handles
 * {@code user_type = SUPER_ADMIN} by granting {@code ROLE_SUPER_ADMIN} authority.
 */
@Service
public class AdminJwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    /**
     * Generates a JWT for an authenticated SuperAdmin.
     *
     * @param superAdminId the SuperAdmin's UUID (used as JWT subject)
     * @return signed JWT string
     */
    public String generateToken(UUID superAdminId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_type", "SUPER_ADMIN");

        return Jwts.builder()
                .claims(claims)
                .subject(superAdminId.toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    /** Extracts all claims from a token — used for token validation if needed later. */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Returns true if the token has not yet expired. */
    public boolean isTokenValid(String token) {
        try {
            return extractAllClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
