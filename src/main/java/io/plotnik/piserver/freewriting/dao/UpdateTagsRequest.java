package io.plotnik.piserver.freewriting.dao;

import java.util.List;

public class UpdateTagsRequest {

    // Дата фрирайта в формате `yyyy-MM-dd`
    private String d;

    // Список тэгов
    private List<String> newTags;

    public String getD() {
        return d;
    }

    public void setD(String d) {
        this.d = d;
    }

    public List<String> getNewTags() {
        return newTags;
    }

    public void setNewTags(List<String> newTags) {
        this.newTags = newTags;
    }

}
