package io.plotnik.piserver.freewriting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.plotnik.piserver.common.OpResult;
import io.plotnik.piserver.freewriting.dao.FwDate;
import io.plotnik.piserver.freewriting.dao.FwNote;
import io.plotnik.piserver.freewriting.dao.FwTag;
import io.plotnik.piserver.freewriting.dao.UpdateTagsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;


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

    @Value("${freewriting.patterns}")
    private String patternsFolder;

    /**
     * База фрирайтов.
     */
    Freewriting fw;

    /**
     * База шаблонов поискаs.
     */
    SearchPatterns searchPatterns;

    /**
     * Маппинг дат на фрирайты.
     */
    Map<LocalDate, FwDate> fmap = new HashMap<>();

    @Autowired
    TagCat tagCat;

    @Autowired
    ServletContext servletContext;

    /**
     * Предзагрузка.
     */
    @PostConstruct
    public void init() {
        try {
            reloadNotes();
            reloadPatterns();
            reloadTags();

        } catch (Exception e) {
            if (e instanceof FwException) {
                log.warn("[FW Exception] " + e.getMessage());
            } else {
                e.printStackTrace();
            }
        }
    }

    @GetMapping(value = "/reloadNotes")
    @Operation(summary = "Перезагрузить фрирайты из папки.")
    public OpResult reloadFwNotes() {
        try {
            reloadNotes();
            reloadPatterns();
            reloadTags();
            
            return new OpResult(true, fw.getFWDates().size() + " notes loaded");

        } catch (Exception e) {
            return new OpResult(false, e.getMessage());
        }
    }

    void reloadNotes() throws FwException, IOException {
        /* Загрузить фрирайты
         */
        fw = new Freewriting(new File(homePath + fwPath));
        log.info("[SUCCESS] " + fw.getFWDates().size() + " notes loaded");

        /* Замапить даты во фрирайты для более быстрого доступа
         */
        for (FwDate w : fw.getFWDates()) {
            fmap.put(w.getDate(), w);
        }

        tagCat.loadTagDefinitions(homePath + tagsPath);
    }

    void reloadPatterns() throws FwException {
        searchPatterns = new SearchPatterns(homePath + patternsFolder, fw.fdates, servletContext.getRealPath("/WEB-INF/"));
    }

    @GetMapping()
    @Operation(summary = "Получить фрирайт по дате.")
    public FwNote getNote(
        @Parameter(description = "Дата фрирайта в формате `yyyy-MM-dd`") @RequestParam(name = "d", defaultValue = "") String datestr)
    {
        try {
            /* Проверить что фрирайт для указанной даты существует,
               иначе вернуть пустую запись.
             */
            LocalDate d = FwDate.parse(datestr);

            FwNote result = loadNoteByDate(d);
            if (result == null) {
                result = new FwNote();
            }
            return result;

        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return new FwNote();
        }

    }

    FwNote loadNoteByDate(LocalDate d) {
        FwDate w = fmap.get(d);
        if (w == null) {
            return null;
        }

        FwNote res = new FwNote();
        try {
            res.setText(Files.readString(Paths.get(w.getPath().getPath())));
            res.setDateStr(Freewriting.nameFormat(d));
            res.setTags(tagCat.getNoteTags(d));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    @GetMapping(value = "/{tag}")
    @Operation(summary = "Получить даты фрирайтов по тэгу.")
    public List<String> getNotesByTag(
        @Parameter(description = "Название тэга") @PathVariable String tag)
    {
        List<String> result = new ArrayList<>();
        List<LocalDate> dates = tagCat.getDatesByTag(tag);
        for (LocalDate d: dates) {
            result.add(FwDate.format(d));
        }
        return result;
    }

    @GetMapping(value = "/tags")
    @Operation(summary = "Вернуть список тэгов, возможно отфильтрованный по заданной строке.")
    public List<String> getFilteredTags(
            @Parameter(description = "Фильтр на имена тэгов") @RequestParam(name = "f", defaultValue = "") String filterstr) {
        return tagCat.getFilteredTags(filterstr);
    }

    @GetMapping(value = "/ctags")
    @Operation(summary = "Вернуть список тэгов, принадлежащих указанной категории")
    public List<FwTag> getTagsByCategory(
            @Parameter(description = "Категория тэгов") @RequestParam(name = "c", defaultValue = "") String cat) {
        return tagCat.getTagsByCategory(cat);
    }

    @GetMapping(value = "/cats")
    @Operation(summary = "Вернуть список категорий")
    public List<String> getCategories() {
        return tagCat.getCategories();
    }

    @GetMapping(value = "/reloadTags")
    @Operation(summary = "Перезагрузить соответствие между фрирайтами и тегами из Аппери.")
    public OpResult reloadTags() {
        try {
            return tagCat.loadNoteToTagsMapping();
            
        } catch (FwException e) {
            System.out.println("[ERROR] " + e.getMessage());
            return new OpResult(false);
        }
    }

    @Operation(summary = "Изменить теги для фрирайта")
    @PostMapping(value = "/updateNoteTags")
    public OpResult updateNoteTags(@RequestBody UpdateTagsRequest req)
    {
        /* Проверить, что фрирайт существует.
         */
        LocalDate date = null;
        try {
            date = FwDate.parse(req.getD());
        } catch (DateTimeParseException e) {
            return new OpResult(false, "Invalid date format");
        }
        if (fmap.get(date) == null) {
            return new OpResult(false, "Unknown note");
        }

        return tagCat.updateNoteTags(date, req.getNewTags());
    }

    @GetMapping(value = "/patterns")
    @Operation(summary = "Вернуть список шаблонов поиска")
    public List<String> getPatterns() {
        return searchPatterns.getPatterns();
    }

    @Operation(summary = "Найти фрирайты соответствующие шаблону поиска")
    @GetMapping(value = "/findPattern")
    public List<String> findPattern(
        @Parameter(description = "Шаблон поиска") @RequestParam(name = "p") String pattern) 
    {
        return searchPatterns.findPattern(pattern);
    }
 }
