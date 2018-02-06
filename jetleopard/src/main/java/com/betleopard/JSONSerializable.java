package com.betleopard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;

/**
 * High-level interface to indicate that a domain type is serializable to JSON
 * using Jackson. Also provides a default serialization method and some helpers.
 * 
 * @author kittylyst
 */
public interface JSONSerializable extends Serializable, LongIndexed {

    /**
     * Produces a string containing a JSON representation of the object. The
     * default implementation just falls back to Jackson Java 8 serialization
     * 
     * @return A {@code String} containing JSON
     */
    public default String toJSONString() {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module());
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException jsonx) {
            throw new RuntimeException(jsonx);
        }
    }

    /**
     * Generic helper method that takes a string and a mapping function
     * 
     * @param <E>       The desired output type
     * @param parseText The input data to be parsed
     * @param fn        A {@code Function} object to take a string bag and produce an instance of {@code E}
     * @return          An instance of {@code E}
     */
    public static <E> E parse(final String parseText, final Function<Map<String, ?>, E> fn) {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        try {
            return fn.apply(mapper.readValue(parseText, new TypeReference<Map<String, ?>>() {
            }));
        } catch (IOException iox) {
            throw new RuntimeException(iox);
        }
    }

    /**
     * Static helper method that produces a {@code LocalDateTime} from a bag
     * of date parts. Used for deserializing date fields.
     * 
     * @param dateBits A bag of date parts
     * @return         A deserialized {@code LocalDateTime} object
     */
    public static LocalDateTime parseDateTime(final Map<String, ?> dateBits) {
        final int year = Integer.parseInt("" + dateBits.get("year"));
        final int month = Integer.parseInt("" + dateBits.get("monthValue"));
        final int day = Integer.parseInt("" + dateBits.get("dayOfMonth"));
        final int hour = Integer.parseInt("" + dateBits.get("hour"));
        final int minute = Integer.parseInt("" + dateBits.get("minute"));

        return LocalDateTime.of(year, month, day, hour, minute);
    }
}
