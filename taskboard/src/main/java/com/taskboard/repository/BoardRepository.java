package com.taskboard.repository;

import com.taskboard.entity.Board;
import com.taskboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByOwner(User owner);
    List<Board> findByOwnerId(Long ownerId);
}
