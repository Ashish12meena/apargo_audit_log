package com.apargo.services.audit.application.ingest;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Reads event JSON into contract types with a private mapper (never the web mapper, so API
 * settings can't change what we accept).
 * <ul>
 *   <li>Unknown properties are skipped and reported: a producer on a newer contract copy
 *       added an optional field this service does not store yet.</li>
 *   <li>Enum values are strict: an unknown value fails decoding (dead-lettered, replayable
 *       after this service updates its contract copy).</li>
 * </ul>
 */
public final class EventDecoder {

    private static final int MAX_ERROR_LENGTH = 300;

    private final ObjectMapper mapper;

    public EventDecoder(UnknownFieldListener unknownFieldListener) {
        Objects.requireNonNull(unknownFieldListener, "unknownFieldListener");
        this.mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .addHandler(new ReportingProblemHandler(unknownFieldListener))
                .build();
    }

    public <T> Decoded<T> decode(byte[] value, Class<T> type) {
        if (value == null || value.length == 0) {
            return Decoded.failure("value is empty");
        }
        try {
            T decoded = mapper.readValue(value, type);
            return decoded == null ? Decoded.failure("value is JSON null") : Decoded.success(decoded);
        } catch (JsonProcessingException e) {
            return Decoded.failure(truncate(e.getOriginalMessage()));
        } catch (IOException e) {
            return Decoded.failure(truncate(e.getMessage()));
        }
    }

    private static String truncate(String message) {
        if (message == null) {
            return "unreadable JSON";
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH) + "...";
    }

    /** Result of decoding: exactly one of {@code value} and {@code error} is set. */
    public record Decoded<T>(T value, String error) {

        static <T> Decoded<T> success(T value) {
            return new Decoded<>(value, null);
        }

        static <T> Decoded<T> failure(String error) {
            return new Decoded<>(null, error);
        }

        public boolean isSuccess() {
            return error == null;
        }
    }

    @FunctionalInterface
    public interface UnknownFieldListener {
        void onUnknownField(String type, String field);
    }

    private static final class ReportingProblemHandler extends DeserializationProblemHandler {

        private final UnknownFieldListener listener;

        private ReportingProblemHandler(UnknownFieldListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser parser,
                                             JsonDeserializer<?> deserializer, Object beanOrClass,
                                             String propertyName) throws IOException {
            String type = beanOrClass instanceof Class<?> c ? c.getSimpleName()
                    : beanOrClass != null ? beanOrClass.getClass().getSimpleName() : "unknown";
            listener.onUnknownField(type, propertyName);
            parser.skipChildren();
            return true;
        }
    }
}
