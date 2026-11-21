package vn.edu.fpt.cinema.catalog.concession.dto;
import org.springframework.data.domain.Page;
import java.util.List;
public record ConcessionPage(List<ConcessionResponse> items, int page, int size, long totalElements, int totalPages) {
    public static ConcessionPage from(Page<ConcessionResponse> page) {
        return new ConcessionPage(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
