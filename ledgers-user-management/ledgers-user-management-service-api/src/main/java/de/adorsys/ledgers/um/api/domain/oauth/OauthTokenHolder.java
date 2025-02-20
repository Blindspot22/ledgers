/*
 * Copyright (c) 2018-2024 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package de.adorsys.ledgers.um.api.domain.oauth;

import lombok.Data;

@Data
public class OauthTokenHolder {
    private String oldToken;
    private boolean finalStage;

    public OauthTokenHolder(String oldToken, boolean finalStage) {
        this.oldToken = oldToken;
        this.finalStage = finalStage;
    }
}
