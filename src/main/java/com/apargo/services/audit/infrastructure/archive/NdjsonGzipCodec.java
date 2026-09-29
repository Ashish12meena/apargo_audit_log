package com.apargo.services.audit.infrastructure.archive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Archive file format: gzip-compressed newline-delimited JSON, one document per line. */
final class NdjsonGzipCodec {

    private NdjsonGzipCodec() {
    }

    static byte[] encode(List<String> lines) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            for (String line : lines) {
                gzip.write(line.getBytes(StandardCharsets.UTF_8));
                gzip.write('\n');
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot encode archive content", e);
        }
        return bytes.toByteArray();
    }

    static int countLines(byte[] gzipped) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
            String content = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            return content.isEmpty() ? 0 : (int) content.chars().filter(c -> c == '\n').count();
        } catch (IOException e) {
            return -1;
        }
    }

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
