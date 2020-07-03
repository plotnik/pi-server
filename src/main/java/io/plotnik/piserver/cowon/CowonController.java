package io.plotnik.piserver.cowon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class CowonController {

    @Value("${lists.home}")
    private String listsHome;  

    @RequestMapping(value="/cowon", method=RequestMethod.GET)
    public CowonAlbum[] getList() {
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(listsHome + "/cowon.json"));
            ObjectMapper mapper = new ObjectMapper();
            CowonAlbum[] jalbums = mapper.readValue(jsonData, CowonAlbum[].class);  
            return jalbums; 
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }     
    }
      
}