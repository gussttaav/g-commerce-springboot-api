package com.gplanet.commerce.api.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class DemoDataResetSchedulerTest {

    @Mock
    private DemoDataService demoDataService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private DemoDataResetScheduler demoDataResetScheduler;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void resetDemoDataResetsWhenLockIsAcquired() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        demoDataResetScheduler.resetDemoData();

        verify(demoDataService).resetDemoData();
    }

    @Test
    void resetDemoDataSkipsWhenAnotherInstanceHoldsLock() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        demoDataResetScheduler.resetDemoData();

        verify(demoDataService, never()).resetDemoData();
    }

    @Test
    void resetDemoDataLogsFailuresInsteadOfThrowing() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        doThrow(new IllegalStateException("Database unavailable")).when(demoDataService).resetDemoData();

        assertDoesNotThrow(() -> demoDataResetScheduler.resetDemoData());
    }
}
