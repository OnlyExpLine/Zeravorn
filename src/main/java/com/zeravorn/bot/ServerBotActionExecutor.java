package com.zeravorn.bot;

import com.zeravorn.ability.AbilitySlot;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.map.Position;
import com.zeravorn.match.MatchSession;
import com.zeravorn.network.ClientRequest;
import com.zeravorn.network.RequestEnvelope;
import com.zeravorn.network.RequestValidationContext;
import com.zeravorn.network.ServerRequestProcessor;
import com.zeravorn.spell.SummonerSpell;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Bot intents enter the same validator, shop, skill and gameplay pipeline as player intents. */
public final class ServerBotActionExecutor implements BotActionExecutor {
    public interface WorldPort {
        Position position(UUID botId);
        /** Must return null when the target is no longer visible and legal. */
        Position visibleLegalTarget(UUID botId, UUID targetId);
        /** Server-side cooldown, mana, range and target check for this ability. */
        boolean canCast(HeroRuntime bot, AbilitySlot slot, UUID targetId);
        void moveToward(UUID botId, Position waypoint);
        Position safeRetreat(UUID botId);
    }

    private final MatchSession match;
    private final ServerRequestProcessor processor;
    private final WorldPort world;
    private final Map<UUID, Long> sequences = new HashMap<>();

    public ServerBotActionExecutor(MatchSession match, ServerRequestProcessor processor, WorldPort world) {
        this.match = Objects.requireNonNull(match);
        this.processor = Objects.requireNonNull(processor);
        this.world = Objects.requireNonNull(world);
    }

    @Override public void execute(HeroRuntime hero, BotDecision decision, BotBuildProfile build) {
        Objects.requireNonNull(hero);
        Objects.requireNonNull(decision);
        Objects.requireNonNull(build);
        if (!match.teams().roster(hero.team()).participants().stream()
                .anyMatch(slot -> slot.playerId().equals(hero.owner()) && slot.bot()))
            throw new IllegalArgumentException("Hero is not a bot-controlled roster slot");
        if (!hero.alive()) return;
        switch (decision.state()) {
            case SPAWN_BUY -> {
                buyNext(hero, build);
                learnBasic(hero);
                move(hero, decision.waypoint());
            }
            case MOVE_ROUTE, PUSH -> move(hero, decision.waypoint());
            case RETREAT -> move(hero, world.safeRetreat(hero.owner()));
            case RECALL -> send(hero, new ClientRequest.Recall(ClientRequest.RecallAction.START));
            case FIGHT, LANE_FARM, ATTACK_STRUCTURE -> attack(hero, decision.targetId());
            case RESPAWN_RETURN -> { /* M14 owns resurrection; dead heroes issue no requests. */ }
        }
    }

    private void buyNext(HeroRuntime hero, BotBuildProfile build) {
        for (String itemId : build.itemIds()) {
            if (hero.inventory().contains(itemId)) continue;
            // A failed/too-expensive first item must not cause an adaptive counter-build.
            send(hero, new ClientRequest.BuyItem(itemId));
            return;
        }
    }

    private void learnBasic(HeroRuntime hero) {
        if (hero.availableSkillPoints() <= 0) return;
        for (AbilitySlot slot : new AbilitySlot[]{AbilitySlot.Q, AbilitySlot.E, AbilitySlot.R, AbilitySlot.F}) {
            var ability = hero.definition().ability(slot);
            if (ability != null && hero.level() >= ability.unlockLevel()
                    && hero.abilityRank(ability.id()) < ability.maxRank()) {
                send(hero, new ClientRequest.UpgradeAbility(ability.id()));
                return;
            }
        }
    }

    private void move(HeroRuntime hero, Position waypoint) {
        if (waypoint != null) world.moveToward(hero.owner(), waypoint);
    }

    private void attack(HeroRuntime hero, UUID targetId) {
        if (targetId == null) return;
        Position target = world.visibleLegalTarget(hero.owner(), targetId);
        Position origin = world.position(hero.owner());
        if (target == null || origin == null) return;
        Position aim = new Position(target.x() - origin.x(), target.y() - origin.y(), target.z() - origin.z());
        if (aim.distanceSquared(new Position(0, 0, 0)) == 0) aim = new Position(1, 0, 0);
        for (AbilitySlot slot : new AbilitySlot[]{AbilitySlot.R, AbilitySlot.Q, AbilitySlot.E, AbilitySlot.F}) {
            var ability = hero.definition().ability(slot);
            if (ability != null && hero.abilityRank(ability.id()) > 0
                    && world.canCast(hero, slot, targetId)) {
                send(hero, new ClientRequest.CastAbility(ability.id(), targetId, aim, target));
                break;
            }
        }
        send(hero, new ClientRequest.BasicAttack(aim, targetId));
    }

    private void send(HeroRuntime hero, ClientRequest request) {
        long sequence = sequences.merge(hero.owner(), 1L, Long::sum);
        processor.process(new RequestValidationContext(match, hero.owner(), hero, SummonerSpell.FLASH, false),
                new RequestEnvelope(RequestEnvelope.CURRENT_VERSION, sequence, request));
    }
}
