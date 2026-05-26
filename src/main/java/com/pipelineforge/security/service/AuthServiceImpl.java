package com.pipelineforge.security.service;

import com.pipelineforge.security.config.JwtProperties;
import com.pipelineforge.security.dto.AuthResponse;
import com.pipelineforge.security.dto.LoginRequest;
import com.pipelineforge.security.dto.RegisterRequest;
import com.pipelineforge.security.dto.UserResponse;
import com.pipelineforge.security.entity.User;
import com.pipelineforge.security.jwt.JwtService;
import com.pipelineforge.security.repository.UserRepository;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServiceImpl implements AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final JwtProperties jwtProperties;

	public AuthServiceImpl(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtService jwtService,
			JwtProperties jwtProperties
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.jwtProperties = jwtProperties;
	}

	@Override
	public AuthResponse register(RegisterRequest request) {
		String email = request.email().toLowerCase();
		if (userRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
		}
		User user = User.builder()
				.name(request.name())
				.email(email)
				.password(passwordEncoder.encode(request.password()))
				.role(request.role())
				.build();
		User saved = userRepository.save(user);
		String token = jwtService.generateToken(saved);
		Instant expiresAt = Instant.now().plusSeconds(jwtProperties.expirationMinutes() * 60);
		return new AuthResponse(token, "Bearer", expiresAt, toUserResponse(saved));
	}

	@Override
	public AuthResponse login(LoginRequest request) {
		String email = request.email().toLowerCase();
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(email, request.password())
		);
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
		String token = jwtService.generateToken(user);
		Instant expiresAt = Instant.now().plusSeconds(jwtProperties.expirationMinutes() * 60);
		return new AuthResponse(token, "Bearer", expiresAt, toUserResponse(user));
	}

	private UserResponse toUserResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole(),
				user.getCreatedAt()
		);
	}
}
