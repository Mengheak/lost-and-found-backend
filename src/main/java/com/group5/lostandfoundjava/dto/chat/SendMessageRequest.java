package com.group5.lostandfoundjava.dto.chat;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Either field may be left out, but the service refuses a message that has neither. */
@Data
@Builder
@NoArgsConstructor // required by Jackson to deserialise @RequestBody
@AllArgsConstructor // @Builder needs it back once an explicit constructor is declared
public class SendMessageRequest {

    @Size(max = 10_000, message = "text length must be less than 10000")
    private String text;

    @Size(max = 1024, message = "imageUrl length must be less than 1024")
    private String imageUrl;
}
