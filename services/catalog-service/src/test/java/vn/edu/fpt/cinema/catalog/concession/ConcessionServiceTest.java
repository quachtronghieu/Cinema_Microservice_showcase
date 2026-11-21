package vn.edu.fpt.cinema.catalog.concession;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.fpt.cinema.catalog.concession.dto.*;
import vn.edu.fpt.cinema.catalog.concession.entity.*;
import vn.edu.fpt.cinema.catalog.concession.repository.ConcessionRepository;
import vn.edu.fpt.cinema.catalog.concession.service.ConcessionService;
import vn.edu.fpt.cinema.catalog.exception.CatalogException;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcessionServiceTest {
    @Mock ConcessionRepository repository;
    @InjectMocks ConcessionService service;
    Concession item;
    @BeforeEach void setup() {
        item=new Concession("PC000001",ConcessionType.PC,"Popcorn","Sweet corn.",new BigDecimal("50000.00"),"/assets/img/concessions/popcorn.png");
        ReflectionTestUtils.setField(item,"version",3L);
    }
    UpdateConcessionRequest update(long version) {
        return new UpdateConcessionRequest(ConcessionType.PC,"  Popcorn  ","Sweet corn.",new BigDecimal("50000"),"popcorn.png",version);
    }
    @Test void identicalNormalizedDataDoesNotFlushOrChangeVersion() {
        when(repository.findForUpdate("PC000001")).thenReturn(Optional.of(item));
        var response=service.update("PC000001",update(3));
        assertThat(response.version()).isEqualTo(3);
        verify(repository,never()).flush();
        verify(repository,never()).save(any());
    }
    @Test void staleVersionIsRejectedBeforeEquality() {
        when(repository.findForUpdate("PC000001")).thenReturn(Optional.of(item));
        assertThatThrownBy(() -> service.update("PC000001",update(2)))
            .isInstanceOfSatisfying(CatalogException.class,e -> assertThat(e.code()).isEqualTo("CONCESSION_VERSION_CONFLICT"));
        verify(repository,never()).flush();
    }
    @Test void inactiveDeleteIgnoresOldVersionWithoutWrite() {
        item.deactivate();
        when(repository.findForUpdate("PC000001")).thenReturn(Optional.of(item));
        service.deactivate("PC000001",0);
        assertThat(item.getVersion()).isEqualTo(3);
        verify(repository,never()).flush();
    }
    @Test void activeDeleteChecksVersion() {
        when(repository.findForUpdate("PC000001")).thenReturn(Optional.of(item));
        assertThatThrownBy(() -> service.deactivate("PC000001",0)).isInstanceOf(CatalogException.class);
        assertThat(item.isActive()).isTrue();
    }
    @Test void validDeleteDeactivatesAndFlushes() {
        when(repository.findForUpdate("PC000001")).thenReturn(Optional.of(item));
        service.deactivate("PC000001",3);
        assertThat(item.isActive()).isFalse();
        verify(repository).flush();
    }
    @Test void missingItemReturnsDomainNotFound() {
        when(repository.findForUpdate("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update("missing",update(0)))
            .isInstanceOfSatisfying(CatalogException.class,e -> assertThat(e.status().value()).isEqualTo(404));
    }
    @Test void customerCannotReadInactiveItemButAdminCan() {
        item.deactivate();
        when(repository.findById("PC000001")).thenReturn(Optional.of(item));
        assertThatThrownBy(() -> service.detail("PC000001",true)).isInstanceOf(CatalogException.class);
        assertThat(service.detail("PC000001",false).active()).isFalse();
    }
    @Test void invalidPageOrSizeRejected() {
        assertThatThrownBy(() -> service.list(null,null,-1,20)).isInstanceOf(CatalogException.class);
        assertThatThrownBy(() -> service.list(null,null,0,101)).isInstanceOf(CatalogException.class);
        verifyNoInteractions(repository);
    }
}
