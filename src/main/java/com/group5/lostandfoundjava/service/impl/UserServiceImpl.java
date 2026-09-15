package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.dto.user.PublicUserResponse;
import com.group5.lostandfoundjava.dto.user.UpdateProfileRequest;
import com.group5.lostandfoundjava.dto.user.UserResponse;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.exception.BadRequestException;
import com.group5.lostandfoundjava.exception.NotFoundException;
import com.group5.lostandfoundjava.mapper.UserMapper;
import com.group5.lostandfoundjava.repository.UserRepository;
import com.group5.lostandfoundjava.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return userMapper.toResponse(findUser(userId));
    }

    /** A {@code null} field means "not sent", so it keeps its current value. */
    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);

        // Checked here rather than in the mapper: an empty name is a rule about what callers may
        // ask for, not part of copying one object onto another.
        if (request.getName() != null && request.getName().isBlank()) {
            throw new BadRequestException("Name must not be blank");
        }

        userMapper.updateEntity(user, request);

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public PublicUserResponse getPublicProfile(UUID userId) {
        return userMapper.toPublicResponse(findUser(userId));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
