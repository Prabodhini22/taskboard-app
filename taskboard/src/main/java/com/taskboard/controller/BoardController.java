package com.taskboard.controller;

import com.taskboard.dto.ActivityDtos.ActivityResponse;
import com.taskboard.dto.BoardDtos.*;
import com.taskboard.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @PostMapping
    public ResponseEntity<BoardResponse> createBoard(@Valid @RequestBody CreateBoardRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.createBoard(req));
    }

    @GetMapping
    public List<BoardResponse> myBoards() {
        return boardService.listMyBoards();
    }

    @GetMapping("/{boardId}")
    public List<ListResponse> boardDetail(@PathVariable Long boardId) {
        return boardService.getBoardDetail(boardId);
    }

    @GetMapping("/{boardId}/activity")
    public List<ActivityResponse> boardActivity(@PathVariable Long boardId) {
        return boardService.getBoardActivity(boardId);
    }

    @PostMapping("/{boardId}/lists")
    public ResponseEntity<ListResponse> createList(@PathVariable Long boardId,
                                                   @Valid @RequestBody CreateListRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.createList(boardId, req));
    }

    @DeleteMapping("/{boardId}/lists/{listId}")
    public ResponseEntity<Void> deleteList(@PathVariable Long boardId, @PathVariable Long listId) {
        boardService.deleteList(boardId, listId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{boardId}/lists/{listId}/cards")
    public ResponseEntity<CardResponse> createCard(@PathVariable Long boardId,
                                                   @PathVariable Long listId,
                                                   @Valid @RequestBody CreateCardRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.createCard(boardId, listId, req));
    }

    @PatchMapping("/{boardId}/cards/{cardId}")
    public CardResponse updateCard(@PathVariable Long boardId,
                                   @PathVariable Long cardId,
                                   @RequestBody UpdateCardRequest req) {
        return boardService.updateCard(boardId, cardId, req);
    }

    @DeleteMapping("/{boardId}/cards/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long boardId, @PathVariable Long cardId) {
        boardService.deleteCard(boardId, cardId);
        return ResponseEntity.noContent().build();
    }
}
