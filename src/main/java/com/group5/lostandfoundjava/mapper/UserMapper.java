package com.group5.lostandfoundjava.mapper;

import com.group5.lostandfoundjava.dto.auth.RegisterRequest;
import com.group5.lostandfoundjava.dto.user.PublicUserResponse;
import com.group5.lostandfoundjava.dto.user.UpdateProfileRequest;
import com.group5.lostandfoundjava.dto.user.UserResponse;
import com.group5.lostandfoundjava.dto.user.UserSummaryResponse;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Translates between {@link User} and the several shapes it is exposed in.
 *
 * <p>Keeping this out of the DTOs themselves is the point of the pattern: a DTO stays a plain
 * carrier of data with no idea what an entity is, and the services get their conversions from an
 * injected bean they can stub in a test.
 *
 * <p>Three different responses exist for one entity because they are seen by different people.
 * {@link UserResponse} goes to the account's own owner and carries the email, phone and role;
 * {@link PublicUserResponse} goes to anyone at all and carries none of those; and
 * {@link UserSummaryResponse} is the small version embedded next to an item or a message.
 */
@Component
public class UserMapper {

    /**
     * Builds the entity for a new sign-up.
     *
     * <p>The hash is passed in rather than computed here: a mapper should not own a security
     * decision, and the encoder lives in the service.
     *
     * <p>The role is hard-coded to {@link Role#USER}. It is deliberately not taken from the request —
     * otherwise anyone could register themselves an administrator.
     */
    public User toEntity(RegisterRequest request, String encodedPassword) {
        if (request == null) {
            return null;
        }
        return new User(
                trimmed(request.getName()),
                normaliseEmail(request.getEmail()),
                trimmed(request.getPhone()),
                encodedPassword,
                Role.USER);
    }

    /**
     * Copies a partial update onto an existing account.
     *
     * <p>A {@code null} field means "not sent" and leaves the current value alone, which is what
     * makes PATCH-style updates work.
     */
    public void updateEntity(User user, UpdateProfileRequest request) {
        if (user == null || request == null) {
            return;
        }
        if (request.getName() != null) {
            user.setName(request.getName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getProfilePhotoUrl() != null) {
            user.setProfilePhotoUrl(request.getProfilePhotoUrl().trim());
        }
    }

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .ratingAvg(user.getRatingAvg())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserSummaryResponse toSummaryResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserSummaryResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .ratingAvg(user.getRatingAvg())
                .build();
    }

    public PublicUserResponse toPublicResponse(User user) {
        if (user == null) {
            return null;
        }
        return PublicUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .ratingAvg(user.getRatingAvg())
                .memberSince(user.getCreatedAt())
                .build();
    }

    public List<UserResponse> toResponseList(List<User> users) {
        if (users == null) {
            return List.of();
        }
        List<UserResponse> responses = new ArrayList<>(users.size());
        users.forEach(user -> responses.add(toResponse(user)));
        return responses;
    }

    /** One spelling of an address per account, so "Jane@Example.COM" cannot register twice. */
    private String normaliseEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String trimmed(String value) {
        return value == null ? null : value.trim();
    }
}
