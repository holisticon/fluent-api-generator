package de.holisticon.toolbox.generator;

import static de.holisticon.toolbox.generator.FluentApiGenerator.DEFAULT_TARGET_DIRECTORY;
import static de.holisticon.toolbox.generator.FluentApiGenerator.fluentApiGenerator;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.vaadin.ui.TextField;

public class FluentApiGeneratorTest {

    @Test
    public void shouldGenerateSimpleJavaFile() throws IOException {

        fluentApiGenerator().rootPackage("d.h").addIgnoredMethodNames("setId", "setParent", "setCurrentBufferedSourceException").build()
                .generateFluentClass(TextField.class);

        System.out.println(Joiner.on("\n").join(Files.readLines(new File(DEFAULT_TARGET_DIRECTORY + "/d/h/FluentTextField.java"), Charsets.UTF_8)));
    }
}
