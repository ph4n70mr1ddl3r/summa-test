package com.summa.repository;

import com.summa.model.BoardTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BoardTaskRepository extends JpaRepository<BoardTask, String> {
    List<BoardTask> findByStatus(String status);
    
    List<BoardTask> findByAssigneeMemberId(String assigneeMemberId);
    
    List<BoardTask> findByInitiativeId(String initiativeId);
    
    List<BoardTask> findByStatusAndPriorityGreaterThanEqual(String status, int priority);
}
