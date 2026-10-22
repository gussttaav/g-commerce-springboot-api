package com.gplanet.commerce.api.configs.app.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.test.context.ActiveProfiles;

import com.gplanet.commerce.api.configs.TestRedisConfiguration;

// The cron only fires on February 29, so the reset doesn't wipe the database other tests share
@SpringBootTest(properties = {
    "demo.reset.enabled=true",
    "demo.reset.cron=0 0 3 29 2 *"
})
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class DemoDataResetSchedulingTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void demoDataResetIsScheduledWithConfiguredCronInUtc() {
        List<CronTask> resetTasks = applicationContext.getBeansOfType(ScheduledTaskHolder.class).values().stream()
                .flatMap(holder -> holder.getScheduledTasks().stream())
                .map(ScheduledTask::getTask)
                .filter(CronTask.class::isInstance)
                .map(CronTask.class::cast)
                .filter(task -> task.getRunnable().toString().endsWith("DemoDataResetScheduler.resetDemoData"))
                .toList();

        assertThat(resetTasks)
                .singleElement()
                .extracting(CronTask::getExpression)
                .isEqualTo("0 0 3 29 2 *");

        // The schedule is in UTC, whatever the time zone of the server
        Clock tokyoClock = Clock.fixed(Instant.parse("2028-02-28T12:00:00Z"), ZoneId.of("Asia/Tokyo"));
        CronTrigger trigger = (CronTrigger) resetTasks.get(0).getTrigger();
        assertThat(trigger.nextExecution(new SimpleTriggerContext(tokyoClock)))
                .isEqualTo(Instant.parse("2028-02-29T03:00:00Z"));
    }
}
