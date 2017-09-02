package ru.mail.android.meetup.codegen;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.analytics.HitBuilders;

import java.util.HashMap;
import java.util.Map;

public class AllSender implements Sender {
    @Override
    public void send(StatisticParams params) {
        Map<String, String> paramMap = new HashMap<>();
        for (StatisticParams.Param param : params.getParams()) {
            paramMap.put(param.getName(), param.getStringValue());
        }

        StatisticParams.Param value = params.getFirstAnnotatedParam(Value.class);

        FlurryAgent.logEvent(params.getClassName() + "_" + params.getMethodName(), paramMap);
        GaTracker.get().send(new HitBuilders.EventBuilder()
                .setCategory(params.getClassName())
                .setAction(params.getMethodName())
                .setValue(value == null ? 0 : (Long) value.getValue())
                .build());
    }
}
