/*
 * Copyright (c) 2018-2024 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package de.adorsys.ledgers.util.hash;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.adorsys.ledgers.util.Base16;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@SuppressWarnings("java:S1130")
public class HashGeneratorImpl implements HashGenerator {
    private static final Logger logger = LoggerFactory.getLogger(HashGeneratorImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
                                                             .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                                             .registerModule(new JavaTimeModule());

    public static final String ERROR_MSG = "Can't generate the hash";

    @Override
    public <T> String hash(HashItem<T> hashItem) throws HashGenerationException {

        // Get string value including hash
        MessageDigest digest;
        byte[] valueAsBytes;
        try {
            String alg = StringUtils.isBlank(hashItem.getAlg()) ? DEFAULT_HASH_ALG : hashItem.getAlg();
            digest = MessageDigest.getInstance(alg);
            valueAsBytes = objectMapper.writeValueAsBytes(hashItem.getItem());
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {//NOSONAR
            logger.error(ERROR_MSG, e);
            throw new HashGenerationException(ERROR_MSG, e);
        }

        return Base16.encode(digest.digest(valueAsBytes));
    }
}
