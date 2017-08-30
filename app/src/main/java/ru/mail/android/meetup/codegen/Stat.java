package ru.mail.android.meetup.codegen;

public class Stat {

    @Statistic
    public interface Main {

        void fabClicked(@Value("length") int length);

        void settingsClicked();

        void someMoreComplexEvent(@Name("type") String type,
                                  @Value("duration") long duration,
                                  @Name("important") boolean important);

    }

}