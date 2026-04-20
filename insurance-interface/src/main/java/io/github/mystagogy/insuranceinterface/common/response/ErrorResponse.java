package io.github.mystagogy.insuranceinterface.common.response;

import java.time.Instant;

public record ErrorResponse(Instant timestamp, String path, String message) {
}
