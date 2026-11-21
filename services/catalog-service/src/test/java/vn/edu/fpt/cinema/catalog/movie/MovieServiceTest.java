package vn.edu.fpt.cinema.catalog.movie;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.fpt.cinema.catalog.exception.CatalogException;
import vn.edu.fpt.cinema.catalog.movie.dto.*;
import vn.edu.fpt.cinema.catalog.movie.entity.*;
import vn.edu.fpt.cinema.catalog.movie.repository.*;
import vn.edu.fpt.cinema.catalog.movie.service.MovieService;
import vn.edu.fpt.cinema.catalog.movie.validation.MovieImages;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MovieServiceTest {
    static final LocalDate TODAY=LocalDate.of(2026,11,4);
    static final byte[] PNG={(byte)0x89,'P','N','G',0x0D,0x0A,0x1A,0x0A,0};
    final MovieRepository repository=mock(MovieRepository.class);
    final MovieImageRepository images=mock(MovieImageRepository.class);
    final MovieService service=new MovieService(repository,images,
        Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),ZoneId.of("Asia/Ho_Chi_Minh")));

    static MockMultipartFile png() { return new MockMultipartFile("image","x.png","image/png",PNG); }
    static CreateMovieRequest create(LocalDate date) {
        return new CreateMovieRequest("Dune","Sci-Fi","A valid movie summary.",120,date,"T13","https://youtu.be/abcdefghijk");
    }
    static UpdateMovieRequest update(LocalDate date, long version) {
        return new UpdateMovieRequest("Dune","Sci-Fi","A valid movie summary.",120,date,"T13","https://youtu.be/abcdefghijk",version);
    }
    static Movie stored(LocalDate date, long version) throws Exception {
        var item=new Movie("MV1","DUNE","Sci-Fi","A valid movie summary.",120,date,"T13","/assets/img/movies/a.jpg","https://youtu.be/abcdefghijk");
        var field=Movie.class.getDeclaredField("version"); field.setAccessible(true); field.set(item,version);
        return item;
    }
    static void assertFieldError(ThrowableAssert.ThrowingCallable call, String field) {
        assertThatThrownBy(call).isInstanceOfSatisfying(CatalogException.class,
            e -> assertThat(e.fieldErrors()).containsKey(field));
    }

    @Test void createRequiresImage() {
        assertFieldError(() -> service.create(create(TODAY.plusMonths(1)),null),"image");
        assertFieldError(() -> service.create(create(TODAY.plusMonths(1)),new MockMultipartFile("image",new byte[0])),"image");
        verifyNoInteractions(repository,images);
    }
    @ParameterizedTest @ValueSource(strings={"<svg></svg>","GIF89a","%PDF-1.7"})
    void contentNotMatchingAnImageSignatureIsRejectedEvenIfDeclaredPng(String content) {
        var file=new MockMultipartFile("image","x.png","image/png",content.getBytes());
        assertFieldError(() -> service.create(create(TODAY.plusMonths(1)),file),"image");
        verifyNoInteractions(images);
    }
    @Test void oversizedImageIs413() {
        var file=new MockMultipartFile("image","x.png","image/png",new byte[(int)MovieImages.MAX_BYTES+1]);
        assertThatThrownBy(() -> service.create(create(TODAY.plusMonths(1)),file))
            .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("IMAGE_TOO_LARGE"));
    }
    @ParameterizedTest @CsvSource({"13","0"})
    void createBeforeTwoWeeksIsRejected(int days) {
        assertFieldError(() -> service.create(create(TODAY.plusDays(days)),png()),"releaseDate");
        verify(repository,never()).saveAndFlush(any());
    }
    @Test void createAfterFiveMonthsIsRejected() {
        assertFieldError(() -> service.create(create(TODAY.plusMonths(5).plusDays(1)),png()),"releaseDate");
    }
    @Test void createBoundariesAreInclusiveAndServerAssignsIdTitleAndUploadUrl() {
        when(repository.saveAndFlush(any())).thenAnswer(call -> {
            var item=(Movie)call.getArgument(0);
            var field=Movie.class.getDeclaredField("version"); field.setAccessible(true); field.set(item,0L);
            return item;
        });
        for (var date : List.of(TODAY.plusWeeks(2),TODAY.plusMonths(5))) {
            var result=service.create(create(date),png());
            assertThat(result.title()).isEqualTo("DUNE");
            assertThat(UUID.fromString(result.movieId()).toString()).isEqualTo(result.movieId());
            assertThat(result.img()).startsWith(MovieImages.UPLOAD_PATH).endsWith(".png");
        }
        verify(images,times(2)).save(argThat(image -> image.getContentType().equals("image/png")));
    }
    @Test void duplicateActiveTitleConflictsBeforeWriting() {
        when(repository.existsByTitleIgnoreCaseAndActiveTrue("DUNE")).thenReturn(true);
        assertThatThrownBy(() -> service.create(create(TODAY.plusMonths(1)),png()))
            .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("MOVIE_TITLE_CONFLICT"));
        verifyNoInteractions(images);
    }
    @Test void indexViolationFromRaceBecomesTitleConflict() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("x",new RuntimeException("UX_Movie_active_title")));
        assertThatThrownBy(() -> service.create(create(TODAY.plusMonths(1)),png()))
            .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("MOVIE_TITLE_CONFLICT"));
    }
    @Test void otherIntegrityFailuresAreNotReportedAsTitleConflicts() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("x",new RuntimeException("CK_Movie_duration")));
        assertThatThrownBy(() -> service.create(create(TODAY.plusMonths(1)),png())).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void staleVersionConflictsBeforeOtherChecks() throws Exception {
        when(repository.findForUpdate("MV1")).thenReturn(Optional.of(stored(TODAY.minusDays(1),5)));
        assertThatThrownBy(() -> service.update("MV1",update(TODAY.plusDays(1),4),null))
            .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("MOVIE_VERSION_CONFLICT"));
    }
    @Test void releasedMovieCannotChangeDateButUnchangedDateIsAllowed() throws Exception {
        var released=TODAY.minusDays(1);
        when(repository.findForUpdate("MV1")).thenReturn(Optional.of(stored(released,0)));
        assertFieldError(() -> service.update("MV1",update(released.plusDays(5),0),null),"releaseDate");
        assertThat(service.update("MV1",update(released,0),null).releaseDate()).isEqualTo(released);
    }
    @Test void unreleasedMovieDateMustStayInFutureWindow() throws Exception {
        when(repository.findForUpdate("MV1")).thenReturn(Optional.of(stored(TODAY.plusDays(20),0)));
        assertFieldError(() -> service.update("MV1",update(TODAY,0),null),"releaseDate");
        assertFieldError(() -> service.update("MV1",update(TODAY.plusMonths(5).plusDays(1),0),null),"releaseDate");
        assertThat(service.update("MV1",update(TODAY.plusDays(1),0),null).releaseDate()).isEqualTo(TODAY.plusDays(1));
    }
    @Test void updateWithoutImageKeepsPoster() throws Exception {
        when(repository.findForUpdate("MV1")).thenReturn(Optional.of(stored(TODAY.plusDays(20),0)));
        assertThat(service.update("MV1",update(TODAY.plusDays(20),0),null).img()).isEqualTo("/assets/img/movies/a.jpg");
        verifyNoInteractions(images);
    }
    @Test void updateWithImageReplacesPoster() throws Exception {
        when(repository.findForUpdate("MV1")).thenReturn(Optional.of(stored(TODAY.plusDays(20),0)));
        assertThat(service.update("MV1",update(TODAY.plusDays(20),0),png()).img()).startsWith(MovieImages.UPLOAD_PATH);
        verify(repository).flush();
    }
    @Test void noOpUpdateDoesNotFlush() throws Exception {
        when(repository.findForUpdate("MV1")).thenReturn(Optional.of(stored(TODAY.plusDays(20),0)));
        service.update("MV1",update(TODAY.plusDays(20),0),null);
        verify(repository,never()).flush();
    }
    @Test void missingMovieIs404() {
        when(repository.findForUpdate("MV1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update("MV1",update(TODAY,0),null))
            .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.code()).isEqualTo("MOVIE_NOT_FOUND"));
    }
    @Test void customerCannotReadInactiveButAdminCan() throws Exception {
        var item=stored(TODAY,0); item.deactivate();
        when(repository.findById("MV1")).thenReturn(Optional.of(item));
        assertThatThrownBy(() -> service.detail("MV1",true)).isInstanceOf(CatalogException.class);
        assertThat(service.detail("MV1",false).active()).isFalse();
    }
    @ParameterizedTest @CsvSource({"-1,20","0,0","0,101"})
    void invalidPaginationIs400(int page, int size) {
        assertThatThrownBy(() -> service.listAdmin(null,null,page,size)).isInstanceOf(CatalogException.class);
        verifyNoInteractions(repository);
    }
    @Test void overlongSearchIs400() {
        assertThatThrownBy(() -> service.listPublic(MovieStatus.NOW_SHOWING,"x".repeat(101),0,20)).isInstanceOf(CatalogException.class);
        verifyNoInteractions(repository);
    }
}
