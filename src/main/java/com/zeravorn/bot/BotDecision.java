package com.zeravorn.bot;

import com.zeravorn.map.Position;

import java.util.UUID;

public record BotDecision(BotState state, UUID targetId, Position waypoint) { }
