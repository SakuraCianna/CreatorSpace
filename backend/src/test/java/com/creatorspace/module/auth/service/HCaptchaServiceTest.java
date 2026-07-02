package com.creatorspace.module.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HCaptchaServiceTest {

    @Test
    void verifyAllowsLocalDevelopmentWhenSecretIsBlank() {
        HCaptchaService service = new HCaptchaService("", new ObjectMapper());

        assertTrue(service.verify(null));
        assertTrue(service.verify(""));
    }

    @Test
    void verifyRejectsBlankTokenWhenSecretConfigured() {
        HCaptchaService service = new HCaptchaService("configured-secret", new ObjectMapper());

        assertFalse(service.verify(null));
        assertFalse(service.verify(" "));
    }
}
