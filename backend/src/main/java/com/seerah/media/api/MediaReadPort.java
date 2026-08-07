package com.seerah.media.api;

import java.util.List;
import java.util.UUID;

/** Published read contract of the {@code media} module. */
public interface MediaReadPort {

    List<MediaView> mediaForEvent(UUID eventId);
}
