package com.altimetrik.spring.account.accountmanagement.models;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
  @Id
  private String id;

  @NotBlank
  @Size(max=4)
  private Long account;

  
  public Long   getAccount() {
    return account;
  }

  public void setAccount(Long account) {
    this.account = account;
  }

  @NotBlank
  @Size(max = 20)
  private String username;

  @NotBlank
  @Size(max = 50)
  @Email
  private String email;

  @NotBlank
  @Size(max = 120)
  private String password;

  @NotBlank
  @Size(max = 120)
  private String firstName;

  @NotBlank
  @Size(max = 120)
  private String lastName;

  @NotBlank
  @Size(max = 120)
  private String zip;

  @NotBlank
  @Size(max = 120)
  private String birthday;

  @DBRef
  private Set<Role> roles = new HashSet<>();



  public User(String username, String email, String password, String firstName, String lastName, String zip, String birthday,Long account) {
    this.username = username;
    this.email = email;
    this.password = password;
    this.firstName = firstName;
    this.lastName = lastName;
    this.zip = zip;
    this.birthday = birthday;
    this.account = account;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
}

public void setLastName(String lastName) {
    this.lastName = lastName;
}

public void setZip(String zip) {
    this.zip = zip;
}

public void setBirthday(String birthday) {
    this.birthday = birthday;
}

public String getFirstName() {
    return firstName;
}

public String getLastName() {
    return lastName;
}

public String getZip() {
    return zip;
}

public String getBirthday() {
    return birthday;
}

public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Set<Role> getRoles() {
    return roles;
  }

  public void setRoles(Set<Role> roles) {
    this.roles = roles;
  }
}