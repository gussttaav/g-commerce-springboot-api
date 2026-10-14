package com.gplanet.commerce.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.context.ActiveProfiles;

import com.gplanet.commerce.api.configs.TestRedisConfiguration;

/**
 * Verifies that enabling key rotation actually schedules {@code KeyRotationScheduler.rotateKeys} (KEY-2).
 */
@SpringBootTest(properties = {
    "oauth2.key-rotation.enabled=true",
    "oauth2.key-rotation.cron=0 0 3 1 * ?"
})
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class KeyRotationSchedulingTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void keyRotationIsScheduledWithConfiguredCron() {
        List<CronTask> rotationTasks = applicationContext.getBeansOfType(ScheduledTaskHolder.class).values().stream()
                .flatMap(holder -> holder.getScheduledTasks().stream())
                .map(ScheduledTask::getTask)
                .filter(CronTask.class::isInstance)
                .map(CronTask.class::cast)
                .filter(task -> task.getRunnable().toString().endsWith("KeyRotationScheduler.rotateKeys"))
                .toList();

        assertThat(rotationTasks)
                .singleElement()
                .extracting(CronTask::getExpression)
                .isEqualTo("0 0 3 1 * ?");
    }
}
