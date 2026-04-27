package org.example.ui.emoji;

import java.util.HashMap;
import java.util.Map;

/**
 * Mood emoji and avatar manager
 */
public class MoodEmojiManager {
    private static final Map<String, MoodEmoji> MOOD_EMOJIS = new HashMap<>();

    static {
        // Initialize mood emojis and their animations
        MOOD_EMOJIS.put("happy", new MoodEmoji("\uD83D\uDE0A", "Happy", "#FFD700"));
        MOOD_EMOJIS.put("sad", new MoodEmoji("\uD83D\uDE22", "Sad", "#4A90E2"));
        MOOD_EMOJIS.put("angry", new MoodEmoji("\uD83D\uDE20", "Angry", "#E94B3C"));
        MOOD_EMOJIS.put("calm", new MoodEmoji("\uD83D\uDE0C", "Calm", "#7ED321"));
        MOOD_EMOJIS.put("excited", new MoodEmoji("\uD83E\uDD29", "Excited", "#F5A623"));
        MOOD_EMOJIS.put("anxious", new MoodEmoji("\uD83D\uDE30", "Anxious", "#BD10E0"));
        MOOD_EMOJIS.put("stressed", new MoodEmoji("\uD83D\uDE29", "Stressed", "#FF6B6B"));
        MOOD_EMOJIS.put("depressed", new MoodEmoji("\uD83D\uDE14", "Depressed", "#5B7C99"));
        MOOD_EMOJIS.put("grateful", new MoodEmoji("\uD83D\uDE19", "Grateful", "#50E3C2"));
        MOOD_EMOJIS.put("content", new MoodEmoji("\uD83D\uDE0A", "Content", "#B8E986"));
    }

    public static MoodEmoji getEmoji(String moodType) {
        if (moodType == null || moodType.isBlank()) {
            return new MoodEmoji("\uD83D\uDE10", "Neutral", "#999999");
        }
        String key = moodType.toLowerCase().trim();
        return MOOD_EMOJIS.getOrDefault(key, new MoodEmoji("\uD83D\uDE10", moodType, "#999999"));
    }

    public static String getEmojiString(String moodType) {
        return getEmoji(moodType).emoji();
    }

    public static String getColorForMood(String moodType) {
        return getEmoji(moodType).color();
    }

    /**
     * Record representing a mood with its emoji, name, and color
     */
    public record MoodEmoji(String emoji, String name, String color) {
        public String getAnimatedEmoji() {
            // Can be extended to return different animation frames
            return emoji;
        }
    }
}
