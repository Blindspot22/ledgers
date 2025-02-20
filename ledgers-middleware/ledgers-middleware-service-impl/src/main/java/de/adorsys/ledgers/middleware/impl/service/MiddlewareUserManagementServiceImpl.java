/*
 * Copyright (c) 2018-2024 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package de.adorsys.ledgers.middleware.impl.service;

import de.adorsys.ledgers.deposit.api.domain.DepositAccountBO;
import de.adorsys.ledgers.deposit.api.service.DepositAccountService;
import de.adorsys.ledgers.keycloak.client.api.KeycloakDataService;
import de.adorsys.ledgers.keycloak.client.model.KeycloakUser;
import de.adorsys.ledgers.middleware.api.domain.account.AccountIdentifierTypeTO;
import de.adorsys.ledgers.middleware.api.domain.account.AccountReferenceTO;
import de.adorsys.ledgers.middleware.api.domain.account.AdditionalAccountInformationTO;
import de.adorsys.ledgers.middleware.api.domain.general.RecoveryPointTO;
import de.adorsys.ledgers.middleware.api.domain.sca.ScaInfoTO;
import de.adorsys.ledgers.middleware.api.domain.um.*;
import de.adorsys.ledgers.middleware.api.exception.MiddlewareModuleException;
import de.adorsys.ledgers.middleware.api.service.MiddlewareRecoveryService;
import de.adorsys.ledgers.middleware.api.service.MiddlewareUserManagementService;
import de.adorsys.ledgers.middleware.impl.converter.AdditionalAccountInformationMapper;
import de.adorsys.ledgers.middleware.impl.converter.KeycloakUserMapper;
import de.adorsys.ledgers.middleware.impl.converter.PageMapper;
import de.adorsys.ledgers.middleware.impl.converter.UserMapper;
import de.adorsys.ledgers.um.api.domain.*;
import de.adorsys.ledgers.um.api.service.UserService;
import de.adorsys.ledgers.util.domain.CustomPageImpl;
import de.adorsys.ledgers.util.domain.CustomPageableImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static de.adorsys.ledgers.middleware.api.exception.MiddlewareErrorCode.INSUFFICIENT_PERMISSION;
import static de.adorsys.ledgers.middleware.api.exception.MiddlewareErrorCode.REQUEST_VALIDATION_FAILURE;
import static java.lang.String.format;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MiddlewareUserManagementServiceImpl implements MiddlewareUserManagementService {
    private static final int NANO_TO_SECOND = 1000000000;
    private final UserService userService;
    private final DepositAccountService depositAccountService;
    private final AccessService accessService;
    private final UserMapper userTOMapper = Mappers.getMapper(UserMapper.class);
    private final PageMapper pageMapper;
    private final AdditionalAccountInformationMapper additionalInfoMapper;
    private final MiddlewareRecoveryService recoveryService;
    private final KeycloakDataService dataService;
    private final KeycloakUserMapper keycloakUserMapper;
    private final KeycloakDataService keycloakDataService;

    @Value("${ledgers.sca.multilevel.enabled:false}")
    private boolean multilevelScaEnable;
    @Value("${ledgers.sca.final.weight:100}")
    private int finalWeight;

    @Override
    @Transactional
    public UserTO create(UserTO user) {
        UserBO createdUser = userService.create(userTOMapper.toUserBO(user));
        try {
            KeycloakUser keycloakUser = keycloakUserMapper.toKeycloakUser(createdUser, user.getPin());
            dataService.createUser(keycloakUser);
        } catch (Exception e) {
            throw MiddlewareModuleException.builder()
                          .errorCode(INSUFFICIENT_PERMISSION)
                          .devMsg(format("Could not register user at IDP msg: %s", e.getMessage()))
                          .build();
        }
        if (createdUser.getUserRoles().contains(UserRoleBO.STAFF)) {
            RecoveryPointTO point = new RecoveryPointTO(format("Registered %s user", user.getLogin()));
            recoveryService.createRecoveryPoint(createdUser.getBranch(), point);
        }
        return userTOMapper.toUserTO(createdUser);
    }

    @Override
    public UserTO findById(String id) {
        return userTOMapper.toUserTO(userService.findById(id));
    }

    @Override
    public UserTO findByUserLogin(String userLogin) {
        return userTOMapper.toUserTO(userService.findByLogin(userLogin));
    }

    @Override
    public UserTO updateScaData(String userLogin, List<ScaUserDataTO> scaDataList) {
        UserBO userBO = userService.updateScaData(userTOMapper.toScaUserDataListBO(scaDataList), userLogin);
        return userTOMapper.toUserTO(userBO);
    }

    @Override
    public void updateAccountAccess(ScaInfoTO scaInfo, String userId, AccountAccessTO access) {
        UserTO user = findById(userId);
        DepositAccountBO account = depositAccountService.getAccountById(access.getAccountId());
        accessService.updateAccountAccessNewAccount(account, userTOMapper.toUserBO(user), access.getScaWeight(),access.getAccessType());
    }

    @Override
    public List<UserTO> listUsers(int page, int size) {
        long start = System.nanoTime();
        List<UserTO> users = userTOMapper.toUserTOList(userService.listUsers(page, size));
        log.info("Retrieving: {} users in {} seconds", users.size(), (double) (System.nanoTime() - start) / NANO_TO_SECOND);
        return users;
    }

    @Override
    public CustomPageImpl<UserTO> getUsersByBranchAndRoles(String countryCode, String branchId, String branchLogin, String userLogin, List<UserRoleTO> roles, Boolean blocked, CustomPageableImpl pageable) {
        return pageMapper.toCustomPageImpl(userService.findUsersByMultipleParamsPaged(countryCode, branchId, branchLogin, userLogin, userTOMapper.toUserRoleBO(roles), blocked, PageRequest.of(pageable.getPage(), pageable.getSize()))
                                                   .map(userTOMapper::toUserTO));
    }

    @Override
    public List<String> getBranchUserLogins(String branchId) {
        return userService.findUserLoginsByBranch(branchId);
    }

    @Override
    public CustomPageImpl<UserTO> getUsersByRoles(List<UserRoleTO> roles, CustomPageableImpl pageable) {
        Page<UserBO> users = userService.getUsersByRoles(userTOMapper.toUserRoleBO(roles), PageRequest.of(pageable.getPage(), pageable.getSize()));
        Page<UserTO> map = users.map(userTOMapper::toUserTO);
        return pageMapper.toCustomPageImpl(map);
    }

    @Override
    public CustomPageImpl<UserExtendedTO> getUsersByBranchAndRolesExtended(String countryCode, String branchId, String branchLogin, String userLogin, List<UserRoleTO> roles, Boolean blocked, CustomPageableImpl pageable) {
        return pageMapper.toCustomPageImpl(userService.findUsersByMultipleParamsPaged(countryCode, branchId, branchLogin, userLogin, userTOMapper.toUserRoleBO(roles), blocked, PageRequest.of(pageable.getPage(), pageable.getSize()))
                                                   .map(userTOMapper::toUserExtendedTO));
    }

    @Override
    public int countUsersByBranch(String branch) {
        return userService.countUsersByBranch(branch);
    }

    @Override
    public UserTO updateUser(String branchId, UserTO user) {
        String userId = Optional.ofNullable(user.getId()).orElseThrow(() -> MiddlewareModuleException.builder()
                                                                                    .errorCode(REQUEST_VALIDATION_FAILURE)
                                                                                    .devMsg("User id is not present in request!")
                                                                                    .build());
        UserBO userFromLedgers = userService.findById(userId);
        UserBO userBO = userTOMapper.toUserBO(user);
        dataService.updateUser(keycloakUserMapper.toKeycloakUser(userBO), userFromLedgers.getLogin());
        updatePasswordByLogin(userFromLedgers.getLogin(), user.getPin());
        return userTOMapper.toUserTO(userService.updateUser(userBO));
    }

    @Override
    public void updatePasswordById(String userId, String password) {
        String login = userService.findById(userId).getLogin();
        updatePasswordByLogin(login, password);
    }

    @Override
    public void updatePasswordByLogin(String login, String password) {
        if (StringUtils.isNotBlank(password)) {
            dataService.resetPassword(login, password);
        }
    }

    @Override
    public boolean checkMultilevelScaRequired(String login, String iban) {
        if (!multilevelScaEnable) {
            return false;
        }
        return userService.findByLogin(login).resolveScaWeightByIban(iban) < finalWeight;
    }

    @Override
    public boolean checkMultilevelScaRequired(String login, List<AccountReferenceTO> references) {
        if (!multilevelScaEnable) {
            return false;
        }
        UserBO user = userService.findByLogin(login);
        if (CollectionUtils.isEmpty(references)) {
            return user.getAccountAccesses().stream()
                           .anyMatch(a -> a.getScaWeight() < finalWeight);
        }
        List<AccountAccessBO> accountAccessBOS = userTOMapper.toAccountAccessFromReferenceList(references);
        return user.resolveMinimalWeightForReferences(accountAccessBOS) < finalWeight;
    }

    @Override
    public List<AdditionalAccountInformationTO> getAdditionalInformation(ScaInfoTO scaInfoHolder, AccountIdentifierTypeTO accountIdentifierType, String accountIdentifier) {
        List<AdditionalAccountInfoBO> info = AccountIdentifierTypeBO.valueOf(accountIdentifierType.name()).getAdditionalAccountInfo(accountIdentifier, userService::findOwnersByIban, userService::findOwnersByAccountId);
        return additionalInfoMapper.toAdditionalAccountInformationTOs(info);
    }

    @Override
    public boolean changeStatus(String userId, boolean isSystemBlock) {
        UserBO user = userService.findById(userId);
        boolean lockStatusToSet = isSystemBlock ? !user.isSystemBlocked() : !user.isBlocked();
        userService.setUserBlockedStatus(userId, isSystemBlock, lockStatusToSet);
        depositAccountService.changeAccountsBlockedStatus(user.getAccountIds(), isSystemBlock, lockStatusToSet);
        return lockStatusToSet;
    }

    @Override
    public void editBasicSelf(String userId, UserTO user) {
        UserBO storedUser = userService.findById(userId);
        String oldLogin = storedUser.getLogin();
        storedUser.setLogin(user.getLogin());
        storedUser.setEmail(user.getEmail());
        storedUser.setPin(user.getPin());
        userService.updateUser(storedUser);
        dataService.updateUser(keycloakUserMapper.toKeycloakUser(storedUser), oldLogin);
        updatePasswordByLogin(storedUser.getLogin(), user.getPin());
    }

    @Override
    public void resetPasswordViaEmail(String login) {
        keycloakDataService.resetPasswordViaEmail(login);
        log.info("Link for password reset was sent to user [{}] email", login);
    }

    @Override
    public String findAccountOwner(String accountId) {
        return userService.findOwnersByAccountId(accountId).iterator().next().getLogin();
    }

}
