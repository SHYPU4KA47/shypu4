package app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Клиент для поиска книг через OpenLibrary API.
 * Предоставляет консольный интерфейс для выполнения поисковых запросов.
 * 
 * @author Ваше Имя
 * @version 1.0
 */
public class OpenLibraryClient {
    private static final String API_URL = "https://openlibrary.org/search.json?q=";
    private static final int TIMEOUT_SECONDS = 10;
    private static final Logger log = LogManager.getLogger(OpenLibraryClient.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Точка входа в приложение.
     * Принимает поисковый запрос из аргументов командной строки или интерактивного ввода.
     * 
     * @param args аргументы командной строки (поисковый запрос)
     */
    public static void main(String[] args) {
        String query;
        if (args.length == 0) {
            System.out.print("Введите поисковый запрос (пример: Толкин): ");
            try (Scanner scanner = new Scanner(System.in)) {
                if (!scanner.hasNextLine()) {
                    System.out.println("Стандартный ввод недоступен, передайте запрос аргументом командной строки.");
                    return;
                }
                query = scanner.nextLine().trim();
                if (query.isEmpty()) {
                    System.out.println("Пустой запрос, завершение.");
                    return;
                }
            }
        } else {
            query = String.join(" ", args);
        }

        OpenLibraryClient client = new OpenLibraryClient();
        try {
            client.searchAndPrint(query, 5);
        } catch (Exception e) {
            log.error("Сбой при обращении к API", e);
            System.err.println("Ошибка: " + e.getMessage());
        }
    }

    /**
     * Выполняет поиск книг и выводит результаты в консоль.
     * 
     * @param query поисковый запрос
     * @param limit максимальное количество выводимых результатов
     * @throws IOException если возникают проблемы с сетью или парсингом
     * @throws InterruptedException если поток выполнения был прерван
     */
    public void searchAndPrint(String query, int limit) throws IOException, InterruptedException {
        List<Doc> docs = fetchDocs(query);
        if (docs.isEmpty()) {
            System.out.println("Результатов нет для запроса: " + query);
            return;
        }

        System.out.println("Показаны первые " + limit + " результатов по запросу: " + query);
        String output = docs.stream()
                .filter(Objects::nonNull)
                .limit(limit)
                .map(this::formatDoc)
                .collect(Collectors.joining(System.lineSeparator()));
        if (!output.isEmpty()) {
            System.out.println(output);
        }
    }

    /**
     * Выполняет поиск и возвращает отформатированные результаты.
     * 
     * @param query поисковый запрос
     * @param limit максимальное количество результатов
     * @return список отформатированных строк с результатами
     * @throws IOException если возникают проблемы с сетью или парсингом
     * @throws InterruptedException если поток выполнения был прерван
     */
    public List<String> searchFormatted(String query, int limit) throws IOException, InterruptedException {
        List<Doc> docs = fetchDocs(query);
        return docs.stream()
                .filter(Objects::nonNull)
                .limit(limit)
                .map(this::formatDoc)
                .toList();
    }

    /**
     * Выполняет HTTP-запрос к OpenLibrary API и возвращает список книг.
     * 
     * @param query поисковый запрос
     * @return список объектов Doc с информацией о книгах
     * @throws IOException если возникают проблемы с сетью или парсингом
     * @throws InterruptedException если поток выполнения был прерван
     */
    private List<Doc> fetchDocs(String query) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = API_URL + encoded;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("Запрос {} вернул статус {}", url, response.statusCode());

        if (response.statusCode() != 200) {
            throw new IOException("API вернул код " + response.statusCode());
        }

        SearchResponse result = mapper.readValue(response.body(), SearchResponse.class);
        return result.docs == null ? List.of() : result.docs;
    }

    /**
     * Форматирует информацию о книге в строку.
     * 
     * @param doc объект с информацией о книге
     * @return отформатированная строка в формате "Название — Автор (Год)"
     */
    String formatDoc(Doc doc) {
        String authors = (doc.authorName == null || doc.authorName.isEmpty())
                ? "Автор не указан"
                : String.join(", ", doc.authorName);
        String year = doc.firstPublishYear == null ? "?" : doc.firstPublishYear.toString();
        return doc.title + " — " + authors + " (" + year + ")";
    }

    /**
     * Класс для хранения ответа от OpenLibrary API.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SearchResponse {
        /** Список найденных книг. */
        public List<Doc> docs;
    }

    /**
     * Класс для хранения информации о книге.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Doc {
        /** Название книги. */
        public String title;
        
        /** Список авторов книги. */
        @JsonProperty("author_name")
        public List<String> authorName;
        
        /** Год первой публикации. */
        @JsonProperty("first_publish_year")
        public Integer firstPublishYear;
    }
}

