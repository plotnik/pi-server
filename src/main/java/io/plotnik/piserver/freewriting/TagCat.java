package io.plotnik.piserver.freewriting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
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

import io.plotnik.piserver.common.ApperyClient;
import io.plotnik.piserver.common.OpResult;

/**
 * Работа с тэгами.
 */
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
    public OpResult loadNoteToTagsMapping() {
        try {
            ApperyTag[] apperyTags = apperyClient.loadNoteTags();

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
     * Веррнуть тэги для указанного фрирайта.
     */
    public Set<FwTag> getNoteTags(LocalDate d) {
        Set<String> dd = noteToTags.get(d);
        Set<FwTag> tt = null;
        if (dd != null) {
            tt = dd.stream()
                .map(name -> new FwTag(name, tagToUrl.get(name)))
                .collect(Collectors.toSet());
        }
        return tt;
    }

    /**
     * Загрузить тэги
     */
    public void loadTagDefinitions(String tagFolderPath) throws IOException {
        File[] tagFiles = new File(tagFolderPath).listFiles();
        for (File tf : tagFiles) {
            if (tf.isFile() && tf.getName().endsWith(".md")) {
                String url = Files.readString(Paths.get(tf.getPath()));
                String tag = tf.getName().substring(0, tf.getName().length() - 3);
                tagToUrl.put(tag, url);
            }
        }
    }
}
