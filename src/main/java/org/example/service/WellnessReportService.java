package org.example.service;

import org.example.model.Journal;
import org.example.model.Mood;

import java.time.LocalDate;
import java.util.List;

public class WellnessReportService {

    public String generateReport(List<Mood> moods, List<Journal> journals, LocalDate today) {
        int moodCount = moods == null ? 0 : moods.size();
        int journalCount = journals == null ? 0 : journals.size();

        int highStress = 0;
        if (moods != null) {
            for (Mood mood : moods) {
                if (mood != null && mood.getStressLevel() != null && mood.getStressLevel() >= 8) {
                    highStress++;
                }
            }
        }

        String date = today == null ? "" : today.toString();
        return "Wellness Report (" + date + ")\n\n"
                + "- Total moods: " + moodCount + "\n"
                + "- Total journals: " + journalCount + "\n"
                + "- High-stress moods (>=8): " + highStress + "\n\n"
                + "Suggestions:\n"
                + (highStress > 0
                ? "- Consider scheduling a check-in appointment and using breathing/grounding exercises.\n"
                : "- Keep a consistent sleep routine and keep journaling your patterns.\n");
    }
}
