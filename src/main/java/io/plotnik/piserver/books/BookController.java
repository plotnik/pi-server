package io.plotnik.piserver.books;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class BookController {

    @Value("${lists.home}")
    private String listsHome;  

    @RequestMapping(value="/books", method=RequestMethod.GET)
    public Book[] getList() {
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(listsHome + "/books.json"));
            ObjectMapper mapper = new ObjectMapper();
            Book[] books = mapper.readValue(jsonData, Book[].class);  
            return books; 
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }     
    }    
}