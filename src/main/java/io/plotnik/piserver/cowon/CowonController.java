package io.plotnik.piserver.cowon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.plotnik.piserver.common.OpResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class CowonController {

    final String COWON_JSON = "/cowon.json";
    final String HISTORY_JSON = "/cowon-history.json";

    static final Logger logger = Logger.getLogger(CowonController.class.getName());

    @Value("${home.path}")
    private String homePath;

    @Value("${lists.path}")
    private String listsPath;

    List<CowonAlbum> jalbums;
    Map<String, CowonHistory> history;

    @PostConstruct
    public void init() {
        //reload();
    }

    @RequestMapping(value = "/cowon", method = RequestMethod.GET)
    public List<CowonAlbum> getList(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "skip", defaultValue = "0") int skip,
            @RequestParam(name = "limit", defaultValue = "0") int limit) {
        final String q2 = q.toLowerCase();
        List<CowonAlbum> result = jalbums;

        // отфильтровать список по поисковому запросу
        if (q.length() > 0) {
            Predicate<CowonAlbum> byQ = a
                    -> (a.name != null && a.name.toLowerCase().contains(q2))
                    || (a.artist != null && a.artist.toLowerCase().contains(q2));

            result = jalbums.stream().filter(byQ).collect(Collectors.toList());
        }

        // отфильтровать список по skip & limit
        if (skip != 0 || limit != 0) {
            int toIndex = result.size();
            if (limit != 0) {
                toIndex = Math.min(skip + limit, toIndex);
            }
            if (skip>toIndex) {
                skip = toIndex;
            }
            result = result.subList(skip, toIndex);
        }

        return result;
    }

    @RequestMapping(value = "/cowon/reload", method = RequestMethod.GET)
    public OpResult reload() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // прочитать из файла список альбомов на плейере
            byte[] jsonData = Files.readAllBytes(Paths.get(homePath, listsPath + COWON_JSON));
            jalbums = mapper.readValue(jsonData, new TypeReference<List<CowonAlbum>>() {
            });

            // прочитать из файла историю
            if (Files.exists(Paths.get(homePath, listsPath + HISTORY_JSON))) {
                jsonData = Files.readAllBytes(Paths.get(homePath, listsPath + HISTORY_JSON));
                history = mapper.readValue(jsonData, new TypeReference<Map<String, CowonHistory>>() {
                });

            } else {
                history = new HashMap<>();
            }

            sortAlbums();

            return new OpResult(true);

        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            return new OpResult(false);
        }
    }

    /**
     * Отсортировать список альбомов.
     */
    void sortAlbums() {
        Collections.sort(jalbums, new Comparator<CowonAlbum>() {
            @Override
            public int compare(CowonAlbum a1, CowonAlbum a2) {
                String k1 = getKey(a1);
                String k2 = getKey(a2);

                // отсортировать по датам последнего прослушивания
                CowonHistory h1 = history.get(k1);
                CowonHistory h2 = history.get(k2);
                long t1 = h1 == null ? 0 : h1.getTstamp();
                long t2 = h2 == null ? 0 : h2.getTstamp();
                if (t1 < t2) {
                    return 1;
                } else if (t1 > t2) {
                    return -1;
                }
                int result = k1.compareTo(k2);
                return result;
            }
        });
    }

    /**
     * Ключ для поиска в истории.
     */
    String getKey(CowonAlbum album) {
        return album.toString();
    }

    @RequestMapping(value = "/cowon/up", method = RequestMethod.POST)
    public OpResult moveUp(@RequestBody CowonAlbum album) {
        if (album == null) {
            return new OpResult(false, "Missing cowon album");
        }

        // найти данны
        final String albumName = album.name;
        final String albumArtist = album.artist;

        Predicate<CowonAlbum> byA = a
                -> Objects.equals(a.name, albumName)
                && Objects.equals(a.artist, albumArtist);

        CowonAlbum a = jalbums.stream().filter(byA).findFirst().get();

        CowonHistory h = new CowonHistory();
        h.setTstamp(System.currentTimeMillis());
        history.put(getKey(a), h);

        sortAlbums();
        saveHistory();

        return new OpResult(true);
    }

    private void saveHistory() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(Paths.get(homePath, listsPath + HISTORY_JSON).toFile(), history);

        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

}
