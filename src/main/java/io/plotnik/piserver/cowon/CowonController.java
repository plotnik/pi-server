package io.plotnik.piserver.cowon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class CowonController {

    @Value("${lists.home}")
    private String listsHome;  

    List<CowonAlbum> jalbums;

    @PostConstruct
    public void init() {
        reload();
    }

    @RequestMapping(value="/cowon", method=RequestMethod.GET)
    public List<CowonAlbum> getList(
            @RequestParam(name="q", defaultValue="") String q,
            @RequestParam(name="skip", defaultValue="0") int skip,
            @RequestParam(name="limit", defaultValue="0") int limit) 
    {
        final String q2 = q.toLowerCase();
        List<CowonAlbum> result = jalbums;

        if (q.length()>0) {
            Predicate<CowonAlbum> byQ = a -> 
                (a.name!=null && a.name.toLowerCase().contains(q2)) || 
                (a.artist!=null && a.artist.toLowerCase().contains(q2));

            result = jalbums.stream().filter(byQ).collect(Collectors.toList());
        }

        if (skip!=0 || limit!=0) {
            int toIndex = result.size();
            if (limit!=0) {
                toIndex = skip + limit;
            }
            result = result.subList(skip, toIndex);
        }
                                         
        return result; 
    }

    @RequestMapping(value="/cowon/reload", method=RequestMethod.GET)
    public void reload() {
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(listsHome + "/cowon.json"));
            ObjectMapper mapper = new ObjectMapper();
            jalbums = mapper.readValue(jsonData, new TypeReference<List<CowonAlbum>>(){});  
                
        } catch (IOException e) {
            e.printStackTrace();
        }     
    }
      
}