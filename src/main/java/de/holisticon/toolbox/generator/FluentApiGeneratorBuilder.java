package de.holisticon.toolbox.generator;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.util.Set;

import org.apache.commons.lang3.builder.Builder;

import com.google.common.collect.Sets;

/**
 * Used to create and configure a {@link FluentApiGenerator} instance.
 * @author Jan Galinski, Holisticon AG
 */
public class FluentApiGeneratorBuilder implements Builder<FluentApiGenerator> {

    private String rootPackage = "";
    private String targetDirectory = FluentApiGenerator.DEFAULT_TARGET_DIRECTORY;
    private String filenamePattern = FluentApiGenerator.DEFAULT_FILENAME_PATTERN;
    private final Set<String> ignoredMethodNames = Sets.newHashSet();

    /**
     * Sets the root package for generated classes. If blank, package of source class is used.
     * @param rootPackage the package for all generated classes.
     * @return this
     */
    public FluentApiGeneratorBuilder rootPackage(final String rootPackage) {
        checkArgument(isNotBlank(rootPackage));
        this.rootPackage = rootPackage;
        return this;
    }

    /**
     * Sets the directory where all generated classes are saved. Defaults to "target/generated-sources/java".
     * @param targetDirectory direcory path
     * @return this
     */
    public FluentApiGeneratorBuilder targetDirectory(final String targetDirectory) {
        checkArgument(isNotBlank(targetDirectory));
        this.targetDirectory = targetDirectory;
        return this;
    }

    /**
     * Sets te filename pattern used to generate Java file.
     * @param pattern fqn pattern for generated Java file. Should contain two placeholders, one for package, one for source class simple name.
     * @return this
     */
    public FluentApiGeneratorBuilder filenamePatter(final String pattern) {
        checkArgument(isNotBlank(pattern));
        checkArgument(pattern.contains("%s"), "pattern must contain placeholder %s");
        this.filenamePattern = pattern;
        return this;
    }

    /**
     * Add ignored method names ("setId"). These methods are skipped during automatic delegation.
     * @param methodNames one or more method names
     * @return this
     */
    public FluentApiGeneratorBuilder addIgnoredMethodNames(final String... methodNames) {
        checkArgument(methodNames != null);
        checkArgument(methodNames.length > 0);

        ignoredMethodNames.addAll(Sets.newHashSet(methodNames));
        return this;
    }

    @Override
    public FluentApiGenerator build() {
        return new FluentApiGenerator(new File(targetDirectory), rootPackage, filenamePattern, ignoredMethodNames);
    }
}
