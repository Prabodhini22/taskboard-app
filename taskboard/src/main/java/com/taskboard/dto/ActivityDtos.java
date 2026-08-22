package com.taskboard.dto;

import java.time.Instant;

public class ActivityDtos {
    public record ActivityResponse(Long id, String action, String description, Instant createdAt) {}
}
