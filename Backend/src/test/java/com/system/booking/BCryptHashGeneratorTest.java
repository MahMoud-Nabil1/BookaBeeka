package com.system.booking;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class BCryptHashGeneratorTest {
    @Test
    void printHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("=== BCRYPT_HASH=" + encoder.encode("superadmin123") + " ===");
    }
}
