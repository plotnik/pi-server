package io.plotnik.piserver.freewriting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.plotnik.piserver.common.ApperyClient;
import io.plotnik.piserver.common.OpResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/fw")
public class FwController {

    private static final Logger log = LoggerFactory.getLogger(FwController.class);

    DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${home.path}")
    private String homePath;

    @Value("${freewriting.path}")
    private String fwPath;

    @Value("${freewriting.tags}")
    private String tagsPath;

    @Autowired
    ApperyClient apperyClient;

    /**
     * База фрирайтов.
     */
    Freewriting fw;

    /**
     * Маппинг дат на фрирайты.
     */
    Map<LocalDate, FwDate> fmap = new HashMap<>();

    /**
     * Маппинг тэга на URL.
     */
    Map<String, String> tags = new HashMap<>();

    /**
     * Маппинг фрирайта на хэштег.
     */
    Map<LocalDate, Set<String>> noteTags = new HashMap<>();

    ObjectMapper om = new ObjectMapper();

    /**
     * Предзагрузка.
     */
    @PostConstruct
    public void init() {
        try {
            reload();
            loadNoteTags();
        } catch (Exception e) {
            if (e instanceof FwException) {
                log.warn("[FW Exception] " + e.getMessage());
            } else {
                e.printStackTrace();
            }
        }
    }

    @GetMapping(value = "/reload")
    @ApiOperation(value = "Переагрузить фрирайты из папки.")
    public OpResult reloadNotes() {
        try {
            reload();
            return new OpResult(true, fw.getFWDates().size() + " notes loaded");

        } catch (Exception e) {
            return new OpResult(false, e.getMessage());
        }
    }

    void reload() throws FwException, IOException {
        /* Загрузить фрирайты
         */
        fw = new Freewriting(homePath + fwPath);
        log.info("[SUCCESS] " + fw.getFWDates().size() + " notes loaded");

        /* Замапить даты во фрирайты для более быстрого доступа
         */
        for (FwDate w : fw.getFWDates()) {
            fmap.put(w.getDate(), w);
        }

        /* Загрузить тэги
         */
        File[] tagFiles = new File(homePath + tagsPath).listFiles();
        for (File tf : tagFiles) {
            if (tf.isFile() && tf.getName().endsWith(".md")) {
                String url = Files.readString(Paths.get(tf.getPath()));
                String name = tf.getName().substring(0, tf.getName().length() - 3);
                tags.put(name, url);
            }
        }
    }

    @GetMapping()
    @ApiOperation(value = "Получить фрирайт по дате.")
    public FwNote getNote(
        @ApiParam(value = "Дата фрирайта в формате `yyyy-MM-dd`") @RequestParam(name = "d", defaultValue = "") String datestr)
    {
        FwNote res = new FwNote();
        try {
            LocalDate d = LocalDate.parse(datestr, df);
            FwDate w = fmap.get(d);
            if (w == null) {
                return res;
            }
            res.setText(Files.readString(Paths.get(w.getPath().getPath())));
            Set<String> dd = noteTags.get(d);
            if (dd != null) {
                Set<FwTag> tt = dd.stream()
                    .map(name -> new FwTag(name, tags.get(name)))
                    .collect(Collectors.toSet());
                res.setTags(tt);
            }

        } catch (DateTimeParseException | IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    @GetMapping(value = "/tags")
    @ApiOperation(value = "Вернуть список имеющихся названий тэгов, возможно отфильтрованный по заданной строке.")
    public List<String> getTags(
            @ApiParam(value = "Фильтр на имена тэгов") @RequestParam(name = "f", defaultValue = "") String filterstr) {
        Stream<String> stm = tags.keySet().stream();
        if (filterstr.length() > 0) {
            String lostr = filterstr.toLowerCase();
            stm = stm.filter(s -> s.toLowerCase().contains(lostr));
        }
        return stm.sorted().collect(Collectors.toList());
    }

    @GetMapping(value = "/loadNoteTags")
    @ApiOperation(value = "Загрузить маппинг \"фрирайт -> теги\" из Аппери.")
    public OpResult loadNoteTags() {
        try {
            /* Загрузить хэштеги из Аппери.
            */
            ApperyTag[] apperyTags = apperyClient.loadNoteTags();

            for (ApperyTag atag : apperyTags) {

                /* Распарсить дату.
                */
                LocalDate date = LocalDate.parse(atag.getDstamp(), df);

                /* Добавить хэштег в маппинг.
                */
                Set<String> tagList = noteTags.get(date);
                if (tagList == null) {
                    tagList = new HashSet<>();
                    noteTags.put(date, tagList);
                }
                String[] strarr = om.readValue(atag.getObj(), String[].class);
                for (String t : strarr) {
                    tagList.add(t);
                }
            }
            return new OpResult(true);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new OpResult(false);
        }
    }

    @ApiOperation(value = "Изменить теги для фрирайта")
    @PostMapping(value = "/updateNoteTags")
    public OpResult updateNoteTags(
        @ApiParam(value = "Дата фрирайта в формате `yyyy-MM-dd`") @RequestParam(name = "d", defaultValue = "") String datestr,
        @ApiParam(value = "Список тэгов") @RequestBody List<String> newTags)
    {
        /* Проверить, что фрирайт существует.
         */
        LocalDate date = null;
        try {
            date = LocalDate.parse(datestr, df);
        } catch (DateTimeParseException e) {
            return new OpResult(false, "Invalid date format");
        }
        if (fmap.get(date) == null) {
            return new OpResult(false, "Unknown note");
        }

        /* Установить тэги в маппинге.
         */
        Set<String> tagList = new HashSet<>();
        for (String t : newTags) {
            String url = tags.get(t);
            if (url == null) {
                return new OpResult(false, "Unknown tag " + t);
            }
            tagList.add(t);
        }
        noteTags.put(date, tagList);
        return apperyClient.updateTagList(datestr, tagList);
    }

 }
