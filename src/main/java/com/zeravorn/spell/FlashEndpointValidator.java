package com.zeravorn.spell;

import com.zeravorn.map.Position;

/** Map/world adapter supplied by later arena integration. */
@FunctionalInterface
public interface FlashEndpointValidator { boolean isSafeEndpoint(Position endpoint); }
