package com.onepiece.otboo.domain.weather.batch.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.onepiece.otboo.domain.notification.enums.AlertStatus;
import com.onepiece.otboo.domain.weather.entity.WeatherAlertOutbox;
import com.onepiece.otboo.domain.weather.enums.WeatherChangeType;
import com.onepiece.otboo.domain.weather.repository.WeatherAlertOutboxRepository;
import com.onepiece.otboo.global.event.event.WeatherChangeEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WeatherAlertSendTaskletTest {

    @Mock
    private WeatherAlertOutboxRepository outboxRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private WeatherAlertSendTasklet tasklet;

    @Test
    void 대기_중인_알림이_없으면_FINISHED를_반환한다() throws Exception {

        // given
        given(outboxRepository.findTop100ByStatus(AlertStatus.PENDING)).willReturn(List.of());

        // when
        RepeatStatus status = tasklet.execute(mock(StepContribution.class),
            mock(ChunkContext.class));

        // then
        assertEquals(RepeatStatus.FINISHED, status);
        verify(outboxRepository).findTop100ByStatus(AlertStatus.PENDING);
    }

    @Test
    void PENDING_알림을_가져와_SENDING_상태로_변경하고_이벤트를_발행한다() throws Exception {

        // given
        UUID outboxId1 = UUID.randomUUID();
        UUID outboxId2 = UUID.randomUUID();

        UUID locationId1 = UUID.randomUUID();
        UUID locationId2 = UUID.randomUUID();

        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        WeatherAlertOutbox outbox1 = WeatherAlertOutbox.builder()
            .locationId(locationId1)
            .userId(userId1)
            .type(WeatherChangeType.WIND_CHANGE)
            .alertDate(LocalDate.of(2026, 1, 5))
            .title("강풍 주의")
            .message("강풍이 예보되어 있습니다.")
            .status(AlertStatus.PENDING)
            .build();

        WeatherAlertOutbox outbox2 = WeatherAlertOutbox.builder()
            .locationId(locationId2)
            .userId(userId2)
            .type(WeatherChangeType.PRECIPITATION_CHANGE)
            .alertDate(LocalDate.of(2026, 1, 5))
            .title("폭우 주의")
            .message("폭우가 예보되어 있습니다.")
            .status(AlertStatus.PENDING)
            .build();

        ReflectionTestUtils.setField(outbox1, "id", outboxId1);
        ReflectionTestUtils.setField(outbox2, "id", outboxId2);

        given(outboxRepository.findTop100ByStatus(AlertStatus.PENDING))
            .willReturn(List.of(outbox1, outbox2));

        // when
        RepeatStatus result = tasklet.execute(mock(StepContribution.class),
            mock(ChunkContext.class));

        // then
        assertEquals(RepeatStatus.CONTINUABLE, result);

        // 1) 저장되는 outbox 상태가 SENDING인지
        ArgumentCaptor<List<WeatherAlertOutbox>> savedCap = ArgumentCaptor.forClass(List.class);
        verify(outboxRepository).saveAll(savedCap.capture());

        List<WeatherAlertOutbox> saved = savedCap.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(o -> o.getStatus() == AlertStatus.SENDING);

        // 2) 이벤트가 outbox 개수만큼 발행되는지 + payload 검증
        ArgumentCaptor<WeatherChangeEvent> eventCap = ArgumentCaptor.forClass(
            WeatherChangeEvent.class);
        verify(publisher, times(2)).publishEvent(eventCap.capture());

        List<WeatherChangeEvent> events = eventCap.getAllValues();
        assertThat(events).hasSize(2);

        // 이벤트 내용까지 비교
        WeatherChangeEvent e1 = events.get(0);
        WeatherChangeEvent e2 = events.get(1);

        assertThat(events).anySatisfy(e -> {
            assertThat(e.outboxId()).isEqualTo(outboxId1);
            assertThat(e.userId()).isEqualTo(userId1);
            assertThat(e.title()).isEqualTo("강풍 주의");
            assertThat(e.message()).isEqualTo("강풍이 예보되어 있습니다.");
        });

        assertThat(events).anySatisfy(e -> {
            assertThat(e.outboxId()).isEqualTo(outboxId2);
            assertThat(e.userId()).isEqualTo(userId2);
            assertThat(e.title()).isEqualTo("폭우 주의");
            assertThat(e.message()).isEqualTo("폭우가 예보되어 있습니다.");
        });

        verify(outboxRepository).findTop100ByStatus(AlertStatus.PENDING);
    }
}