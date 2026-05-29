package com.cmulagent.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceSlot {
    private String id;
    private String name;
    private String type;
    private int maxCapacity;
    private int usedCapacity;
    private boolean available;
}