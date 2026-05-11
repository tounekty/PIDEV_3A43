package org.example.event;

/**
 * Résultat d'une recommandation personnalisée pour un événement.
 */
public class RecommendationResult {

    private final Event  event;
    private final double score;       // 0.0 – 1.0
    private final String reason;      // explication lisible

    public RecommendationResult(Event event, double score, String reason) {
        this.event  = event;
        this.score  = Math.min(1.0, Math.max(0.0, score));
        this.reason = reason;
    }

    public Event  getEvent()  { return event; }
    public double getScore()  { return score; }
    public String getReason() { return reason; }

    /** Score as percentage string, e.g. "87%" */
    public String getScorePercent() {
        return String.format("%.0f%%", score * 100);
    }
}
