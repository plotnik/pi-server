package io.plotnik.piserver.freewriting;

import java.util.Set;

public class FwNote {

    String text;

    String dateStr;

    Set<FwTag> tags;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Set<FwTag> getTags() {
        return tags;
    }

    public void setTags(Set<FwTag> tags) {
        this.tags = tags;
    }

    public String getDateStr() {
        return dateStr;
    }

    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }

}
