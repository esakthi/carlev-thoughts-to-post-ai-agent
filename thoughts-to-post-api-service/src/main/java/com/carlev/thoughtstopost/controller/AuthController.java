package com.carlev.thoughtstopost.controller;

import com.carlev.thoughtstopost.dto.AuthResponse;
import com.carlev.thoughtstopost.dto.LoginRequest;
import com.carlev.thoughtstopost.dto.RegisterRequest;
import com.carlev.thoughtstopost.model.UserAccount;
import com.carlev.thoughtstopost.repository.UserAccountRepository;
import com.carlev.thoughtstopost.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        if (userAccountRepository.existsById(request.getEmail())) {
            return ResponseEntity.badRequest().body("User already exists");
        }

        UserAccount user = UserAccount.builder()
                .userId(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userAccountRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwtToken)
                .expiresIn(jwtService.getExpirationTime())
                .build());
    }
}
