package com.taskboard.dto;

/**
 * Broadcast to /topic/board/{boardId} whenever a card is created, updated, moved, or deleted.
 * type is one of: CARD_CREATED, CARD_UPDATED, CARD_MOVED, CARD_DELETED, LIST_CREATED
 */
public record BoardEvent(String type, Object payload) {
}
