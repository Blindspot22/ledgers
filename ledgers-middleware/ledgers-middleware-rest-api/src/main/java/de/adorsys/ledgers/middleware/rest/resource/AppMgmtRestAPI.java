/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.ledgers.middleware.rest.resource;

import de.adorsys.ledgers.middleware.api.domain.um.BearerTokenTO;
import de.adorsys.ledgers.middleware.api.domain.um.UserTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "LDG001 - Application Management", description = "Application management")
public interface AppMgmtRestAPI {
    String BASE_PATH = "/management/app";

    @GetMapping("/ping")
    @Operation(tags = UnprotectedEndpoint.UNPROTECTED_ENDPOINT, summary = "Echo the server")
    ResponseEntity<String> ping();

    @PostMapping("/init")
    @Operation(summary = "Initializes the deposit account module.")
    ResponseEntity<Void> initApp();

    @PostMapping("/admin")
    @Operation(tags = UnprotectedEndpoint.UNPROTECTED_ENDPOINT, summary = "Creates the admin account. This is only done if the application has no account yet. Returns a bearer token admin can use to proceed with further operations.")
    ResponseEntity<BearerTokenTO> admin(@RequestBody(required = true) UserTO adminUser);
}
