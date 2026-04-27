package org.example.service;

import org.example.model.Mood;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Alert detection service for monitoring mood patterns.
 */
public class AlertService {
    private static final String[] BAD_MOODS = {"sad", "angry", "depressed", "anxious", "stressed", "bad"};
    private static final String[] GOOD_MOODS = {"happy", "joyful", "great", "excellent", "good"};

    /**
     * Detect all alerts from mood history.
     */
    public List<MoodAlert> detectAlerts(List<Mood> moods) {
        List<MoodAlert> alerts = new ArrayList<>();

        // Check for bad mood streaks (3+ bad moods in a row)
        alerts.addAll(detectBadMoodStreaks(moods));

        // Check for low mood days
        alerts.addAll(detectLowMoodDays(moods));

        // Check for mood pattern changes
        alerts.addAll(detectMoodSwings(moods));

        // Check for extended bad period this month
        alerts.addAll(detectBadMoodMonth(moods));

        return alerts;
    }

    /**
     * Detect 3+ consecutive bad moods.
     */
    private List<MoodAlert> detectBadMoodStreaks(List<Mood> moods) {
        List<MoodAlert> alerts = new ArrayList<>();

        // Sort by date ascending (oldest first)
        List<Mood> sortedMoods = moods.stream()
                .sorted((a, b) -> a.getMoodDate().compareTo(b.getMoodDate()))
                .collect(Collectors.toList());

        int streakCount = 0;
        List<Mood> streakMoods = new ArrayList<>();

        for (Mood mood : sortedMoods) {
            if (isBadMood(mood.getMoodType())) {
                streakCount++;
                streakMoods.add(mood);

                // Alert when 3+ bad moods detected
                if (streakCount >= 3) {
                    String message = String.format(
                            "%d consecutive days with bad mood (%s). Recent dates: %s to %s",
                            streakCount,
                            streakMoods.get(0).getMoodType(),
                            streakMoods.get(Math.max(0, streakMoods.size() - 3)).getMoodDate(),
                            streakMoods.get(streakMoods.size() - 1).getMoodDate()
                    );

                    alerts.add(new MoodAlert(
                            "BAD_MOOD_STREAK",
                            "WARNING Bad Mood Streak Detected",
                            message,
                            Math.min(5, streakCount),  // Severity increases with streak length
                            LocalDate.now(),
                            new ArrayList<>(streakMoods)
                    ));
                }
            } else {
                // Reset streak
                streakCount = 0;
                streakMoods.clear();
            }
        }

        return alerts;
    }

    /**
     * Detect days with all bad moods.
     */
    private List<MoodAlert> detectLowMoodDays(List<Mood> moods) {
        List<MoodAlert> alerts = new ArrayList<>();

        // Group by date
        var moodsByDate = moods.stream()
                .collect(Collectors.groupingBy(Mood::getMoodDate));

        for (var entry : moodsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<Mood> dayMoods = entry.getValue();

            // If all moods for a day are bad
            if (!dayMoods.isEmpty() && dayMoods.stream().allMatch(m -> isBadMood(m.getMoodType()))) {
                alerts.add(new MoodAlert(
                        "LOW_MOOD_DAY",
                        "SAD Difficult Day",
                        "All mood entries for " + date + " were negative",
                        3,
                        date,
                        dayMoods
                ));
            }
        }

        return alerts;
    }

    /**
     * Detect rapid mood swings.
     */
    private List<MoodAlert> detectMoodSwings(List<Mood> moods) {
        List<MoodAlert> alerts = new ArrayList<>();

        if (moods.size() < 2) return alerts;

        // Sort by date
        List<Mood> sorted = moods.stream()
                .sorted((a, b) -> a.getMoodDate().compareTo(b.getMoodDate()))
                .collect(Collectors.toList());

        int swingCount = 0;
        for (int i = 0; i < sorted.size() - 1; i++) {
            Mood current = sorted.get(i);
            Mood next = sorted.get(i + 1);

            // Check if mood swings from good to bad or vice versa
            boolean currentBad = isBadMood(current.getMoodType());
            boolean nextBad = isBadMood(next.getMoodType());

            if (currentBad != nextBad) {
                swingCount++;
            } else {
                swingCount = 0;
            }

            // Alert on 3 consecutive swings
            if (swingCount >= 3) {
                alerts.add(new MoodAlert(
                        "MOOD_SWINGS",
                        "SWING Rapid Mood Swings",
                        "Detected " + swingCount + " consecutive mood swings. Consider self-care activities.",
                        3,
                        LocalDate.now(),
                        sorted.subList(Math.max(0, i - 3), i + 1)
                ));
                swingCount = 0;  // Reset to avoid duplicate alerts
            }
        }

        return alerts;
    }

    /**
     * Detect extended bad mood period this month.
     */
    private List<MoodAlert> detectBadMoodMonth(List<Mood> moods) {
        List<MoodAlert> alerts = new ArrayList<>();

        LocalDate now = LocalDate.now();
        List<Mood> thisMonth = moods.stream()
                .filter(m -> m.getMoodDate().getYear() == now.getYear() 
                        && m.getMoodDate().getMonth() == now.getMonth())
                .collect(Collectors.toList());

        if (thisMonth.isEmpty()) return alerts;

        long badMoodCount = thisMonth.stream()
                .filter(m -> isBadMood(m.getMoodType()))
                .count();

        long totalMoodCount = thisMonth.size();

        // If more than 50% bad moods this month
        if (badMoodCount > totalMoodCount / 2) {
            int percentage = (int) ((badMoodCount * 100) / totalMoodCount);
            alerts.add(new MoodAlert(
                    "BAD_MOOD_MONTH",
                    "📈 Concerning Month Trend",
                    percentage + "% of moods this month have been negative. Consider reaching out for support.",
                    4,
                    LocalDate.now(),
                    thisMonth
            ));
        }

        return alerts;
    }

    private boolean isBadMood(String moodType) {
        if (moodType == null) return false;
        String lower = moodType.toLowerCase();
        for (String bad : BAD_MOODS) {
            if (lower.contains(bad)) return true;
        }
        return false;
    }

    private boolean isGoodMood(String moodType) {
        if (moodType == null) return false;
        String lower = moodType.toLowerCase();
        for (String good : GOOD_MOODS) {
            if (lower.contains(good)) return true;
        }
        return false;
    }
}
