package org.example.event;

/**
 * Résultat d'une prédiction de succès pour un créneau d'événement.
 *
 * successProbability : score entre 0.0 et 1.0 (ex: 0.82 = 82%)
 * expectedParticipants : estimation du nombre de participants
 * explanation : texte lisible expliquant les facteurs du score
 * badge : "🟢 Excellent" / "🟡 Bon" / "🔴 Risqué"
 */
public class PredictionResult {

    private double successProbability;   // 0.0 – 1.0
    private double expectedParticipants; // estimation
    private String explanation;          // bullet-point reasons
    private String badge;                // visual label

    public PredictionResult() {}

    public PredictionResult(double successProbability,
                            double expectedParticipants,
                            String explanation) {
        this.successProbability   = clamp(successProbability);
        this.expectedParticipants = Math.max(0, expectedParticipants);
        this.explanation          = explanation;
        this.badge                = computeBadge(this.successProbability);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public double getSuccessProbability()   { return successProbability; }
    public double getExpectedParticipants() { return expectedParticipants; }
    public String getExplanation()          { return explanation; }
    public String getBadge()                { return badge; }

    /** Returns probability as a percentage string, e.g. "82%" */
    public String getProbabilityPercent() {
        return String.format("%.0f%%", successProbability * 100);
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setSuccessProbability(double v) {
        this.successProbability = clamp(v);
        this.badge = computeBadge(this.successProbability);
    }

    public void setExpectedParticipants(double v) {
        this.expectedParticipants = Math.max(0, v);
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static double clamp(double v) {
        return Math.min(1.0, Math.max(0.0, v));
    }

    private static String computeBadge(double prob) {
        if (prob >= 0.75) return "🟢 Excellent";
        if (prob >= 0.55) return "🟡 Bon";
        return "🔴 Risqué";
    }

    @Override
    public String toString() {
        return String.format("PredictionResult{prob=%.0f%%, participants=%.0f, badge=%s}",
                successProbability * 100, expectedParticipants, badge);
    }
}
