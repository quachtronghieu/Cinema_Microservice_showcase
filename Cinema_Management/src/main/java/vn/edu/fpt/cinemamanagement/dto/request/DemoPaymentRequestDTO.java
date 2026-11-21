package vn.edu.fpt.cinemamanagement.dto.request;

/** The selected outcome is accepted only by the internal demo payment flow. */
public class DemoPaymentRequestDTO {
    private String outcome;

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }
}
