/*
 * Copyright (c) 2018-2024 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package de.adorsys.ledgers.um.api.service;

import de.adorsys.ledgers.um.api.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface UserService {

    /**
     * Creates a new user
     *
     * @param user User business object
     * @return A persisted user
     */
    UserBO create(UserBO user);

    /**
     * Finds a User by its identifier
     *
     * @param id User identifier
     * @return a User
     */
    UserBO findById(String id);

    /**
     * Finds a User by its login
     *
     * @param login User identifier
     * @return a User
     */
    UserBO findByLogin(String login);

    UserBO findByLoginOrEmail(String loginOrEmail);

    /**
     * Update SCA methods by user login
     *
     * @param scaDataList user methods
     * @param userLogin   user login
     * @return The user object.
     */
    UserBO updateScaData(List<ScaUserDataBO> scaDataList, String userLogin);

    UserBO updateAccountAccess(String userLogin, List<AccountAccessBO> accountAccessListBO);

    List<UserBO> listUsers(int page, int size);

    /**
     * Stores a consent in the consent database and returns the original consent
     * if already existing there.
     *
     * @param consentBO the consent object
     * @return the ais consent stored
     */
    AisConsentBO storeConsent(AisConsentBO consentBO);

    /**
     * Loads a consent given the consent id. Throws a consent not found exception.
     *
     * @param consentId the consent id
     * @return the corresponding ais consent.
     */
    AisConsentBO loadConsent(String consentId);

    /**
     * @param countryCode Country Code
     * @param branchId    id of STAFF user
     * @param branchLogin login of STAFF user
     * @param userLogin   login of CUSTOMER user
     * @param roles       List of Roles to filter for
     * @param blocked     Boolean representation of User status
     * @param pageable    pagination info
     * @return Page of Users
     */
    Page<UserExtendedBO> findUsersByMultipleParamsPaged(String countryCode, String branchId, String branchLogin, String userLogin, List<UserRoleBO> roles, Boolean blocked, Pageable pageable);

    Map<String, String> findBranchIdsByMultipleParameters(String countryCode, String branchId, String branchLogin);

    /**
     * Returns list of user logins for given branch.
     *
     * @param branchId branch identifier.
     * @return list of logins.
     */
    List<String> findUserLoginsByBranch(String branchId);

    /**
     * Returns list of users for given branch, which were created after the given date and time.
     *
     * @param branchId branch identifier.
     * @param created  date and time.
     * @return list of users.
     */
    List<UserBO> findUsersByBranchAndCreatedAfter(String branchId, LocalDateTime created);

    /**
     * Counts amount of users for a branch
     *
     * @param branch branch
     * @return amount of users
     */
    int countUsersByBranch(String branch);

    /**
     * Updates user
     *
     * @param userBO user to update
     * @return user entity
     */
    UserBO updateUser(UserBO userBO);

    /**
     * Finds user by IBAN
     *
     * @param iban iban
     * @return user Entity
     */
    List<UserBO> findUsersByIban(String iban);

    /**
     * Finds account owners by IBAN
     *
     * @param iban iban
     * @return owner of account
     */
    List<UserBO> findOwnersByIban(String iban);

    /**
     * Finds account owners by account id
     *
     * @param accountId account id
     * @return users
     */
    List<UserBO> findOwnersByAccountId(String accountId);

    void setBranchBlockedStatus(String userId, boolean isSystemBlock, boolean statusToSet);

    void setUserBlockedStatus(String userId, boolean isSystemBlock, boolean statusToSet);

    boolean isPresentBranchCode(String bban);

    Page<UserBO> getUsersByRoles(List<UserRoleBO> roles, Pageable pageable);

    String decodeStaticTan(String staticTan);
}
