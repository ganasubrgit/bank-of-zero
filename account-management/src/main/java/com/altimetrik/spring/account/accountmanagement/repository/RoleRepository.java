package com.altimetrik.spring.account.accountmanagement.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.altimetrik.spring.account.accountmanagement.models.ERole;
import com.altimetrik.spring.account.accountmanagement.models.Role;

public interface RoleRepository extends MongoRepository<Role, String> {
  Optional<Role> findByName(ERole name);
}