package io.plotnik.piserver.freewriting;

import java.util.Set;

public class FwNote {

    String text;

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

}
