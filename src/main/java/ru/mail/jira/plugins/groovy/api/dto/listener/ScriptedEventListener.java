package ru.mail.jira.plugins.groovy.api.dto.listener;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScriptedEventListener {
    private final int id;
    private final String script;
    private final String uuid;
    private final boolean alwaysTrack;
    private final ConditionDescriptor condition;
}
