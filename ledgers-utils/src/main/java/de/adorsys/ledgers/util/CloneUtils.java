/*
 * Copyright (c) 2018-2024 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package de.adorsys.ledgers.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class CloneUtils {
    private static final ObjectMapper objectMapper;

    private CloneUtils() {
    }

    static {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    public static <T> T cloneObject(Object in, Class<T> type) {
        if (in == null) {
            return null;
        }
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(in);
            return objectMapper.readValue(bytes, type);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> List<T> cloneList(List<?> in, Class<T> type) {
        return in.stream().map(t -> cloneObject(t, type)).collect(Collectors.toList());
    }
}
