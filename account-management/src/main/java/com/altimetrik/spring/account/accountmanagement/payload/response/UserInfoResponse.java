package com.altimetrik.spring.account.accountmanagement.payload.response;

import java.util.List;

import javax.crypto.SecretKey;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

public class UserInfoResponse {
  private String id;
  private String username;
  private String token;
  private SecretKey key;
  private String email;
  private List<String> roles;

  public UserInfoResponse(String id, String username, String email, String token, SecretKey secretKey, List<String> roles) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.roles = roles;
    this.token = token;
    this.key = secretKey;
  }

  public SecretKey getKey() {
    return key;
}

public void setKey(SecretKey key) {
    this.key = key;
} 

public String getToken() {
    return token;
}

public void setToken(String token) {
    this.token = token;
}

public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public List<String> getRoles() {
    return roles;
  }
}