package ru.mail.jira.plugins.groovy.impl.cf;

import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.DateField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.rest.FieldJsonRepresentation;
import com.atlassian.jira.issue.fields.rest.json.JsonData;
import com.atlassian.jira.issue.fields.rest.json.JsonType;
import com.atlassian.jira.issue.fields.rest.json.JsonTypeBuilder;
import com.atlassian.jira.rest.Dates;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

@Scanned
public class DateTimeScriptedCFType extends ScriptedCFType<Date, Date> implements DateField {
    private final DateTimeFormatterFactory dateTimeFormatterFactory;
    private final DateTimeFormatter datePickerFormatter;

    protected DateTimeScriptedCFType(
        @ComponentImport DateTimeFormatterFactory dateTimeFormatterFactory,
        FieldValueExtractor valueExtractor
    ) {
        super(valueExtractor, Date.class);
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
        this.datePickerFormatter = dateTimeFormatterFactory.formatter().forLoggedInUser().withStyle(DateTimeStyle.DATE_TIME_PICKER);
    }

    @Override
    public JsonType getJsonSchema(CustomField customField) {
        return JsonTypeBuilder.custom(JsonType.DATETIME_TYPE, this.getKey(), customField.getIdAsLong());
    }

    @Override
    public FieldJsonRepresentation getJsonFromIssue(CustomField field, Issue issue, boolean renderedVersionRequested, @Nullable FieldLayoutItem fieldLayoutItem) {
        Date date = this.getValueFromIssue(field, issue);
        if (date == null) {
            return new FieldJsonRepresentation(new JsonData(null));
        } else {
            FieldJsonRepresentation pair = new FieldJsonRepresentation(new JsonData(Dates.asTimeString(date)));
            if (renderedVersionRequested) {
                pair.setRenderedData(new JsonData(dateTimeFormatterFactory.formatter().forLoggedInUser().format(date)));// 234
            }

            return pair;
        }
    }

    @Override
    public void fillStaticVelocityParams(Map<String, Object> params) {
        params.put("dateTimePicker", Boolean.TRUE);
        params.put("datePickerFormatter", this.datePickerFormatter);
        params.put("titleFormatter", this.datePickerFormatter.withStyle(DateTimeStyle.COMPLETE));
        params.put("iso8601Formatter", this.datePickerFormatter.withStyle(DateTimeStyle.ISO_8601_DATE_TIME));
    }

    @Override
    public void fillDynamicVelocityParams(Map<String, Object> params, Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {}
}
