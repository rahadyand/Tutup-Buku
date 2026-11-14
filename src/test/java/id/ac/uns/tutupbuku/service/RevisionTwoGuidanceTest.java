package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RevisionTwoGuidanceTest {
    @Test
    void greetingUsesOnlyCurrentInputAndCompleteRecognizedNim() {
        assertTrue(
                NimGuidance.welcome("Anto", "L0125001")
                        .contains("Anto dari Informatika angkatan 2025!"));
        assertTrue(
                NimGuidance.welcome("Dina", "L0224009")
                        .contains("Dina dari Sains Data angkatan 2024!"));
        for (String input : new String[] {"", "L02", "L9925001", "0001", "L01ab001"}) {
            String value = NimGuidance.welcome("Anto", input);
            assertFalse(value.contains("angkatan 20"));
            assertFalse(value.contains("dari Informatika"));
            assertFalse(value.contains("dari Sains Data"));
        }
        assertFalse(NimGuidance.welcome("", "L0125001").contains("Anto"));
        assertFalse(NimGuidance.welcome("", "L0125001").contains("Contoh"));
    }
}
