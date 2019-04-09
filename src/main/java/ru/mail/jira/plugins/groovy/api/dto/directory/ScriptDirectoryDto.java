package ru.mail.jira.plugins.groovy.api.dto.directory;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Getter @Setter
@XmlRootElement
public class ScriptDirectoryDto extends ScriptDirectoryForm {
    @XmlElement
    private Integer id;
    @XmlElement
    private String fullName;
    @XmlElement
    private String parentName;
}
