package io.plotnik.piserver.freewriting.dao;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Ссылка на файл фрирайта.
 */
public class FwDate {

    private static DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    LocalDate date;
    File path;
    boolean root;

    public FwDate(LocalDate date, File path, boolean root) {
        this.date = date;
        this.path = path;
        this.root = root;
    }

    public LocalDate getDate() {
        return date;
    }

    public File getPath() {
        return path;
    }

    public boolean isRoot() {
        return root;
    }

    public static LocalDate parse(String datestr) throws DateTimeParseException {
        return LocalDate.parse(datestr, df);
    }

    public static String format(LocalDate date) {
        return df.format(date);
    }
}
