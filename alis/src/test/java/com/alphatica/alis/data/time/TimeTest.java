package com.alphatica.alis.data.time;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TimeTest {

    @Test
    void shouldConvertToLocalDate() {
        assertEquals(new Time(2025_01_01).toLocalDate(), LocalDate.of(2025, 1, 1));
        assertEquals(new Time(1900_02_28).toLocalDate(), LocalDate.of(1900, 2, 28));
        assertEquals(new Time(2020_02_29).toLocalDate(), LocalDate.of(2020, 2, 29));
    }

    @Test
    @SuppressWarnings("java:S5778")
    void shouldThrowException() {
        assertThrows(DateTimeException.class, () -> new Time(2025_00_01).toLocalDate());
        assertThrows(DateTimeException.class, () -> new Time(2025_13_01).toLocalDate());
        assertThrows(DateTimeException.class, () -> new Time(2025_01_32).toLocalDate());
        assertThrows(DateTimeException.class, () -> new Time(2025_02_29).toLocalDate());
        assertThrows(DateTimeException.class, () -> new Time(1900_02_29).toLocalDate());
        assertThrows(DateTimeException.class, () -> new Time(2025_04_31).toLocalDate());
    }
}