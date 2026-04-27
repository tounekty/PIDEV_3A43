package org.example.service;

import org.example.model.Mood;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistics about moods.
 */
public class MoodStatistics {
    private final List<Mood> moods;

    public MoodStatistics(List<Mood> moods) {
        this.moods = moods;
    }

    public int getTotalCount() {
        return moods.size();
    }

    public String getDominantMood() {
        if (moods.isEmpty()) return "None";
        
        Map<String, Integer> moodCounts = new HashMap<>();
        for (Mood mood : moods) {
            moodCounts.put(mood.getMoodType(), moodCounts.getOrDefault(mood.getMoodType(), 0) + 1);
        }
        
        return moodCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");
    }

    public int getDominantMoodCount() {
        if (moods.isEmpty()) return 0;
        
        Map<String, Integer> moodCounts = new HashMap<>();
        for (Mood mood : moods) {
            moodCounts.put(mood.getMoodType(), moodCounts.getOrDefault(mood.getMoodType(), 0) + 1);
        }
        
        return moodCounts.values().stream().max(Integer::compare).orElse(0);
    }

    public Map<String, Integer> getMoodDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        for (Mood mood : moods) {
            distribution.put(mood.getMoodType(), distribution.getOrDefault(mood.getMoodType(), 0) + 1);
        }
        return distribution;
    }

    public String getStatsSummary() {
        if (moods.isEmpty()) {
            return "No mood data yet";
        }
        return String.format("Total: %d | Dominant: %s (%d)", 
                getTotalCount(), 
                getDominantMood(), 
                getDominantMoodCount());
    }
}
