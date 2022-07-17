package io.plotnik.piserver.freewriting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.plotnik.piserver.common.ApperyClient;
import io.plotnik.piserver.common.OpResult;
import io.plotnik.piserver.freewriting.dao.ApperyTag;
import io.plotnik.piserver.freewriting.dao.FwDate;
import io.plotnik.piserver.freewriting.dao.FwNote;
import io.plotnik.piserver.freewriting.dao.FwTag;

/**
 * Работа с тэгами.
 */
@Service
public class TagCat {

    @Autowired
    ApperyClient apperyClient;

    ObjectMapper om = new ObjectMapper();

    /**
     * Маппинг тэга на URL.
     */
    private Map<String, String> tagToUrl = new HashMap<>();

    /**
     * Маппинг фрирайта на хэштег.
     */
    private Map<LocalDate, Set<String>> noteToTags = new HashMap<>();

    /**
     * Маппинг тэга на категории.
     */
    private Map<String, Set<String>> tagToCats = new HashMap<>();

    /**
     * Маппинг категории на тэги.
     */
    private Map<String, Set<String>> catToTags = new HashMap<>();

    /**
     * Вернуть список имеющихся названий тэгов,
     * возможно отфильтрованный по заданной строке.
     */
    public List<String> getFilteredTags(String filterstr) {
        Stream<String> stm = tagToUrl.keySet().stream();
        if (filterstr.length() > 0) {
            String lostr = filterstr.toLowerCase();
            stm = stm.filter(s -> s.toLowerCase().contains(lostr));
        }
        return stm.sorted().collect(Collectors.toList());
    }

    /**
     * Установить тэги в маппинге.
     */
    public OpResult updateNoteTags(LocalDate date, List<String> newTags) {
        Set<String> tagList = new HashSet<>();
        for (String t : newTags) {
            String url = tagToUrl.get(t);
            if (url == null) {
                return new OpResult(false, "Unknown tag " + t);
            }
            tagList.add(t);
        }
        noteToTags.put(date, tagList);
        return apperyClient.updateTagList(FwDate.format(date), tagList);
    }

    /**
     * Загрузить связки между фрирайтами и тегами из Аппери.
     */
    public OpResult loadNoteToTagsMapping() throws FwException {
        try {
            ApperyTag[] apperyTags = apperyClient.loadNoteTags();
            if (apperyTags == null) {
                throw new FwException("[WARNING] Tags not found in Appery.io");
            }

            for (ApperyTag atag : apperyTags) {

                /* Распарсить дату.
                */
                LocalDate date = FwDate.parse(atag.getDstamp());

                /* Добавить хэштег в маппинг.
                */
                Set<String> tagList = noteToTags.get(date);
                if (tagList == null) {
                    tagList = new HashSet<>();
                    noteToTags.put(date, tagList);
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

    /**
     * Вернуть тэги для указанного фрирайта.
     */
    public Set<FwTag> getNoteTags(LocalDate d) {
        Set<String> dd = noteToTags.get(d);
        Set<FwTag> tt = null;
        if (dd != null) {
            /* Пройти по списку тэгов для фрирайта.
             */
            tt = dd.stream()
                .map(name -> {
                    return new FwTag(name, tagToUrl.get(name));
                })
                .collect(Collectors.toSet());
        }
        return tt;
    }

    /**
     * Загрузить тэги
     */
    public void loadTagDefinitions(String tagFolderPath) throws IOException, FwException {
        File[] tagFiles = new File(tagFolderPath).listFiles();
        for (File tf : tagFiles) {
            if (tf.isFile() && tf.getName().endsWith(".md")) {
                /* Получить название тэга из имени файла.
                 */
                String tag = tf.getName().substring(0, tf.getName().length() - 3);

                String url = null;
                List<String> lines = Files.readAllLines(Paths.get(tf.getPath()));
                for (String line: lines) {
                    String ln = line.trim();
                    if (ln.length() > 0) {
                        if (ln.startsWith("#")) {
                            String cat = ln.substring(1).trim();
                            addTagCategory(tag, cat);
                        } else {
                            url = ln;
                        }
                    }
                }
                if (url == null) {
                    throw new FwException("URL not found in tag: " + tag);
                }

                tagToUrl.put(tag, url);
            }
        }
    }

    /**
     * Мы поддерживаем двустороннюю связку между тзгами и категориями
     */
    private void addTagCategory(String tag, String cat) {
        Set<String> cats = tagToCats.get(tag);
        if (cats == null) {
            cats = new HashSet<>();
            tagToCats.put(tag, cats);
        }
        cats.add(cat);

        Set<String> tags = catToTags.get(cat);
        if (tags == null) {
            tags = new HashSet<>();
            catToTags.put(cat, tags);
        }
        tags.add(tag);
    }

    /**
     * Вернуть список тэгов с линками, принадлежащих указанной категории
     */
    public List<FwTag> getTagsByCategory(String cat) {
        Set<String> tags = catToTags.get(cat);
        if (tags != null) {
            return tags.stream().map(name -> {
                return new FwTag(name, tagToUrl.get(name));
            })
            .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Вернуть список категорий, отсортированный по алфавиту
     */
    public List<String> getCategories() {
        //System.out.println("catToTags: " + catToTags.size());
        //System.out.println("tagToCats: " + tagToCats.size());
        List<String> cats = new ArrayList<>(catToTags.keySet());
        Collections.sort(cats);
        return cats;
    }

    /**
     * Получить даты фрирайтов по тэгу
     */
    public List<LocalDate> getDatesByTag(String tag) {
        ArrayList<LocalDate> result = new ArrayList<>();
        noteToTags.entrySet().stream().forEach(entry -> {
            if (entry.getValue().contains(tag)) {
                result.add(entry.getKey());
            }
        });
        return result;
    }

}
