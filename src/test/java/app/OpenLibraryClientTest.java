package app;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenLibraryClientTest {

    @Test
    void formatsDocWithAuthorAndYear() {
        OpenLibraryClient.Doc doc = new OpenLibraryClient.Doc();
        doc.title = "Test Title";
        doc.authorName = List.of("Автор");
        doc.firstPublishYear = 2020;

        OpenLibraryClient client = new OpenLibraryClient();
        String formatted = client.formatDoc(doc);

        assertEquals("Test Title — Автор (2020)", formatted);
    }

    @Test
    void formatsDocWithMissingAuthorAndYear() {
        OpenLibraryClient.Doc doc = new OpenLibraryClient.Doc();
        doc.title = "Another Book";

        OpenLibraryClient client = new OpenLibraryClient();
        String formatted = client.formatDoc(doc);

        assertEquals("Another Book — Автор не указан (?)", formatted);
    }
}

