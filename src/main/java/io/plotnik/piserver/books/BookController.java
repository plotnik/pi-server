package io.plotnik.piserver.books;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class BookController {

    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    @Value("${home.path}")
    private String homePath;

    @Value("${lists.path}")
    private String listsPath;

    @RequestMapping(value="/books", method=RequestMethod.GET)
    public Book[] getList() {
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(homePath, listsPath, "books.json"));
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