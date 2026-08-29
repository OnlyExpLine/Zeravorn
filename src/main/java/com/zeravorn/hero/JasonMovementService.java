package com.zeravorn.hero;

import com.zeravorn.combat.CrowdControlService;
import java.util.UUID;

public final class JasonMovementService {
    private static final double DASH_DISTANCE = 2.0;
    private static final double FLIGHT_DISTANCE = 5.0;
    private final CrowdControlService crowdControl;

    public JasonMovementService(CrowdControlService crowdControl) { this.crowdControl = crowdControl; }
    public JasonMovementResult dash(HeroRuntime caster) {
        if (!caster.alive()) return JasonMovementResult.rejected("DEAD");
        if (crowdControl.blocksMovement(caster.owner())) return JasonMovementResult.rejected("ROOT_BLOCK");
        return new JasonMovementResult(true, "", DASH_DISTANCE);
    }
    public JasonMovementResult flight(HeroRuntime caster) {
        if (!caster.alive()) return JasonMovementResult.rejected("DEAD");
        if (crowdControl.blocksMovement(caster.owner())) return JasonMovementResult.rejected("ROOT_BLOCK");
        return new JasonMovementResult(true, "", FLIGHT_DISTANCE);
    }
}
