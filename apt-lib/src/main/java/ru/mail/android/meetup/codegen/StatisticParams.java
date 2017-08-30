package ru.mail.android.meetup.codegen;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

public class StatisticParams {

    public static class Param {

        private final String name;
        private final Object value;
        private final List<Class<? extends Annotation>> annotations;

        @SafeVarargs
        public Param(String name, Object value, Class<? extends Annotation>... annotations) {
            this.name = name;
            this.value = value;
            this.annotations = Arrays.asList(annotations);
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }

        public String getStringValue() {
            return value == null ? null : value.toString();
        }

        public boolean hasAnnotation(Class<? extends Annotation> clazz) {
            for (Class<? extends Annotation> annotation : annotations) {
                if (annotation == clazz) {
                    return true;
                }
            }
            return false;
        }

    }

    private final String className;
    private final String methodName;
    private final List<Param> params;

    public StatisticParams(String className, String methodName, List<Param> params) {
        this.className = className;
        this.methodName = methodName;
        this.params = params;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<Param> getParams() {
        return params;
    }

    public Param getFirstAnnotatedParam(Class<? extends Annotation> clazz) {
        for (Param param : params) {
            if (param.hasAnnotation(clazz)) {
                return param;
            }
        }
        return null;
    }

}
