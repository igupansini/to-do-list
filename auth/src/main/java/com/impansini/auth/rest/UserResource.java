package com.impansini.auth.rest;

import com.impansini.auth.domain.Authority;
import com.impansini.auth.domain.User;
import com.impansini.auth.domain.dto.KeyAndPasswordDTO;
import com.impansini.auth.domain.dto.LoginRequestDTO;
import com.impansini.auth.repository.AuthorityRepository;
import com.impansini.auth.security.JwtUtil;
import com.impansini.auth.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/auth/api")
public class UserResource {

    private final Logger log = LoggerFactory.getLogger(UserResource.class);

    private final UserService userService;
    private final AuthorityRepository authorityRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public UserResource(UserService userService, AuthorityRepository authorityRepository, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authorityRepository = authorityRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user) {
        log.debug("REST request to save a user: {}", user);
        if (user.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A new user cannot already have an ID");
        }
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already in use");
        }
        if (userService.findByUsername(user.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already in use");
        }
        Optional<Authority> userAuthority = authorityRepository.findByName("USER");
        if (userAuthority.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "USER authority not found");
        }
        user.getAuthorities().add(userAuthority.get());
        User result = userService.save(user);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequestDTO user) {
        log.debug("REST request to login for user: {}", user.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
            );
            String jwt = jwtUtil.generateToken(authentication);
            return ResponseEntity.ok(jwt);
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    @PostMapping("/reset-password/start")
    public void requestPasswordReset(@RequestBody String email) {
        log.debug("REST request to generate password reset key for email: {}", email);
        Optional<User> user = userService.requestPasswordReset(email);
        if (user.isPresent()) {
            log.debug("Password reset email should be sent here");
        } else {
            log.debug("Password reset requested for non existing email");
        }
    }

    @PostMapping("/reset-password/finish")
    public void finishPasswordReset(@Valid @RequestBody KeyAndPasswordDTO keyAndPassword) {
        log.debug("REST request to finish password reset");
        Optional<User> user = userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());

        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No user was found for this key");
        }
    }
}
