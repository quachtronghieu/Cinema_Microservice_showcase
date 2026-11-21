package vn.edu.fpt.cinema.catalog.concession.dto;
import vn.edu.fpt.cinema.catalog.concession.entity.*;
import java.math.BigDecimal;
public record ConcessionResponse(String concessionId, ConcessionType type, String name, String description,
                                 BigDecimal price, String img, boolean active, long version) {
    public static ConcessionResponse from(Concession item) {
        return new ConcessionResponse(item.getConcessionId(), item.getType(), item.getName(), item.getDescription(),
            item.getPrice(), item.getImg(), item.isActive(), item.getVersion());
    }
}
