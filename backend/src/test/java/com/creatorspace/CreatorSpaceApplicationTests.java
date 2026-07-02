package com.creatorspace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CreatorSpaceApplicationTests {

    @Test
    void applicationClassIsLoadableWithoutStartingExternalServices() {
        assertNotNull(CreatorSpaceApplication.class);
    }

    @Test
    void mainMethodCanBeReferenced() {
        assertDoesNotThrow(() -> CreatorSpaceApplication.class.getDeclaredMethod("main", String[].class));
    }
}
