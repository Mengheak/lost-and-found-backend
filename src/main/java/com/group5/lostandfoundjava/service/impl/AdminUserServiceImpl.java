package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.exception.BadRequestException;
import com.group5.lostandfoundjava.exception.NotFoundException;
import com.group5.lostandfoundjava.dto.user.UserResponse;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.repository.UserRepository;
import com.group5.lostandfoundjava.service.AdminUserService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    public AdminUserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String keyword, Pageable pageable) {
        String term = keyword == null ? "" : keyword.trim();

        Page<User> page = term.isEmpty()
                ? userRepository.findAll(pageable)
                : userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, pageable);

        return PageResponse.from(page.map(UserResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse get(UUID userId) {
        return UserResponse.from(findUser(userId));
    }

    /**
     * Two guards keep the admin area from becoming unreachable: an admin cannot change their own
     * role, and the last remaining admin cannot be demoted.
     *
     * <p>The affected user keeps their old permissions until their next login or token refresh,
     * because the role is baked into the access token when it is issued.
     */
    @Override
    @Transactional
    public UserResponse updateRole(UUID actingAdminId, UUID userId, Role role) {
        User user = findUser(userId);

        // Setting the role a user already has changes nothing, so it needs no guarding.
        if (user.getRole() == role) {
            return UserResponse.from(user);
        }
        if (user.getId().equals(actingAdminId)) {
            throw new BadRequestException("You cannot change your own role");
        }
        if (user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BadRequestException("Cannot demote the last remaining admin");
        }

        user.setRole(role);
        return UserResponse.from(userRepository.save(user));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
