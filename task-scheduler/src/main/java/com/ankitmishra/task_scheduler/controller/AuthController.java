package com.ankitmishra.task_scheduler.controller;

import com.ankitmishra.task_scheduler.domain.User;
import com.ankitmishra.task_scheduler.security.JwtService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request){
        Authentication auth=authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.get("username"),
                        request.get("password")
                )
        );

        User user=(User) auth.getPrincipal();
        String token=jwtService.generateToken(user.getUsername(), user.getRole());

        return ResponseEntity.ok(Map.of(
                "token",token,
                "role",user.getRole(),
                "username",user.getUsername()
        ));

    }
}
