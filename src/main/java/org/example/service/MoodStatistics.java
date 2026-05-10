package org.example.service;

import org.example.model.Mood;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MoodStatistics {
    private final List<Mood> moods;

    public MoodStatistics(List<Mood> moods) {
        this.moods = moods == null ? List.of() : List.copyOf(moods);
    }

    public int total() {
        return moods.size();
    }

    public Map<String, Long> countByType() {
        return moods.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(m -> safeType(m.getMoodType()), Collectors.counting()));
    }

    private String safeType(String type) {
        return type == null || type.isBlank() ? "unknown" : type.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
