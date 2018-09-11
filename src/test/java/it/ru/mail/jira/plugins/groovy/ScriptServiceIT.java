package it.ru.mail.jira.plugins.groovy;

import com.adaptavist.shrinkwrap.atlassian.plugin.api.AtlassianPluginArchive;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.ImmutableMap;
import org.jboss.arquillian.container.test.api.BeforeDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.mail.jira.plugins.groovy.api.script.ScriptType;
import ru.mail.jira.plugins.groovy.api.service.ScriptService;
import ru.mail.jira.plugins.groovy.impl.FileUtil;

import javax.inject.Inject;

import java.nio.file.Paths;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class ScriptServiceIT {
    @ComponentImport
    @Inject
    private ScriptService scriptService;

    @BeforeDeployment
    public static Archive<?> useSpringScannerOne(Archive<?> archive) {
        return archive
            .as(AtlassianPluginArchive.class)
            .addClass(FileUtil.class)
            .addAsResource(Paths.get("src/examples/groovy/tests/jsonSlurper.groovy").toFile(), "tests/jsonSlurper.groovy")
            .withSpringScannerOne(false);
    }

    @Test
    public void basicScriptShouldRun() throws Exception {
        Object result = scriptService.executeScript(null, "return 'test'", ScriptType.CONSOLE, ImmutableMap.of());

        assertEquals(result, "test");
    }

    @Test
    public void jsonShouldBeParsed() throws Exception {
        String script = FileUtil.readArquillianExample("tests/jsonSlurper");

        Object result = scriptService.executeScript(null, script, ScriptType.CONSOLE, ImmutableMap.of());

        assertEquals(result, ImmutableMap.of("test", "value"));
    }
}
