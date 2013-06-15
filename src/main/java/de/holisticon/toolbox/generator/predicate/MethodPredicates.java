package de.holisticon.toolbox.generator.predicate;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public enum MethodPredicates implements Predicate<Method> {

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
    ;

    private static final String SET = "set";

}
