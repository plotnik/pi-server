package io.plotnik.piserver.films;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class FilmController {

    @Value("${lists.home}")
    private String listsHome;  

    @RequestMapping(value="/films", method=RequestMethod.GET)
    public Film[] getList() {
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(listsHome + "/films.json"));
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonData, Film[].class);  
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }     
    }  

}