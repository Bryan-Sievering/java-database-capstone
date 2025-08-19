package com.project.back_end.services;

import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
// Java
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class TokenService {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    private SecretKey signingKey;

    @Value("${jwt.secret}")
    private String secret;

    private final long EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

    public TokenService(AdminRepository adminRepository,
                        DoctorRepository doctorRepository,
                        PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @PostConstruct
    public void init() {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Always pass the numeric ID as string, e.g., tokenService.generateToken(user.getId().toString())
    public String generateToken(String idAsString) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .setSubject(idAsString)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

// Java
public String extractIdentifier(String token) {
    Jws<Claims> jws = Jwts.parser()           // returns JwtParserBuilder on newer JJWT
            .setSigningKey(signingKey)
            .build()                          // build the parser
            .parseClaimsJws(token);
    return jws.getBody().getSubject();
}

    // Validate token by checking existence of the ID for the given user type
    public boolean validateToken(String token, String userType) {
        try {
            String subject = extractIdentifier(token);
            Long id = Long.parseLong(subject);

            return switch (userType.toLowerCase()) {
                case "admin" -> adminRepository.existsById(id);
                case "doctor" -> doctorRepository.existsById(id);
                case "patient" -> patientRepository.existsById(id);
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    public SecretKey getSigningKey() {
        return signingKey;
    }

    public Long extractDoctorId(String token) {
        String subject = extractIdentifier(token);
        try {
            Long id = Long.parseLong(subject);
            if (!doctorRepository.existsById(id)) {
                throw new RuntimeException("Doctor not found");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid doctor identifier in token");
        }
    }

    public Long extractPatientId(String token) {
        String subject = extractIdentifier(token);
        try {
            Long id = Long.parseLong(subject);
            if (!patientRepository.existsById(id)) {
                throw new RuntimeException("Patient not found");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid patient identifier in token");
        }
    }
}