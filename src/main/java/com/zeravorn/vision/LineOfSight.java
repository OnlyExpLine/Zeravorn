package com.zeravorn.vision;

import com.zeravorn.map.Position;

@FunctionalInterface
public interface LineOfSight { boolean hasLineOfSight(Position from, Position to); }
