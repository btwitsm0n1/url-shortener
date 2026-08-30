package com.askumni.urlshortener.service;

import org.springframework.stereotype.Component;

/**
 * Converts a numeric database ID into a short alphanumeric code, and back.
 *
 * WHY BASE62?
 * A URL-safe short code can only use [a-z, A-Z, 0-9] = 62 characters.
 * Base62 packs numbers into fewer characters than base10 (decimal),
 * e.g. ID 125 -> "cb" instead of "125". Bigger the ID, more the savings.
 *
 * WHY ENCODE THE DB ID INSTEAD OF RANDOM STRINGS?
 * - Guaranteed uniqueness (no collision, no need to re-check DB)
 * - O(1) generation, no retry loop
 * TRADE-OFF: IDs are sequential, so short codes are predictable/guessable.
 * (This is exactly the kind of trade-off interviewers want you to discuss —
 * mention the alternative: random hash + collision check, which trades
 * predictability for extra DB lookups.)
 */
@Component
public class Base62Encoder {

    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length(); // 62

    public String encode(long id) {
        if (id == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        long value = id;
        while (value > 0) {
            int remainder = (int) (value % BASE);
            sb.append(ALPHABET.charAt(remainder));
            value /= BASE;
        }
        return sb.reverse().toString();
    }

    public long decode(String shortCode) {
        long result = 0;
        for (char c : shortCode.toCharArray()) {
            result = result * BASE + ALPHABET.indexOf(c);
        }
        return result;
    }
}
