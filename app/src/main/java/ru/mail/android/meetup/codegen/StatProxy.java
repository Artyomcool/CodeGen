package ru.mail.android.meetup.codegen;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.analytics.HitBuilders;

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
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String className = clazz.getSimpleName();
                String methodName = method.getName();

                className = className.substring(className.lastIndexOf("$") + 1);

                FlurryAgent.logEvent(className + "_" + methodName);
                GaTracker.get().send(new HitBuilders.EventBuilder()
                        .setCategory(className)
                        .setAction(methodName)
                        .build());

                return null;
            }
        });
    }

}
