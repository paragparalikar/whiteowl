package com.whiteowl.workbench.common;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.function.Supplier;

public final class AddToCollectionMenuBuilder {

    private static final String NO_ITEMS_LABEL = "None available";
    private static final int MENU_ICON_SIZE = 16;

    private AddToCollectionMenuBuilder() {
    }

    public static Menu build(String menuLabel,
                             Ikon icon,
                             Supplier<List<? extends NamedScripCollection>> collectionsSupplier,
                             Supplier<List<String>> scripIdsSupplier,
                             Runnable onAdded) {
        Menu menu = new Menu(menuLabel);
        if (icon != null) {
            FontIcon fontIcon = new FontIcon(icon);
            fontIcon.setIconSize(MENU_ICON_SIZE);
            menu.setGraphic(fontIcon);
        }
        menu.setOnShowing(e -> rebuild(menu, collectionsSupplier, scripIdsSupplier, onAdded));
        rebuild(menu, collectionsSupplier, scripIdsSupplier, onAdded);
        return menu;
    }

    private static void rebuild(Menu menu,
                                Supplier<List<? extends NamedScripCollection>> collectionsSupplier,
                                Supplier<List<String>> scripIdsSupplier,
                                Runnable onAdded) {
        menu.getItems().clear();
        List<? extends NamedScripCollection> collections = collectionsSupplier.get();
        if (collections == null || collections.isEmpty()) {
            MenuItem empty = new MenuItem(NO_ITEMS_LABEL);
            empty.setDisable(true);
            menu.getItems().add(empty);
            return;
        }
        for (NamedScripCollection collection : collections) {
            MenuItem item = new MenuItem(collection.getName());
            item.setOnAction(ev -> {
                List<String> scripIds = scripIdsSupplier.get();
                if (scripIds != null) {
                    scripIds.forEach(collection::addScrip);
                    if (onAdded != null) onAdded.run();
                }
            });
            menu.getItems().add(item);
        }
    }

}
