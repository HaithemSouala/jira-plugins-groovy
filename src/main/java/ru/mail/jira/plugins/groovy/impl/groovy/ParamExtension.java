package ru.mail.jira.plugins.groovy.impl.groovy;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import ru.mail.jira.plugins.groovy.api.dto.ScriptParamDto;
import ru.mail.jira.plugins.groovy.api.script.ParamType;
import ru.mail.jira.plugins.groovy.api.script.ParseContext;
import ru.mail.jira.plugins.groovy.api.script.WithParam;

public class ParamExtension extends CompilationCustomizer {
    private final ParseContextHolder parseContextHolder;

    public ParamExtension(ParseContextHolder parseContextHolder) {
        super(CompilePhase.CANONICALIZATION);

        this.parseContextHolder = parseContextHolder;
    }

    @Override
    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        ParseContext parseContext = parseContextHolder.get();
        if (parseContext.getCompletedExtensions().contains(ParamExtension.class)) {
            return;
        }

        for (Statement statement : source.getAST().getStatementBlock().getStatements()) {
            if (statement instanceof ExpressionStatement) {
                ExpressionStatement castedStatement = (ExpressionStatement) statement;
                if (castedStatement.getExpression() instanceof DeclarationExpression) {
                    DeclarationExpression expression = (DeclarationExpression) castedStatement.getExpression();

                    if (expression.getLeftExpression() instanceof VariableExpression) {
                        VariableExpression leftExpression = (VariableExpression) expression.getLeftExpression();

                        ExpressionStatement statement1 = (ExpressionStatement) statement;
                        for (AnnotationNode annotationNode : statement1.getExpression().getAnnotations()) {
                            if (annotationNode.getClassNode().getTypeClass().equals(WithParam.class)) {
                                String varName = leftExpression.getName();
                                String displayName = null;
                                ParamType type = null;
                                Boolean optional;

                                Expression displayNameExpression = annotationNode.getMember("displayName");
                                if (displayNameExpression instanceof ConstantExpression) {
                                    displayName = (String) ((ConstantExpression) displayNameExpression).getValue();
                                }

                                Expression typeExpression = annotationNode.getMember("type");
                                if (typeExpression instanceof PropertyExpression) {
                                    Expression property = ((PropertyExpression) typeExpression).getProperty();
                                    if (property instanceof ConstantExpression) {
                                        type = ParamType.valueOf((String) ((ConstantExpression) property).getValue());
                                    }
                                }

                                Expression optionalExpression = annotationNode.getMember("optional");
                                if (optionalExpression instanceof ConstantExpression) {
                                    optional = (Boolean) ((ConstantExpression) optionalExpression).getValue();
                                } else {
                                    optional = false;
                                }

                                if (type == null) {
                                    throw new IllegalArgumentException("type must be present");
                                }

                                if (!type.getType().equals(leftExpression.getType().getTypeClass())) {
                                    throw new IllegalArgumentException(type.name() + " must be declared with class " + type.getType());
                                }

                                if (parseContext.isExtended()) {
                                    parseContext.getParameters().add(new ScriptParamDto(varName, displayName, type, optional));
                                }
                                expression.setRightExpression(new VariableExpression(varName));

                                break;
                            }
                        }
                    }
                }
            }
        }

        parseContext.getCompletedExtensions().add(ParamExtension.class);
    }
}