# Template-Based UI System for Mood & Journal Manager

This document explains the new template-based architecture for the Mood & Journal Manager application.

## Overview

The application has been converted from FXML-based UI (Scene Builder) to a **code-based template system** with no manual XML editing required.

## Architecture

### Template Classes

#### 1. **UITemplate** Interface
Base interface for all templates that build UIs.
```java
public interface UITemplate {
    Parent build();
}
```

#### 2. **FormTemplate**
Creates consistent form layouts with label-control pairs using a fluent API.

**Usage:**
```java
FormTemplate form = new FormTemplate()
        .addField("Mood Type:", moodTypeField)
        .addField("Mood Date:", moodDatePicker)
        .addField("Note:", moodNoteArea)
        .setPadding(new Insets(10));
```

#### 3. **ButtonBarTemplate**
Creates consistent button bars with automatic action bindings.

**Usage:**
```java
ButtonBarTemplate buttons = new ButtonBarTemplate()
        .addButton("Create", this::handleCreate)
        .addButton("Delete", this::handleDelete)
        .setSpacing(10);

HBox buttonBar = buttons.build();
```

#### 4. **TableTemplate**
Creates styled tables with configurable columns.

**Usage:**
```java
TableTemplate<Mood> table = new TableTemplate<>();
table.addColumn("ID", "id", 70)
     .addColumn("Type", "moodType", 150)
     .setItems(moodList);

TableView<Mood> view = table.build();
```

#### 5. **CRUDTabTemplate**
Creates a complete CRUD tab with:
- Form section
- Action buttons
- Search/Filter bar
- Statistics label
- Table display

**Usage:**
```java
CRUDTabTemplate<Mood> moodTab = new CRUDTabTemplate<>("Mood CRUD", form, table)
        .setSearchPlaceholder("Search moods...")
        .setSortOptions(List.of("Date desc", "Date asc", "Type A-Z"))
        .withActions(
                this::handleCreate,
                this::handleUpdate,
                this::handleDelete,
                this::handleClear,
                this::handleRefresh
        )
        .withFilters(
                this::handleApplyFilters,
                this::handleResetFilters
        );
```

#### 6. **MainLayoutTemplate**
Creates the application main layout with:
- Title bar
- Tab pane

**Usage:**
```java
MainLayoutTemplate layout = new MainLayoutTemplate(
        "My App Title",
        tab1,
        tab2,
        tab3
);
Parent root = layout.build();
```

## How It Works

### Step 1: Design CRUD Forms
```java
FormTemplate moodForm = new FormTemplate()
        .addField("Mood Type:", moodTypeField)
        .addField("Mood Date:", moodDatePicker)
        .addField("Note:", moodNoteArea);
```

### Step 2: Create Tables
```java
TableView<Mood> moodTable = new TableView<>();
moodIdColumn = new TableColumn<>("ID");
moodTypeColumn = new TableColumn<>("Mood Type");
moodTable.getColumns().addAll(moodIdColumn, moodTypeColumn);
```

### Step 3: Build CRUD Tabs
```java
CRUDTabTemplate<Mood> moodTab = new CRUDTabTemplate<>("Mood CRUD", moodForm, moodTable)
        .withActions(
                this::handleCreateMood,
                this::handleUpdateMood,
                this::handleDeleteMood,
                this::handleClearMoodForm,
                this::handleRefreshMoods
        )
        .withFilters(
                this::handleApplyMoodFilters,
                this::handleResetMoodFilters
        );
```

### Step 4: Assemble Main Layout
```java
MainLayoutTemplate layout = new MainLayoutTemplate(
        "Mood & Journal Manager",
        moodTab.build(),
        journalTab.build()
);
return layout.build();
```

## Benefits

✅ **No FXML Files** - All UI is code-based  
✅ **No Scene Builder** - No visual tool dependency  
✅ **Reusable Components** - Template classes handle common patterns  
✅ **Fluent API** - Easy-to-read method chaining  
✅ **Consistent Styling** - Templates enforce UI consistency  
✅ **Type-Safe** - Full IDE support and compile-time checking  
✅ **Easy to Maintain** - Changes are just Java code  
✅ **Easy to Test** - Components are mockable and testable  

## Template Customization

### Extending FormTemplate
```java
FormTemplate customForm = new FormTemplate()
        .addField("Field 1:", textField1)
        .addField("Field 2:", textField2)
        .setPadding(new Insets(15));
```

### Extending CRUDTabTemplate
```java
CRUDTabTemplate<MyEntity> tab = new CRUDTabTemplate<>("Tab Title", form, table)
        .setSearchPlaceholder("Custom placeholder")
        .setSortOptions(customSortList)
        .withActions(action1, action2, action3, action4, action5)
        .withFilters(filterAction1, filterAction2);
```

## File Structure

```
src/main/java/org/example/ui/
├── template/
│   ├── UITemplate.java           # Base interface
│   ├── FormTemplate.java         # Form builder
│   ├── ButtonBarTemplate.java    # Button bar builder
│   ├── TableTemplate.java        # Table builder
│   ├── CRUDTabTemplate.java      # CRUD tab builder
│   └── MainLayoutTemplate.java   # Main layout builder
├── MainController.java           # Main application controller (code-only)
└── MoodJournalApp.java          # Application entry point
```

## Migration Guide

If you're migrating from FXML:

1. **Remove** all `.fxml` files
2. **Remove** `@FXML` annotations from fields and methods
3. **Replace** FXML loading with template building:
   ```java
   // OLD - FXML approach
   FXMLLoader loader = new FXMLLoader(getClass().getResource("view.fxml"));
   
   // NEW - Template approach
   MainLayoutTemplate layout = new MainLayoutTemplate("Title", tab1, tab2);
   return layout.build();
   ```

## Best Practices

1. **Keep Templates Lightweight** - Templates should handle layout, not business logic
2. **Separate Concerns** - Keep controller logic separate from template building
3. **Reuse Templates** - Create custom template classes for repeated patterns
4. **Use Fluent API** - Chain method calls for readability
5. **Extract Methods** - Break complex layouts into smaller builder methods

## Advanced: Creating Custom Templates

```java
public class CustomPanelTemplate {
    private final VBox panel;
    
    public CustomPanelTemplate(String title) {
        this.panel = new VBox(10);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        this.panel.getChildren().add(titleLabel);
    }
    
    public CustomPanelTemplate addContent(Node node) {
        this.panel.getChildren().add(node);
        return this;
    }
    
    public VBox build() {
        return this.panel;
    }
}
```

## No More Scene Builder! 🎉

This template system gives you the power of programmatic UI without the complexity. All your UI is version-controllable Java code.
