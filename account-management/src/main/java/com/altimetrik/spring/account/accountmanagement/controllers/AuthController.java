package com.altimetrik.spring.account.accountmanagement.controllers;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import io.jsonwebtoken.Jwts;
import com.github.loki4j.client.util.Loki4jLogger;

import org.slf4j.LoggerFactory;
import com.github.loki4j.slf4j.marker.LabelMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.altimetrik.spring.account.accountmanagement.models.ERole;
import com.altimetrik.spring.account.accountmanagement.models.Role;
import com.altimetrik.spring.account.accountmanagement.models.User;
import com.altimetrik.spring.account.accountmanagement.payload.request.LoginRequest;
import com.altimetrik.spring.account.accountmanagement.payload.request.SignupRequest;
import com.altimetrik.spring.account.accountmanagement.payload.response.UserInfoResponse;
import com.altimetrik.spring.account.accountmanagement.payload.response.MessageResponse;
import com.altimetrik.spring.account.accountmanagement.repository.RoleRepository;
import com.altimetrik.spring.account.accountmanagement.repository.UserRepository;
import com.altimetrik.spring.account.accountmanagement.security.jwt.JwtUtils;
import com.altimetrik.spring.account.accountmanagement.security.services.UserDetailsImpl;
import org.json.simple.JSONObject;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final Logger LOG = LoggerFactory.getLogger(AuthController.class);


  @Autowired
  AuthenticationManager authenticationManager;

  @Autowired
  UserRepository userRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  JwtUtils jwtUtils;

   @Value("${altimetrik.app.jwtSecret}")
  private String jwtSecret;

  private static final SecureRandom secureRandom = new SecureRandom(); //threadsafe
  private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder(); //threadsafe


  @PostMapping("/signin")
  public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

    ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

    List<String> roles = userDetails.getAuthorities().stream()
        .map(item -> item.getAuthority())
        .collect(Collectors.toList());
    Instant inst = Instant.now(); 

    String jwtToken = Jwts.builder()
        .claim("name", userDetails.getUsername())
        .claim("email", userDetails.getEmail())
        .claim("account", userDetails.getAccount())
        .setSubject("account")
        .setId(UUID.randomUUID().toString())
        .setIssuedAt(Date.from(inst))
        .setExpiration(Date.from(inst.plus(5l, ChronoUnit.MINUTES)))
        .compact();
    LOG.info("Login Succesful");
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
        .body(new UserInfoResponse(userDetails.getId(),
                                   userDetails.getUsername(),
                                   userDetails.getEmail(),
                                   jwtToken,
                                   Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)),
                                   roles));
  }

  @PostMapping("/signup")
  public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      LOG.error("Error: Username is already taken!");
      return ResponseEntity
          .badRequest()
          .body(new MessageResponse("Error: Username is already taken!"));
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      LOG.error("Error: Email is already in use!");
      return ResponseEntity
          .badRequest()
          .body(new MessageResponse("Error: Email is already in use!"));
    }
    Long account_id = Long.valueOf(new Date().getTime());
    // Create new user's account
    User user = new User(signUpRequest.getUsername(), 
                         signUpRequest.getEmail(),
                         encoder.encode(signUpRequest.getPassword()),
                         signUpRequest.getFirstName(),
                         signUpRequest.getLastName(),
                         signUpRequest.getZip(),
                         signUpRequest.getBirthday(),
                         account_id
                         );

    Set<String> strRoles = signUpRequest.getRoles();
    Set<Role> roles = new HashSet<>();

    if (strRoles == null) {
      Role userRole = roleRepository.findByName(ERole.ROLE_USER)
          .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
      roles.add(userRole);
    } else {
      strRoles.forEach(role -> {
        switch (role) {
        case "admin":
          Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(adminRole);

          break;
        case "mod":
          Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(modRole);

          break;
        default:
          Role userRole = roleRepository.findByName(ERole.ROLE_USER)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(userRole);
        }
      });
    }

    user.setRoles(roles);
    userRepository.save(user);
    LOG.info("New User "+ signUpRequest.getUsername()+" has been registerred succesfully");
    return ResponseEntity.ok().body(account_id);
  }
}