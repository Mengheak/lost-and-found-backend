package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.common.exception.BadRequestException;
import com.group5.lostandfoundjava.common.exception.NotFoundException;
import com.group5.lostandfoundjava.dto.user.PublicUserResponse;
import com.group5.lostandfoundjava.dto.user.UpdateProfileRequest;
import com.group5.lostandfoundjava.dto.user.UserResponse;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.repository.UserRepository;
import com.group5.lostandfoundjava.service.UserService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return UserResponse.from(findUser(userId));
    }

    /** A {@code null} field means "not sent", so it keeps its current value. */
    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BadRequestException("Name must not be blank");
            }
            user.setName(request.name().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().trim());
        }
        if (request.profilePhotoUrl() != null) {
            user.setProfilePhotoUrl(request.profilePhotoUrl().trim());
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public PublicUserResponse getPublicProfile(UUID userId) {
        return PublicUserResponse.from(findUser(userId));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
