package id.ac.uns.tutupbuku.service;

import java.math.BigDecimal;

/** Input Indonesia menerima koma atau titik desimal; pemisah ribuan tidak didukung. */
public final class DecimalInput {
    private DecimalInput() { }

    public static BigDecimal parse(String input) {
        if (input == null || !input.trim().matches("[+-]?[0-9]+([.,][0-9]+)?")) {
            throw new IllegalArgumentException("Isi angka tanpa pemisah ribuan, misalnya 87,5.");
        }
        return new BigDecimal(input.trim().replace(',', '.'));
    }
}
