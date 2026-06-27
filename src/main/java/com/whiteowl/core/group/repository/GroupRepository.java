package com.whiteowl.core.group.repository;

import com.whiteowl.core.group.model.Group;

import java.util.List;

public interface GroupRepository {

    List<Group> loadAll();

    void saveAll(List<Group> groups);

}
