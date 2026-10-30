package com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * REST response resource with the latest answers of the producer.
 *
 * @param answers the answers that have not expired, newest first
 */
@Schema(name = "AnswerHistoryResource", description = "The latest answers of the producer, newest first")
@NullMarked
public record AnswerHistoryResource(List<AssistantAnswerResource> answers) {
}
