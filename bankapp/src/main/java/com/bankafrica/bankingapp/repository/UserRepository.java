package com.bankafrica.bankingapp.repository;

import com.bankafrica.bankingapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByIdNumber(String idNumber);

    boolean existsByEmail(String email);

    boolean existsByIdNumber(String idNumber);
}