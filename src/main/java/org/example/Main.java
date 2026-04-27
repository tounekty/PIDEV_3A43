package org.example;

import org.example.model.Journal;
import org.example.model.Mood;
import org.example.repository.JournalRepository;
import org.example.repository.MoodRepository;
import org.example.db.SchemaInitializer;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final MoodRepository MOOD_REPOSITORY = new MoodRepository();
    private static final JournalRepository JOURNAL_REPOSITORY = new JournalRepository();

    public static void main(String[] args) {
        try {
            SchemaInitializer.ensureSchema();
        } catch (SQLException e) {
            System.out.println("Failed to initialize DB schema: " + e.getMessage());
            return;
        }
        printSchemaHint();
        while (true) {
            try {
                printMenu();
                String choice = SCANNER.nextLine().trim();
                switch (choice) {
                    case "1" -> createMood();
                    case "2" -> listMoods();
                    case "3" -> updateMood();
                    case "4" -> deleteMood();
                    case "5" -> createJournal();
                    case "6" -> listJournals();
                    case "7" -> updateJournal();
                    case "8" -> deleteJournal();
                    case "0" -> {
                        System.out.println("Bye.");
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=== Mood & Journal CRUD ===");
        System.out.println("1. Create mood");
        System.out.println("2. List moods");
        System.out.println("3. Update mood");
        System.out.println("4. Delete mood");
        System.out.println("5. Create journal");
        System.out.println("6. List journals");
        System.out.println("7. Update journal");
        System.out.println("8. Delete journal");
        System.out.println("0. Exit");
        System.out.print("Choose: ");
    }

    private static void createMood() throws SQLException {
        System.out.print("Mood type (happy/sad/...): ");
        String moodType = SCANNER.nextLine().trim();

        LocalDate moodDate = readDate("Mood date (YYYY-MM-DD): ");

        System.out.print("Note: ");
        String note = SCANNER.nextLine().trim();

        Integer stressLevel = readNullableLevel("Stress level 1-10 (empty if unknown): ");
        Integer energyLevel = readNullableLevel("Energy level 1-10 (empty if unknown): ");

        Mood mood = new Mood(0, moodType, moodDate, note, stressLevel, energyLevel);
        int id = MOOD_REPOSITORY.create(mood);
        System.out.println("Mood created with id: " + id);
    }

    private static void listMoods() throws SQLException {
        List<Mood> moods = MOOD_REPOSITORY.findAll();
        if (moods.isEmpty()) {
            System.out.println("No moods found.");
            return;
        }
        moods.forEach(System.out::println);
    }

    private static void updateMood() throws SQLException {
        int id = readInt("Mood id to update: ");
        Mood existing = MOOD_REPOSITORY.findById(id);
        if (existing == null) {
            System.out.println("Mood not found.");
            return;
        }

        System.out.print("New mood type [" + existing.getMoodType() + "]: ");
        String moodType = SCANNER.nextLine().trim();
        if (moodType.isBlank()) moodType = existing.getMoodType();

        System.out.print("New mood date [" + existing.getMoodDate() + "] (YYYY-MM-DD, empty to keep): ");
        String dateInput = SCANNER.nextLine().trim();
        LocalDate moodDate = dateInput.isBlank() ? existing.getMoodDate() : LocalDate.parse(dateInput);

        System.out.print("New note [" + existing.getNote() + "]: ");
        String note = SCANNER.nextLine().trim();
        if (note.isBlank()) note = existing.getNote();

        Integer stressLevel = readNullableLevel("New stress level [" + (existing.getStressLevel() == null ? "none" : existing.getStressLevel()) + "] (1-10, empty keep): ");
        if (stressLevel == null) stressLevel = existing.getStressLevel();

        Integer energyLevel = readNullableLevel("New energy level [" + (existing.getEnergyLevel() == null ? "none" : existing.getEnergyLevel()) + "] (1-10, empty keep): ");
        if (energyLevel == null) energyLevel = existing.getEnergyLevel();

        Mood updated = new Mood(id, moodType, moodDate, note, stressLevel, energyLevel);
        boolean ok = MOOD_REPOSITORY.update(updated);
        System.out.println(ok ? "Mood updated." : "Mood not updated.");
    }

    private static void deleteMood() throws SQLException {
        int id = readInt("Mood id to delete: ");
        boolean ok = MOOD_REPOSITORY.delete(id);
        System.out.println(ok ? "Mood deleted." : "Mood not found.");
    }

    private static void createJournal() throws SQLException {
        System.out.print("Title: ");
        String title = SCANNER.nextLine().trim();

        System.out.print("Content: ");
        String content = SCANNER.nextLine().trim();

        LocalDate entryDate = readDate("Entry date (YYYY-MM-DD): ");

        Integer moodId = readNullableInt("Mood id (empty if none): ");

        Journal journal = new Journal(0, title, content, entryDate, moodId);
        int id = JOURNAL_REPOSITORY.create(journal);
        System.out.println("Journal created with id: " + id);
    }

    private static void listJournals() throws SQLException {
        List<Journal> journals = JOURNAL_REPOSITORY.findAll();
        if (journals.isEmpty()) {
            System.out.println("No journals found.");
            return;
        }
        journals.forEach(System.out::println);
    }

    private static void updateJournal() throws SQLException {
        int id = readInt("Journal id to update: ");
        Journal existing = JOURNAL_REPOSITORY.findById(id);
        if (existing == null) {
            System.out.println("Journal not found.");
            return;
        }

        System.out.print("New title [" + existing.getTitle() + "]: ");
        String title = SCANNER.nextLine().trim();
        if (title.isBlank()) title = existing.getTitle();

        System.out.print("New content [" + existing.getContent() + "]: ");
        String content = SCANNER.nextLine().trim();
        if (content.isBlank()) content = existing.getContent();

        System.out.print("New entry date [" + existing.getEntryDate() + "] (YYYY-MM-DD, empty to keep): ");
        String dateInput = SCANNER.nextLine().trim();
        LocalDate entryDate = dateInput.isBlank() ? existing.getEntryDate() : LocalDate.parse(dateInput);

        System.out.print("New mood id [" + (existing.getMoodId() == null ? "none" : existing.getMoodId()) + "] (empty keep, 0 clear): ");
        String moodIdInput = SCANNER.nextLine().trim();
        Integer moodId;
        if (moodIdInput.isBlank()) {
            moodId = existing.getMoodId();
        } else {
            int parsed = Integer.parseInt(moodIdInput);
            moodId = parsed == 0 ? null : parsed;
        }

        Journal updated = new Journal(id, title, content, entryDate, moodId);
        boolean ok = JOURNAL_REPOSITORY.update(updated);
        System.out.println(ok ? "Journal updated." : "Journal not updated.");
    }

    private static void deleteJournal() throws SQLException {
        int id = readInt("Journal id to delete: ");
        boolean ok = JOURNAL_REPOSITORY.delete(id);
        System.out.println(ok ? "Journal deleted." : "Journal not found.");
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = SCANNER.nextLine().trim();
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static Integer readNullableInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = SCANNER.nextLine().trim();
            if (raw.isBlank()) {
                return null;
            }
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number or leave empty.");
            }
        }
    }

    private static Integer readNullableLevel(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = SCANNER.nextLine().trim();
            if (raw.isBlank()) {
                return null;
            }
            try {
                int value = Integer.parseInt(raw);
                if (value >= 1 && value <= 10) {
                    return value;
                }
                System.out.println("Please enter a level between 1 and 10 or leave empty.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid level or leave empty.");
            }
        }
    }

    private static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = SCANNER.nextLine().trim();
            try {
                return LocalDate.parse(raw);
            } catch (Exception e) {
                System.out.println("Please use format YYYY-MM-DD.");
            }
        }
    }

    private static void printSchemaHint() {
        System.out.println("""
                Make sure these tables exist in your MySQL DB:

                CREATE TABLE mood (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  mood_type VARCHAR(50) NOT NULL,
                  mood_date DATE NOT NULL,
                  stress_level INT NULL,
                  energy_level INT NULL,
                  note TEXT
                );

                CREATE TABLE journal (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  title VARCHAR(255) NOT NULL,
                  content TEXT NOT NULL,
                  entry_date DATE NOT NULL,
                  mood_id INT NULL,
                  CONSTRAINT fk_journal_mood FOREIGN KEY (mood_id) REFERENCES mood(id)
                    ON DELETE SET NULL ON UPDATE CASCADE
                );
                """);
    }
}
