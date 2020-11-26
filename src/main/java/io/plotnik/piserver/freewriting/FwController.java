package io.plotnik.piserver.freewriting;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.plotnik.piserver.common.OpResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

@RestController
@RequestMapping("/fw")
public class FwController {

    private static final Logger log = LoggerFactory.getLogger(FwController.class);

    @Value("${home.path}")
    private String homePath;

    @Value("${freewriting.path}")
    private String fwPath;

    @Value("${freewriting.tags}")
    private String tagsPath;

    /**
     * База фрирайтов.
     */
    Freewriting fw;

    /**
     * Маппинг дат на фрирайты.
     */
    Map<LocalDate, FwDate> fmap = new HashMap<>();

    /**
     * Маппинг хэштэга на URL.
     */
    Map<String, String> tags = new HashMap<>();

    Map<LocalDate, Set<String>> noteTags = new HashMap<>();

    DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Предзагрузка.
     */
    @PostConstruct
    public void init() {
        try {
            /* Загрузить фрирайты
             */
            fw = new Freewriting(homePath + fwPath);

            /* Замапить даты во фрирайты для более быстрого доступа
             */
            for (FwDate w : fw.getFWDates()) {
                fmap.put(w.getDate(), w);
            }

            /* Загрузить тэги
             */
            File[] tagFiles = new File(homePath + tagsPath).listFiles();
            for (File tf: tagFiles) {
                if (tf.isFile() && tf.getName().endsWith(".md")) {
                    String url = Files.readString(Paths.get(tf.getPath()));
                    String name = tf.getName().substring(0, tf.getName().length()-3);
                    tags.put(name, url);
                }
            }

        } catch (Exception e) {
            if (e instanceof FwException) {
                log.warn("[FW Exception] " + e.getMessage());
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Получить фрирайт по дате.
     */
    @GetMapping()
    public FwNote getNote(@RequestParam(name = "d", defaultValue = "") String datestr) {
        FwNote res = new FwNote();
        try {
            LocalDate d = LocalDate.parse(datestr, df);
            FwDate w = fmap.get(d);
            if (w == null) {
                return null;
            }
            res.setText(Files.readString(Paths.get(w.getPath().getPath())));

        } catch (DateTimeParseException e) {
        } catch (IOException e) {
        }

        return res;
    }

    @GetMapping(value = "/tags")
    public List<String> getTags(@RequestParam(name = "f", defaultValue = "") String filterstr) {
        Stream<String> stm = tags.keySet().stream();
        if (filterstr.length() > 0) {
            String lostr = filterstr.toLowerCase();
            stm = stm.filter(s -> s.toLowerCase().contains(lostr));
        }
        return stm.sorted().collect(Collectors.toList());
        /*
        List<String> res = new ArrayList<>(tags.keySet());
        Collections.sort(res);
        return res;
        */
    }

    @PostMapping(value = "{d}/tag/{t}")
    public OpResult addTagToNote(@PathVariable String d,
                                 @PathVariable String t) {

        /* Проверить, что хэштэг существует
         */
        if (tags.get(t) == null) {
            return new OpResult(false, "Unknown tag");
        }

        /* Проверить, что фрирайт существует.
         */
        LocalDate date = null;
        try {
            date = LocalDate.parse(d, df);
        } catch (DateTimeParseException e) {
            return new OpResult(false, "Invalid date format");
        }
        if (fmap.get(date) == null) {
            return new OpResult(false, "Unknown note");
        }

        /* Добавить хэштег в маппинг.
         */
        Set<String> tagList = noteTags.get(date);
        if (tagList == null) {
            tagList = new HashSet<>();
            noteTags.put(date, tagList);
        }
        tagList.add(t);

        return new OpResult(true);
    }

}
