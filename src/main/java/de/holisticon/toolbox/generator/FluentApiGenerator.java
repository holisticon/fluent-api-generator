package de.holisticon.toolbox.generator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Collections2.filter;
import static com.sun.codemodel.JExpr._new;
import static com.sun.codemodel.JExpr._this;
import static com.sun.codemodel.JMod.FINAL;
import static com.sun.codemodel.JMod.PRIVATE;
import static com.sun.codemodel.JMod.PUBLIC;
import static de.holisticon.toolbox.generator.predicate.MethodPredicates.IS_PUBLIC;
import static de.holisticon.toolbox.generator.predicate.MethodPredicates.NOT_DEPRECATED;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import javax.annotation.Generated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JVar;

import de.holisticon.toolbox.generator.predicate.MethodPredicates;

/**
 * Do not access directly, use {@link FluentApiGeneratorBuilder}.
 * @author Jan Galinski, Holisticon AG
 */
public class FluentApiGenerator {

    public static FluentApiGeneratorBuilder fluentApiGenerator() {
        return new FluentApiGeneratorBuilder();
    }

    private static final String SET = "set";
    private static final String GET = "get";
    public static final String DEFAULT_FILENAME_PATTERN = "%s.Fluent%s";
    public static final String DEFAULT_TARGET_DIRECTORY = "target/generated-sources/java";

    public static final int PRIVATE_FINAL = PRIVATE + FINAL;
    public static final int PUBLIC_FINAL = PUBLIC + FINAL;

    private final JCodeModel codeModel = new JCodeModel();
    private final JPackage rootpackage;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final File targetDirectory;

    private final Predicate<Method> filterApplicableMethods;

    @SuppressWarnings("unchecked")
    FluentApiGenerator(final File targetDirectory, final String rootPackage, final Set<String> ignoredMethods) {
        checkArgument(targetDirectory != null);
        if (!targetDirectory.exists()) {
            checkArgument(targetDirectory.mkdirs(), format("target directory '%s' could not be created.", targetDirectory.getAbsolutePath()));
        } else {
            checkArgument(targetDirectory.isDirectory());
            checkArgument(targetDirectory.canWrite());
        }
        checkArgument(isNotBlank(rootPackage));
        checkArgument(ignoredMethods != null);

        this.targetDirectory = targetDirectory;
        this.rootpackage = codeModel._package(rootPackage);

        final Predicate<Method> notIgnored = new Predicate<Method>() {

            @Override
            public boolean apply(final Method method) {
                final String name = method.getName();
                final boolean ignored = ignoredMethods.contains(name);
                if (ignored) {
                    logger.info("ignoring method {}", method);
                    return false;
                }
                return true;
            }
        };

        this.filterApplicableMethods = Predicates.and(IS_PUBLIC, NOT_DEPRECATED, notIgnored);
    }

    static void createDirectories(final File directory) {

    }

    public void generateFluentClass(final String canonicalClassname) {
        try {
            addClass(Class.forName(canonicalClassname));
        } catch (final ClassNotFoundException e) {
            propagate(e);
        }
    }

    public void generateCode() {
        try {
            codeModel.build(targetDirectory);
        } catch (final IOException e) {
            propagate(e);
        }
    }

    public FluentApiGenerator addClass(final Class<?> sourceClass) {
        try {
            final JDefinedClass definedClass = codeModel._class(format(DEFAULT_FILENAME_PATTERN, rootpackage.name(), sourceClass.getSimpleName()));

            // _package.javadoc().append("CHECKSTYLE:OFF - generated class");

            definedClass.annotate(Generated.class);
            final JFieldVar field = createFieldAndGetter(definedClass, sourceClass);

            final Collection<Method> filterMethods = filterMethods(sourceClass);
            for (final Method m : filterMethods) {

                if (MethodPredicates.IS_SETTER.apply(m)) {
                    createSettersForSourceClass(definedClass, m, field);
                }
            }
            return this;

        } catch (final JClassAlreadyExistsException e) {
            throw propagate(e);
        }            // ...
    }

    private Collection<Method> filterMethods(final Class<?> sourceClass) {
        return filter(Sets.newHashSet(sourceClass.getMethods()), filterApplicableMethods);
    }

    private void createSettersForSourceClass(final JDefinedClass definedClass, final Method setter, final JFieldVar field) {
        final String name = uncapitalize(removeStart(setter.getName(), SET));
        final JMethod method = definedClass.method(PUBLIC, definedClass, name);

        final Class<?> parameterType = setter.getParameterTypes()[0];
        final JVar param = method.param(FINAL, parameterType, name);

        method.body().invoke(field, setter.getName()).arg(param);
        method.body()._return(_this());

        // default for boolean setters
        if (parameterType.getCanonicalName().equals("boolean")) {
            final JMethod booleanMethod = definedClass.method(PUBLIC, definedClass, name);
            booleanMethod.body()._return(_this().invoke(method).arg(JExpr.TRUE));
            booleanMethod.javadoc().addReturn().add("#" + name + "(true)");
        }

    }

    private JFieldVar createFieldAndGetter(final JDefinedClass definedClass, final Class<?> internalType) {
        final JFieldVar field = definedClass.field(PRIVATE_FINAL, internalType, uncapitalize(internalType.getSimpleName()),
                _new(codeModel._ref(internalType)));
        field.javadoc().add("delegate");
        final JMethod method = definedClass.method(PUBLIC_FINAL, internalType, GET);
        method.body()._return(field);
        method.javadoc().addReturn().add("the created instance");
        return field;
    }
}
