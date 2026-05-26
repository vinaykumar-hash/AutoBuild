package com.pipelineforge.security.jwt;

import com.pipelineforge.security.config.JwtProperties;
import com.pipelineforge.security.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
	private final JwtProperties properties;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
	}

	public String generateToken(User user) {
		Instant now = Instant.now();
		Instant expiration = now.plusSeconds(properties.expirationMinutes() * 60);
		return Jwts.builder()
				.subject(user.getEmail())
				.issuer(properties.issuer())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiration))
				.claim("role", user.getRole().name())
				.signWith(signingKey())
				.compact();
	}

	public String extractUsername(String token) {
		return extractAllClaims(token).getSubject();
	}

	public boolean isTokenValid(String token, String expectedUsername) {
		String username = extractUsername(token);
		return username.equals(expectedUsername) && !isTokenExpired(token);
	}

	public String extractRole(String token) {
		Object role = extractAllClaims(token).get("role");
		return role == null ? null : role.toString();
	}

	public Instant expirationInstant() {
		return Instant.now().plusSeconds(properties.expirationMinutes() * 60);
	}

	private boolean isTokenExpired(String token) {
		Date expiration = extractAllClaims(token).getExpiration();
		return expiration.before(new Date());
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private SecretKey signingKey() {
		String secret = properties.secret();
		if (secret == null || secret.length() < 32) {
			throw new IllegalStateException("JWT secret must be at least 32 characters");
		}
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}
}
