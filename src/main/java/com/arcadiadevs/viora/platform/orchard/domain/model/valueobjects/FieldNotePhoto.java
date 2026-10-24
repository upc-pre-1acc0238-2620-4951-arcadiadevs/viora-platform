package com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects;

/**
 * Photo attached to a field note: a JPEG or PNG of at most {@value #MAX_BYTES} bytes. The type is read from the
 * bytes themselves, never trusted from the request.
 *
 * @param content   the image bytes
 * @param mediaType {@code image/jpeg} or {@code image/png}
 */
public record FieldNotePhoto(byte[] content, String mediaType) {

    public static final int MAX_BYTES = 1024 * 1024;

    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";

    public FieldNotePhoto {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("orchard.field_note.photo.empty");
        }
        if (content.length > MAX_BYTES) {
            throw new IllegalArgumentException("orchard.field_note.photo.too_large");
        }
        var detected = mediaTypeOf(content);
        if (detected.isEmpty() || !detected.equals(mediaType)) {
            throw new IllegalArgumentException("orchard.field_note.photo.unsupported");
        }
        content = content.clone();
    }

    /**
     * Builds a photo from uploaded bytes, detecting their type.
     *
     * @param content the image bytes
     * @return the photo
     * @throws IllegalArgumentException if the bytes are empty, too large or neither JPEG nor PNG
     */
    public static FieldNotePhoto of(byte[] content) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("orchard.field_note.photo.empty");
        }
        return new FieldNotePhoto(content, mediaTypeOf(content));
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    private static String mediaTypeOf(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return JPEG;
        }
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return PNG;
        }
        return "";
    }
}
