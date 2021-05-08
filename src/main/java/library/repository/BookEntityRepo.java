package library.repository;

import library.domain.BookEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BookEntityRepo extends JpaRepository<BookEntity, Long> {

    // поиск книг по имени книги и/или фио автора
    // AuthorFioContaining... - поиск совпадения в поле fio объекта, находящегося в поле author книги
    List<BookEntity> findByNameContainingIgnoreCaseOrAuthorRuFioContainingIgnoreCaseOrderByName(String name, String fio);
    List<BookEntity> findByNameContainingIgnoreCaseOrAuthorEnFioContainingIgnoreCaseOrderByName(String name, String fio);

    // поиск книг по имени книги и/или фио автора с постраничностью
    Page<BookEntity> findByNameContainingIgnoreCaseOrAuthorRuFioContainingIgnoreCaseOrderByName(String name, String fio, Pageable pageable);
    Page<BookEntity> findByNameContainingIgnoreCaseOrAuthorEnFioContainingIgnoreCaseOrderByName(String name, String fio, Pageable pageable);

    // создает страницу (Page) книг (BookEntity) для которых заполнены все поля, кроме content
    @Query("select new BookEntity(b.id, b.name, b.pageCount, b.isbn, b.genre, b.author, b.publisher, b.publishYear, " +
            "b.image, b.avgRating, b.totalVoteCount, b.totalRating, b.viewCount, b.description) from BookEntity b")
    Page<BookEntity> findAllWithoutContent(Pageable pageable);  // возвращает список книг с постраничностью

    // обновляет книгу по id добавляя в нее контент
    @Modifying(clearAutomatically = true)
    @Query("update BookEntity b set b.content=:content where b.id=:id")     // :content - это ссылка на @Param("content")
    void updateContent(@Param("content") byte[] content, @Param("id") long id);

    // обновляет количество просмотров книги по id
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("update BookEntity b set b.viewCount=:viewCount where b.id=:id")
    void updateViewCount(@Param("id") long id, @Param("viewCount") long viewCount);

    // обновляет данные рейтинга книги по id
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("update BookEntity b set b.totalRating=:tRating, b.totalVoteCount=:tVoteCount, b.avgRating=:avgRating where b.id=:id")
    void updateRating(@Param("id") long id, @Param("tRating") long tRating, @Param("tVoteCount") long tVoteCount, @Param("avgRating") int avgRating);

    // Для топовых книг показываем только изображение (в классе Book должен быть соответствующий конструктор)
    @Query("select new BookEntity(b.id, b.image) from BookEntity b")
    List<BookEntity> findTopBooks(Pageable pageable);     // у книг будет заполнены только id и image

    // поиск книг по жанру
    @Query("select new BookEntity(b.id, b.name, b.pageCount, b.isbn, b.genre, b.author, b.publisher, b.publishYear, " +
            "b.image, b.avgRating, b.totalVoteCount, b.totalRating, b.viewCount, b.description) from BookEntity b " +
            "WHERE b.genre.id = :genreId")
    Page<BookEntity> findByGenre(@Param("genreId") long genreId, Pageable pageable);

    // получение контента по id
    @Query("SELECT b.content FROM BookEntity b WHERE b.id=:id")
    byte[] getContent(@Param("id") long id);

    // получить все ISBN кроме ISBN книги с указанным id
    @Query("SELECT b.isbn FROM BookEntity b WHERE :id IS NULL OR b.isbn NOT IN (SELECT b1.isbn FROM BookEntity b1 WHERE b.id = :id)")
    List<String> getAllisbnExceptThis(@Param("id") Long id);
}
