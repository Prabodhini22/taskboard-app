package com.taskboard.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class BoardDtos {

    public record CreateBoardRequest(@NotBlank String title, String description) {}

    public record BoardResponse(Long id, String title, String description, Instant createdAt) {}

    public record CreateListRequest(@NotBlank String title) {}

    public record ListResponse(Long id, String title, Integer position, List<CardResponse> cards) {}

    public record CreateCardRequest(@NotBlank String title, String description) {}

    public record UpdateCardRequest(String title, String description, Long listId, Integer position, Long assigneeId) {}

    public record CardResponse(Long id, String title, String description, Integer position,
                                Long listId, Long assigneeId, String assigneeName, Instant updatedAt) {}
}
