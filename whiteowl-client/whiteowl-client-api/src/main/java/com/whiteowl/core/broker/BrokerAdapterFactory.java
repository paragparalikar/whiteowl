package com.whiteowl.core.broker;

import com.whiteowl.core.account.model.Account;

public interface BrokerAdapterFactory {

    BrokerAdapter createAdapter(Account account);

    void closeAll();

}
