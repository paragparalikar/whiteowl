package com.whiteowl.core.account.repository;

import static com.whiteowl.core.account.model.Account.DEFAULT_POSITION_SIZE;

import com.whiteowl.core.account.model.Account;
import com.whiteowl.core.portfolio.model.BrokerType;

import java.util.List;

public record AccountDto(String id, String name, String brokerType, String userId, String password, String pin,
                         double positionSize) {

    public AccountDto {
        if (positionSize <= 0) positionSize = DEFAULT_POSITION_SIZE;
    }

    static AccountDto fromAccount(Account account) {
        return new AccountDto(
                account.getId(),
                account.getName(),
                account.getBrokerType().name(),
                account.getUserId(),
                account.getPassword(),
                account.getPin(),
                account.getPositionSize()
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
                .positionSize(positionSize)
                .build();
    }

    static List<AccountDto> fromAccounts(List<Account> accounts) {
        return accounts.stream().map(AccountDto::fromAccount).toList();
    }

    static List<Account> toAccounts(List<AccountDto> dtos) {
        return dtos.stream().map(AccountDto::toAccount).toList();
    }

}
