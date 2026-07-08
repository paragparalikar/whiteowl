package com.whiteowl.core.account.service;

import com.whiteowl.core.account.model.Account;
import com.whiteowl.core.account.repository.AccountRepository;
import com.whiteowl.core.portfolio.model.BrokerType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public final class AccountService {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 255;
    private static final String FIELD_NAME = "Name";
    private static final String FIELD_USER_ID = "User ID";
    private static final String FIELD_PASSWORD = "Password";
    private static final String FIELD_PIN = "PIN";
    private static final String ERR_REQUIRED = "%s is required";
    private static final String ERR_LENGTH = "%s must be between %d and %d characters";
    private static final String ERR_NAME_DUPLICATE = "An account with this name already exists";
    private static final String ERR_USER_ID_DUPLICATE = "An account with this User ID already exists";

    private final AccountRepository repository;
    private final List<Account> accounts;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
        this.accounts = new ArrayList<>(repository.loadAll());
        log.info("AccountService initialized with {} accounts", accounts.size());
    }

    public List<Account> getAccounts() {
        return List.copyOf(accounts);
    }

    public Optional<String> validate(String name, BrokerType brokerType, String userId,
                                     String password, String pin, String excludeId) {
        Optional<String> error = validateField(FIELD_NAME, name);
        if (error.isPresent()) return error;
        error = validateField(FIELD_USER_ID, userId);
        if (error.isPresent()) return error;
        error = validateField(FIELD_PASSWORD, password);
        if (error.isPresent()) return error;
        error = validateField(FIELD_PIN, pin);
        if (error.isPresent()) return error;
        if (brokerType == null) return Optional.of(String.format(ERR_REQUIRED, "Broker Type"));
        if (isDuplicateName(name, excludeId)) return Optional.of(ERR_NAME_DUPLICATE);
        if (isDuplicateUserId(userId, excludeId)) return Optional.of(ERR_USER_ID_DUPLICATE);
        return Optional.empty();
    }

    public Account create(String name, BrokerType brokerType, String userId, String password, String pin,
                           double positionSize) {
        Account account = Account.builder()
                .id(UUID.randomUUID().toString())
                .name(name.trim())
                .brokerType(brokerType)
                .userId(userId.trim())
                .password(password.trim())
                .pin(pin.trim())
                .positionSize(positionSize)
                .build();
        accounts.add(account);
        persist();
        log.info("Created account: name={}, broker={}", account.getName(), account.getBrokerType());
        return account;
    }

    public void update(Account account, String name, BrokerType brokerType,
                       String userId, String password, String pin, double positionSize) {
        account.setName(name.trim());
        account.setBrokerType(brokerType);
        account.setUserId(userId.trim());
        account.setPassword(password.trim());
        account.setPin(pin.trim());
        account.setPositionSize(positionSize);
        persist();
        log.info("Updated account: name={}, broker={}", account.getName(), account.getBrokerType());
    }

    public void delete(Account account) {
        accounts.remove(account);
        persist();
        log.info("Deleted account: name={}", account.getName());
    }

    private void persist() {
        repository.saveAll(accounts);
    }

    private Optional<String> validateField(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.of(String.format(ERR_REQUIRED, fieldName));
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            return Optional.of(String.format(ERR_LENGTH, fieldName, MIN_LENGTH, MAX_LENGTH));
        }
        return Optional.empty();
    }

    private boolean isDuplicateName(String name, String excludeId) {
        String trimmed = name.trim();
        return accounts.stream()
                .anyMatch(a -> a.getName().equalsIgnoreCase(trimmed) && !a.getId().equals(excludeId));
    }

    private boolean isDuplicateUserId(String userId, String excludeId) {
        String trimmed = userId.trim();
        return accounts.stream()
                .anyMatch(a -> a.getUserId().equalsIgnoreCase(trimmed) && !a.getId().equals(excludeId));
    }

}
