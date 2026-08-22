package com.taskboard.service;

import com.taskboard.dto.ActivityDtos.ActivityResponse;
import com.taskboard.dto.BoardDtos.*;
import com.taskboard.dto.BoardEvent;
import com.taskboard.entity.*;
import com.taskboard.exception.ApiExceptions.AccessDeniedException;
import com.taskboard.exception.ApiExceptions.ResourceNotFoundException;
import com.taskboard.repository.*;
import com.taskboard.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardListRepository listRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final CurrentUserProvider currentUserProvider;
    private final SimpMessagingTemplate messagingTemplate;

    // ---------- Boards ----------

    @Transactional
    public BoardResponse createBoard(CreateBoardRequest req) {
        User owner = currentUserProvider.getCurrentUser();
        Board board = Board.builder()
                .title(req.title())
                .description(req.description())
                .owner(owner)
                .build();
        board = boardRepository.save(board);
        return toBoardResponse(board);
    }

    public List<BoardResponse> listMyBoards() {
        User owner = currentUserProvider.getCurrentUser();
        return boardRepository.findByOwner(owner).stream().map(this::toBoardResponse).toList();
    }

    public List<ListResponse> getBoardDetail(Long boardId) {
        Board board = getOwnedBoard(boardId);
        return listRepository.findByBoardIdOrderByPositionAsc(board.getId()).stream()
                .map(this::toListResponse)
                .toList();
    }

    public List<ActivityResponse> getBoardActivity(Long boardId) {
        getOwnedBoard(boardId); // ownership check
        return activityRepository.findTop20ByBoardIdOrderByCreatedAtDesc(boardId).stream()
                .map(this::toActivityResponse)
                .toList();
    }

    private Board getOwnedBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found: " + boardId));
        User current = currentUserProvider.getCurrentUser();
        if (!board.getOwner().getId().equals(current.getId())) {
            throw new AccessDeniedException("You do not have access to this board");
        }
        return board;
    }

    // ---------- Lists ----------

    @Transactional
    public ListResponse createList(Long boardId, CreateListRequest req) {
        Board board = getOwnedBoard(boardId);
        int nextPosition = listRepository.findByBoardIdOrderByPositionAsc(boardId).size();

        BoardList list = BoardList.builder()
                .title(req.title())
                .position(nextPosition)
                .board(board)
                .build();
        list = listRepository.save(list);

        ListResponse response = toListResponse(list);
        broadcast(boardId, "LIST_CREATED", response);
        logActivity(board, boardId, "LIST_CREATED", "Created list \"" + list.getTitle() + "\"");
        return response;
    }

    @Transactional
    public void deleteList(Long boardId, Long listId) {
        Board board = getOwnedBoard(boardId);
        BoardList list = listRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found: " + listId));
        String listTitle = list.getTitle();
        listRepository.delete(list);
        broadcast(boardId, "LIST_DELETED", listId);
        logActivity(board, boardId, "LIST_DELETED", "Deleted list \"" + listTitle + "\"");
    }

    // ---------- Cards ----------

    @Transactional
    public CardResponse createCard(Long boardId, Long listId, CreateCardRequest req) {
        Board board = getOwnedBoard(boardId); // ownership check
        BoardList list = listRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found: " + listId));

        int nextPosition = cardRepository.findByListIdOrderByPositionAsc(listId).size();

        Card card = Card.builder()
                .title(req.title())
                .description(req.description())
                .dueDate(req.dueDate())
                .position(nextPosition)
                .list(list)
                .build();
        card = cardRepository.save(card);

        CardResponse response = toCardResponse(card);
        broadcast(boardId, "CARD_CREATED", response);
        logActivity(board, boardId, "CARD_CREATED", "Created card \"" + card.getTitle() + "\" in \"" + list.getTitle() + "\"");
        return response;
    }

    @Transactional
    public CardResponse updateCard(Long boardId, Long cardId, UpdateCardRequest req) {
        Board board = getOwnedBoard(boardId); // ownership check
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));

        boolean moved = false;
        String targetListTitle = null;

        if (req.title() != null) card.setTitle(req.title());
        if (req.description() != null) card.setDescription(req.description());
        if (req.dueDate() != null) card.setDueDate(req.dueDate());
        if (req.position() != null) card.setPosition(req.position());

        if (req.listId() != null && !req.listId().equals(card.getList().getId())) {
            BoardList newList = listRepository.findById(req.listId())
                    .orElseThrow(() -> new ResourceNotFoundException("List not found: " + req.listId()));
            card.setList(newList);
            targetListTitle = newList.getTitle();
            moved = true;
        }

        if (req.assigneeId() != null) {
            User assignee = userRepository.findById(req.assigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.assigneeId()));
            card.setAssignee(assignee);
        }

        card = cardRepository.save(card);
        CardResponse response = toCardResponse(card);
        broadcast(boardId, moved ? "CARD_MOVED" : "CARD_UPDATED", response);

        if (moved) {
            logActivity(board, boardId, "CARD_MOVED", "Moved card \"" + card.getTitle() + "\" to \"" + targetListTitle + "\"");
        } else {
            logActivity(board, boardId, "CARD_UPDATED", "Updated card \"" + card.getTitle() + "\"");
        }

        return response;
    }

    @Transactional
    public void deleteCard(Long boardId, Long cardId) {
        Board board = getOwnedBoard(boardId); // ownership check
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));
        String cardTitle = card.getTitle();
        cardRepository.delete(card);
        broadcast(boardId, "CARD_DELETED", cardId);
        logActivity(board, boardId, "CARD_DELETED", "Deleted card \"" + cardTitle + "\"");
    }

    // ---------- helpers ----------

    private void broadcast(Long boardId, String type, Object payload) {
        messagingTemplate.convertAndSend("/topic/board/" + boardId, new BoardEvent(type, payload));
    }

    private void logActivity(Board board, Long boardId, String action, String description) {
        Activity activity = Activity.builder()
                .board(board)
                .action(action)
                .description(description)
                .build();
        activity = activityRepository.save(activity);
        broadcast(boardId, "ACTIVITY_LOGGED", toActivityResponse(activity));
    }

    private ActivityResponse toActivityResponse(Activity a) {
        return new ActivityResponse(a.getId(), a.getAction(), a.getDescription(), a.getCreatedAt());
    }

    private BoardResponse toBoardResponse(Board b) {
        return new BoardResponse(b.getId(), b.getTitle(), b.getDescription(), b.getCreatedAt());
    }

    private ListResponse toListResponse(BoardList l) {
        List<CardResponse> cards = cardRepository.findByListIdOrderByPositionAsc(l.getId()).stream()
                .map(this::toCardResponse)
                .toList();
        return new ListResponse(l.getId(), l.getTitle(), l.getPosition(), cards);
    }

    private CardResponse toCardResponse(Card c) {
        Long assigneeId = c.getAssignee() != null ? c.getAssignee().getId() : null;
        String assigneeName = c.getAssignee() != null ? c.getAssignee().getName() : null;
        return new CardResponse(c.getId(), c.getTitle(), c.getDescription(), c.getPosition(),
                c.getList().getId(), assigneeId, assigneeName, c.getDueDate(), c.getUpdatedAt());
    }
}
