package vn.edu.fpt.cinema.booking.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.TreeMap;
import java.util.stream.Collectors;
import vn.edu.fpt.cinema.booking.dto.CustomerBookingRequest;

/** SHA-256 of a normalised request: the same purchase hashes the same regardless of seat or line order. */
public final class RequestHasher {
    private static final String VERSION = "v1";

    private RequestHasher() {}

    public static String hash(String operation, CustomerBookingRequest request) {
        var concessions = new TreeMap<String, Integer>();
        request.concessionsOrEmpty().forEach(item -> concessions.merge(item.concessionId().trim(), item.quantity(), Integer::sum));
        String voucher = request.voucherCode() == null ? "" : request.voucherCode().trim().toUpperCase(Locale.ROOT);
        String canonical = operation + ":" + VERSION
            + "|hold=" + request.holdId().trim()
            + "|showtime=" + request.showtimeId().trim()
            + "|seats=" + request.showtimeSeatIds().stream().map(String::trim).sorted().collect(Collectors.joining(","))
            + "|concessions=" + concessions.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(","))
            + "|voucher=" + voucher;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
