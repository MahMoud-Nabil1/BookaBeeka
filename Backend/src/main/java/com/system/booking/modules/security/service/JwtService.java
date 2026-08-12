package com.system.booking.modules.security.service;

import com.system.booking.modules.security.dto.CustomerAuthDTO;
import com.system.booking.modules.security.dto.StaffAuthDTO;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    public String generateStaffToken(StaffAuthDTO staff) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_type", "STAFF");
        claims.put("role", staff.role());
        claims.put("tenant_id", staff.tenantId());
        claims.put("branch_id", staff.branchId());

        return buildToken(claims, staff.id().toString());
    }

    public String generateCustomerToken(CustomerAuthDTO customer) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_type", "CUSTOMER");

        return buildToken(claims, customer.id().toString());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}