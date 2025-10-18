package com.dorandoran.batch.common;

import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class TimeProvider {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public ZonedDateTime nowKst() {
        return ZonedDateTime.now(KST);
    }

    public ZoneId kst() {
        return KST;
    }
}


