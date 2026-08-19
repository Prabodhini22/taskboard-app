package com.taskboard.repository;

import com.taskboard.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByListIdOrderByPositionAsc(Long listId);
}
