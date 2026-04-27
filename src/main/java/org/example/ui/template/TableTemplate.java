package org.example.ui.template;

import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * Template for creating consistent table layouts.
 */
public class TableTemplate<T> {
    private final TableView<T> tableView;

    public TableTemplate() {
        this.tableView = new TableView<>();
        this.tableView.setPrefHeight(320);
    }

    public <S> TableTemplate<T> addColumn(String columnName, String property, double prefWidth) {
        TableColumn<T, S> column = new TableColumn<>(columnName);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(prefWidth);
        this.tableView.getColumns().add(column);
        return this;
    }

    public <S> TableTemplate<T> addColumn(String columnName, javafx.util.Callback<TableColumn.CellDataFeatures<T, S>, javafx.beans.value.ObservableValue<S>> cellFactory, double prefWidth) {
        TableColumn<T, S> column = new TableColumn<>(columnName);
        column.setCellValueFactory(cellFactory);
        column.setPrefWidth(prefWidth);
        this.tableView.getColumns().add(column);
        return this;
    }

    public TableTemplate<T> setItems(ObservableList<T> items) {
        this.tableView.setItems(items);
        return this;
    }

    public TableView<T> build() {
        return this.tableView;
    }
}
