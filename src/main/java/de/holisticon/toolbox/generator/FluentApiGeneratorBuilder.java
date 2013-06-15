package de.holisticon.toolbox.generator;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.util.Set;

import org.apache.commons.lang3.builder.Builder;

import com.google.common.collect.Sets;

public class FluentApiGeneratorBuilder implements Builder<FluentApiGenerator> {

    private String rootPackage = "";
    private String targetDirectory = FluentApiGenerator.DEFAULT_TARGET_DIRECTORY;;
    private final Set<String> ignoredMethodNames = Sets.newHashSet();

    public FluentApiGeneratorBuilder rootPackage(final String rootPackage) {
        checkArgument(isNotBlank(rootPackage));
        this.rootPackage = rootPackage;
        return this;
    }

    public FluentApiGeneratorBuilder targetDirectory(final String targetDirectory) {
        checkArgument(isNotBlank(targetDirectory));
        this.targetDirectory = targetDirectory;
        return this;
    }

    public FluentApiGeneratorBuilder addIgnoredMethodNames(final String... methodNames) {
        checkArgument(methodNames != null);
        checkArgument(methodNames.length > 0);

        ignoredMethodNames.addAll(Sets.newHashSet(methodNames));

        return this;
    }

    @Override
    public FluentApiGenerator build() {
        return new FluentApiGenerator(new File(targetDirectory), rootPackage, ignoredMethodNames);
    }
}
