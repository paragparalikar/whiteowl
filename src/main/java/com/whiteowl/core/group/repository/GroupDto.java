package com.whiteowl.core.group.repository;

import com.whiteowl.core.group.model.Group;

import java.util.List;

public record GroupDto(String name, List<String> scripIds) {

    static GroupDto fromGroup(Group g) {
        return new GroupDto(g.getName(), List.copyOf(g.getScripIds()));
    }

    Group toGroup() {
        return new Group(name, scripIds != null ? scripIds : List.of());
    }

    static List<GroupDto> fromGroups(List<Group> groups) {
        return groups.stream().map(GroupDto::fromGroup).toList();
    }

    static List<Group> toGroups(List<GroupDto> dtos) {
        return dtos.stream().map(GroupDto::toGroup).toList();
    }

}
