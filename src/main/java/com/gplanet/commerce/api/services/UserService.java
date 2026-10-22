package com.gplanet.commerce.api.services;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gplanet.commerce.api.configs.app.props.DemoProperties;
import com.gplanet.commerce.api.dtos.user.UserDTO;
import com.gplanet.commerce.api.dtos.user.UserMapper;
import com.gplanet.commerce.api.dtos.user.UserResponseDTO;
import com.gplanet.commerce.api.dtos.user.UserUpdateDTO;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.exceptions.DuplicateResourceException;
import com.gplanet.commerce.api.exceptions.PasswordMismatchException;
import com.gplanet.commerce.api.exceptions.ProtectedAccountException;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.repositories.UserRepository;
import com.gplanet.commerce.api.utilities.DatabaseOperationHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class that handles user-related operations including registration,
 * authentication, profile updates, and password management.
 * <p>
 * The email, password and role of the shared demo accounts ({@link DemoProperties#getProtectedAccounts()}) can't be
 * changed, so no visitor can lock the others out of them.
 * </p>
 * 
 * @author Gustavo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private static final String USER_NOT_FOUND_MSG = "User not found";
    private static final String DUPLICATE_EMAIL_MSG = "A user with this email already exists";
    private static final String PROTECTED_CREDENTIALS_MSG =
        "The email and password of the demo accounts can't be changed; register your own account to try it";
    private static final String PROTECTED_ROLE_MSG = "The role of the demo accounts can't be changed";
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoProperties demoProperties;

    /**
     * Registers a new user in the system.
     *
     * @param userDTO Data transfer object containing user registration information
     * @return UserResponseDTO containing the created user's information
     * @throws DuplicateResourceException if another user already has the email
     */
    @Transactional
    public UserResponseDTO registerUser(UserDTO userDTO) {
        log.info("Registering new user with email: {}", userDTO.email());
        // Checked up front so the client gets a clear message; the unique constraint still guards races
        if (userRepository.existsByEmail(userDTO.email())) {
            throw new DuplicateResourceException(DUPLICATE_EMAIL_MSG);
        }
        User user = new User();
        user.setName(userDTO.name());
        user.setEmail(userDTO.email());
        user.setPassword(passwordEncoder.encode(userDTO.password()));
        user.setRole(userDTO.role());
        user.setCreationDate(LocalDateTime.now());

        User savedUser = DatabaseOperationHandler.executeOperation(() -> 
            userRepository.save(user)
        );
        log.info("User successfully registered with ID: {}", savedUser.getId());
        return userMapper.toUserResponseDTO(savedUser);
    }

    /**
     * Updates a user's profile information.
     * 
     * @param userId ID of the user
     * @param profileDTO Data transfer object containing new profile information
     * @return UserResponseDTO containing the updated user information
     * @throws UsernameNotFoundException if user is not found
     * @throws DuplicateResourceException if another user already has the new email
     * @throws ProtectedAccountException if the update would change the email or password of a demo account
     */
    @Transactional
    public UserResponseDTO updateProfile(Long userId, UserUpdateDTO profileDTO) {
        log.info("Updating profile for user ID: {}", userId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MSG));
        if (demoProperties.isProtectedAccount(user.getEmail()) && changesCredentials(user, profileDTO)) {
            throw new ProtectedAccountException(PROTECTED_CREDENTIALS_MSG);
        }
        // Update basic profile fields only when provided (partial update semantics)
        if (profileDTO.newEmail() != null) {
            String newEmail = profileDTO.newEmail().trim();
            if (newEmail.isEmpty()) {
                throw new IllegalArgumentException("The new email cannot be empty");
            }
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new DuplicateResourceException(DUPLICATE_EMAIL_MSG);
            }
            user.setEmail(newEmail);
        }

        if (profileDTO.name() != null) {
            String name = profileDTO.name().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("The name cannot be empty");
            }
            user.setName(name);
        }

        // If any password field is present, attempt password change flow
        if (profileDTO.currentPassword() != null || profileDTO.newPassword() != null
                || profileDTO.confirmPassword() != null) {
            changePassword(user, profileDTO);
        }

        User updatedUser = DatabaseOperationHandler.executeOperation(() -> 
            userRepository.save(user)
        );
        log.info("Profile updated successfully for user: {}", updatedUser.getEmail());
        return userMapper.toUserResponseDTO(updatedUser);
    }

    /**
     * Whether a profile update would change the email or the password of the user. Any password field counts as a
     * password change, even an invalid one.
     *
     * @param user the existing user
     * @param profileDTO the profile update
     * @return true if the update changes the email or the password
     */
    private static boolean changesCredentials(User user, UserUpdateDTO profileDTO) {
        boolean changesEmail = profileDTO.newEmail() != null && !profileDTO.newEmail().trim().equals(user.getEmail());
        return changesEmail || profileDTO.currentPassword() != null || profileDTO.newPassword() != null
                || profileDTO.confirmPassword() != null;
    }

    /**
     * Helper method to change the password of the user when updating the profile.
     *
     * @param user the existing user whose password is going to change 
     * @param profileDTO user profile with password change details
     * @throws IllegalArgumentException if any of the password change parameters is null
     * @throws UsernameNotFoundException if the user is not found
     * @throws PasswordMismatchException if the current password is not correct or 
     *          new password is not confirmed correctly
     */
    private void changePassword(User user, UserUpdateDTO profileDTO) {
        log.info("Password change attempt for user: {}", user.getEmail());

        // Require all three fields for a password change
        if (profileDTO.currentPassword() == null || profileDTO.newPassword() == null
                || profileDTO.confirmPassword() == null) {
            throw new IllegalArgumentException(
                    "The fields currentPassword, newPassword and confirmPassword are required");
        }

        // Verify current password
        if (!passwordEncoder.matches(profileDTO.currentPassword(), user.getPassword())) {
            throw new PasswordMismatchException("The current password is incorrect");
        }

        // Verify new password is not the same as current
        if (passwordEncoder.matches(profileDTO.newPassword(), user.getPassword())) {
            throw new IllegalArgumentException("The new password must be different from the current password");
        }

        // Verify password confirmation
        if (!profileDTO.newPassword().equals(profileDTO.confirmPassword())) {
            throw new PasswordMismatchException("The new password and its confirmation don't match");
        }

        user.setPassword(passwordEncoder.encode(profileDTO.newPassword()));
        log.info("Password updated for user: {}", user.getEmail());
    }
    
    /**
     * Retrieves a user's profile information.
     * 
     * @param userId ID of the user
     * @return UserResponseDTO containing the user's information
     * @throws UsernameNotFoundException if user is not found
     */
    public UserResponseDTO getProfile(Long userId) {
        log.debug("Retrieving profile for user ID: {}", userId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MSG));
        return userMapper.toUserResponseDTO(user);
    }
    

    /**
     * Lists all users in the system with pagination support.
     * 
     * @param page The page number (zero-based)
     * @param size The page size
     * @param sort The field to sort by
     * @param direction The sort direction (ASC or DESC)
     * @return Page of UserResponseDTO containing paginated users' information
     */
    public Page<UserResponseDTO> listUsers(int page, int size, String sort, String direction) {
        log.debug("Listing users with pagination - page: {}, size: {}, sort: {}, direction: {}", 
                page, size, sort, direction);
        
        // Create Pageable object with sort direction
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        // Get paginated result
        Page<User> usersPage = userRepository.findAll(pageable);
        
        // Map to DTOs
        Page<UserResponseDTO> result = usersPage.map(userMapper::toUserResponseDTO);
        
        log.debug("Found {} users on page {} of {}", 
                result.getNumberOfElements(), 
                result.getNumber() + 1,  // +1 for human-readable page number
                result.getTotalPages());
                
        return result;
    }

    /**
     * Updates a user's role.
     * 
     * @param userId ID of the user
     * @param newRole New role to assign
     * @throws ResourceNotFoundException if user is not found
     * @throws ProtectedAccountException if the user is a demo account and the role is different
     */
    @Transactional
    public void changeRole(Long userId, User.Role newRole) {
        log.info("Changing role to {} for user ID: {}", newRole, userId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No user exists with the provided ID"
            ));
        if (newRole != user.getRole() && demoProperties.isProtectedAccount(user.getEmail())) {
            throw new ProtectedAccountException(PROTECTED_ROLE_MSG);
        }
        
        user.setRole(newRole);
        userRepository.save(user);
        log.info("Role successfully updated for user ID: {}", userId);
    }
}
