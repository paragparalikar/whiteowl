package com.whiteowl.core.examplegroup.repository;

import com.whiteowl.core.examplegroup.model.ExampleGroup;

import java.util.List;

public interface ExampleGroupRepository {

    List<ExampleGroup> loadAll();

    void saveAll(List<ExampleGroup> groups);

}
