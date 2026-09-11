package com.whiteowl.workbench.group.repository;

import com.whiteowl.workbench.group.model.Group;

import java.util.List;

public interface GroupRepository {

    List<Group> loadAll();

    void saveAll(List<Group> groups);

}
