package vn.edu.fpt.cinema.catalog.concession.service;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinema.catalog.concession.dto.*;
import vn.edu.fpt.cinema.catalog.concession.entity.*;
import vn.edu.fpt.cinema.catalog.concession.repository.ConcessionRepository;
import vn.edu.fpt.cinema.catalog.concession.validation.ConcessionImages;
import vn.edu.fpt.cinema.catalog.exception.CatalogException;
import java.math.RoundingMode;
import java.util.UUID;
import static org.springframework.http.HttpStatus.*;

@Service
@Transactional(readOnly = true)
public class ConcessionService {
    private final ConcessionRepository repository;
    public ConcessionService(ConcessionRepository repository) { this.repository = repository; }

    public ConcessionPage list(ConcessionType type, Boolean active, int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new CatalogException(BAD_REQUEST, "VALIDATION_ERROR", "Invalid pagination");
        Specification<Concession> filter = (root, query, cb) -> cb.conjunction();
        if (type != null) filter = filter.and((root, query, cb) -> cb.equal(root.get("type"), type));
        if (active != null) filter = filter.and((root, query, cb) -> cb.equal(root.get("active"), active));
        return ConcessionPage.from(repository.findAll(filter, PageRequest.of(page, size, Sort.by("concessionId")))
                .map(ConcessionResponse::from));
    }
    public ConcessionResponse detail(String id, boolean customer) {
        var item = repository.findById(id).orElseThrow(ConcessionService::notFound);
        if (customer && !item.isActive()) throw notFound();
        return ConcessionResponse.from(item);
    }
    @Transactional
    public ConcessionResponse create(CreateConcessionRequest request) {
        var item = new Concession(UUID.randomUUID().toString(), request.type(), request.name(), request.description(),
            request.price().setScale(2, RoundingMode.UNNECESSARY), ConcessionImages.publicPath(request.img()));
        return ConcessionResponse.from(repository.saveAndFlush(item));
    }
    @Transactional
    public ConcessionResponse update(String id, UpdateConcessionRequest request) {
        var item = repository.findForUpdate(id).orElseThrow(ConcessionService::notFound);
        checkVersion(item, request.version()); // Must precede data equality.
        var price = request.price().setScale(2, RoundingMode.UNNECESSARY);
        var image = ConcessionImages.publicPath(request.img());
        if (!item.matches(request.type(), request.name(), request.description(), price, image)) {
            item.update(request.type(), request.name(), request.description(), price, image);
            repository.flush(); // @Version increments only for a dirty managed entity.
        }
        return ConcessionResponse.from(item);
    }
    @Transactional
    public void deactivate(String id, long version) {
        if (version < 0) throw new CatalogException(BAD_REQUEST, "VALIDATION_ERROR", "Invalid version");
        var item = repository.findForUpdate(id).orElseThrow(ConcessionService::notFound);
        if (!item.isActive()) return;
        checkVersion(item, version);
        item.deactivate();
        repository.flush();
    }
    private static void checkVersion(Concession item, long version) {
        if (item.getVersion() != version)
            throw new CatalogException(CONFLICT, "CONCESSION_VERSION_CONFLICT", "Concession changed. Read it again before updating.");
    }
    private static CatalogException notFound() {
        return new CatalogException(NOT_FOUND, "CONCESSION_NOT_FOUND", "Concession not found");
    }
}
