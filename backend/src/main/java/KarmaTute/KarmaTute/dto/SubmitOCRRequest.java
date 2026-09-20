package KarmaTute.KarmaTute.dto;
public class SubmitOCRRequest {
    private String extractedText;
    private Double confidenceScore;
    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
}
