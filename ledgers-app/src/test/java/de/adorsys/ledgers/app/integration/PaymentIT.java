/*
 * Copyright (c) 2018-2023 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package de.adorsys.ledgers.app.integration;

import de.adorsys.ledgers.app.BaseContainersTest;
import de.adorsys.ledgers.app.LedgersApplication;
import de.adorsys.ledgers.app.TestDBConfiguration;
import de.adorsys.ledgers.app.it_endpoints.ManagementStage;
import de.adorsys.ledgers.app.it_endpoints.StatusStage;
import de.adorsys.ledgers.app.it_endpoints.OperationStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.List;
import static de.adorsys.ledgers.app.integration.UserManagementIT.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"testcontainers-it", "sandbox"})
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = LedgersApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ContextConfiguration(classes = {TestDBConfiguration.class},
        initializers = { PaymentIT.Initializer.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PaymentIT extends BaseContainersTest<ManagementStage, OperationStage, StatusStage> {
    public static final String PSU_LOGIN = "anton.brueckner";
    public static final String PSU_LOGIN_NEW = "newuser12345";
    public static final String PSU_EMAIL_NEW = "newuser12345@mail.de";
    public static final String PSU_PASSWORD = "12345";
    public static final String CHALLENGE_VALUE = "123456";
    public static final String ADMIN_LOGIN = "admin";
    public static final String ADMIN_PASSWORD = "admin123";
    public static final String SINGLE_PAYMENT_TYPE = "SINGLE";
    public static final String BULK_PAYMENT_TYPE = "BULK";
    public static final List<String> BULK_PAYMENT_CREDITORS_IBAN = List.of("DE38760700240320465700", "DE80760700240271232400");


    @Test
    void testCreateSinglePayment() {
        given()
                .obtainTokenFromKeycloak(PSU_LOGIN, PSU_PASSWORD);
        when()
                .createSinglePayment("payment.json", "DE80760700240271232400")
                .scaStart("sca_start_payment.json")
                .listScaMethods()
                .selectScaMethod("SMTP_OTP")
                .reportChallengeValue(CHALLENGE_VALUE)
                .getStatus().pathStr("scaStatus", stat -> assertThat(stat).isEqualTo("finalised"));

        then()
                .paymentStatus().pathStr("transactionStatus", status -> assertThat(status).isEqualTo("ACCP"));
    }


    @Test
    void testCreateBulkPayment() {
        given()
                .obtainTokenFromKeycloak(PSU_LOGIN, PSU_PASSWORD);

        when()
                .createBulkPayment("bulk_payment.json", "DE80760700240271232400")
                .scaStart("sca_start_payment.json")
                .listScaMethods()
                .selectScaMethod("SMTP_OTP")
                .reportChallengeValue(CHALLENGE_VALUE)
                .getStatus().pathStr("scaStatus", stat -> assertThat(stat).isEqualTo("finalised"));

        then()
                .paymentStatus().pathStr("transactionStatus", status -> assertThat(status).isEqualTo("ACCP"))
                .readPaymentFromDB()
                .verifyPaymentEntity(payment -> assertThat(payment.get("payment_type")).isEqualTo(BULK_PAYMENT_TYPE))
                .readPaymentTargetsFromDB()
                .verifyPaymentTargetsIban(paymentTargets -> {
                    assertThat(paymentTargets.size()).isEqualTo(2);
                    assertThat(paymentTargets).containsAll(BULK_PAYMENT_CREDITORS_IBAN);
                });
    }

    @Test
    void testCreateNewUserAndCreateSinglePayment() {
        String newIban = "DE62500105174439235992";
        given()
                .obtainTokenFromKeycloak(ADMIN_LOGIN, ADMIN_PASSWORD)
                .createNewUserAsAdmin(PSU_LOGIN_NEW, PSU_EMAIL_NEW, "")
                .createNewAccountForUser("new_account.json", newIban)
                .accountByIban(newIban)
                .depositCash("deposit_amount.json", "100000")
                .obtainTokenFromKeycloak(PSU_LOGIN_NEW, PSU_PASSWORD);

        when()
                .createSinglePayment("payment.json", newIban)
                .scaStart("sca_start_payment.json")
                .listScaMethods()
                .selectScaMethod("SMTP_OTP")
                .reportChallengeValue(CHALLENGE_VALUE)
                .getStatus().pathStr("scaStatus", stat -> assertThat(stat).isEqualTo("finalised"));

        then()
                .paymentStatus().pathStr("transactionStatus", status -> assertThat(status).isEqualTo("ACCP"));
    }

    @Test
    void testCreateNewUserAndCreateBulkPayment() {
        String newIban = "DE62500105174439235992";
        given()
                .obtainTokenFromKeycloak(ADMIN_LOGIN, ADMIN_PASSWORD)
                .createNewUserAsAdmin(PSU_LOGIN_NEW, PSU_EMAIL_NEW, "")
                .createNewAccountForUser("new_account.json", newIban)
                .accountByIban(newIban)
                .depositCash("deposit_amount.json", "10000")
                .obtainTokenFromKeycloak(PSU_LOGIN_NEW, PSU_PASSWORD);

        when()
                .createBulkPayment("bulk_payment.json", newIban)
                .scaStart("sca_start_payment.json")
                .listScaMethods()
                .selectScaMethod("SMTP_OTP")
                .reportChallengeValue(CHALLENGE_VALUE)
                .getStatus().pathStr("scaStatus", stat -> assertThat(stat).isEqualTo("finalised"));

        then()
                .paymentStatus().pathStr("transactionStatus", status -> assertThat(status).isEqualTo("ACCP"))
                .readPaymentFromDB()
                .verifyPaymentEntity( payment -> assertThat(payment.get("payment_type")).isEqualTo(BULK_PAYMENT_TYPE))
                .readPaymentTargetsFromDB()
                .verifyPaymentTargetsIban( paymentTarget -> {
                    assertThat(paymentTarget.size()).isEqualTo(2);
                    assertThat(paymentTarget).containsAll(BULK_PAYMENT_CREDITORS_IBAN);
                });
    }


    @Test
    void blockUserAndFailedPayment() {
        given()
                .obtainTokenFromKeycloak(ADMIN, ADMIN_PASSWORD)
                .getUserIdByLogin(PSU_LOGIN)
                .changeStatusUser()
                .getAllUsers()
                .path("findAll { o -> o.login.equals(\"" + PSU_LOGIN + "\") }[0].blocked", blocked -> assertThat(blocked).isEqualTo(true))
                .obtainTokenFromKeycloak(PSU_LOGIN, PSU_PASSWORD);

        when()
                .failedSinglePayment("payment.json", "DE80760700240271232400");


        then()
                .pathStr("devMessage", message -> assertThat(message).isEqualTo("Access Denied! You're trying to access resources you have no permission for."));
    }

}
