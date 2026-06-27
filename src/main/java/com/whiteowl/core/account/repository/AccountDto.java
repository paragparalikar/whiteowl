package com.whiteowl.core.account.repository;

import static com.whiteowl.core.account.model.Account.DEFAULT_PREFERRED_POSITION_COUNT;

import com.whiteowl.core.account.model.Account;
import com.whiteowl.core.portfolio.model.BrokerType;

import java.util.List;

public record AccountDto(String id, String name, String brokerType, String userId, String password, String pin,
                         int preferredPositionCount) {

    public AccountDto {
        if (preferredPositionCount <= 0) preferredPositionCount = DEFAULT_PREFERRED_POSITION_COUNT;
    }

    static AccountDto fromAccount(Account account) {
        return new AccountDto(
                account.getId(),
                account.getName(),
                account.getBrokerType().name(),
                account.getUserId(),
                account.getPassword(),
                account.getPin(),
                account.getPreferredPositionCount()
        );
    }

    Account toAccount() {
        return Account.builder()
                .id(id)
                .name(name)
                .brokerType(BrokerType.valueOf(brokerType))
                .userId(userId)
                .password(password)
                .pin(pin)
                .preferredPositionCount(preferredPositionCount)
                .build();
    }

    static List<AccountDto> fromAccounts(List<Account> accounts) {
        return accounts.stream().map(AccountDto::fromAccount).toList();
    }

    static List<Account> toAccounts(List<AccountDto> dtos) {
        return dtos.stream().map(AccountDto::toAccount).toList();
    }

}
