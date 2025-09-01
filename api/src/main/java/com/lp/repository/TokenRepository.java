package com.lp.repository;

import com.lp.entities.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Integer> {
    @Query("""
select t from Token t inner join User u on t.user.id = u.id where u.id = :userId and t.revoked = false and t.tokenType = com.lp.enums.TokenType.ACCESS
""")
    List<Token> findValidAccessTokensByUserId(Integer userId);

    @Query("""
select t from Token t inner join User u on t.user.id = u.id where u.id = :userId and t.revoked = false and t.tokenType = com.lp.enums.TokenType.REFRESH
""")
    List<Token> findValidRefreshTokensByUserId(Integer userId);

    Optional<Token> findByToken(String token);
}
