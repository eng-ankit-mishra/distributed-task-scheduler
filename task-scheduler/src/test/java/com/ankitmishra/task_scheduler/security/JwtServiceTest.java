package com.ankitmishra.task_scheduler.security;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JwtServiceTest {
    private JwtService jwtService;

    private static final String SECRET=
            "dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0cy1tdXN0LWJlLWxvbmctZW5vdWdo";

    @BeforeEach
    void  setUp(){
        jwtService=new JwtService();
        ReflectionTestUtils.setField(jwtService,"jwtSecret",SECRET);
        ReflectionTestUtils.setField(jwtService,"jwtExpirationMs",8640000L);

    }

    @Test
    void generateToken_extractedUserNameShouldMatchInput(){
        String token=jwtService.generateToken("admin","ROLE_ADMIN");
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
    }

    @Test
    void generateToken_extractedRoleShouldMatchInput(){
        String token=jwtService.generateToken("admin","ROLE_ADMIN");
        assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidTokenAndMatchingUser(){
        String token=jwtService.generateToken("admin","ROLE_ADMIN");
        UserDetails userDetails=buildUserDetails("admin");

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }


    @Test
    void isTokenValid_shouldReturnFalseForWrongUsername() {
        String token = jwtService.generateToken("admin", "ROLE_ADMIN");
        UserDetails wrongUser = buildUserDetails("viewer");

        assertThat(jwtService.isTokenValid(token, wrongUser)).isFalse();
    }

    @Test
    void parseClaims_shouldThrowForTamperedToken() {
        String token = jwtService.generateToken("admin", "ROLE_ADMIN");
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        assertThatThrownBy(() -> jwtService.extractUsername(tampered))
                .isInstanceOf(Exception.class);
    }

    private UserDetails buildUserDetails(String username) {
        return User.withUsername(username)
                .password("password")
                .authorities(List.of())
                .build();
    }
    




}
