package com.group5.lostandfoundjava.service;

import com.group5.lostandfoundjava.dto.user.PublicUserResponse;
import com.group5.lostandfoundjava.dto.user.UpdateProfileRequest;
import com.group5.lostandfoundjava.dto.user.UserResponse;
import java.util.UUID;

/** Reading and editing user profiles. */
public interface UserService {

    /** The caller's own profile, including private fields such as the email. */
    UserResponse getProfile(UUID userId);

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    /** Somebody else's profile, trimmed down to the fields that are safe to show. */
    PublicUserResponse getPublicProfile(UUID userId);
}
