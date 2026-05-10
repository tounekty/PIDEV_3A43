package org.example.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.example.model.Journal;
import org.example.model.Mood;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MoodJournalPdfService {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Path exportMoods(Path outputFile, List<Mood> moods) throws IOException {
        if (outputFile == null) {
            throw new IOException("Output file is required.");
        }
        Files.createDirectories(outputFile.toAbsolutePath().getParent());

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;
                float leading = 16;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.newLineAtOffset(margin, y);
                cs.showText("Mood Tracker Export");
                cs.newLineAtOffset(0, -leading * 2);

                cs.setFont(PDType1Font.HELVETICA, 11);
                if (moods == null || moods.isEmpty()) {
                    cs.showText("No moods found.");
                } else {
                    for (Mood mood : moods) {
                        if (mood == null) continue;
                        String line = "#" + mood.getId()
                                + " | " + safe(mood.getMoodType())
                                + " | " + (mood.getMoodDate() == null ? "" : mood.getMoodDate().format(DATE_FMT))
                                + " | stress=" + (mood.getStressLevel() == null ? "-" : mood.getStressLevel())
                                + " | energy=" + (mood.getEnergyLevel() == null ? "-" : mood.getEnergyLevel());
                        cs.showText(truncate(line, 110));
                        cs.newLineAtOffset(0, -leading);
                        if (mood.getNote() != null && !mood.getNote().isBlank()) {
                            cs.showText("note: " + truncate(mood.getNote().replaceAll("\\s+", " "), 115));
                            cs.newLineAtOffset(0, -leading);
                        }
                        cs.newLineAtOffset(0, -leading);
                    }
                }

                cs.endText();
            }

            doc.save(outputFile.toFile());
        }

        return outputFile;
    }

    public Path exportJournals(Path outputFile, List<Journal> journals) throws IOException {
        if (outputFile == null) {
            throw new IOException("Output file is required.");
        }
        Files.createDirectories(outputFile.toAbsolutePath().getParent());

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;
                float leading = 16;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.newLineAtOffset(margin, y);
                cs.showText("Journal Export");
                cs.newLineAtOffset(0, -leading * 2);

                cs.setFont(PDType1Font.HELVETICA, 11);
                if (journals == null || journals.isEmpty()) {
                    cs.showText("No journal entries found.");
                } else {
                    for (Journal journal : journals) {
                        if (journal == null) continue;
                        String line = "#" + journal.getId()
                                + " | " + safe(journal.getTitle())
                                + " | " + (journal.getEntryDate() == null ? "" : journal.getEntryDate().format(DATE_FMT))
                                + " | moodId=" + (journal.getMoodId() == null ? "-" : journal.getMoodId());
                        cs.showText(truncate(line, 110));
                        cs.newLineAtOffset(0, -leading);
                        if (journal.getContent() != null && !journal.getContent().isBlank()) {
                            cs.showText(truncate(journal.getContent().replaceAll("\\s+", " "), 120));
                            cs.newLineAtOffset(0, -leading);
                        }
                        cs.newLineAtOffset(0, -leading);
                    }
                }

                cs.endText();
            }

            doc.save(outputFile.toFile());
        }

        return outputFile;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 3)) + "...";
    }
}
