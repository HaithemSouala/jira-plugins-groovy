package ru.mail.jira.plugins.groovy.api.entity;

import net.java.ao.OneToMany;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;

public interface Listener extends AbstractScript {
    @NotNull
    String getUuid();
    void setUuid(String uuid);

    @NotNull
    @StringLength(StringLength.UNLIMITED)
    String getScriptBody();
    void setScriptBody(String scriptBody);

    @NotNull
    @StringLength(StringLength.UNLIMITED)
    String getCondition();
    void setCondition(String condition);

    Boolean isAlwaysTrack();
    void setAlwaysTrack(Boolean alwaysTrack);

    @OneToMany(reverse = "getListener")
    ListenerChangelog[] getChangelogs();
}
