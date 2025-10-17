package io.plotnik.piserver.books;

import io.plotnik.piserver.config.FwConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class BookController {

    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    @Autowired
    private FwConfig fwConfig;

    @Value("${lists.path}")
    private String listsPath;

    @RequestMapping(value="/books", method=RequestMethod.GET)
    public Book[] getList() {
        try {
            var booksJsonPath = fwConfig.path().resolve(listsPath).resolve("books.json");
            var jsonData = Files.readAllBytes(booksJsonPath);
            ObjectMapper mapper = new ObjectMapper();
            Book[] books = mapper.readValue(jsonData, Book[].class);
            log.info(books.length + " books");
            return books;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}