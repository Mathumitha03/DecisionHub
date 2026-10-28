package com.decisionhub.repository.authentication;

import com.decisionhub.entity.authentication.BlockedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockedEmailRepository extends JpaRepository<BlockedEmail, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<BlockedEmail> findByEmailIgnoreCase(String email);
}
