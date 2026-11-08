package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class TestData {
    private TestData() { }
    static BigDecimal n(String value) { return new BigDecimal(value); }
    static AssessmentItem free(String id, String weight) { return AssessmentItem.free(id, id, n(weight)); }
    static AssessmentItem actual(String id, String weight, String value) {
        return new AssessmentItem(id, id, n(weight), Optional.of(new Score(n(value), n("100"))));
    }
    static void equalNumber(String expected, BigDecimal actual) {
        assertEquals(0, n(expected).compareTo(actual), () -> "Expected " + expected + ", got " + actual);
    }
}
