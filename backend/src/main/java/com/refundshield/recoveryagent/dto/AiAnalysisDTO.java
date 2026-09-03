package com.refundshield.recoveryagent.dto;

public class AiAnalysisDTO {
    private String summary;
    private String reason;
    private String recommendation;
    private String message;
    private String confidence;

    public AiAnalysisDTO() {}

    public AiAnalysisDTO(String summary, String reason, String recommendation, String message, String confidence) {
        this.summary = summary;
        this.reason = reason;
        this.recommendation = recommendation;
        this.message = message;
        this.confidence = confidence;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
}
