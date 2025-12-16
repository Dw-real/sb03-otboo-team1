package com.onepiece.otboo.global.event.listener;

import com.onepiece.otboo.domain.profile.dto.request.ProfileImageUpdatedDto;
import com.onepiece.otboo.domain.profile.entity.Profile;
import com.onepiece.otboo.domain.profile.exception.ProfileNotFoundException;
import com.onepiece.otboo.domain.profile.repository.ProfileRepository;
import com.onepiece.otboo.global.event.event.ProfileImageReplaceEvent;
import com.onepiece.otboo.global.sse.SseService;
import com.onepiece.otboo.global.storage.S3Storage;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileImageReplaceEventListener {

    private final ProfileRepository profileRepository;
    private final SseService sseService;
    private final S3Storage storage;

    @Async("binaryContentExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ProfileImageReplaceEvent event) {
        try {
            // 1) 업로드
            String newKey = storage.uploadBytes(event.prefix(), event.payload());

            // 2) 프로필 재조회 & 반영
            Profile profile = profileRepository.findByUserId(event.userId())
                .orElseThrow(() -> new ProfileNotFoundException(event.userId()));
            profile.updateProfileImageUrl(newKey);

            profileRepository.flush();

            String presignedUrl = storage.generatePresignedUrl(newKey);
            sseService.send(event.userId(), "profiles",
                new ProfileImageUpdatedDto(event.userId(), presignedUrl, Instant.now()));

            // 3) 이전 키 삭제
            String oldKey = event.oldKey();
            if (oldKey != null && !oldKey.equals(newKey)) {
                try {
                    storage.deleteFile(oldKey);
                } catch (Exception e) {
                    log.warn("[ProfileImageReplaceEventHandler] 이전 이미지 삭제 실패 - key: {}", oldKey, e);
                }
            }

            log.info("[ProfileImageReplaceEventHandler] 완료 - userId: {}, newKey: {}",
                event.userId(), newKey);
        } catch (Exception e) {
            log.error("[ProfileImageReplaceEventHandler] 실패 - userId: {}", event.userId(), e);
        }
    }
}
