package com.whiteowl.core.account.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiteowl.core.account.model.Account;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class FileAccountRepository implements AccountRepository {

    private static final String FILE_NAME = "accounts.json";
    private static final TypeReference<List<AccountDto>> LIST_TYPE = new TypeReference<>() {};

    private final Path filePath;
    private final ObjectMapper objectMapper;

    public FileAccountRepository() {
        this(Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"), "data", FILE_NAME));
    }

    public FileAccountRepository(Path filePath) {
        this.filePath = filePath;
        this.objectMapper = new ObjectMapper();
        log.info("FileAccountRepository initialized with path={}", filePath);
    }

    @Override
    public List<Account> loadAll() {
        if (!Files.exists(filePath)) return new ArrayList<>();
        try {
            List<AccountDto> dtos = objectMapper.readValue(filePath.toFile(), LIST_TYPE);
            log.debug("Loaded {} accounts", dtos.size());
            return new ArrayList<>(AccountDto.toAccounts(dtos));
        } catch (IOException e) {
            log.error("Failed to load accounts", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveAll(List<Account> accounts) {
        try {
            ensureParentExists();
            List<AccountDto> dtos = AccountDto.fromAccounts(accounts);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), dtos);
            log.debug("Saved {} accounts", dtos.size());
        } catch (IOException e) {
            log.error("Failed to save accounts", e);
        }
    }

    private void ensureParentExists() throws IOException {
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

}
