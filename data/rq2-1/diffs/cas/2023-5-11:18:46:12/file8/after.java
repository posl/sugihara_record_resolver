package org.apereo.cas.util.model;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.val;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * This is {@link Capacity}.
 *
 * @author Misagh Moayyed
 * @since 6.2.0
 */
@Getter
@SuperBuilder
public class Capacity implements Serializable {
    @Serial
    private static final long serialVersionUID = -331719796564884951L;

    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)*)\\s*(\\S+)");

    private final UnitOfMeasure unitOfMeasure;

    private final Double size;

    /**
     * Parse.
     *
     * @param capacity the capacity
     * @return the capacity
     */
    public static Capacity parse(final String capacity) {
        val matcher = SIZE_PATTERN.matcher(capacity);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid capacity definition: " + capacity);
        }
        val count = Double.parseDouble(matcher.group(1));
        val unit = UnitOfMeasure.valueOf(matcher.group(3).toUpperCase(Locale.ENGLISH));
        return Capacity.builder().unitOfMeasure(unit).size(count).build();
    }

    /**
     * Capacity units of measure.
     */
    public enum UnitOfMeasure {
        /**
         * Bytes.
         */
        B,
        /**
         * KiloBytes.
         */
        KB,
        /**
         * MegaBytes.
         */
        MB,
        /**
         * GigaBytes.
         */
        GB,
        /**
         * TeraBytes.
         */
        TB
    }
}
