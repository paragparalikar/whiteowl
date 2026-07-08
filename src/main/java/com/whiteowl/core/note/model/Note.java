package com.whiteowl.core.note.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public final class Note {

    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 255;

    @Setter private String name;
    @Setter private String content;

    public Note(String name) {
        this(name, "");
    }

    public Note(String name, String content) {
        validateName(name);
        this.name = name;
        this.content = content;
    }

    private static void validateName(String name) {
        if (name == null || name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Note name must be between 1 and 255 characters");
        }
    }

}
