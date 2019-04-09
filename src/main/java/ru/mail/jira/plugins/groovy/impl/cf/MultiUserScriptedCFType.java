package ru.mail.jira.plugins.groovy.impl.cf;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.UserField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.rest.FieldJsonRepresentation;
import com.atlassian.jira.issue.fields.rest.json.JsonData;
import com.atlassian.jira.issue.fields.rest.json.JsonType;
import com.atlassian.jira.issue.fields.rest.json.JsonTypeBuilder;
import com.atlassian.jira.issue.fields.rest.json.UserBeanFactory;
import com.atlassian.jira.notification.type.UserCFNotificationTypeAware;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

//todo: maybe extend MultiUserCFType, so it can be used in permission schemes
@Scanned
public class MultiUserScriptedCFType extends ScriptedCFType<List, ApplicationUser> implements UserField, UserCFNotificationTypeAware {
    private final UserBeanFactory userBeanFactory;
    private final JiraAuthenticationContext authenticationContext;

    public MultiUserScriptedCFType(
        @ComponentImport UserBeanFactory userBeanFactory,
        @ComponentImport JiraAuthenticationContext authenticationContext,
        FieldValueExtractor valueExtractor
    ) {
        super(valueExtractor, List.class);
        this.userBeanFactory = userBeanFactory;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public JsonType getJsonSchema(CustomField customField) {
        return JsonTypeBuilder.customArray(JsonType.USER_TYPE, this.getKey(), customField.getIdAsLong());
    }

    @Override
    public FieldJsonRepresentation getJsonFromIssue(CustomField field, Issue issue, boolean b, @Nullable FieldLayoutItem fieldLayoutItem) {
        return new FieldJsonRepresentation(
            new JsonData(
                this.userBeanFactory.createBeanCollection(
                    this.getValueFromIssue(field, issue),
                    this.authenticationContext.getLoggedInUser()
                )
            )
        );
    }

    @Override
    public void fillStaticVelocityParams(Map<String, Object> params) {}

    @Override
    public void fillDynamicVelocityParams(Map<String, Object> params, Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {}
}
