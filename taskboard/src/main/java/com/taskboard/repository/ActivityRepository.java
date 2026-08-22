package com.taskboard.repository;

import com.taskboard.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findTop20ByBoardIdOrderByCreatedAtDesc(Long boardId);
}
