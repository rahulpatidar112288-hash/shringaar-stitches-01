package com.shringaar.store.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.shringaar.store.config.StoreProperties;

import org.springframework.stereotype.Component;

/** Formatting used by templates as ${@fmt.money(price)}. Indian digit grouping: ₹1,20,000. */
@Component("fmt")
public class Formats {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DOW = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH);

    private final StoreProperties store;

    public Formats(StoreProperties store) {
        this.store = store;
    }

    public String money(BigDecimal amount) {
        if (amount == null) return "";
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        boolean negative = value.signum() < 0;
        String plain = value.abs().toPlainString();
        String whole = plain.substring(0, plain.indexOf('.'));
        String paise = plain.substring(plain.indexOf('.') + 1);
        String grouped = groupIndian(whole) + ("00".equals(paise) ? "" : "." + paise);
        return (negative ? "-" : "") + store.currencySymbol() + grouped;
    }

    /** 12345678 → 1,23,45,678 (last three digits, then pairs). */
    static String groupIndian(String digits) {
        if (digits.length() <= 3) return digits;
        String last3 = digits.substring(digits.length() - 3);
        String rest = digits.substring(0, digits.length() - 3);
        StringBuilder sb = new StringBuilder();
        int firstGroup = rest.length() % 2 == 0 ? 2 : 1;
        sb.append(rest, 0, firstGroup);
        for (int i = firstGroup; i < rest.length(); i += 2) sb.append(',').append(rest, i, i + 2);
        return sb.append(',').append(last3).toString();
    }

    public String day(LocalDate d) { return d == null ? "" : d.format(DAY); }
    public String dow(LocalDate d) { return d == null ? "" : d.format(DOW); }
    public String time(LocalTime t) { return t == null ? "" : t.format(TIME).toLowerCase(Locale.ENGLISH); }
    public String stamp(Instant i) { return i == null ? "" : i.atZone(ZONE).format(STAMP); }
}
