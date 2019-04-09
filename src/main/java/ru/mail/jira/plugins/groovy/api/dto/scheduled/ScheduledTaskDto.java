package ru.mail.jira.plugins.groovy.api.dto.scheduled;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.groovy.api.dto.ChangelogDto;
import ru.mail.jira.plugins.groovy.api.dto.PickerOption;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Getter @Setter
@XmlRootElement
public class ScheduledTaskDto extends ScheduledTaskForm {
    @XmlElement
    private int id;
    @XmlElement
    private String uuid;
    @XmlElement
    private PickerOption user;
    @XmlElement
    private PickerOption issueWorkflow;
    @XmlElement
    private PickerOption issueWorkflowAction;
    @XmlElement
    private boolean enabled;
    @XmlElement
    private RunInfo lastRunInfo;
    @XmlElement
    private String nextRunDate;
    @XmlElement
    private List<ChangelogDto> changelogs;
}
