package vn.edu.fpt.cinema.catalog.concession.dto;

import jakarta.validation.constraints.*;
import vn.edu.fpt.cinema.catalog.concession.entity.ConcessionType;
import vn.edu.fpt.cinema.catalog.concession.validation.ApprovedImage;
import java.math.BigDecimal;

public record CreateConcessionRequest(
    @NotNull ConcessionType type,
    @NotBlank @Size(min=2, max=50) @Pattern(regexp="[A-Za-z0-9 ]+") String name,
    @NotBlank @Size(min=5, max=200) @Pattern(regexp="[A-Za-z0-9 ,.]+") String description,
    @NotNull @DecimalMin(value="0", inclusive=false)
    @Digits(integer=17, fraction=0, message="Price must be a whole VND amount with at most 17 integer digits") BigDecimal price,
    @NotBlank @ApprovedImage String img
) {
    public CreateConcessionRequest {
        name = name == null ? null : name.trim();
        description = description == null ? null : description.trim();
        price = price == null ? null : price.stripTrailingZeros();
    }
}
