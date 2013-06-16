package de.holisticon.toolbox.generator.predicate;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.or;
import static com.google.common.base.Predicates.not;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public enum MethodPredicates implements Predicate<Method> {

    IS_FLUENT_CANDIDATE {

        @Override
        @SuppressWarnings("unchecked")
        public boolean apply(final Method method) {
            return and(IS_PUBLIC, or(PREFIX_IS_ADD, PREFIX_IS_SET), IS_VOID, HAS_ONE_PARAM, NOT_DEPRECATED, not(IS_SYNTHETIC)).apply(method);
        }
    },
    /**
     * Rules for setters:
     * <ul>
     * <li>public</li>
     * <li>void</li>
     * <li>name starts with "set"</li>
     * <li>method takes on argument</li>
     * </ul>
     */
    IS_SETTER {

        @Override
        @SuppressWarnings("unchecked")
        public boolean apply(final Method method) {
            return Predicates.and(IS_PUBLIC, IS_VOID, HAS_ONE_PARAM).apply(method) && method.getName().startsWith(SET);
        }
    },
    IS_ADDER {

        @Override
        @SuppressWarnings("unchecked")
        public boolean apply(final Method method) {
            return Predicates.and(IS_PUBLIC, IS_VOID, HAS_ONE_PARAM).apply(method) && method.getName().startsWith(ADD);
        }
    },
    IS_VOID {

        @Override
        public boolean apply(final Method method) {
            return method.getReturnType().equals(Void.TYPE);
        }
    },
    HAS_ONE_PARAM {

        @Override
        public boolean apply(final Method method) {
            return method.getParameterTypes().length == 1;
        }
    },
    IS_DEPRECATED {

        @Override
        public boolean apply(final Method method) {
            return method.isAnnotationPresent(Deprecated.class);
        }
    },
    NOT_DEPRECATED {

        @Override
        public boolean apply(final Method method) {
            return !IS_DEPRECATED.apply(method);
        }
    },
    IS_PUBLIC {

        @Override
        public boolean apply(final Method method) {
            return Modifier.isPublic(method.getModifiers());
        }
    },
    PREFIX_IS_ADD {

        @Override
        public boolean apply(final Method method) {
            return method.getName().startsWith(ADD);
        }

    },
    PREFIX_IS_SET {

        @Override
        public boolean apply(final Method method) {
            return method.getName().startsWith(SET);
        }

    },
    IS_SYNTHETIC {

        @Override
        public boolean apply(final Method method) {
            return method.isSynthetic();
        }
    },
    IS_BRIDGED {

        @Override
        public boolean apply(final Method method) {
            return method.isBridge();
        }
    },
    ;

    public static final String ADD = "add";
    public static final String SET = "set";

}
