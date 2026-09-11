package com.whiteowl.core.account.repository;

import com.whiteowl.core.account.model.Account;

import java.util.List;

public interface AccountRepository {

    List<Account> loadAll();

    void saveAll(List<Account> accounts);

}
