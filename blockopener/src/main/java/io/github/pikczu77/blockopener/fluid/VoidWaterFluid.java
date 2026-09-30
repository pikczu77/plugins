package io.github.pikczu77.blockopener.fluid;

import io.github.pikczu77.blockopener.registry.ModDamageTypes;
import io.github.pikczu77.blockopener.registry.ModFluids;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * "Black water" poured by the Bedrock Bucket. It behaves like a tiny piece of the void:
 * it swallows dropped items, drags living things down and hurts them through armor.
 * It never makes new sources and only flows a few blocks, so it stays where it is poured.
 */
public abstract class VoidWaterFluid extends FlowingFluid {
	@Override
	public Fluid getFlowing() {
		return ModFluids.FLOWING_VOID_WATER;
	}

	@Override
	public Fluid getSource() {
		return ModFluids.VOID_WATER;
	}

	@Override
	public Item getBucket() {
		// A normal bucket cannot hold the void; see VoidWaterBlock#pickupBlock.
		return Items.AIR;
	}

	@Override
	public void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
		if (random.nextInt(4) == 0) {
			level.addParticle(
				ParticleTypes.REVERSE_PORTAL,
				pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble() * 0.9, pos.getZ() + random.nextDouble(),
				0.0, 0.02, 0.0
			);
		}
		if (random.nextInt(200) == 0) {
			level.playLocalSound(pos, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.15F, 0.5F + random.nextFloat() * 0.3F, false);
		}
	}

	@Override
	public @Nullable ParticleOptions getDripParticle() {
		return ParticleTypes.DRIPPING_OBSIDIAN_TEAR;
	}

	@Override
	protected boolean canConvertToSource(ServerLevel level) {
		return false;
	}

	@Override
	protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
		// The void swallows whatever it flows into, no drops.
	}

	@Override
	protected void entityInside(Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
			serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY() + 0.2, entity.getZ(), 12, 0.15, 0.15, 0.15, 0.05);
			entity.discard();
			return;
		}
		if (!(entity instanceof LivingEntity living) || living instanceof Player player && (player.isCreative() || player.isSpectator())) {
			return;
		}
		// Heavy and sticky: slow sideways, pulled down.
		Vec3 motion = living.getDeltaMovement();
		living.setDeltaMovement(motion.x * 0.5, Math.min(motion.y, 0.0) - 0.03, motion.z * 0.5);
		living.resetFallDistance();
		living.hurtServer(serverLevel, ModDamageTypes.source(serverLevel, ModDamageTypes.VOID_WATER), 4.0F);
		if (serverLevel.getGameTime() % 20 == 0) {
			living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
		}
	}

	@Override
	public int getSlopeFindDistance(LevelReader level) {
		return 2;
	}

	@Override
	public int getDropOff(LevelReader level) {
		return 2;
	}

	@Override
	public int getTickDelay(LevelReader level) {
		return 10;
	}

	@Override
	public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
		return false;
	}

	@Override
	protected float getExplosionResistance() {
		return 100.0F;
	}

	@Override
	public BlockState createLegacyBlock(FluidState state) {
		return ModFluids.VOID_WATER_BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
	}

	@Override
	public boolean isSame(Fluid fluid) {
		return fluid == ModFluids.VOID_WATER || fluid == ModFluids.FLOWING_VOID_WATER;
	}

	@Override
	public Optional<SoundEvent> getPickupSound() {
		return Optional.of(SoundEvents.BUCKET_FILL);
	}

	public static class Flowing extends VoidWaterFluid {
		@Override
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		@Override
		public int getAmount(FluidState state) {
			return state.getValue(LEVEL);
		}

		@Override
		public boolean isSource(FluidState state) {
			return false;
		}
	}

	public static class Source extends VoidWaterFluid {
		@Override
		public int getAmount(FluidState state) {
			return 8;
		}

		@Override
		public boolean isSource(FluidState state) {
			return true;
		}
	}
}
