package com.bmu1093a.quill.auth.service;

import com.bmu1093a.quill.auth.error.UserNotFoundException;
import com.bmu1093a.quill.auth.error.WrongPasswordException;
import com.bmu1093a.quill.auth.model.dto.login.LoginRequestDto;
import com.bmu1093a.quill.auth.model.dto.login.LoginResponseDto;
import com.bmu1093a.quill.auth.model.dto.register.RegisterRequestDto;
import com.bmu1093a.quill.auth.model.dto.register.RegisterResponseDto;
import com.bmu1093a.quill.auth.model.entity.Role;
import com.bmu1093a.quill.auth.model.entity.User;
import com.bmu1093a.quill.auth.repository.UserRepository;
import com.bmu1093a.quill.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public RegisterResponseDto register(RegisterRequestDto registerRequestDto) {
        if (userRepository.findByEmail(registerRequestDto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(registerRequestDto.getUsername());
        user.setEmail(registerRequestDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
        user.setRole(Role.USER);

        userRepository.save(user);

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getRole().name());

        registerRequestDto.setPassword(null);

        return new RegisterResponseDto(user.getId(),
                registerRequestDto.getUsername(),
                registerRequestDto.getEmail(),
                user.getRole(),accessToken,refreshToken,"Qeydiyyat ugurlu oldu.");
    }

    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new WrongPasswordException("Wrong password");
        }

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getRole().name());

        return new LoginResponseDto(user.getId(), user.getUsername(),
                user.getEmail(), user.getRole(), accessToken, refreshToken,
                "Login ugurlu oldu");
    }

}
