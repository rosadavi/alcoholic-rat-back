package com.rosadavi.alcoholic_rat.auth.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.rosadavi.alcoholic_rat.domain.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private static final String ISSUER = "alcoholic-rat";

    private final Algorithm algorithm;
    private final Duration expriation;

    public TokenService(@Value("$(app.jwt.secret)") String secret, @Value("$(app.jwt.expiration-hours)") long expirationHours) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expriation = Duration.ofHours(expirationHours);
    }

    public String generateToken(User user) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.getLogin())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(expriation))
                .sign(algorithm);
    }

    // return token login, or null if token is invalid/expired
    public String validateToken(String token) {
        try {
            return JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException e ) {
            return null;
        }
    }
}
