package io.plotnik.piserver.freewriting.dao;

import java.util.List;

public class SearchPattern {

    String title;

    List<String> s;

    List<List<String>> i;

    String fname;

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getS() {
        return s;
    }

    public void setS(List<String> s) {
        this.s = s;
    }

    public List<List<String>> getI() {
        return i;
    }

    public void setI(List<List<String>> i) {
        this.i = i;
    }
}
