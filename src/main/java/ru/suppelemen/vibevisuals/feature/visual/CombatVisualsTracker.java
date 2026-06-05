package ru.suppelemen.vibevisuals.feature.visual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Packet-independent kill detector. Remembers entities the local player has
 * recently attacked and, while they stay loaded, tracks their last position.
 * When such an entity dies (mob health hits 0) or is removed from the world
 * (death / respawn / disconnect) within a short window, it fires the Kill
 * Effect at the last spot it was seen.
 *
 * <p>No packets are read or sent — detection is purely local game state, so an
 * anticheat has nothing to react to.
 */
public final class CombatVisualsTracker {

    private static final class Watch {
        Vec3d lastPos;
        int ticksLeft;

        Watch(Vec3d pos) {
            this.lastPos = pos;
            this.ticksLeft = WATCH_TICKS;
        }
    }

    private static final int WATCH_TICKS = 40; // ~2s window to attribute a kill
    private static final Map<Integer, Watch> WATCHING = new HashMap<>();

    private CombatVisualsTracker() {
    }

    /** Call from AttackEntityCallback (client side) for every entity hit. */
    public static void onAttack(PlayerEntity attacker, Entity target) {
        if (!(target instanceof LivingEntity)) {
            return;
        }
        WATCHING.put(target.getId(), new Watch(target.getLerpedPos(1.0f)));
    }

    public static void tick(MinecraftClient client) {
        if (WATCHING.isEmpty()) {
            return;
        }
        if (client.world == null) {
            WATCHING.clear();
            return;
        }

        Iterator<Map.Entry<Integer, Watch>> it = WATCHING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Watch> e = it.next();
            Watch w = e.getValue();
            Entity entity = client.world.getEntityById(e.getKey());

            boolean dead = entity == null
                    || entity.isRemoved()
                    || (entity instanceof LivingEntity le && (le.isDead() || le.getHealth() <= 0.0f));

            if (dead) {
                KillEffect.spawn(w.lastPos);
                it.remove();
                continue;
            }

            // Still alive — keep its position fresh for when it does go down.
            w.lastPos = entity.getLerpedPos(1.0f);
            if (--w.ticksLeft <= 0) {
                it.remove();
            }
        }
    }
}
