package com.decisionhub.repository.authentication;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.decisionhub.entity.authentication.OAuthAccount;
import com.decisionhub.entity.authentication.User;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<OAuthAccount> findByProviderAndUser(String provider, User user);

    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
}
