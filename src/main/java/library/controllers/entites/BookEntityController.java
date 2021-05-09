package library.controllers.entites;

import library.domain.BookEntity;
import library.model.BookModel;
import library.service.AuthorEntityService;
import library.service.BookEntityService;
import library.service.GenreEntityService;
import library.service.PublisherEntityService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class BookEntityController {
    /** Инстансы используемых сервисов */
    AuthorEntityService authorService;
    BookEntityService bookService;
    GenreEntityService genreService;
    PublisherEntityService publisherService;

    public BookEntityController(AuthorEntityService authorService, BookEntityService bookService,
                                GenreEntityService genreService, PublisherEntityService publisherService) {
        this.authorService = authorService;
        this.bookService = bookService;
        this.genreService = genreService;
        this.publisherService = publisherService;
    }

    // Отправляет форму на создание книги
    @GetMapping("/books/new")
    public String getBookCreateForm(RedirectAttributes redirectAttr) {
        BookModel book = new BookModel();
        redirectAttr.addFlashAttribute("EditableBook", book);
        redirectAttr.addFlashAttribute("actionURL", "/books");
        redirectAttr.addFlashAttribute("actionMethod", "POST");
        redirectAttr.addFlashAttribute("ShowEditModelWindow", true);
        redirectAttr.addFlashAttribute("allAuthors", authorService.getAll());
        redirectAttr.addFlashAttribute("allPublishers", publisherService.getAll());
        return "redirect:/main_page";
    }

    // Создает новую книгу
    @PostMapping(value = "/books", consumes = { "multipart/form-data" })
    public String createBook(@ModelAttribute("EditableBook") @Valid BookModel model,
                             BindingResult binding, RedirectAttributes redirectAttr) {

        validateProcess(model, binding, redirectAttr);
        redirectAttr.addFlashAttribute("actionMethod", "POST");     // метод, которым отправлять форму после редактирования
        redirectAttr.addFlashAttribute("actionURL", "/books");      // адрес, на который отправлять

        return "redirect:/main_page";
    }

    // Отправляет форму на редактирование книги
    @GetMapping("/books/{id}/edit")
    public String getBookEditForm(@PathVariable("id") long id, RedirectAttributes redirectAttr) {
        BookModel book = bookService.get(id).toBookModel();
        redirectAttr.addFlashAttribute("EditableBook", book);
        redirectAttr.addFlashAttribute("ShowEditModelWindow", true);
        redirectAttr.addFlashAttribute("actionURL", "/books/" + book.getId());
        redirectAttr.addFlashAttribute("actionMethod", "PATCH");
        redirectAttr.addFlashAttribute("allAuthors", authorService.getAll());
        redirectAttr.addFlashAttribute("allPublishers", publisherService.getAll());
        return "redirect:/main_page";
    }

    // Обновляет книгу (принимает заполненную форму на редактирование)
    @PatchMapping(value = "/books/{id}", consumes = { "multipart/form-data" })
    public String bookEdit(@PathVariable("id") long id,
                           @ModelAttribute("EditableBook") @Valid BookModel model,
                           BindingResult binding, RedirectAttributes redirectAttr) {

        validateProcess(model, binding, redirectAttr);
        redirectAttr.addFlashAttribute("actionMethod", "PATCH");                     // метод, которым отправлять форму после редактирования
        redirectAttr.addFlashAttribute("actionURL", "/books/" + model.getId());      // адрес, на который отправлять

        return "redirect:/main_page";
    }

    // Удаляет книгу
    @DeleteMapping("/books/{id}")
    public String deleteBook(@PathVariable("id") long id) {
        bookService.delete(bookService.get(id));
        return "redirect:/main_page";
    }


    /** Методы для получения отдельных полей книги */

    // Предоставляет обложку книги по ее id
    @GetMapping("/books/img")
    public void getImage(HttpServletResponse response, @RequestParam("id") int id, Model model) throws IOException {
        BookEntity book = bookService.get(id);

        byte[] imageBytes = book.getImage();
        if (imageBytes == null) return;
        response.setContentType("image/jpg");
        response.setContentLength(imageBytes.length);
        OutputStream os = response.getOutputStream();
        os.write(imageBytes);
        os.close();
    }

    // Предоставляет содержание книги (pdf) по ее id
    @GetMapping("/books/content")
    public void getContent(HttpServletResponse response, @RequestParam("id") int id) throws IOException {
        BookEntity book = bookService.get(id);

        byte[] contentBytes = book.getContent();
        if (contentBytes == null) return;
        response.setContentType("application/pdf");
        response.setContentLength(contentBytes.length);
        OutputStream os = response.getOutputStream();
        os.write(contentBytes);
        os.close();

        bookService.updateViewCount(id, book.getViewCount() + 1);
    }

    /** Вспомогательные методы контроллера */

    // проверяет корректность заполненных данных, если все правильно - сохраняет объект,
    //      если нет - подготавливает RedirectAttributes для перенаправления на повторное редактирование формы
    public void validateProcess(BookModel model, BindingResult binding, RedirectAttributes redirectAttr) {

        // если были загружены обложка или контент, сохраняем их в поля image и content
        MultipartFile uploadedImg = model.getUploadedImage();
        MultipartFile uploadedCont = model.getUploadedContent();
        try {
            if (uploadedImg != null && uploadedImg.getSize() > 199) model.setImage(uploadedImg.getBytes());
                // если это новая книга, и обложка не была загружена, сохраняем дефолтную обложку (no-cover)
            else if (model.getId() == null) {
                String path = "src/main/resources/static/images/no-cover.jpg";
                model.setImage(Files.readAllBytes(Paths.get(path)));
            }
            if (uploadedCont != null && uploadedCont.getSize() > 199) model.setContent(uploadedCont.getBytes());
        } catch (IOException e) { e.printStackTrace(); }

        // создаем список сообщений об ошибках, отправляемый пользователю
        List<String> errorMessages = new ArrayList<>();
        if (binding.hasErrors()) {
            for (ObjectError error : binding.getAllErrors()) {
                errorMessages.add(error.getDefaultMessage());
            }
        }

        // редактирование получившегося списка
        for (int i=0; i<errorMessages.size(); i++) {
            // на случай, если пользователь оставит поля publishYear или pageCount пустыми (проблема конвертации)
            if (errorMessages.get(i).contains("Failed to convert property value of type 'java.lang.String' to required type 'int' for property 'pageCount';")) {
                errorMessages.remove(i);
                errorMessages.add(i, "Введите количество страниц");
            }
            if (errorMessages.get(i).contains("Failed to convert property value of type 'java.lang.String' to required type 'int' for property 'publishYear';")) {
                errorMessages.remove(i);
                errorMessages.add(i, "Укажите год издания");
            }
        }

        // проверка наличия обложки
        if (model.getImage() == null || model.getImage().length < 200) errorMessages.add("Загрузите обложку книги (jpg, png или gif не менее 200 байт)");

        // проверка ISBN
        String isbn = model.getIsbn();
        if (isbn == null || isbn.isEmpty()) errorMessages.add("Заполните ISBN");
        else {
            String isbnTrim = isbn.replaceAll("-", "");
            if (isbnTrim.replaceAll("\\d", "").length() != 0) errorMessages.add("ISBN не должен включать ничего кроме цифр и дефисов!");
            else if ((isbnTrim.length() - isbn.length()) > 4) errorMessages.add("Слишком много дефисов в ISBN");
            else if (isbnTrim.length() < 10 || isbnTrim.length() > 13)
                errorMessages.add("ISBN должен включать от 10 до 13 цифр (допускается использовать дефисы между ними)");
            else {
                for (String isb : bookService.getAllISBN(model.getId())) {
                    if (isb.replaceAll("-", "").equals(isbnTrim)) errorMessages.add("Книга с таким ISBN уже есть в базе");
                }
            }
        }

        // проверка года
        int year = LocalDate.now().getYear();
        Integer publishYear = model.getPublishYear();
        if (model.getPublishYear() != null && (publishYear < 400 || publishYear > year)) errorMessages.add("Проверьте год издания");

        // если форма заполнена правильно - сохраняем объект, иначе перенаправляем пользователя снова на страницу редактированя
        if (errorMessages.isEmpty()) {
            // если форма была заполнена правильно, сохраняем данные в БД
            BookEntity book = model.toBookEntity(authorService, genreService, publisherService);
            bookService.save(book);
        } else {
            // передаем контроллеру, вызываемому по redirect, список ошибок и прочие данные, необходимые для повторного редактирования объекта
            redirectAttr.addFlashAttribute("errors", errorMessages);                        // список сообщений об ошибках
            redirectAttr.addFlashAttribute("EditableBook", model);                          // редактуруемый объект
            redirectAttr.addFlashAttribute("ShowEditModelWindow", true);                 // показывать модальное окно редактирования
            redirectAttr.addFlashAttribute("allAuthors", authorService.getAll());           // список авторов
            redirectAttr.addFlashAttribute("allPublishers", publisherService.getAll());     // список издательств
        }
    }
}
