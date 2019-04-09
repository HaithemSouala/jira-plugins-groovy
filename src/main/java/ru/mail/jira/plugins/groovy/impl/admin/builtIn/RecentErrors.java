package ru.mail.jira.plugins.groovy.impl.admin.builtIn;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.pocketknife.api.querydsl.DatabaseAccessor;
import com.atlassian.pocketknife.api.querydsl.schema.DialectProvider;
import com.atlassian.pocketknife.api.querydsl.util.OnRollback;
import com.google.common.collect.ImmutableList;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.groovy.api.dto.ScriptParamDto;
import ru.mail.jira.plugins.groovy.api.dto.directory.ScriptDirectoryDto;
import ru.mail.jira.plugins.groovy.api.dto.execution.ScriptExecutionSummary;
import ru.mail.jira.plugins.groovy.api.entity.EntityType;
import ru.mail.jira.plugins.groovy.api.repository.ScriptRepository;
import ru.mail.jira.plugins.groovy.api.service.admin.BuiltInScript;
import ru.mail.jira.plugins.groovy.impl.repository.querydsl.QAbstractScript;
import ru.mail.jira.plugins.groovy.util.CustomFieldHelper;
import ru.mail.jira.plugins.groovy.util.QueryDslTables;
import ru.mail.jira.plugins.groovy.util.ScriptUtil;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RecentErrors implements BuiltInScript<List<ScriptExecutionSummary>> {
    private final ApplicationProperties applicationProperties;
    private final DateTimeFormatter dateTimeFormatter;
    private final DatabaseAccessor databaseAccessor;
    private final CustomFieldHelper customFieldHelper;
    private final ScriptRepository scriptRepository;

    @Autowired
    public RecentErrors(
        @ComponentImport ApplicationProperties applicationProperties,
        @ComponentImport DateTimeFormatter dateTimeFormatter,
        DatabaseAccessor databaseAccessor,
        CustomFieldHelper customFieldHelper,
        ScriptRepository scriptRepository
    ) {
        this.applicationProperties = applicationProperties;
        this.dateTimeFormatter = dateTimeFormatter;
        this.databaseAccessor = databaseAccessor;
        this.customFieldHelper = customFieldHelper;
        this.scriptRepository = scriptRepository;
    }

    @Override
    public List<ScriptExecutionSummary> run(ApplicationUser currentUser, Map<String, Object> params) throws Exception {
        String baseUrl = applicationProperties.getString(APKeys.JIRA_BASEURL);

        NumberPath<Integer> idPath = Expressions.numberPath(Integer.class, "ENTITY_ID");
        StringPath uuidPath = Expressions.stringPath("UUID");
        StringPath entityTypePath = Expressions.stringPath("ENTITY_TYPE");
        StringPath namePath = Expressions.stringPath("NAME");
        NumberExpression<Long> errorCountPath = QueryDslTables.SCRIPT_EXECUTION.count().as("errorCount");
        DateTimeExpression<Timestamp> lastErrorDatePath = QueryDslTables.SCRIPT_EXECUTION.DATE.max().as("lastErrorDate");
        NumberPath<Integer> directoryIdPath = Expressions.numberPath(Integer.class, "DIRECTORY_ID");

        Map<Integer, ScriptDirectoryDto> directories = scriptRepository
            .getAllDirectories()
            .stream()
            .collect(Collectors.toMap(
                ScriptDirectoryDto::getId,
                Function.identity()
            ));

        return databaseAccessor.run(connection -> {
            SQLQueryFactory queryFactory = connection.query();

            Expression<Tuple> union = SQLExpressions.unionAll(
                getAbstractScriptQuery(QueryDslTables.REGISTRY_SCRIPT, EntityType.REGISTRY_SCRIPT),
                getAbstractScriptQuery(QueryDslTables.JQL_FUNCTION, EntityType.JQL_FUNCTION),
                getAbstractScriptQuery(QueryDslTables.LISTENER, EntityType.LISTENER),
                getAbstractScriptQuery(QueryDslTables.REST, EntityType.REST),
                getAbstractScriptQuery(QueryDslTables.SCHEDULED_TASK, EntityType.SCHEDULED_TASK),
                SQLExpressions
                    .select(
                        QueryDslTables.FIELD_CONFIG.FIELD_CONFIG_ID.as("ENTITY_ID"),
                        Expressions.constant(""),
                        QueryDslTables.FIELD_CONFIG.UUID,
                        Expressions.constant(EntityType.CUSTOM_FIELD.name()),
                        Expressions.nullExpression()
                    )
                    .from(QueryDslTables.FIELD_CONFIG)
            ).as("union");

            SQLQuery<Tuple> query = queryFactory
                .select(
                    QueryDslTables.SCRIPT_EXECUTION.SCRIPT_ID,
                    QueryDslTables.SCRIPT_EXECUTION.INLINE_ID,
                    //mins are a hacky way to get these fields without specifying them in group by
                    idPath.min(),
                    namePath.min(),
                    entityTypePath.min(),
                    directoryIdPath.min(),
                    errorCountPath,
                    lastErrorDatePath
                )
                .from(union)
                .rightJoin(QueryDslTables.SCRIPT_EXECUTION)
                .on(
                    uuidPath.eq(QueryDslTables.SCRIPT_EXECUTION.INLINE_ID)
                        .orAllOf(
                            idPath.eq(QueryDslTables.SCRIPT_EXECUTION.SCRIPT_ID),
                            QueryDslTables.SCRIPT_EXECUTION.INLINE_ID.isNull()
                        )
                )
                .where(QueryDslTables.SCRIPT_EXECUTION.SUCCESSFUL.isFalse())
                .groupBy(QueryDslTables.SCRIPT_EXECUTION.SCRIPT_ID, QueryDslTables.SCRIPT_EXECUTION.INLINE_ID);

            if (connection.getDialectConfig().getDatabaseInfo().getSupportedDatabase() == DialectProvider.SupportedDatabase.MYSQL) {
                query = query.orderBy(new OrderSpecifier<>(Order.ASC, Expressions.nullExpression()));
            }

            //work around for h2
            query.setUseLiterals(true);

            return query
                .fetch()
                .stream()
                .map(row -> {
                    ScriptExecutionSummary result = new ScriptExecutionSummary();

                    result.setErrorCount(row.get(errorCountPath));

                    Timestamp lastErrorDate = row.get(lastErrorDatePath);
                    if (lastErrorDate != null) {
                        result.setLastErrorDate(dateTimeFormatter.withStyle(DateTimeStyle.COMPLETE).format(lastErrorDate));
                        result.setLastErrorTimestamp(lastErrorDate.getTime());
                    }

                    String entityTypeName = row.get(4, String.class);
                    if (entityTypeName != null) {
                        EntityType entityType = EntityType.valueOf(entityTypeName);

                        Integer entityId = row.get(2, Integer.class);

                        if (entityId != null) {
                            result.setUrl(baseUrl + ScriptUtil.getPermalink(entityType, entityId));
                            result.setType(entityType);

                            if (entityType == EntityType.REGISTRY_SCRIPT) {
                                String name = row.get(3, String.class);
                                Integer directoryId = row.get(5, Integer.class);

                                result.setName(ScriptUtil.getExpandedName(directories, directories.get(directoryId)) +  "/" + name);
                            } else if (entityType != EntityType.CUSTOM_FIELD) {
                                result.setName(row.get(3, String.class));
                            } else {
                                String fieldName = customFieldHelper.getFieldName((long) entityId);
                                if (fieldName != null) {
                                    result.setName(fieldName);
                                } else {
                                    result.setName("[unknown field]");
                                }
                            }
                        }
                    } else {
                        result.setName(row.get(QueryDslTables.SCRIPT_EXECUTION.INLINE_ID));
                    }

                    return result;
                })
                .collect(Collectors.toList());
        }, OnRollback.NOOP);
    }

    private SQLQuery<Tuple> getAbstractScriptQuery(QAbstractScript abstractScript, EntityType entityType) {
        NumberPath<Integer> directoryIdPath = Expressions.numberPath(Integer.class, "DIRECTORY_ID");

        return SQLExpressions
            .select(
                abstractScript.ID.as("ENTITY_ID"),
                abstractScript.NAME.as("NAME"),
                abstractScript.UUID.as("UUID"),
                Expressions.asString(Expressions.constantAs(entityType.name(), Expressions.stringPath("ENTITY_TYPE"))),
                entityType == EntityType.REGISTRY_SCRIPT ? directoryIdPath : Expressions.nullExpression()
            )
            .from(abstractScript);
    }

    @Override
    public String getKey() {
        return "recentErrors";
    }

    @Override
    public String getI18nKey() {
        return "ru.mail.jira.plugins.groovy.adminScripts.builtIn.recentErrors";
    }

    @Override
    public String getResultWidth() {
        return "large";
    }

    @Override
    public boolean isHtml() {
        return false;
    }

    @Override
    public List<ScriptParamDto> getParams() {
        return ImmutableList.of();
    }
}
