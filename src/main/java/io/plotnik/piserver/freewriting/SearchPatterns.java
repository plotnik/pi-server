package io.plotnik.piserver.freewriting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.plotnik.piserver.freewriting.dao.FwDate;
import io.plotnik.piserver.freewriting.dao.SearchPattern;

public class SearchPatterns {

    String home;

    List<FwDate> fdates;

    ObjectMapper om = new ObjectMapper();

    Map<String, SearchPattern> patterns = new HashMap<>();

    // Сортировка поисковых паттернов
    Map<String, Long> sortingOrder = new HashMap<>();

    String sortingOrderFile = "fw-patterns-sort.json";

    public final static DateTimeFormatter ymdFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    List<String> cachedResult;

    String cachedTitle;

    public SearchPatterns(String home, List<FwDate> fdates) throws FwException {
        this.home = home;
        this.fdates = fdates;
        loadPatterns();
        loadSortingOrder();
    }

    /** 
     * Прочитать определения поисковых паттернов
     */
    void loadPatterns() throws FwException {

        File pdir = new File(home);
        if (!pdir.exists()) {
            throw new FwException("Folder not found: " + pdir.getPath());
        }
        
        File[] pfiles = pdir.listFiles();
        int count = 0;
        for (File it : pfiles) {
            if (it.getName().endsWith(".json")) {
                try {
                    String text = Files.readString(Paths.get(it.getPath()));
                    SearchPattern p = om.readValue(text, SearchPattern.class);

                    p.setFname(it.getName().substring(0, it.getName().length() - 5));
                    patterns.put(p.getTitle(), p);
                    count++;

                } catch (JsonProcessingException ej) {
                    ej.printStackTrace();
                } catch (IOException ei) {
                    ei.printStackTrace();
                }
            }
        }
    }

    // Прочитать сортировку поисковых паттернов
    void loadSortingOrder() {
        String targetFolderPath = System.getProperty("user.home") + "/.plotnik";
        File orderFile = new File(targetFolderPath, sortingOrderFile);
        if (orderFile.exists()) {
            try {
                String text = Files.readString(Paths.get(orderFile.getPath()));
                sortingOrder = om.readValue(text, new TypeReference<Map<String, Long>>() {
                });

            } catch (JsonProcessingException ej) {
                ej.printStackTrace();
            } catch (IOException ei) {
                ei.printStackTrace();
            }
        }
    }

    void updateSortingOrder(String pattern) {
        try {
            sortingOrder.put(pattern, System.currentTimeMillis());
            String targetFolderPath = System.getProperty("user.home") + "/.plotnik";
            File orderFile = new File(targetFolderPath, sortingOrderFile);
            om.writeValue(orderFile, sortingOrder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getPatterns() {
        List<String> keys = new ArrayList<>(patterns.keySet());
        keys.sort((s1, s2) -> {
            long k1 = sortingOrder.get(s1)==null? 0: sortingOrder.get(s1);
            long k2 = sortingOrder.get(s2)==null? 0: sortingOrder.get(s2);
            return (int)(k2 - k1);
        });
        return keys;
    }

    public List<String> findPattern(String title) {
        if (title.equals(cachedTitle)) {
            return cachedResult;
        }

        SearchPattern pattern = patterns.get(title);
        if (pattern == null) {
            return new ArrayList<String>();
        }

        updateSortingOrder(pattern.getTitle());

        Set<LocalDate> dates = new HashSet<>();
        if (pattern.getS() != null) {
            dates.addAll(extractPages(pattern.getS()));
        }

        if (pattern.getI() != null) {
            dates.addAll(extractIntervals(pattern.getI()));
        }

        List<LocalDate> dlist = new ArrayList<>(dates);
        dlist.sort((d1, d2) -> -d1.compareTo(d2));
        List<String> result = dlist.stream().map(it -> it.format(ymdFormat)).collect(Collectors.toList());

        cachedResult = result;
        cachedTitle = title;
        return result;
    }

    /**
     * Собрать в результирующий файл фрирайты, содержащие указанные шаблоны.
     * @param patterns Список ключевых слов
     */
    List<LocalDate> extractPages(List<String> patterns) {
        List<LocalDate> result = new ArrayList<>();
        try {
            /* Пройти по всем файлам фрирайтов
             */
            for (FwDate fdate: fdates) {

                /* Поискать паттерны в строчках файла
                 */
                File t = fdate.getPath();
                String text = Files.readString(Paths.get(t.getPath()));
                String[] sents = text.split("\\.");
                
                boolean found = false;
                for (int i=0; i<sents.length; i++) {
                    String sent = sents[i].toLowerCase();
                    if (contains(sent, patterns)) {
                        found = true;
                    }
                }

                /* Если что-то найдено хотя бы один раз, добавить фрирайт в результаты поиска.
                 */
                if (found) {
                    result.add(fdate.getDate());
                }    
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    List<LocalDate> extractIntervals(List<List<String>> intervals) {
        List<LocalDate> result = new ArrayList<>();
        for (List<String> iv : intervals) {
            LocalDate dt1 = LocalDate.parse(iv.get(0), ymdFormat);
            LocalDate dt2 = LocalDate.parse(iv.get(1), ymdFormat);

            for (LocalDate d = dt1; d.isBefore(dt2) || d.isEqual(dt2); d = d.plusDays(1)) {
                final LocalDate date = d;
                List<FwDate> found = fdates.stream().filter(it -> it.getDate().equals(date)).collect(Collectors.toList());
                if (found.size() > 0) {
                    result.add(found.get(0).getDate());
                }
            }
        }
        return result;
    }

    /**
     * Указанная строка содержит хотя бы один из списка указанных шаблонов.
     */
    boolean contains(String s, List<String> patterns) {
        boolean result = false;
        for (String it: patterns) {
            if (s.contains(it)) {
                result = true;
            }
        }
        return result;
    }
}
