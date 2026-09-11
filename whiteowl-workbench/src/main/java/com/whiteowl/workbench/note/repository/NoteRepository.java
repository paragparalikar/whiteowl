package com.whiteowl.workbench.note.repository;

import com.whiteowl.workbench.note.model.Note;

import java.util.List;

public interface NoteRepository {

    List<Note> loadAll();

    void save(Note note);

    void delete(String name);

    void rename(String oldName, String newName);

}
