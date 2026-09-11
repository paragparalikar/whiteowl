package com.whiteowl.core.account.repository;

import static com.whiteowl.core.account.model.Account.DEFAULT_GTT_EXPIRY_WARNING_DAYS;
import static com.whiteowl.core.account.model.Account.DEFAULT_MAX_CONCENTRATION_PERCENTAGE;
import static com.whiteowl.core.account.model.Account.DEFAULT_POSITION_SIZE;
import static com.whiteowl.core.account.model.Account.DEFAULT_STALE_HOLDING_DAYS;
import static com.whiteowl.core.account.model.Account.DEFAULT_TIGHT_STOP_LOSS_PERCENTAGE;
import static com.whiteowl.core.account.model.Account.DEFAULT_MIN_RISK_REWARD_RATIO;
import static com.whiteowl.core.account.model.Account.DEFAULT_WIDE_STOP_LOSS_PERCENTAGE;

import com.whiteowl.core.account.model.Account;
import com.whiteowl.core.portfolio.model.BrokerType;

import java.util.List;

public record AccountDto(String id, String name, String brokerType, String userId, String password, String pin,
                         double positionSize, double wideStopLossPercentage, double tightStopLossPercentage,
                         double maxConcentrationPercentage, int staleHoldingDays, int gttExpiryWarningDays,
                         double minRiskRewardRatio) {

    public AccountDto {
        if (positionSize <= 0) positionSize = DEFAULT_POSITION_SIZE;
        if (wideStopLossPercentage <= 0) wideStopLossPercentage = DEFAULT_WIDE_STOP_LOSS_PERCENTAGE;
        if (tightStopLossPercentage <= 0) tightStopLossPercentage = DEFAULT_TIGHT_STOP_LOSS_PERCENTAGE;
        if (maxConcentrationPercentage <= 0) maxConcentrationPercentage = DEFAULT_MAX_CONCENTRATION_PERCENTAGE;
        if (staleHoldingDays <= 0) staleHoldingDays = DEFAULT_STALE_HOLDING_DAYS;
        if (gttExpiryWarningDays <= 0) gttExpiryWarningDays = DEFAULT_GTT_EXPIRY_WARNING_DAYS;
        if (minRiskRewardRatio <= 0) minRiskRewardRatio = DEFAULT_MIN_RISK_REWARD_RATIO;
    }

    static AccountDto fromAccount(Account account) {
        return new AccountDto(
                account.getId(),
                account.getName(),
                account.getBrokerType().name(),
                account.getUserId(),
                account.getPassword(),
                account.getPin(),
                account.getPositionSize(),
                account.getWideStopLossPercentage(),
                account.getTightStopLossPercentage(),
                account.getMaxConcentrationPercentage(),
                account.getStaleHoldingDays(),
                account.getGttExpiryWarningDays(),
                account.getMinRiskRewardRatio()
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
                .wideStopLossPercentage(wideStopLossPercentage)
                .tightStopLossPercentage(tightStopLossPercentage)
                .maxConcentrationPercentage(maxConcentrationPercentage)
                .staleHoldingDays(staleHoldingDays)
                .gttExpiryWarningDays(gttExpiryWarningDays)
                .minRiskRewardRatio(minRiskRewardRatio)
                .build();
    }

    static List<AccountDto> fromAccounts(List<Account> accounts) {
        return accounts.stream().map(AccountDto::fromAccount).toList();
    }

    static List<Account> toAccounts(List<AccountDto> dtos) {
        return dtos.stream().map(AccountDto::toAccount).toList();
    }

}
