package vn.edu.fpt.cinemamanagement.dto.request;

import java.util.ArrayList;
import java.util.List;

/** Input shared by the customer booking preview and confirmation endpoints. */
public class CustomerBookingRequestDTO {
    private String holdId;
    private String showtimeId;
    private List<String> showtimeSeatIds = new ArrayList<>();
    private List<ConcessionItemDTO> concessions = new ArrayList<>();
    private String voucherCode;

    public String getHoldId() { return holdId; }
    public void setHoldId(String holdId) { this.holdId = holdId; }
    public String getShowtimeId() { return showtimeId; }
    public void setShowtimeId(String showtimeId) { this.showtimeId = showtimeId; }
    public List<String> getShowtimeSeatIds() { return showtimeSeatIds; }
    public void setShowtimeSeatIds(List<String> showtimeSeatIds) { this.showtimeSeatIds = showtimeSeatIds; }
    public List<ConcessionItemDTO> getConcessions() { return concessions; }
    public void setConcessions(List<ConcessionItemDTO> concessions) { this.concessions = concessions; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public static class ConcessionItemDTO {
        private String concessionId;
        private int quantity;

        public String getConcessionId() { return concessionId; }
        public void setConcessionId(String concessionId) { this.concessionId = concessionId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
