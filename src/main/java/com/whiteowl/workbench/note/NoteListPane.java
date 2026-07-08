package com.whiteowl.workbench.note;

import com.whiteowl.core.note.model.Note;
import com.whiteowl.core.note.repository.NoteRepository;
import com.whiteowl.workbench.common.ConfirmationDialog;
import com.whiteowl.workbench.common.NameInputDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

public final class NoteListPane extends VBox {

    private static final String PANE_STYLE = "note-list-pane";
    private static final String TOOLBAR_STYLE = "note-list-toolbar";
    private static final String SEARCH_FIELD_STYLE = "note-list-search-field";
    private static final String ADD_BUTTON_STYLE = "note-list-add-button";
    private static final String LIST_STYLE = "note-list";
    private static final String CELL_NAME_STYLE = "note-cell-name";
    private static final String CELL_EDIT_STYLE = "note-cell-edit";
    private static final String CELL_DELETE_STYLE = "note-cell-delete";
    private static final String ICON_STYLE = "note-icon";
    private static final String SEARCH_PROMPT = "Search notes...";
    private static final String CREATE_DIALOG_TITLE = "New Note";
    private static final String CREATE_PROMPT = "Enter note name";
    private static final String DELETE_CONFIRM_TITLE = "Delete Note";
    private static final String DELETE_CONFIRM_FORMAT = "Are you sure you want to delete '%s'?";
    private static final String DELETE_CONFIRM_BUTTON = "Delete";
    private static final int ICON_SIZE = 14;
    private static final int CELL_ICON_SIZE = 12;
    private static final int TOOLBAR_GAP = 4;
    private static final int FIXED_CELL_HEIGHT = 24;

    private final NoteRepository noteRepository;
    private final ObservableList<Note> notes;
    private final FilteredList<Note> filteredNotes;
    private final ListView<Note> noteListView;
    private Consumer<Note> onEditNote;
    private Consumer<String> onNoteDeleted;

    public NoteListPane(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
        this.notes = FXCollections.observableArrayList(noteRepository.loadAll());
        this.filteredNotes = new FilteredList<>(notes, n -> true);
        this.noteListView = buildNoteListView();
        getStyleClass().add(PANE_STYLE);
        buildLayout();
    }

    public void setOnEditNote(Consumer<Note> handler) {
        this.onEditNote = handler;
    }

    public void setOnNoteDeleted(Consumer<String> handler) {
        this.onNoteDeleted = handler;
    }

    public void refreshList() {
        notes.setAll(noteRepository.loadAll());
    }

    private void buildLayout() {
        HBox toolbar = buildToolbar();
        VBox.setVgrow(noteListView, Priority.ALWAYS);
        getChildren().addAll(toolbar, noteListView);
    }

    private HBox buildToolbar() {
        TextField searchField = new TextField();
        searchField.setPromptText(SEARCH_PROMPT);
        searchField.getStyleClass().add(SEARCH_FIELD_STYLE);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal == null ? "" : newVal.toLowerCase();
            filteredNotes.setPredicate(note ->
                    filter.isEmpty() || note.getName().toLowerCase().contains(filter));
        });
        HBox.setHgrow(searchField, Priority.ALWAYS);
        FontIcon addIcon = new FontIcon(FluentUiRegularAL.ADD_16);
        addIcon.setIconSize(ICON_SIZE);
        addIcon.getStyleClass().add(ICON_STYLE);
        Button addButton = new Button(null, addIcon);
        addButton.getStyleClass().add(ADD_BUTTON_STYLE);
        addButton.setFocusTraversable(false);
        addButton.setOnAction(e -> createNote());
        HBox toolbar = new HBox(TOOLBAR_GAP, searchField, addButton);
        toolbar.getStyleClass().add(TOOLBAR_STYLE);
        toolbar.setPadding(new Insets(4));
        return toolbar;
    }

    private ListView<Note> buildNoteListView() {
        ListView<Note> listView = new ListView<>(filteredNotes);
        listView.getStyleClass().add(LIST_STYLE);
        listView.setFixedCellSize(FIXED_CELL_HEIGHT);
        listView.setCellFactory(lv -> new NoteCell());
        return listView;
    }

    private void createNote() {
        NameInputDialog dialog = new NameInputDialog(CREATE_DIALOG_TITLE, CREATE_PROMPT);
        dialog.show(getScene().getWindow());
        String name = dialog.getEnteredName();
        if (name != null) {
            Note note = new Note(name);
            noteRepository.save(note);
            notes.add(note);
            noteListView.getSelectionModel().select(note);
            fireEditNote(note);
        }
    }

    private void deleteNote(Note note) {
        ConfirmationDialog dialog = new ConfirmationDialog(
                DELETE_CONFIRM_TITLE,
                String.format(DELETE_CONFIRM_FORMAT, note.getName()),
                DELETE_CONFIRM_BUTTON);
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            String deletedName = note.getName();
            noteRepository.delete(deletedName);
            notes.remove(note);
            if (onNoteDeleted != null) {
                onNoteDeleted.accept(deletedName);
            }
        }
    }

    private void fireEditNote(Note note) {
        if (onEditNote != null) {
            onEditNote.accept(note);
        }
    }

    private final class NoteCell extends ListCell<Note> {

        @Override
        protected void updateItem(Note item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(item.getName());
            nameLabel.getStyleClass().add(CELL_NAME_STYLE);
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            FontIcon editIcon = new FontIcon(FluentUiRegularAL.EDIT_16);
            editIcon.setIconSize(CELL_ICON_SIZE);
            editIcon.getStyleClass().add(ICON_STYLE);
            Button editBtn = new Button(null, editIcon);
            editBtn.getStyleClass().add(CELL_EDIT_STYLE);
            editBtn.setFocusTraversable(false);
            editBtn.setOnAction(e -> fireEditNote(item));
            FontIcon deleteIcon = new FontIcon(FluentUiRegularAL.DELETE_16);
            deleteIcon.setIconSize(CELL_ICON_SIZE);
            deleteIcon.getStyleClass().add(ICON_STYLE);
            Button deleteBtn = new Button(null, deleteIcon);
            deleteBtn.getStyleClass().add(CELL_DELETE_STYLE);
            deleteBtn.setFocusTraversable(false);
            deleteBtn.setOnAction(e -> deleteNote(item));
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox cellBox = new HBox(nameLabel, spacer, editBtn, deleteBtn);
            cellBox.setAlignment(Pos.CENTER_LEFT);
            setGraphic(cellBox);
            setText(null);
            setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) fireEditNote(item);
            });
        }

    }

}
