package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

/**
 * The structured output asked from the model: only the answer text.
 *
 * @param text the answer text
 */
public record ModelText(String text) {
}
