package com.shringaar.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.shringaar.store.config.StoreProperties;
import com.shringaar.store.service.Formats;

import org.junit.jupiter.api.Test;

class FormatsTest {

    private final Formats fmt = new Formats(new StoreProperties("S", "", "0", "", "", "", "", "", "", "₹", null, "x", "Asia/Kolkata", null));

    @Test
    void usesIndianDigitGrouping() {
        assertThat(fmt.money(new BigDecimal("950"))).isEqualTo("₹950");
        assertThat(fmt.money(new BigDecimal("6500"))).isEqualTo("₹6,500");
        assertThat(fmt.money(new BigDecimal("85000.00"))).isEqualTo("₹85,000");
        assertThat(fmt.money(new BigDecimal("170000"))).isEqualTo("₹1,70,000");
        assertThat(fmt.money(new BigDecimal("12345678.5"))).isEqualTo("₹1,23,45,678.50");
    }
}
