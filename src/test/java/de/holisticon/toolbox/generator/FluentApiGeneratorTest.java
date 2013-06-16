package de.holisticon.toolbox.generator;

import static com.google.common.collect.Collections2.filter;
import static de.holisticon.toolbox.generator.FluentApiGenerator.DEFAULT_TARGET_DIRECTORY;
import static de.holisticon.toolbox.generator.FluentApiGenerator.fluentApiGenerator;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.vaadin.ui.TextField;

import de.holisticon.toolbox.generator.predicate.MethodPredicates;

public class FluentApiGeneratorTest {

    private static final String PATH = DEFAULT_TARGET_DIRECTORY + "/d/h/FluentTextField.java";

    private final JavaCompiler systemJavaCompiler = ToolProvider.getSystemJavaCompiler();
    private final Logger logger = LoggerFactory.getLogger(FluentApiGeneratorTest.class);

    @Test
    public void shouldGenerateSimpleJavaFile() throws IOException {

        fluentApiGenerator().rootPackage("d.h").addIgnoredMethodNames("setId", "setParent", "setCurrentBufferedSourceException").build()
                .addClass(TextField.class).generateCode();

        System.out.println(Joiner.on("\n").join(Files.readLines(new File(PATH), Charsets.UTF_8)));

        assertTrue(systemJavaCompiler.run(null, null, null, PATH) < 1);
    }

    @Test
    @Ignore
    public void shouldReturnMethodsOfInterest() throws Exception {
        final Set<Method> methods = Sets.newHashSet(TextField.class.getMethods());
        final Collection<Method> filter = filter(methods, MethodPredicates.IS_FLUENT_CANDIDATE);
        final Set<String> sorted = Sets.newTreeSet();
        for (final Method m : filter) {
            sorted.add(format("%s(%s)", m.getName(), m.getGenericParameterTypes()[0]));
        }
        System.out.println(Joiner.on("\n").join(sorted));
        assertThat(filter.size(), is(20));

    }

}
