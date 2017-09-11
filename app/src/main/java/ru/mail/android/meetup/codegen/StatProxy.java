package ru.mail.android.meetup.codegen;

public class StatProxy {

    public static <T> T send(Class<T> clazz) {
        return Stat$$$StatGenerated.getInstance(clazz);
    }

}
