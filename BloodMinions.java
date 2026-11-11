package com.example.bloodmod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.UUID;

/** Turns mobs that touch blood into undead pets that follow the nearest player. */
public final class BloodMinions {
    private static final String TAG_PREFIX = "bloodpet:";
    private static final DustParticleEffect BLOOD_DUST =
            new DustParticleEffect(new Vector3f(0.7f, 0.02f, 0.02f), 1.4f);

    private BloodMinions() {}

    public static UUID ownerOf(Entity entity) {
        for (String tag : entity.getCommandTags()) {
            if (tag.startsWith(TAG_PREFIX)) {
                try {
                    return UUID.fromString(tag.substring(TAG_PREFIX.length()));
                } catch (IllegalArgumentException ignored) {
                    // not one of ours
                }
            }
        }
        return null;
    }

    public static void tryConvert(ServerWorld world, MobEntity mob) {
        if (!mob.isAlive() || ownerOf(mob) != null) return;
        if (mob instanceof EnderDragonEntity || mob instanceof WitherEntity || mob instanceof WardenEntity) return;
        if (mob instanceof TameableEntity tameable && tameable.isTamed()) return;

        PlayerEntity owner = world.getClosestPlayer(mob, 32.0);
        if (owner == null || owner.isSpectator()) return;

        bloodRay(world, mob.getBoundingBox().getCenter());

        if (isUndead(mob)) {
            // Already undead: keep it (and its gear) and just bind it to the player.
            mob.addCommandTag(TAG_PREFIX + owner.getUuidAsString());
            makePet(mob, owner.getUuid());
            nameIt(mob, mob);
            return;
        }

        Entity created = undeadTypeFor(mob).create(world);
        if (!(created instanceof MobEntity pet)) return;

        pet.refreshPositionAndAngles(mob.getX(), mob.getY(), mob.getZ(), mob.getYaw(), 0.0f);
        pet.addCommandTag(TAG_PREFIX + owner.getUuidAsString());
        nameIt(pet, mob);

        mob.discard();
        world.spawnEntity(pet); // the entity-load hook attaches the pet behavior
    }

    private static void nameIt(MobEntity pet, MobEntity original) {
        pet.setCustomName(Text.literal("Undead ").append(original.getType().getName()));
    }

    private static boolean isUndead(MobEntity m) {
        return m instanceof ZombieEntity || m instanceof AbstractSkeletonEntity
                || m instanceof ZombieHorseEntity || m instanceof SkeletonHorseEntity;
    }

    private static EntityType<?> undeadTypeFor(MobEntity mob) {
        if (mob instanceof PigEntity) return EntityType.ZOMBIFIED_PIGLIN;
        if (mob instanceof AbstractHorseEntity) return EntityType.ZOMBIE_HORSE;
        if (mob instanceof VillagerEntity) return EntityType.ZOMBIE_VILLAGER;
        return EntityType.ZOMBIE;
    }

    private static void bloodRay(ServerWorld world, Vec3d at) {
        for (int i = 0; i < 24; i++) {
            world.spawnParticles(BLOOD_DUST, at.x, at.y + i * 0.4, at.z, 3, 0.06, 0.0, 0.06, 0.0);
        }
        world.spawnParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 12, 0.3, 0.5, 0.3, 0.02);
        world.playSound(null, at.x, at.y, at.z,
                SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 1.0f, 0.7f);
    }

    /** Attach pet behavior: follow the owner, fight monsters, never hurt the player. */
    public static void makePet(MobEntity mob, UUID owner) {
        mob.targetSelector.clear(goal -> true);
        mob.targetSelector.add(1, new PetTargetGoal(mob));
        mob.goalSelector.add(4, new FollowOwnerGoal(mob, owner));
        mob.setTarget(null);
        mob.setPersistent();
        // No burning in daylight.
        mob.addStatusEffect(new StatusEffectInstance(
                StatusEffects.FIRE_RESISTANCE, StatusEffectInstance.INFINITE, 0, false, false));
    }

    /** Attacks hostile monsters, but never other pets. */
    private static class PetTargetGoal extends ActiveTargetGoal<HostileEntity> {
        PetTargetGoal(MobEntity mob) {
            super(mob, HostileEntity.class, true);
        }

        @Override
        public void start() {
            super.start();
            LivingEntity target = this.mob.getTarget();
            if (target != null && ownerOf(target) != null) {
                this.mob.setTarget(null);
            }
        }
    }

    private static class FollowOwnerGoal extends Goal {
        private final MobEntity mob;
        private final UUID ownerId;
        private PlayerEntity owner;
        private int recalc;

        FollowOwnerGoal(MobEntity mob, UUID ownerId) {
            this.mob = mob;
            this.ownerId = ownerId;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            PlayerEntity p = mob.getWorld().getPlayerByUuid(ownerId);
            if (p == null || p.isSpectator()) return false;
            this.owner = p;
            return mob.squaredDistanceTo(p) > 36.0; // start following beyond 6 blocks
        }

        @Override
        public boolean shouldContinue() {
            return owner != null && owner.isAlive() && mob.squaredDistanceTo(owner) > 9.0;
        }

        @Override
        public void start() {
            recalc = 0;
        }

        @Override
        public void tick() {
            mob.getLookControl().lookAt(owner, 30.0f, 30.0f);
            if (--recalc > 0) return;
            recalc = 10;
            if (mob.squaredDistanceTo(owner) > 576.0) { // farther than 24 blocks: catch up instantly
                mob.refreshPositionAfterTeleport(owner.getX(), owner.getY(), owner.getZ());
                mob.getNavigation().stop();
            } else {
                mob.getNavigation().startMovingTo(owner, 1.2);
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }
    }
}
