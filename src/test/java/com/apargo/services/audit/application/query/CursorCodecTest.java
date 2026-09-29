package com.apargo.services.audit.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.apargo.platform.contract.event.EventIds;
import com.apargo.services.audit.common.error.ApiException;
import com.apargo.services.audit.common.error.ErrorCode;

class CursorCodecTest {

    @Test
    void roundTrips() {
        PageCursor cursor = new PageCursor(Instant.parse("2026-09-25T08:45:20.123Z"), EventIds.newEventId());

        assertThat(CursorCodec.decode(CursorCodec.encode(cursor))).isEqualTo(cursor);
    }

    @Test
    void blankMeansFirstPage() {
        assertThat(CursorCodec.decode(null)).isNull();
        assertThat(CursorCodec.decode(" ")).isNull();
    }

    @Test
    void rejectsTamperedCursors() {
        assertThatThrownBy(() -> CursorCodec.decode("bm90LWEtY3Vyc29y"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(e.fieldErrors()).extracting("field").containsExactly("cursor");
                });
    }
}
