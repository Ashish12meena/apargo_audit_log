package com.apargo.services.audit.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.apargo.services.audit.common.error.ApiException;

class TimeRangeTest {

    private static final Instant NOW = Instant.parse("2026-09-28T12:00:00Z");
    private static final QuerySettings SETTINGS =
            new QuerySettings(Duration.ofDays(7), Duration.ofDays(31), 20, 100, 1000);

    @Test
    void defaultsToTheLastSevenDays() {
        TimeRange range = TimeRange.resolve(null, null, NOW, SETTINGS);

        assertThat(range.to()).isEqualTo(NOW);
        assertThat(range.from()).isEqualTo(NOW.minus(Duration.ofDays(7)));
    }

    @Test
    void rejectsAnInvertedRange() {
        assertThatThrownBy(() -> TimeRange.resolve(NOW, NOW.minusSeconds(1), NOW, SETTINGS))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.fieldErrors()).extracting("field").containsExactly("from"));
    }

    @Test
    void rejectsARangeLongerThanTheMaximum() {
        assertThatThrownBy(() -> TimeRange.resolve(NOW.minus(Duration.ofDays(40)), NOW, NOW, SETTINGS))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.fieldErrors()).extracting("code").containsExactly("OUT_OF_RANGE"));
    }

    @Test
    void validatesPageSize() {
        assertThat(SETTINGS.resolvePageSize(null)).isEqualTo(20);
        assertThatThrownBy(() -> SETTINGS.resolvePageSize(101)).isInstanceOf(ApiException.class);
    }
}
