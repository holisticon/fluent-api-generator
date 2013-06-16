package de.holisticon.toolbox.generator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Collections2.filter;
import static com.sun.codemodel.JExpr._new;
import static com.sun.codemodel.JExpr._this;
import static com.sun.codemodel.JMod.FINAL;
import static com.sun.codemodel.JMod.PRIVATE;
import static com.sun.codemodel.JMod.PUBLIC;
import static com.sun.codemodel.JMod.STATIC;
import static de.holisticon.toolbox.generator.predicate.MethodPredicates.IS_FLUENT_CANDIDATE;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import javax.annotation.Generated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.sun.codemodel.ClassType;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForEach;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JVar;

import de.holisticon.toolbox.generator.predicate.ConstructorPredicates;
import de.holisticon.toolbox.generator.predicate.MethodPredicates;

/**
 * Do not access directly, use {@link FluentApiGeneratorBuilder}.
 * @author Jan Galinski, Holisticon AG
 */
public class FluentApiGenerator {

    public static FluentApiGeneratorBuilder fluentApiGenerator() {
        return new FluentApiGeneratorBuilder();
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String SET = "set";
    private static final String GET = "get";
    public static final String DEFAULT_FILENAME_PATTERN = "%s.Fluent%s";
    public static final String ADDER_PATTERN = "%ss";
    public static final String DEFAULT_TARGET_DIRECTORY = "target/generated-sources/java";

    public static final int PRIVATE_FINAL = PRIVATE + FINAL;
    public static final int PUBLIC_FINAL = PUBLIC + FINAL;
    public static final int PUBLIC_STATIC = PUBLIC + STATIC;

    private final JCodeModel codeModel = new JCodeModel();
    private final JPackage rootpackage;

    private final File targetDirectory;

    private final Predicate<Method> filterApplicableMethods;

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

        this.filterApplicableMethods = Predicates.and(IS_FLUENT_CANDIDATE, notIgnored);
    }

    private final class GeneratedClass {

        private final Class<?> sourceClass;
        private final JClass sourceClassRef;
        private final String sourceClassName;
        private final String delegateFieldName;

        private final JFieldVar delegateField;
        private final JDefinedClass definedClass;

        public GeneratedClass(final Class<?> sourceClass) {
            sourceClassRef = codeModel.ref(sourceClass);

            this.sourceClass = sourceClass;
            this.sourceClassName = sourceClass.getSimpleName();
            this.delegateFieldName = uncapitalize(sourceClassName);

            try {
                final String fullyqualifiedName = format(DEFAULT_FILENAME_PATTERN, rootpackage.name(), sourceClassName);
                definedClass = codeModel._class(PUBLIC_FINAL, fullyqualifiedName, ClassType.CLASS);
                annotateWithGenerated();

                delegateField = createFieldAndGetter();

                hideDefaultConstructor(definedClass, delegateField);
                addFactoryMethodForConstructors();

                // _package.javadoc().append("CHECKSTYLE:OFF - generated class");

                final Collection<Method> filterMethods = filterMethods(sourceClass);
                for (final Method m : filterMethods) {

                    if (MethodPredicates.IS_SETTER.apply(m)) {
                        createSettersForSourceClass(m);
                    } else if (MethodPredicates.IS_ADDER.apply(m)) {
                        createAdderForSourceClass(m);
                    }

                }

            } catch (final JClassAlreadyExistsException e) {
                throw propagate(e);
            }
        }

        private void addFactoryMethodForConstructors() {

            for (final Constructor<?> c : ConstructorPredicates.IS_CONSTRUCTOR_CANDIDATE.filterConstructors(sourceClass)) {
                final JMethod factoryMethod = definedClass.method(PUBLIC_STATIC, definedClass, delegateFieldName);
                final JInvocation newDelegate = _new(sourceClassRef);
                final Class<?>[] parameterTypes = c.getParameterTypes();

                for (int i = 0; i < parameterTypes.length; i++) {
                    final JVar param = factoryMethod.param(parameterTypes[i], "arg" + i);
                    newDelegate.arg(param);
                }

                factoryMethod.body()._return(_new(definedClass).arg(newDelegate));
            }
        }

        private void annotateWithGenerated() {
            definedClass.annotate(Generated.class).param("value", FluentApiGenerator.class.getCanonicalName())
                    .param("date", DateFormat.getDateTimeInstance().format(new Date()));
        }

        private JFieldVar createFieldAndGetter() {
            final JFieldVar field = definedClass.field(PRIVATE_FINAL, sourceClass, delegateFieldName);
            field.javadoc().add("delegate");
            final JMethod method = definedClass.method(PUBLIC_FINAL, sourceClass, GET);
            method.body()._return(field);
            method.javadoc().addReturn().add("the created instance");
            return field;
        }

        private void hideDefaultConstructor(final JDefinedClass definedClass, final JFieldVar delegateField) {
            final JMethod constructor = definedClass.constructor(PRIVATE);
            constructor.javadoc().add("Hide constructor, use static factory methods.");
            // constructor.param(FINAL, delegateField.type(), definedClass.name());
            constructor.body().assign(_this().ref(delegateField), constructor.param(FINAL, delegateField.type(), uncapitalize(delegateField.name())));
        }

        private void createSettersForSourceClass(final Method setter) {
            final String name = uncapitalize(removeStart(setter.getName(), SET));
            final JMethod method = definedClass.method(PUBLIC, definedClass, name);

            final Class<?> parameterType = setter.getParameterTypes()[0];
            final Type genericType = setter.getGenericParameterTypes()[0];

            final JVar param = method.param(FINAL, parameterType, name);

            method.body().invoke(delegateField, setter.getName()).arg(param);
            method.body()._return(_this());

            // default for boolean setters
            if (parameterType.getCanonicalName().equals("boolean")) {
                final JMethod booleanMethod = definedClass.method(PUBLIC, definedClass, name);
                booleanMethod.body()._return(_this().invoke(method).arg(JExpr.TRUE));
                booleanMethod.javadoc().addReturn().add("#" + name + "(true)");
            }

        }

        private void createAdderForSourceClass(final Method adder) {
            final JMethod method = definedClass.method(PUBLIC, definedClass, format(ADDER_PATTERN, adder.getName()));

            final JVar param = method.varParam(adder.getParameterTypes()[0], uncapitalize(removeStart(method.name(), MethodPredicates.ADD)));

            final JBlock block = method.body();
            final String loopVariable = removeEnd(param.name(), "s");
            // the param.type is array, so elementType must be used
            final JForEach forEach = block.forEach(param.type().elementType(), loopVariable, param);
            forEach.body().invoke(delegateField, adder.getName()).arg(forEach.var());

            block._return(_this());
        }
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
        new GeneratedClass(sourceClass);
        return this;
    }

    private Collection<Method> filterMethods(final Class<?> sourceClass) {
        return filter(Sets.newHashSet(sourceClass.getMethods()), filterApplicableMethods);
    }

}
