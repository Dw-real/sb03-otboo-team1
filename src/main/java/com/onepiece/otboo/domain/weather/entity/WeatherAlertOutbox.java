package com.onepiece.otboo.domain.weather.entity;

import com.onepiece.otboo.domain.notification.enums.AlertStatus;
import com.onepiece.otboo.domain.weather.enums.WeatherChangeType;
import com.onepiece.otboo.global.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "weather_alert_outboxes")
public class WeatherAlertOutbox extends BaseEntity {

    @Column(nullable = false)
    private UUID locationId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WeatherChangeType type;

    @Column(nullable = false)
    private LocalDate alertDate;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    public static WeatherAlertOutbox create(UUID locId, UUID userId, WeatherChangeType type,
        LocalDate alertDate,
        String title,
        String msg) {
        return WeatherAlertOutbox.builder()
            .locationId(locId)
            .userId(userId)
            .type(type)
            .alertDate(alertDate)
            .title(title)
            .message(msg)
            .status(AlertStatus.PENDING)
            .build();
    }

    public void updateStatus(AlertStatus status) {
        this.status = status;
    }
}
