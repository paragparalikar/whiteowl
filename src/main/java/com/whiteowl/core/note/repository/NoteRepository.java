package com.whiteowl.core.note.repository;

import com.whiteowl.core.note.model.Note;

import java.util.List;

public interface NoteRepository {

    List<Note> loadAll();

    void save(Note note);

    void delete(String name);

    void rename(String oldName, String newName);

}
