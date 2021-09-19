package io.plotnik.piserver.freewriting;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.plotnik.piserver.common.OpResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    TagCat tagCat = new TagCat();

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

        tagCat.loadTagDefinitions(homePath + tagsPath);
    }

    @GetMapping()
    @ApiOperation(value = "Получить фрирайт по дате.")
    public FwNote getNote(
        @ApiParam(value = "Дата фрирайта в формате `yyyy-MM-dd`") @RequestParam(name = "d", defaultValue = "") String datestr)
    {
        FwNote res = new FwNote();
        try {
            /* Проверить что фрирайт для указанной даты существует,
               иначе вернуть пустую запись.
             */
            LocalDate d = FwDate.parse(datestr);
            FwDate w = fmap.get(d);
            if (w == null) {
                return res;
            }

            res.setText(Files.readString(Paths.get(w.getPath().getPath())));
            res.setDateStr(Freewriting.nameFormat(d));
            res.setTags(tagCat.getNoteTags(d));

        } catch (DateTimeParseException | IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    @GetMapping(value = "/tags")
    @ApiOperation(value = "Вернуть список имеющихся названий тэгов, возможно отфильтрованный по заданной строке.")
    public List<String> getFilteredTags(
            @ApiParam(value = "Фильтр на имена тэгов") @RequestParam(name = "f", defaultValue = "") String filterstr) {
        return tagCat.getFilteredTags(filterstr);
    }

    @GetMapping(value = "/loadNoteTags")
    @ApiOperation(value = "Загрузить маппинг \"фрирайт -> теги\" из Аппери.")
    public OpResult loadNoteTags() {
        return tagCat.loadNoteToTagsMapping();
    }

    @ApiOperation(value = "Изменить теги для фрирайта")
    @PostMapping(value = "/updateNoteTags")
    public OpResult updateNoteTags(
        @ApiParam(value = "Дата фрирайта в формате `yyyy-MM-dd`") @RequestParam(name = "d") String datestr,
        @ApiParam(value = "Список тэгов") @RequestBody List<String> newTags)
    {
        /* Проверить, что фрирайт существует.
         */
        LocalDate date = null;
        try {
            date = FwDate.parse(datestr);
        } catch (DateTimeParseException e) {
            return new OpResult(false, "Invalid date format");
        }
        if (fmap.get(date) == null) {
            return new OpResult(false, "Unknown note");
        }

        return tagCat.updateNoteTags(date, newTags);
    }

 }
