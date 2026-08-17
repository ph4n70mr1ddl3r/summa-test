package com.summa.repository;

import com.summa.model.GovernanceSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GovernanceSettingRepository extends JpaRepository<GovernanceSetting, String> {
}
