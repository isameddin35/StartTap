package com.bmu1093a.quill.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDto {

    private String email;

    private String username;

    private String firstname;

    private String lastname;

    private String password;
}
