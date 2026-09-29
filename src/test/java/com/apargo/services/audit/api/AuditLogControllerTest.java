package com.apargo.services.audit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.apargo.services.audit.TestEvents;
import com.apargo.services.audit.api.context.RequestContextFilter;
import com.apargo.services.audit.api.context.RequestContextResolver;
import com.apargo.services.audit.api.controller.AuditLogController;
import com.apargo.services.audit.api.error.GlobalExceptionHandler;
import com.apargo.services.audit.application.port.out.TransientFailureException;
import com.apargo.services.audit.application.query.AuditLogFilter;
import com.apargo.services.audit.application.query.AuditLogQueryService;
import com.apargo.services.audit.application.query.AuditStatsService;
import com.apargo.services.audit.application.query.CursorResult;
import com.apargo.services.audit.common.constant.ApiHeaders;
import com.apargo.services.audit.common.error.ApiException;
import com.apargo.services.audit.common.error.ErrorCode;
import com.apargo.services.audit.common.error.FieldErrorCode;
import com.apargo.services.audit.domain.mapping.AuditLogFactory;
import com.apargo.services.audit.domain.model.AuditLog;
import com.apargo.services.audit.domain.policy.RetentionPolicy;

/** Web layer only: headers → context, the response wrapper, and error mapping. */
class AuditLogControllerTest {

    private static final String PATH = "/api/v1/audit-logs";

    private final AuditLogQueryService queryService = mock(AuditLogQueryService.class);
    private final AuditStatsService statsService = mock(AuditStatsService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AuditLogController(queryService, statsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new RequestContextResolver())
                .addFilters(new RequestContextFilter())
                .build();
    }

    @Test
    void returnsACursorPageInTheStandardWrapper() throws Exception {
        AuditLog log = new AuditLogFactory(new RetentionPolicy(Duration.ofDays(30), Duration.ofDays(30)))
                .create(TestEvents.audit(), TestEvents.NOW);
        when(queryService.search(any(), isNull(), eq(20))).thenReturn(new CursorResult<>(List.of(log), 20, "next-1"));

        mvc.perform(get(PATH).param("size", "20").param("module", "TEMPLATE")
                        .header(ApiHeaders.ORG_ID, "1001").header(ApiHeaders.PROJECT_ID, "2001")
                        .header(ApiHeaders.REQUEST_ID, "req-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(ApiHeaders.REQUEST_ID, "req-123"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].eventId").value(log.eventId()))
                .andExpect(jsonPath("$.data.items[0].changes[0].oldValue").value("DRAFT"))
                .andExpect(jsonPath("$.data.pagination.nextCursor").value("next-1"))
                .andExpect(jsonPath("$.data.pagination.hasNext").value(true))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.meta.requestId").value("req-123"));

        ArgumentCaptor<AuditLogFilter> filter = ArgumentCaptor.forClass(AuditLogFilter.class);
        verify(queryService).search(filter.capture(), isNull(), eq(20));
        assertThat(filter.getValue().orgId()).isEqualTo(1001L);
        assertThat(filter.getValue().projectId()).isEqualTo(2001L);
        assertThat(filter.getValue().module()).isEqualTo("TEMPLATE");
    }

    @Test
    void requiresTheOrgHeader() throws Exception {
        mvc.perform(get(PATH))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.path").value(PATH))
                .andExpect(header().exists(ApiHeaders.REQUEST_ID));
        verifyNoInteractions(queryService);
    }

    @Test
    void refusesPlatformLevelDataOnPublicRoutes() throws Exception {
        mvc.perform(get(PATH).header(ApiHeaders.ORG_ID, "0"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void rendersFieldErrorsAs422() throws Exception {
        when(queryService.search(any(), any(), any()))
                .thenThrow(ApiException.validation("size", FieldErrorCode.OUT_OF_RANGE, "size must be between 1 and 100"));

        mvc.perform(get(PATH).param("size", "500").header(ApiHeaders.ORG_ID, "1001"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("size"))
                .andExpect(jsonPath("$.errors[0].code").value("OUT_OF_RANGE"));
    }

    @Test
    void rejectsAnUnparseableDateAs422() throws Exception {
        mvc.perform(get(PATH).param("from", "yesterday").header(ApiHeaders.ORG_ID, "1001"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].field").value("from"))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"));
    }

    @Test
    void returnsNotFoundForAnUnknownEvent() throws Exception {
        when(queryService.get(eq("missing"), eq(1001L), isNull()))
                .thenThrow(new ApiException(ErrorCode.AUDIT_LOG_NOT_FOUND));

        mvc.perform(get(PATH + "/missing").header(ApiHeaders.ORG_ID, "1001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AUDIT_LOG_NOT_FOUND"));
    }

    @Test
    void mapsStoreOutagesTo503WithRetryAfter() throws Exception {
        when(queryService.search(any(), any(), any())).thenThrow(new TransientFailureException("down", null));

        mvc.perform(get(PATH).header(ApiHeaders.ORG_ID, "1001"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists(ApiHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
    }
}
