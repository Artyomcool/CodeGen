package ru.mail.android.meetup.codegen;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.analytics.HitBuilders;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StatProxy {

    private static final Map<Class<?>, Object> IMPLEMENTATIONS = new HashMap<>();

    public static <T> T send(Class<T> clazz) {
        Object result;
        synchronized (IMPLEMENTATIONS) {
            result = IMPLEMENTATIONS.get(clazz);
        }
        if (result != null) {
            return clazz.cast(result);
        }

        result = createProxy(clazz);

        Object old;
        synchronized (IMPLEMENTATIONS) {
            old = IMPLEMENTATIONS.get(clazz);
            if (old == null) {
                IMPLEMENTATIONS.put(clazz, result);
            }
        }

        return clazz.cast(old == null ? result : old);
    }

    private static <T> Object createProxy(final Class<T> clazz) {
        try {
            return Class.forName(clazz.getName() + "$$$StatGenerated").newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
