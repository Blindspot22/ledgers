/*
 * Copyright (c) 2018-2024 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package de.adorsys.ledgers.um.api.service;

import de.adorsys.ledgers.um.api.domain.EmailVerificationBO;
import de.adorsys.ledgers.um.api.domain.EmailVerificationStatusBO;

public interface ScaVerificationService {

    EmailVerificationBO findByScaIdAndStatusNot(String scaId, EmailVerificationStatusBO status);

    EmailVerificationBO findByToken(String token);

    EmailVerificationBO findByTokenAndStatus(String token, EmailVerificationStatusBO statusBO);

    void updateEmailVerification(EmailVerificationBO emailVerificationBO);

    boolean sendMessage(String subject, String from, String email, String message);

    void deleteByScaId(String scaId);
}