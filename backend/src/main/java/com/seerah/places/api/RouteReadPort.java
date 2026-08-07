package com.seerah.places.api;

import java.util.List;
import java.util.UUID;

/** Read the routes associated with an event, for the map. */
public interface RouteReadPort {

    List<RouteView> routesForEvent(UUID eventId);
}
