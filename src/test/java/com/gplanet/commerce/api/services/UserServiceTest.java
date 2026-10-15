package com.gplanet.commerce.api.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gplanet.commerce.api.dtos.user.UserDTO;
import com.gplanet.commerce.api.dtos.user.UserMapper;
import com.gplanet.commerce.api.dtos.user.UserResponseDTO;
import com.gplanet.commerce.api.dtos.user.UserUpdateDTO;
import com.gplanet.commerce.api.entities.User;
import com.gplanet.commerce.api.exceptions.ApiException;
import com.gplanet.commerce.api.exceptions.ResourceNotFoundException;
import com.gplanet.commerce.api.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    public static final String TEST_NAME = "Test User";
    public static final String TEST_USERNAME = "test@example.com";
    public static final String TEST_PASSWD = "testPasswd123!";
    private static final Long TEST_USER_ID = 1L;
    private static final Long NON_EXISTENT_USER_ID = 999L;

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;
    
    @InjectMocks
    private UserService userService;

    private User createTestUser() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setName(TEST_NAME);
        user.setEmail(TEST_USERNAME);
        user.setPassword(TEST_PASSWD);
        user.setRole(User.Role.USER);
        user.setCreationDate(LocalDateTime.now());
        return user;
    }
    
    private UserDTO createTestUserDTO() {
        return new UserDTO(TEST_NAME, TEST_USERNAME, "password123");
    }
    
    @Test
    void registerUserValidDataSuccess() {
        // Arrange
        UserDTO userDTO = createTestUserDTO();
        User savedUser = createTestUser();
        UserResponseDTO expectedResponse = new UserResponseDTO(
            1L, userDTO.name(), userDTO.email(), 
            User.Role.USER, savedUser.getCreationDate());
        
        when(passwordEncoder.encode(userDTO.password())).thenReturn(TEST_PASSWD);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toUserResponseDTO(savedUser)).thenReturn(expectedResponse);
        
        // Act
        UserResponseDTO result = userService.registerUser(userDTO);
        
        // Assert
        assertNotNull(result);
        assertEquals(savedUser.getId(), result.id());
        assertEquals(savedUser.getName(), result.name());
        assertEquals(savedUser.getEmail(), result.email());
        assertEquals(User.Role.USER, result.role());
        
        verify(passwordEncoder).encode(userDTO.password());
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void registerUserExistingEmailThrowsException() {
        // Arrange
        UserDTO userDTO = new UserDTO(TEST_NAME, "existing@example.com", "password123");
        
        when(passwordEncoder.encode(userDTO.password())).thenReturn(TEST_PASSWD);
        when(userRepository.save(any(User.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate email"));
        
        assertThrows(ApiException.class, () -> 
            userService.registerUser(userDTO));
        
        verify(passwordEncoder).encode(userDTO.password());
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void updateProfileValidDataSuccess() {
        // Arrange
        User existingUser = createTestUser();
        UserUpdateDTO profileDTO = new UserUpdateDTO(
            "Updated Name", "new@example.com");
        User updatedUser = createTestUser();
        updatedUser.setName(profileDTO.name());
        updatedUser.setEmail(profileDTO.newEmail());
        UserResponseDTO expectedResponse = new UserResponseDTO(
            1L, profileDTO.name(), profileDTO.newEmail(), 
            User.Role.USER, LocalDateTime.now());
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toUserResponseDTO(updatedUser)).thenReturn(expectedResponse);
        
        // Act
        UserResponseDTO result = userService.updateProfile(TEST_USER_ID, profileDTO);
        
        // Assert
        assertNotNull(result);
        assertEquals(profileDTO.name(), result.name());
        assertEquals(profileDTO.newEmail(), result.email());
    }
    
    @Test
    void changePasswordValidDataSuccess() {
        // Arrange
        UserUpdateDTO profileDTO = new UserUpdateDTO(
            "oldPassword", "newPassword", "newPassword");
        
        User user = new User();
        user.setPassword("encodedOldPassword");
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(profileDTO.currentPassword(), user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(profileDTO.newPassword())).thenReturn("encodedNewPassword");
        when(userRepository.save(user)).thenReturn(user);
        
        // Act & Assert
        assertDoesNotThrow(() -> 
            userService.updateProfile(TEST_USER_ID, profileDTO));
        
        verify(userRepository).save(user);
        assertEquals("encodedNewPassword", user.getPassword());
    }


    @Test
    void getProfileExistingUserSuccess() {
        // Arrange
        User user = createTestUser();
        UserResponseDTO expectedResponse = new UserResponseDTO(
            user.getId(), user.getName(), user.getEmail(), 
            user.getRole(), user.getCreationDate()
        );

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponseDTO(user)).thenReturn(expectedResponse);

        // Act
        UserResponseDTO result = userService.getProfile(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse.id(), result.id());
        assertEquals(expectedResponse.name(), result.name());
        assertEquals(expectedResponse.email(), result.email());
        assertEquals(expectedResponse.role(), result.role());
        
        verify(userRepository).findById(TEST_USER_ID);
        verify(userMapper).toUserResponseDTO(user);
    }

    @Test
    void getProfileNonExistentUserThrowsException() {
        // Arrange
        when(userRepository.findById(NON_EXISTENT_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> 
            userService.getProfile(NON_EXISTENT_USER_ID));
        
        verify(userRepository).findById(NON_EXISTENT_USER_ID);
        verify(userMapper, never()).toUserResponseDTO(any());
    }

    @Test
    void listUsersSuccess() {
        // Arrange
        List<User> users = Arrays.asList(
            createTestUser(),
            createTestUser()
        );
        Page<User> usersPage = new PageImpl<>(users);
        List<UserResponseDTO> expectedResponses = users.stream()
            .map(u -> new UserResponseDTO(u.getId(), u.getName(), 
                u.getEmail(), u.getRole(), u.getCreationDate()))
            .collect(Collectors.toList());

        when(userRepository.findAll(any(Pageable.class))).thenReturn(usersPage);
        when(userMapper.toUserResponseDTO(any(User.class)))
            .thenReturn(expectedResponses.get(0), expectedResponses.get(1));

        // Act
        Page<UserResponseDTO> result = userService.listUsers(0, 10, "email", "ASC");

        // Assert
        Page<UserResponseDTO> expectedPage = new PageImpl<>(expectedResponses);
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(expectedPage.getTotalElements(), result.getTotalElements());
        verify(userRepository).findAll(any(Pageable.class));
        verify(userMapper, times(2)).toUserResponseDTO(any(User.class));
    }

    @Test
    void changeRoleExistingUserSuccess() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser();
        User.Role newRole = User.Role.ADMIN;

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        assertDoesNotThrow(() -> userService.changeRole(userId, newRole));

        // Assert
        assertEquals(newRole, user.getRole());
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void changeRoleNonExistentUserThrowsException() {
        // Arrange
        User.Role newRole = User.Role.ADMIN;

        when(userRepository.findById(NON_EXISTENT_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            userService.changeRole(NON_EXISTENT_USER_ID, newRole));
        
        verify(userRepository).findById(NON_EXISTENT_USER_ID);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeRoleSameRoleSuccess() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser();
        User.Role sameRole = user.getRole();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        assertDoesNotThrow(() -> userService.changeRole(userId, sameRole));

        // Assert
        assertEquals(sameRole, user.getRole());
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }
}
