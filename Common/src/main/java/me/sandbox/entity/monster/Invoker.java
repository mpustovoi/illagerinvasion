package me.sandbox.entity.monster;

import fuzs.illagerinvasion.init.ModRegistry;
import me.sandbox.util.SpellParticleUtil;
import me.sandbox.util.TeleportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Invoker extends SpellcasterIllager implements PowerableMob {
    private static final EntityDataAccessor<Boolean> SHIELDED = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bossBar = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS).setDarkenScreen(true);
    public boolean inSecondPhase = false;
    public int cooldown;
    public int tpcooldown;
    public boolean isAoeCasting = false;
    public int fangaoecooldown;
    public boolean isShielded = false;
    public boolean currentlyShielded;
    public int shieldduration;
    public boolean canCastShield;
    public int damagecount;
    @Nullable
    private Sheep wololoTarget;
    private AttributeMap attributeContainer;

    public Invoker(EntityType<? extends Invoker> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 80;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtTargetOrWololoTarget());
        this.goalSelector.addGoal(3, new AvoidEntityGoal<Player>(this, Player.class, 8.0f, 0.6, 1.0));
        this.goalSelector.addGoal(5, new AreaDamageGoal());
        this.goalSelector.addGoal(4, new CastTeleportGoal());
        this.goalSelector.addGoal(5, new SummonVexGoal());
        this.goalSelector.addGoal(5, new ConjureAoeFangsGoal());
        this.goalSelector.addGoal(6, new ConjureFangsGoal());
        this.goalSelector.addGoal(6, new WololoGoal());
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0f, 1.0f));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>(this, Player.class, true).setUnseenMemoryTicks(300));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<AbstractVillager>(this, AbstractVillager.class, false).setUnseenMemoryTicks(300));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<IronGolem>(this, IronGolem.class, false));
    }

    @Override
    public boolean isPowered() {
        return this.getShieldedState();
    }

    @Override
    public AttributeMap getAttributes() {
        if (this.attributeContainer == null) {
            this.attributeContainer = new AttributeMap(Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 250.0D).add(Attributes.MOVEMENT_SPEED, 0.36D).add(Attributes.KNOCKBACK_RESISTANCE, 0.3D).add(Attributes.ATTACK_DAMAGE, 8.0D).build());
        }
        return this.attributeContainer;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!super.doHurtTarget(target)) {
            return false;
        }
        if (target instanceof LivingEntity) {
            ((LivingEntity) target).addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0), this);
        }
        return true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SHIELDED, false);
        super.defineSynchedData();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setShieldedState(nbt.getBoolean("Invul"));
        if (this.hasCustomName()) {
            this.bossBar.setName(this.getDisplayName());
        }
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossBar.setName(this.getDisplayName());
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return SoundEvents.EVOKER_CELEBRATE;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putBoolean("Invul", this.isShielded);
        super.addAdditionalSaveData(nbt);
    }

    public boolean getShieldedState() {
        return this.entityData.get(SHIELDED);
    }

    public void setShieldedState(boolean isShielded) {
        this.entityData.set(SHIELDED, isShielded);
    }

    @Override
    protected void customServerAiStep() {
        --this.tpcooldown;
        --this.cooldown;
        --this.fangaoecooldown;
        super.customServerAiStep();
        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.isAoeCasting && this.isCastingSpell()) {
            SpellParticleUtil.sendSpellParticles(this, (ServerLevel) this.level(), ParticleTypes.SMOKE, 2, 0.06D);
        }
        if (this.damagecount >= 2) {
            this.setShieldedState(true);
        }
        if (this.getShieldedState()) {
            if (this.level() instanceof ServerLevel) {
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.5, this.getZ(), 1, 0.5D, 0.7D, 0.5D, 0.15D);
            }
        }
        Vec3 vec3d = this.getDeltaMovement();
        if (!this.onGround() && vec3d.y < 0.0) {
            this.setDeltaMovement(vec3d.multiply(1.0, 0.6, 1.0));
        }
        if (this.level() instanceof ServerLevel) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.3, this.getZ(), 1, 0.2D, 0.2D, 0.2D, 0.005D);
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (super.isAlliedTo(other)) {
            return true;
        }
        if (other instanceof Surrendered) {
            return this.isAlliedTo(((Surrendered) other).getOwner());
        }
        if (other instanceof LivingEntity && ((LivingEntity) other).getMobType() == MobType.ILLAGER) {
            return this.getTeam() == null && other.getTeam() == null;
        }
        return false;
    }

    @Override
    public AbstractIllager.IllagerArmPose getArmPose() {
        if (this.isAggressive()) {
            return AbstractIllager.IllagerArmPose.ATTACKING;
        }
        if (this.isCastingSpell()) {
            return IllagerArmPose.SPELLCASTING;
        }
        if (this.isCelebrating()) {
            return IllagerArmPose.CELEBRATING;
        }
        return AbstractIllager.IllagerArmPose.CROSSED;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if ((source.getDirectEntity()) instanceof AbstractArrow) {
            if (this.getShieldedState()) {
                return false;
            } else {
                this.damagecount++;
            }
        }
        if (!((source.getDirectEntity()) instanceof AbstractArrow)) {
            if (this.getShieldedState()) {
                if ((source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE))) {
                    return false;
                } else {
                    if (this.level() instanceof ServerLevel) {
                        ((ServerLevel) this.level()).sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1, this.getZ(), 30, 0.5D, 0.7D, 0.5D, 0.5D);
                    }
                    this.playSound(ModRegistry.INVOKER_SHIELD_BREAK_SOUND_EVENT, 1.0f, 1.0f);
                    this.setShieldedState(false);
                    this.damagecount = 0;
                }
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModRegistry.INVOKER_AMBIENT_SOUND_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModRegistry.INVOKER_DEATH_SOUND_EVENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModRegistry.INVOKER_HURT_SOUND_EVENT;
    }

    @Nullable Sheep getWololoTarget() {
        return this.wololoTarget;
    }

    void setWololoTarget(@Nullable Sheep sheep) {
        this.wololoTarget = sheep;
    }

    @Override
    protected SoundEvent getCastingSoundEvent() {
        return ModRegistry.INVOKER_COMPLETE_CAST_SOUND_EVENT;
    }

    @Override
    public void applyRaidBuffs(int wave, boolean unused) {
    }


    class LookAtTargetOrWololoTarget extends SpellcasterIllager.SpellcasterCastingSpellGoal {
        LookAtTargetOrWololoTarget() {
        }

        @Override
        public void tick() {
            if (Invoker.this.getTarget() != null) {
                Invoker.this.getLookControl().setLookAt(Invoker.this.getTarget(), Invoker.this.getMaxHeadYRot(), Invoker.this.getMaxHeadXRot());
            } else if (Invoker.this.getWololoTarget() != null) {
                Invoker.this.getLookControl().setLookAt(Invoker.this.getWololoTarget(), Invoker.this.getMaxHeadYRot(), Invoker.this.getMaxHeadXRot());
            }
        }
    }

    class SummonVexGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
        private final TargetingConditions closeVexPredicate = TargetingConditions.forNonCombat().range(16.0).ignoreLineOfSight().ignoreInvisibilityTesting();

        SummonVexGoal() {
        }

        @Override
        public boolean canUse() {
            if (!super.canUse()) {
                return false;
            }
            if (Invoker.this.inSecondPhase) {
                return false;
            }
            int i = Invoker.this.level().getNearbyEntities(Surrendered.class, this.closeVexPredicate, Invoker.this, Invoker.this.getBoundingBox().inflate(20.0)).size();
            return 3 > i;
        }

        @Override
        protected int getCastingTime() {
            return 100;
        }

        @Override
        protected int getCastingInterval() {
            return 340;
        }

        @Override
        protected void performSpellCasting() {
            ServerLevel serverWorld = (ServerLevel) Invoker.this.level();
            for (int i = 0; i < 4; ++i) {
                BlockPos blockPos = Invoker.this.blockPosition().offset(-2 + Invoker.this.random.nextInt(5), 1, -2 + Invoker.this.random.nextInt(5));
                Surrendered surrendered = EntityRegistry.SURRENDERED.create(Invoker.this.level());
                surrendered.moveTo(blockPos, 0.0f, 0.0f);
                surrendered.finalizeSpawn(serverWorld, Invoker.this.level().getCurrentDifficultyAt(blockPos), MobSpawnType.MOB_SUMMONED, null, null);
                surrendered.setOwner(Invoker.this);
                surrendered.setBounds(blockPos);
                surrendered.setLifeTicks(20 * (30 + Invoker.this.random.nextInt(90)));
                serverWorld.addFreshEntityWithPassengers(surrendered);
            }
        }

        @Override
        protected SoundEvent getSpellPrepareSound() {
            return ModRegistry.INVOKER_SUMMON_CAST_SOUND_EVENT;
        }

        @Override
        protected SpellcasterIllager.IllagerSpell getSpell() {
            return SpellcasterIllager.IllagerSpell.SUMMON_VEX;
        }
    }

    class ConjureFangsGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
        ConjureFangsGoal() {
        }

        public boolean canUse() {
            if (!super.canUse()) {
                return false;
            }
            return !Invoker.this.inSecondPhase;
        }

        @Override
        protected int getCastingTime() {
            return 40;
        }

        @Override
        protected int getCastingInterval() {
            return 100;
        }

        @Override
        protected void performSpellCasting() {
            LivingEntity livingEntity = Invoker.this.getTarget();
            double d = Math.min(livingEntity.getY(), Invoker.this.getY());
            double e = Math.max(livingEntity.getY(), Invoker.this.getY()) + 1.0;
            float f = (float) Mth.atan2(livingEntity.getZ() - Invoker.this.getZ(), livingEntity.getX() - Invoker.this.getX());

            if (Invoker.this.distanceToSqr(livingEntity) < 9.0) {
                float g;
                int i;
                for (i = 0; i < 5; ++i) {
                    g = f + (float) i * (float) Math.PI * 0.4f;
                    this.conjureFangs(Invoker.this.getX() + (double) Mth.cos(g) * 1.5, Invoker.this.getZ() + (double) Mth.sin(g) * 1.5, d, e, g, 0);
                }
                for (i = 0; i < 8; ++i) {
                    g = f + (float) i * (float) Math.PI * 2.0f / 8.0f + 1.2566371f;
                    this.conjureFangs(Invoker.this.getX() + (double) Mth.cos(g) * 2.5, Invoker.this.getZ() + (double) Mth.sin(g) * 2.5, d, e, g, 3);
                }
                for (i = 0; i < 8; ++i) {
                    g = f + (float) i * (float) Math.PI * 2.0f / 8.0f + 1.2566371f;
                    this.conjureFangs(Invoker.this.getX() + (double) Mth.cos(g) * 3.5, Invoker.this.getZ() + (double) Mth.sin(g) * 2.5, d, e, g, 3);
                }
            } else {
                for (int i = 0; i < 16; ++i) {
                    double h = 1.25 * (double) (i + 1);
                    this.conjureFangs(Invoker.this.getX() + (double) Mth.cos(f) * h, Invoker.this.getZ() + (double) Mth.sin(f) * h, d, e, f, i);
                }
                for (int i = 0; i < 16; ++i) {
                    double h = 1.25 * (double) (i + 1);
                    this.conjureFangs(Invoker.this.getX() + (double) Mth.cos(f + 0.4f) * h, Invoker.this.getZ() + (double) Mth.sin(f + 0.3f) * h, d, e, f, i);
                }
                for (int i = 0; i < 16; ++i) {
                    double h = 1.25 * (double) (i + 1);
                    this.conjureFangs(Invoker.this.getX() + (double) Mth.cos(f - 0.4f) * h, Invoker.this.getZ() + (double) Mth.sin(f - 0.3f) * h, d, e, f, i);
                }
            }
        }

        private void conjureFangs(double x, double z, double maxY, double y, float yaw, int warmup) {
            BlockPos blockPos = BlockPos.containing(x, y, z);
            boolean bl = false;
            double d = 0.0;
            do {
                BlockState blockState2;
                VoxelShape voxelShape;
                BlockPos blockPos2;
                BlockState blockState;
                if (!(blockState = Invoker.this.level().getBlockState(blockPos2 = blockPos.below())).isFaceSturdy(Invoker.this.level(), blockPos2, Direction.UP))
                    continue;
                if (!Invoker.this.level().isEmptyBlock(blockPos) && !(voxelShape = (blockState2 = Invoker.this.level().getBlockState(blockPos)).getCollisionShape(Invoker.this.level(), blockPos)).isEmpty()) {
                    d = voxelShape.max(Direction.Axis.Y);
                }
                bl = true;
                break;
            } while ((blockPos = blockPos.below()).getY() >= Mth.floor(maxY) - 1);
            if (bl) {
                Invoker.this.level().addFreshEntity(new InvokerFangs(Invoker.this.level(), x, (double) blockPos.getY() + 0.2 + d, z, yaw, warmup, Invoker.this));
            }
        }

        @Override
        protected SoundEvent getSpellPrepareSound() {
            return ModRegistry.INVOKER_FANGS_CAST_SOUND_EVENT;
        }

        @Override
        protected SpellcasterIllager.IllagerSpell getSpell() {
            return IllagerSpell.FANGS;
        }
    }

    public class WololoGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
        private final TargetingConditions convertibleSheepPredicate = TargetingConditions.forNonCombat().range(16.0).selector(livingEntity -> ((Sheep) livingEntity).getColor() == DyeColor.BLUE);

        @Override
        public boolean canUse() {
            if (Invoker.this.getTarget() != null) {
                return false;
            }
            if (Invoker.this.isCastingSpell()) {
                return false;
            }
            if (Invoker.this.tickCount < this.nextAttackTickCount) {
                return false;
            }
            if (!Invoker.this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                return false;
            }
            List<Sheep> list = Invoker.this.level().getNearbyEntities(Sheep.class, this.convertibleSheepPredicate, Invoker.this, Invoker.this.getBoundingBox().inflate(16.0, 4.0, 16.0));
            if (list.isEmpty()) {
                return false;
            }
            Invoker.this.setWololoTarget(list.get(Invoker.this.random.nextInt(list.size())));
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return Invoker.this.getWololoTarget() != null && this.attackWarmupDelay > 0;
        }

        @Override
        public void stop() {
            super.stop();
            Invoker.this.setWololoTarget(null);
        }

        @Override
        protected void performSpellCasting() {
            Sheep sheepEntity = Invoker.this.getWololoTarget();
            if (sheepEntity != null && sheepEntity.isAlive()) {
                sheepEntity.setColor(DyeColor.RED);
            }
        }

        @Override
        protected int getCastWarmupTime() {
            return 40;
        }

        @Override
        protected int getCastingTime() {
            return 140;
        }

        @Override
        protected int getCastingInterval() {
            return 600;
        }

        @Override
        protected SoundEvent getSpellPrepareSound() {
            return SoundEvents.EVOKER_PREPARE_WOLOLO;
        }

        @Override
        protected SpellcasterIllager.IllagerSpell getSpell() {
            return SpellcasterIllager.IllagerSpell.WOLOLO;
        }
    }

    public class AreaDamageGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {

        @Override
        public boolean canUse() {
            if (Invoker.this.getTarget() == null) {
                return false;
            }
            if (Invoker.this.cooldown < 0) {
                Invoker.this.isAoeCasting = true;
                return true;
            }
            return false;
        }

        private List<LivingEntity> getTargets() {
            return Invoker.this.level().getEntitiesOfClass(LivingEntity.class, Invoker.this.getBoundingBox().inflate(6), entity -> !(entity instanceof AbstractIllager) && !(entity instanceof Surrendered) && !(entity instanceof Ravager));
        }

        private void knockBack(Entity entity) {
            double d = entity.getX() - Invoker.this.getX();
            double e = entity.getZ() - Invoker.this.getZ();
            double f = Math.max(d * d + e * e, 0.001);
            entity.push(d / f * 6, 0.65, e / f * 6);
        }

        protected void knockback(LivingEntity target) {
            this.knockBack(target);
            target.hurtMarked = true;
        }


        @Override
        public void stop() {
            Invoker.this.isAoeCasting = false;
            super.stop();
        }

        private void buff(LivingEntity entity) {
            this.knockback(entity);
            entity.hurt(Invoker.this.damageSources().magic(), 11.0f);
            double x = entity.getX();
            double y = entity.getY() + 1;
            double z = entity.getZ();
            ((ServerLevel) Invoker.this.level()).sendParticles(ParticleTypes.SMOKE, x, y + 1, z, 10, 0.2D, 0.2D, 0.2D, 0.015D);
        }

        @Override
        protected void performSpellCasting() {
            Invoker.this.cooldown = 300;
            this.getTargets().forEach(this::buff);
            Invoker.this.isAoeCasting = false;
            double posx = Invoker.this.getX();
            double posy = Invoker.this.getY();
            double posz = Invoker.this.getZ();
            ((ServerLevel) Invoker.this.level()).sendParticles(ParticleTypes.LARGE_SMOKE, posx, posy + 1, posz, 350, 1.0D, 0.8D, 1.0D, 0.3D);
        }

        @Override
        protected int getCastWarmupTime() {
            return 50;
        }

        @Override
        protected int getCastingTime() {
            return 50;
        }

        @Override
        protected int getCastingInterval() {
            return 400;
        }

        @Override
        protected SoundEvent getSpellPrepareSound() {
            return ModRegistry.INVOKER_BIG_CAST_SOUND_EVENT;
        }

        @Override
        protected SpellcasterIllager.IllagerSpell getSpell() {
            return IllagerSpell.BLINDNESS;
        }
    }

    public class CastTeleportGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
        Invoker sorcerer = Invoker.this;

        @Override
        public boolean canUse() {
            if (Invoker.this.getTarget() == null) {
                return false;
            }
            if (Invoker.this.isCastingSpell()) {
                return false;
            }
            return Invoker.this.tpcooldown < 0 && !(this.getTargets().isEmpty());
        }

        private List<LivingEntity> getTargets() {
            return Invoker.this.level().getEntitiesOfClass(LivingEntity.class, Invoker.this.getBoundingBox().inflate(6), entity -> ((entity instanceof Player && !((Player) entity).getAbilities().instabuild)) || (entity instanceof IronGolem));
        }

        @Override
        public boolean canContinueToUse() {
            return !this.getTargets().isEmpty();
        }

        @Override
        public void stop() {
            super.stop();
        }

        @Override
        public void start() {
            super.start();
            Invoker.this.tpcooldown = 180;
        }

        @Override
        protected void performSpellCasting() {
            double x = this.sorcerer.getX();
            double y = this.sorcerer.getY() + 1;
            double z = this.sorcerer.getZ();
            if (this.sorcerer.level() instanceof ServerLevel) {
                ((ServerLevel) Invoker.this.level()).sendParticles(ParticleTypes.SMOKE, x, y, z, 30, 0.3D, 0.5D, 0.3D, 0.015D);
            }
            TeleportUtil.tryRandomTeleport(Invoker.this);
        }

        @Override
        protected int getCastWarmupTime() {
            return 30;
        }

        @Override
        protected int getCastingTime() {
            return 30;
        }

        @Override
        protected int getCastingInterval() {
            return 400;
        }

        @Override
        protected SoundEvent getSpellPrepareSound() {
            return ModRegistry.INVOKER_TELEPORT_CAST_SOUND_EVENT;
        }

        @Override
        protected SpellcasterIllager.IllagerSpell getSpell() {
            return IllagerSpell.BLINDNESS;
        }
    }

    public class ConjureAoeFangsGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {

        @Override
        public boolean canUse() {
            if (Invoker.this.getTarget() == null) {
                return false;
            }
            if (this.getTargets().isEmpty()) {
                return false;
            }
            if (Invoker.this.isCastingSpell()) {
                return false;
            }
            return Invoker.this.fangaoecooldown < 0;
        }

        private List<LivingEntity> getTargets() {
            return Invoker.this.level().getEntitiesOfClass(LivingEntity.class, Invoker.this.getBoundingBox().inflate(18), entity -> !(entity instanceof Monster));
        }

        private void conjureFangs(double x, double z, double maxY, double y, float yaw, int warmup) {
            BlockPos blockPos = BlockPos.containing(x, y, z);
            boolean bl = false;
            double d = 0.0;
            do {
                BlockState blockState2;
                VoxelShape voxelShape;
                BlockPos blockPos2;
                BlockState blockState;
                if (!(blockState = Invoker.this.level().getBlockState(blockPos2 = blockPos.below())).isFaceSturdy(Invoker.this.level(), blockPos2, Direction.UP))
                    continue;
                if (!Invoker.this.level().isEmptyBlock(blockPos) && !(voxelShape = (blockState2 = Invoker.this.level().getBlockState(blockPos)).getCollisionShape(Invoker.this.level(), blockPos)).isEmpty()) {
                    d = voxelShape.max(Direction.Axis.Y);
                }
                bl = true;
                break;
            } while ((blockPos = blockPos.below()).getY() >= Mth.floor(maxY) - 1);
            if (bl) {
                Invoker.this.level().addFreshEntity(new InvokerFangs(Invoker.this.level(), x, (double) blockPos.getY() + 0.2 + d, z, yaw, warmup + 4, Invoker.this));
            }
        }

        @Override
        public void stop() {
            super.stop();
        }

        @Override
        protected void performSpellCasting() {
            for (LivingEntity livingEntity : this.getTargets()) {
                double d = Math.min(livingEntity.getY(), Invoker.this.getY());
                double e = Math.max(livingEntity.getY(), Invoker.this.getY()) + 1.0;
                float f = (float) Mth.atan2(livingEntity.getZ() - Invoker.this.getZ(), livingEntity.getX() - Invoker.this.getX());
                float g;
                int i;
                for (i = 0; i < 5; ++i) {
                    g = f + (float) i * (float) Math.PI * 0.4f;
                    this.conjureFangs(livingEntity.getX() + (double) Mth.cos(g) * 1.5, livingEntity.getZ() + (double) Mth.sin(g) * 1.5, d, e, g, 0);
                }
            }
            Invoker.this.fangaoecooldown = 100;
        }

        @Override
        protected int getCastingTime() {
            return 40;
        }

        @Override
        protected int getCastingInterval() {
            return 100;
        }

        @Override
        protected SoundEvent getSpellPrepareSound() {
            return ModRegistry.INVOKER_FANGS_CAST_SOUND_EVENT;
        }

        @Override
        protected SpellcasterIllager.IllagerSpell getSpell() {
            return IllagerSpell.FANGS;
        }
    }
}

