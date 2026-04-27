package org.example.service;

import org.example.model.Journal;
import org.example.model.Mood;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PreventiveWellnessAssistantService {
    private static final Set<String> NEGATIVE_MOODS = Set.of("sad", "angry", "anxious", "stressed", "depressed");
    private static final Set<String> POSITIVE_MOODS = Set.of("happy", "calm", "excited", "grateful", "content");
    private static final List<String> NEGATIVE_TERMS = List.of(
            "tired", "exhausted", "burnout", "overwhelmed", "stress", "stressed", "anxious",
            "sad", "hopeless", "alone", "angry", "pressure", "panic", "can't", "cannot",
            "unmotivated", "no motivation", "empty", "drained", "worthless"
    );
    private static final List<String> BURNOUT_TERMS = List.of(
            "burnout", "exhausted", "drained", "overwhelmed", "pressure", "tired", "no motivation", "unmotivated"
    );

    public WellnessReport analyze(List<Mood> moods, List<Journal> journals, LocalDate today) {
        List<Mood> recentMoods = moods.stream()
                .filter(mood -> mood.getMoodDate() != null)
                .filter(mood -> !mood.getMoodDate().isBefore(today.minusDays(13)))
                .sorted(Comparator.comparing(Mood::getMoodDate))
                .toList();

        List<Journal> recentJournals = journals.stream()
                .filter(journal -> journal.getEntryDate() != null)
                .filter(journal -> !journal.getEntryDate().isBefore(today.minusDays(13)))
                .sorted(Comparator.comparing(Journal::getEntryDate))
                .toList();

        if (recentMoods.isEmpty() && recentJournals.isEmpty()) {
            return new WellnessReport(
                    "GREEN",
                    "No risk pattern detected yet",
                    "Not enough recent data is available for a strong preventive reading. Start by logging mood, stress, energy, and short journal notes for a few days.",
                    "0 recent mood entries",
                    "0 recent journal entries",
                    "n/a",
                    "n/a",
                    List.of("The assistant needs recent entries before it can detect trend changes."),
                    List.of(
                            "Log one mood entry today with stress and energy levels.",
                            "Write a short journal note when something feels emotionally important.",
                            "Review the assistant again after three or more entries."
                    )
            );
        }

        double averageStress = averageLevel(recentMoods, true);
        double averageEnergy = averageLevel(recentMoods, false);
        double moodTrend = moodTrend(recentMoods);
        int negativeJournalSignals = countTerms(recentJournals, NEGATIVE_TERMS);
        int burnoutSignals = countTerms(recentJournals, BURNOUT_TERMS);
        int consistencyGaps = countRecentGaps(recentMoods, today);

        List<String> signals = new ArrayList<>();
        int riskScore = 0;

        if (!Double.isNaN(averageStress) && averageStress >= 7.0) {
            riskScore += 3;
            signals.add("Stress levels appear elevated across recent mood entries.");
        } else if (!Double.isNaN(averageStress) && averageStress >= 5.5) {
            riskScore += 1;
            signals.add("Stress is present at a moderate level and should be watched.");
        }

        if (!Double.isNaN(averageEnergy) && averageEnergy <= 3.5) {
            riskScore += 3;
            signals.add("Energy levels are low, which may indicate emotional fatigue.");
        } else if (!Double.isNaN(averageEnergy) && averageEnergy <= 5.0) {
            riskScore += 1;
            signals.add("Energy is slightly reduced compared with a balanced range.");
        }

        if (moodTrend <= -1.0) {
            riskScore += 2;
            signals.add("Mood appears to be gradually declining across recent entries.");
        }

        if (negativeJournalSignals >= 4) {
            riskScore += 2;
            signals.add("Repeated negative thoughts or pressure-related words were detected in recent journals.");
        }

        if (burnoutSignals >= 2) {
            riskScore += 2;
            signals.add("Signs connected to emotional exhaustion or possible burnout risk are developing.");
        }

        if (consistencyGaps >= 3) {
            riskScore += 1;
            signals.add("Logging consistency dropped recently, which can make early warning signs easier to miss.");
        }

        if (signals.isEmpty()) {
            signals.add("You seem stable and balanced recently based on the available entries.");
        }

        String status = riskScore >= 7 ? "RED" : riskScore >= 3 ? "ORANGE" : "GREEN";
        String headline = switch (status) {
            case "RED" -> "High preventive attention recommended";
            case "ORANGE" -> "Early warning signs detected";
            default -> "Stable and balanced recently";
        };
        String summary = switch (status) {
            case "RED" -> "Recent data suggests rising emotional load. This does not diagnose a condition, but it is a clear signal to reduce pressure and ask for support before overload builds further.";
            case "ORANGE" -> "Some recent patterns point toward increasing stress, lower energy, or repeated negative themes. A small preventive adjustment now may help avoid burnout later.";
            default -> "Your recent mood, stress, energy, and journal patterns look relatively steady. Keep tracking consistently so changes are noticed early.";
        };

        return new WellnessReport(
                status,
                headline,
                summary,
                recentMoods.size() + " recent mood entries",
                recentJournals.size() + " recent journal entries",
                formatMetric(averageStress),
                formatMetric(averageEnergy),
                signals,
                recommendations(status, averageStress, averageEnergy, negativeJournalSignals, burnoutSignals)
        );
    }

    private List<String> recommendations(String status, double averageStress, double averageEnergy, int negativeSignals, int burnoutSignals) {
        List<String> items = new ArrayList<>();
        if ("RED".equals(status)) {
            items.add("Reduce tomorrow's workload to the most essential tasks and postpone one non-urgent responsibility.");
            items.add("Tell a trusted person, counselor, or professional that your recent data suggests emotional overload may be building.");
        } else if ("ORANGE".equals(status)) {
            items.add("Plan one low-pressure recovery block today before stress has a chance to accumulate.");
            items.add("Choose one realistic coping action and repeat it for the next three days.");
        } else {
            items.add("Keep the current routine steady and continue logging stress and energy levels.");
            items.add("Use the assistant weekly to catch changes before they become serious.");
        }
        if (!Double.isNaN(averageStress) && averageStress >= 6.0) {
            items.add("Identify the biggest stress source and write one concrete boundary or next step for it.");
        }
        if (!Double.isNaN(averageEnergy) && averageEnergy <= 5.0) {
            items.add("Protect sleep, hydration, and a short screen-free break to rebuild energy.");
        }
        if (negativeSignals > 0 || burnoutSignals > 0) {
            items.add("In the next journal entry, separate facts from repeated thoughts so patterns become easier to challenge.");
        }
        return items.stream().distinct().limit(5).toList();
    }

    private double averageLevel(List<Mood> moods, boolean stress) {
        return moods.stream()
                .map(stress ? Mood::getStressLevel : Mood::getEnergyLevel)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(Double.NaN);
    }

    private double moodTrend(List<Mood> moods) {
        if (moods.size() < 4) {
            return 0.0;
        }
        int midpoint = moods.size() / 2;
        double earlier = moods.subList(0, midpoint).stream().mapToInt(this::moodScore).average().orElse(0.0);
        double recent = moods.subList(midpoint, moods.size()).stream().mapToInt(this::moodScore).average().orElse(0.0);
        return recent - earlier;
    }

    private int moodScore(Mood mood) {
        String type = mood.getMoodType() == null ? "" : mood.getMoodType().toLowerCase(Locale.ROOT);
        if (POSITIVE_MOODS.stream().anyMatch(type::contains)) {
            return 2;
        }
        if (NEGATIVE_MOODS.stream().anyMatch(type::contains)) {
            return -2;
        }
        return 0;
    }

    private int countTerms(List<Journal> journals, List<String> terms) {
        String text = journals.stream()
                .map(journal -> (journal.getTitle() == null ? "" : journal.getTitle()) + " " + (journal.getContent() == null ? "" : journal.getContent()))
                .collect(Collectors.joining(" "))
                .toLowerCase(Locale.ROOT);
        int count = 0;
        for (String term : terms) {
            if (text.contains(term)) {
                count++;
            }
        }
        return count;
    }

    private int countRecentGaps(List<Mood> moods, LocalDate today) {
        Map<LocalDate, Long> byDate = moods.stream().collect(Collectors.groupingBy(Mood::getMoodDate, Collectors.counting()));
        int gaps = 0;
        for (int i = 0; i < 7; i++) {
            if (!byDate.containsKey(today.minusDays(i))) {
                gaps++;
            }
        }
        return gaps;
    }

    private String formatMetric(double value) {
        return Double.isNaN(value) ? "n/a" : String.format(Locale.US, "%.1f / 10", value);
    }

    public record WellnessReport(
            String status,
            String headline,
            String summary,
            String moodCoverage,
            String journalCoverage,
            String averageStress,
            String averageEnergy,
            List<String> warningSigns,
            List<String> recommendations
    ) {
    }
}
