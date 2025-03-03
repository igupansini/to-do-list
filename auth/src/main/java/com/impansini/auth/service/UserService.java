package com.impansini.auth.service;

import com.impansini.auth.domain.User;
import com.impansini.auth.repository.UserRepository;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Autowired
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User save(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        log.debug("Request to save a user: {}", user);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        log.debug("Request to get a user by username: {}", username);
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        log.debug("Request to get a user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    @Transactional
    public Optional<User> requestPasswordReset(String email) {
        log.debug("Request to generate password reset key for email: {}", email);
        return userRepository
                .findByEmail(email)
                .map(user -> {
                    user.setPasswordResetKey(RandomStringUtils.random(10, true, true));
                    user.setPasswordResetDate(LocalDateTime.now());
                    userRepository.save(user);
                    return user;
                });
    }

    @Transactional
    public Optional<User> completePasswordReset(String newPassword, String key) {
        log.debug("Request to reset user password for key: {}", key);
        return userRepository
                .findByPasswordResetKey(key)
                .filter(user -> user.getPasswordResetDate().isAfter(LocalDateTime.now().minusHours(24)))
                .map(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setPasswordResetKey(null);
                    user.setPasswordResetDate(null);
                    userRepository.save(user);
                    return user;
                });
    }
}
