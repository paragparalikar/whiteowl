package com.whiteowl.scripting.examplegroup.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class ExampleGroup {

    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 255;

    @Setter private String name;
    private final List<Example> examples;

    public ExampleGroup(String name) {
        validateName(name);
        this.name = name;
        this.examples = new ArrayList<>();
    }

    public ExampleGroup(String name, List<Example> examples) {
        validateName(name);
        this.name = name;
        this.examples = new ArrayList<>(examples);
    }

    public void addExample(Example example) {
        examples.add(example);
    }

    public void removeExample(int index) {
        if (index >= 0 && index < examples.size()) {
            examples.remove(index);
        }
    }

    public void moveExample(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= examples.size()) return;
        if (toIndex < 0 || toIndex >= examples.size()) return;
        Example example = examples.remove(fromIndex);
        examples.add(toIndex, example);
    }

    private static void validateName(String name) {
        if (name == null || name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Example group name must be between 1 and 255 characters");
        }
    }

}
