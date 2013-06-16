package de.holisticon.toolbox.generator.predicate;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Lists.newArrayList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collection;

import com.google.common.base.Predicate;

public enum ConstructorPredicates implements Predicate<Constructor<?>> {
    IS_CONSTRUCTOR_CANDIDATE {

        @Override
        public boolean apply(final Constructor<?> constructor) {
            return and(not(IS_SYTHETIC), IS_PUBLIC).apply(constructor);
        }
    },
    IS_SYTHETIC {

        @Override
        public boolean apply(final Constructor<?> constructor) {
            return constructor.isSynthetic();
        }
    },
    IS_ACCESSIBLE {

        @Override
        public boolean apply(final Constructor<?> constructor) {
            return constructor.isAccessible();
        }
    },
    IS_PUBLIC {

        @Override
        public boolean apply(final Constructor<?> constructor) {
            return Modifier.isPublic(constructor.getModifiers());
        }
    },
    ;

    public Collection<Constructor<?>> filterConstructors(final Class<?> type) {
        final Collection<Constructor<?>> allConstructors = newArrayList(type.getConstructors());

        return filter(allConstructors, this);

    }
}
