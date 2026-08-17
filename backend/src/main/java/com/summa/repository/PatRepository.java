package com.summa.repository;

import com.summa.model.Pat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PatRepository extends JpaRepository<Pat, String> {
    Optional<Pat> findByTokenHash(String tokenHash);
    
    Optional<Pat> findByIdAndRevokedAtIsNull(String id);
    
    java.util.List<Pat> findByMemberId(String memberId);
    
    long countByMemberIdAndRevokedAtIsNull(String memberId);
}
