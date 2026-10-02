package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Exact bytes of a rendered PDF dossier. The array is copied on the way in and on the way out, so the stored
 * content, and therefore its hash, cannot be altered through a retained reference.
 */
public final class DossierDocument {

    /** Upper bound of a stored dossier: 10 MiB. */
    public static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final byte[] content;

    /**
     * Wraps the bytes of a PDF document.
     *
     * @param content PDF bytes, starting with the {@code %PDF-} header and at most {@value #MAX_BYTES} bytes
     * @throws IllegalArgumentException if the content is missing, not a PDF or too large
     */
    public DossierDocument(byte[] content) {
        if (content == null || content.length < PDF_MAGIC.length
                || !Arrays.equals(content, 0, PDF_MAGIC.length, PDF_MAGIC, 0, PDF_MAGIC.length)) {
            throw new IllegalArgumentException("settlement.certification.document.invalid");
        }
        if (content.length > MAX_BYTES) {
            throw new IllegalArgumentException("settlement.certification.document.too_large");
        }
        this.content = content.clone();
    }

    /**
     * Returns a copy of the document bytes.
     *
     * @return the PDF bytes
     */
    public byte[] content() {
        return content.clone();
    }

    /**
     * Returns the size of the document.
     *
     * @return number of bytes
     */
    public int size() {
        return content.length;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof DossierDocument document
                && Arrays.equals(content, document.content));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "DossierDocument[size=" + content.length + " bytes]";
    }
}
