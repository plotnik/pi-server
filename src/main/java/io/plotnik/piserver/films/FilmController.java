package io.plotnik.piserver.films;

import io.plotnik.piserver.config.FwConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class FilmController {

    @Autowired
    private FwConfig fwConfig;  

    @Value("${lists.path}")
    private String listsPath;  

    @RequestMapping(value="/films", method=RequestMethod.GET)
    public Film[] getList() {
        try {
            byte[] jsonData = Files.readAllBytes(fwConfig.path().resolve(listsPath).resolve("films.json"));
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonData, Film[].class);  
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }     
    }  

}