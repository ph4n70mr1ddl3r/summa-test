package com.summa.repository;

import com.summa.model.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NodeRepository extends JpaRepository<Node, String> {
    Optional<Node> findByPubkey(String pubkey);
    
    List<Node> findByStatus(String status);
    
    long countByStatus(String status);
}
