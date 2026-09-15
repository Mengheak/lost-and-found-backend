package com.group5.lostandfoundjava.dto.item;

import com.group5.lostandfoundjava.entity.enums.ItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor // required by Jackson to deserialise @RequestBody
@AllArgsConstructor // @Builder needs it back once an explicit constructor is declared
public class UpdateItemStatusRequest {

    private ItemStatus status;
}
