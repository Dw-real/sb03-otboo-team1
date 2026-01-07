package com.onepiece.otboo.domain.weather.batch.tasklet;

import com.onepiece.otboo.domain.notification.enums.AlertStatus;
import com.onepiece.otboo.domain.weather.entity.WeatherAlertOutbox;
import com.onepiece.otboo.domain.weather.repository.WeatherAlertOutboxRepository;
import com.onepiece.otboo.global.event.event.WeatherChangeEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherAlertSendTasklet implements Tasklet {

    private final WeatherAlertOutboxRepository outboxRepository;
    private final ApplicationEventPublisher publisher;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
        throws Exception {

        List<WeatherAlertOutbox> outboxes = outboxRepository.findTop100ByStatus(
            AlertStatus.PENDING);

        if (outboxes.isEmpty()) {
            log.info("[WeatherAlertSendTasklet] 보낼 알림 없음!!");
            return RepeatStatus.FINISHED;
        }

        for (WeatherAlertOutbox outbox : outboxes) {
            outbox.updateStatus(AlertStatus.SENDING);

            publisher.publishEvent(new WeatherChangeEvent(
                outbox.getId(),
                outbox.getUserId(),
                outbox.getTitle(),
                outbox.getMessage()
            ));
        }

        outboxRepository.saveAll(outboxes);
        return RepeatStatus.CONTINUABLE;
    }
}
