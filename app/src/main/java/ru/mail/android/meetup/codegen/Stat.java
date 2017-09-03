package ru.mail.android.meetup.codegen;

public class Stat {

    @Statistic(AllSender.class)
    public interface Main {

        void fabClicked(@Value int length);

        void settingsClicked();

        void someMoreComplexEvent(String type,
                                  @Value long duration,
                                  boolean important);

    }

}