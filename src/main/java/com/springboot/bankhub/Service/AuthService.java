package com.springboot.bankhub.Service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.springboot.bankhub.AuthConfig.JwtUtil;
import com.springboot.bankhub.Dao.UserDao;
import com.springboot.bankhub.Models.AuthRequest;
import com.springboot.bankhub.Models.RegisterRequest;
import com.springboot.bankhub.Models.User;

import java.util.HashMap;
import java.util.Map;
import com.springboot.bankhub.Utils.UserAlreadyExistsException;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(@Lazy AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserDetailsService userDetailsService, UserDao userDao, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    public ResponseEntity<?> register(RegisterRequest registerRequest) {

        if (userDao.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email " + registerRequest.getEmail() + " is already in use.");
        }

        if (userDao.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username " + registerRequest.getUsername() + " is already in use.");
        }

        try {
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setEmail(registerRequest.getEmail());
            user.setFirstName(registerRequest.getFirstName());
            user.setLastName(registerRequest.getLastName());

            // Set the role directly using the UserRole enum from RegisterRequest
            user.setRole(registerRequest.getRole());

            userDao.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "User created successfully");

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Map<String, String>> login(AuthRequest authRequest) throws Exception {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );
        } catch (AuthenticationException e) {
            // return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect username or password");
            Map<String, String> response = new HashMap<>();
            response.put("message", "Incorrect username or password");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);

        Map<String, String> response = new HashMap<>();
        response.put("token", jwt);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
