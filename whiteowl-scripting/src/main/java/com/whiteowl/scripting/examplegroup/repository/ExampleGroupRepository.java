package com.whiteowl.scripting.examplegroup.repository;

import com.whiteowl.scripting.examplegroup.model.ExampleGroup;

import java.util.List;

public interface ExampleGroupRepository {

    List<ExampleGroup> loadAll();

    void saveAll(List<ExampleGroup> groups);

}
