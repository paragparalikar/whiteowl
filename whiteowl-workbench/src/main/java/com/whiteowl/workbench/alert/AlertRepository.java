package com.whiteowl.workbench.alert;

import java.util.List;

public interface AlertRepository {

    List<AlertDefinition> loadAll();

    void save(AlertDefinition alertDefinition);

    void delete(String alertDefinitionId);

    void saveAll(List<AlertDefinition> alertDefinitions);
}
