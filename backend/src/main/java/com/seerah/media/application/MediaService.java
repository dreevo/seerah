package com.seerah.media.application;

import com.seerah.media.api.MediaReadPort;
import com.seerah.media.api.MediaRegistrar;
import com.seerah.media.api.MediaView;
import com.seerah.media.application.port.out.MediaStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Application service for media: registration, placement, and reads. */
@Service
@Transactional
public class MediaService implements MediaReadPort, MediaRegistrar {

    private final MediaStore store;

    public MediaService(MediaStore store) {
        this.store = store;
    }

    @Override
    public UUID registerAsset(RegisterAsset c) {
        if (c.attribution() == null || c.attribution().isBlank()) {
            throw new IllegalArgumentException("a media asset must carry attribution (§12.9)");
        }
        return store.saveAsset(c.s3Key(), c.kind(), c.mimeType(), c.byteSize(),
                c.licence(), c.attribution(), c.sourceUrl());
    }

    @Override
    public void linkToEvent(UUID mediaId, UUID eventId, int ordinal, String caption) {
        store.link(mediaId, eventId, ordinal, caption);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaView> mediaForEvent(UUID eventId) {
        return store.forEvent(eventId).stream()
                .map(m -> new MediaView(m.kind(), m.caption(), m.attribution(),
                        m.licence(), m.sourceUrl(), m.s3Key()))
                .toList();
    }
}
