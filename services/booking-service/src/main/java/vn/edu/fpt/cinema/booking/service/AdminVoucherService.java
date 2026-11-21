package vn.edu.fpt.cinema.booking.service;

import static org.springframework.http.HttpStatus.*;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinema.booking.dto.CreateVoucherRequest;
import vn.edu.fpt.cinema.booking.dto.EditVoucherRequest;
import vn.edu.fpt.cinema.booking.dto.VoucherPage;
import vn.edu.fpt.cinema.booking.dto.VoucherResponse;
import vn.edu.fpt.cinema.booking.exception.BookingException;
import vn.edu.fpt.cinema.booking.repository.AdminVoucherRepository;

@Service
@Transactional(readOnly = true)
public class AdminVoucherService {
    private final AdminVoucherRepository repository;
    private final Validator validator;

    public AdminVoucherService(AdminVoucherRepository repository, Validator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public VoucherPage list(Boolean status, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BookingException(BAD_REQUEST, "VALIDATION_ERROR", "Invalid pagination");
        }
        return repository.list(status, page, size);
    }

    public VoucherResponse detail(String id) {
        validateId(id);
        return repository.find(id).orElseThrow(AdminVoucherService::notFound);
    }

    @Transactional
    public VoucherResponse create(CreateVoucherRequest request) {
        validate(request);
        String id = UUID.randomUUID().toString();
        try {
            repository.insert(id, request);
        } catch (DuplicateKeyException e) {
            throw duplicateCode();
        }
        return repository.find(id).orElseThrow(AdminVoucherService::notFound);
    }

    @Transactional
    public VoucherResponse edit(String id, EditVoucherRequest request) {
        validateId(id);
        validate(request);
        VoucherResponse current = repository.lock(id).orElseThrow(AdminVoucherService::notFound);
        var merged = new CreateVoucherRequest(
            request.code() == null ? current.code() : request.code(),
            request.voucherName() == null ? current.voucherName() : request.voucherName(),
            request.discountType() == null ? current.discountType() : request.discountType(),
            request.discountValue() == null ? current.discountValue() : request.discountValue(),
            request.usageLimit() == null ? current.usageLimit() : request.usageLimit());
        validate(merged); // A value-only PATCH must also respect the locked row's discount type.

        Map<String, Object> changes = new LinkedHashMap<>();
        if (request.code() != null && !request.code().equals(current.code())) changes.put("code", request.code());
        if (request.discountType() != null && request.discountType() != current.discountType()) {
            changes.put("discount_type", request.discountType().name());
        }
        if (request.discountValue() != null && request.discountValue().compareTo(current.discountValue()) != 0) {
            changes.put("discount_value", request.discountValue());
        }
        boolean protectedEdit = !changes.isEmpty();
        if (protectedEdit && (current.usedCount() != 0 || current.reservedCount() != 0)) throw restricted();
        if (merged.usageLimit() < (long) current.usedCount() + current.reservedCount()) throw limitConflict();

        if (request.voucherName() != null && !request.voucherName().equals(current.voucherName())) {
            changes.put("voucher_name", request.voucherName());
        }
        if (request.usageLimit() != null && request.usageLimit() != current.usageLimit()) {
            changes.put("usage_limit", request.usageLimit());
        }
        if (request.status() != null && request.status() != current.status()) changes.put("status", request.status());
        if (changes.isEmpty()) return current;
        try {
            if (repository.update(id, changes, merged.usageLimit(), protectedEdit) != 1) {
                throw new IllegalStateException("Locked voucher update did not affect one row");
            }
        } catch (DuplicateKeyException e) {
            throw duplicateCode();
        }
        return repository.find(id).orElseThrow(AdminVoucherService::notFound);
    }

    @Transactional
    public void disable(String id) {
        validateId(id);
        VoucherResponse current = repository.lock(id).orElseThrow(AdminVoucherService::notFound);
        if (current.status() && repository.update(id, Map.of("status", false), current.usageLimit(), false) != 1) {
            throw new IllegalStateException("Locked voucher disable did not affect one row");
        }
    }

    private <T> void validate(T request) {
        if (request == null) throw new BookingException(BAD_REQUEST, "VALIDATION_ERROR", "Request body is required");
        var violations = validator.validate(request);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
    }

    private static void validateId(String id) {
        if (id == null || id.isBlank() || id.length() > 36) {
            throw new BookingException(BAD_REQUEST, "VALIDATION_ERROR", "Invalid voucher ID");
        }
    }

    private static BookingException notFound() {
        return new BookingException(NOT_FOUND, "VOUCHER_NOT_FOUND", "Voucher not found");
    }
    private static BookingException duplicateCode() {
        return new BookingException(CONFLICT, "VOUCHER_CODE_EXISTS", "Voucher code already exists");
    }
    private static BookingException restricted() {
        return new BookingException(CONFLICT, "VOUCHER_EDIT_RESTRICTED", "Voucher has used or reserved uses");
    }
    private static BookingException limitConflict() {
        return new BookingException(CONFLICT, "VOUCHER_LIMIT_CONFLICT", "Usage limit is below used and reserved uses");
    }
}
