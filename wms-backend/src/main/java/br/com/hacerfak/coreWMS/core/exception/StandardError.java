package br.com.hacerfak.coreWMS.core.exception;

import java.time.Instant;

public record StandardError(
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path) {
}
