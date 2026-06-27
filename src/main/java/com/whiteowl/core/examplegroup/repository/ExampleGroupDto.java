package com.whiteowl.core.examplegroup.repository;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.examplegroup.model.Example;
import com.whiteowl.core.examplegroup.model.ExampleGroup;

import java.util.List;

public record ExampleGroupDto(String name, List<ExampleDto> examples) {

    public record ExampleDto(String scripId, String timeframe, long timestamp) {

        static ExampleDto fromExample(Example e) {
            return new ExampleDto(e.getScripId(), e.getTimeframe().name(), e.getTimestamp());
        }

        Example toExample() {
            return new Example(scripId, Timeframe.valueOf(timeframe), timestamp);
        }

    }

    static ExampleGroupDto fromGroup(ExampleGroup g) {
        List<ExampleDto> dtos = g.getExamples().stream().map(ExampleDto::fromExample).toList();
        return new ExampleGroupDto(g.getName(), dtos);
    }

    ExampleGroup toGroup() {
        List<Example> items = examples != null
                ? examples.stream().map(ExampleDto::toExample).toList()
                : List.of();
        return new ExampleGroup(name, items);
    }

    static List<ExampleGroupDto> fromGroups(List<ExampleGroup> groups) {
        return groups.stream().map(ExampleGroupDto::fromGroup).toList();
    }

    static List<ExampleGroup> toGroups(List<ExampleGroupDto> dtos) {
        return dtos.stream().map(ExampleGroupDto::toGroup).toList();
    }

}
