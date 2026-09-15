package com.group5.lostandfoundjava.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body used both to create and to rename a category. Admins only. */
@Data
@Builder
@NoArgsConstructor // required by Jackson to deserialise @RequestBody
@AllArgsConstructor // @Builder needs it back once an explicit constructor is declared
public class CategoryRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name length must be less than 100")
    private String name;

    @Size(max = 1024, message = "iconUrl length must be less than 1024")
    private String iconUrl;
}
