package com.bmu1093a.quill.auth.model.dto.login;

import com.bmu1093a.quill.auth.model.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private String accessToken;
    private String refreshToken;
    private String message;
}
