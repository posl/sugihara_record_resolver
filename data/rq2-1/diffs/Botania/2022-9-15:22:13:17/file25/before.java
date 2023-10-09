/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.entity;

import com.google.common.base.Predicates;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.NotNull;

import vazkii.botania.api.corporea.ICorporeaNode;
import vazkii.botania.api.corporea.ICorporeaSpark;
import vazkii.botania.common.impl.corporea.DummyCorporeaNode;
import vazkii.botania.common.integration.corporea.CorporeaNodeDetectors;
import vazkii.botania.common.item.ItemTwigWand;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.lib.ModTags;

import java.util.*;

public class EntityCorporeaSpark extends EntitySparkBase implements ICorporeaSpark {
	private static final int SCAN_RANGE = 8;

	private static final String TAG_MASTER = "master";
	private static final String TAG_CREATIVE = "creative";

	private static final EntityDataAccessor<Boolean> MASTER = SynchedEntityData.defineId(EntityCorporeaSpark.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> CREATIVE = SynchedEntityData.defineId(EntityCorporeaSpark.class, EntityDataSerializers.BOOLEAN);

	private ICorporeaSpark master;
	private Set<ICorporeaSpark> connections = new LinkedHashSet<>();
	private List<ICorporeaSpark> relatives = new ArrayList<>();
	private boolean firstTick = true;

	public EntityCorporeaSpark(EntityType<EntityCorporeaSpark> type, Level world) {
		super(type, world);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(MASTER, false);
		entityData.define(CREATIVE, false);
	}

	@NotNull
	@Override
	public ItemStack getPickResult() {
		return new ItemStack(isCreative() ? ModItems.corporeaSparkCreative : isMaster() ? ModItems.corporeaSparkMaster : ModItems.corporeaSpark);
	}

	@Override
	public void tick() {
		super.tick();

		if (level.isClientSide) {
			return;
		}

		ICorporeaNode node = getSparkNode();
		if (node instanceof DummyCorporeaNode && !level.getBlockState(getAttachPos()).is(ModTags.Blocks.CORPOREA_SPARK_OVERRIDE)) {
			dropAndKill();
			return;
		}

		if (isMaster()) {
			master = this;
		}

		if (firstTick) {
			if (isMaster()) {
				restartNetwork();
			} else {
				findNetwork();
			}

			firstTick = false;
		}

		if (master != null && (!master.entity().isAlive() || master.getNetwork() != getNetwork())) {
			master = null;
		}
	}

	private void dropAndKill() {
		spawnAtLocation(new ItemStack(isCreative() ? ModItems.corporeaSparkCreative : isMaster() ? ModItems.corporeaSparkMaster : ModItems.corporeaSpark), 0F);
		discard();
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);
		connections.remove(this);
		restartNetwork();
	}

	@Override
	public void introduceNearbyTo(Set<ICorporeaSpark> network, ICorporeaSpark master) {
		relatives.clear();
		for (ICorporeaSpark spark : getNearbySparks()) {
			if (spark == null || network.contains(spark)
					|| spark.getNetwork() != getNetwork()
					|| spark.isMaster() || !spark.entity().isAlive()) {
				continue;
			}

			network.add(spark);
			relatives.add(spark);
			spark.introduceNearbyTo(network, master);
		}

		this.master = master;
		this.connections = network;
	}

	@SuppressWarnings("unchecked")
	private List<ICorporeaSpark> getNearbySparks() {
		return (List) level.getEntitiesOfClass(Entity.class, new AABB(getX() - SCAN_RANGE, getY() - SCAN_RANGE, getZ() - SCAN_RANGE, getX() + SCAN_RANGE, getY() + SCAN_RANGE, getZ() + SCAN_RANGE), Predicates.instanceOf(ICorporeaSpark.class));
	}

	private void restartNetwork() {
		connections = new LinkedHashSet<>();
		relatives = new ArrayList<>();

		if (master != null) {
			ICorporeaSpark oldMaster = master;
			master = null;

			oldMaster.introduceNearbyTo(new LinkedHashSet<>(), oldMaster);
		}
	}

	private void findNetwork() {
		for (ICorporeaSpark spark : getNearbySparks()) {
			if (spark.getNetwork() == getNetwork() && spark.entity().isAlive()) {
				ICorporeaSpark master = spark.getMaster();
				if (master != null) {
					this.master = master;
					restartNetwork();

					break;
				}
			}
		}
	}

	private static void displayRelatives(Player player, List<ICorporeaSpark> checked, ICorporeaSpark spark) {
		if (spark == null) {
			return;
		}

		List<ICorporeaSpark> sparks = spark.getRelatives();
		if (sparks.isEmpty()) {
			EntityManaSpark.particleBeam(player, spark.entity(), spark.getMaster().entity());
		} else {
			for (ICorporeaSpark endSpark : sparks) {
				if (!checked.contains(endSpark)) {
					EntityManaSpark.particleBeam(player, spark.entity(), endSpark.entity());
					checked.add(endSpark);
					displayRelatives(player, checked, endSpark);
				}
			}
		}
	}

	@Override
	public ICorporeaNode getSparkNode() {
		return CorporeaNodeDetectors.findNode(level, this);
	}

	@Override
	public Set<ICorporeaSpark> getConnections() {
		return connections;
	}

	@Override
	public List<ICorporeaSpark> getRelatives() {
		return relatives;
	}

	@Override
	public void onItemExtracted(ItemStack stack) {
		((ServerLevel) level).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), getX(), getY(), getZ(), 10, 0.125, 0.125, 0.125, 0.05);
	}

	@Override
	public void onItemsRequested(List<ItemStack> stacks) {
		List<Item> shownItems = new ArrayList<>();
		for (ItemStack stack : stacks) {
			if (!shownItems.contains(stack.getItem())) {
				shownItems.add(stack.getItem());
				((ServerLevel) level).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), getX(), getY(), getZ(), 10, 0.125, 0.125, 0.125, 0.05);
			}
		}
	}

	@Override
	public ICorporeaSpark getMaster() {
		return master;
	}

	public void setMaster(boolean master) {
		entityData.set(MASTER, master);
	}

	@Override
	public boolean isMaster() {
		return entityData.get(MASTER);
	}

	public void setCreative(boolean creative) {
		entityData.set(CREATIVE, creative);
	}

	@Override
	public boolean isCreative() {
		return entityData.get(CREATIVE);
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (isAlive() && !stack.isEmpty()) {
			if (stack.getItem() instanceof ItemTwigWand) {
				if (!level.isClientSide) {
					if (player.isShiftKeyDown()) {
						dropAndKill();
						if (isMaster()) {
							restartNetwork();
						}
					} else {
						displayRelatives(player, new ArrayList<>(), master);
					}
				}
				return InteractionResult.sidedSuccess(level.isClientSide);
			} else if (stack.getItem() instanceof DyeItem dye) {
				DyeColor color = dye.getDyeColor();
				if (color != getNetwork()) {
					if (!level.isClientSide) {
						setNetwork(color);

						stack.shrink(1);
					}

					return InteractionResult.sidedSuccess(level.isClientSide);
				}
			} else if (stack.is(ModItems.phantomInk)) {
				if (!level.isClientSide) {
					setInvisible(true);
				}
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
		}

		return InteractionResult.PASS;
	}

	@Override
	public void setNetwork(DyeColor color) {
		if (color == getNetwork()) {
			return;
		}

		super.setNetwork(color);

		// Do not access world during deserialization
		if (!firstTick) {
			if (isMaster()) {
				restartNetwork();
			} else {
				findNetwork();
			}
		}
	}

	@Override
	protected void readAdditionalSaveData(@NotNull CompoundTag cmp) {
		super.readAdditionalSaveData(cmp);
		setMaster(cmp.getBoolean(TAG_MASTER));
		setCreative(cmp.getBoolean(TAG_CREATIVE));
	}

	@Override
	protected void addAdditionalSaveData(@NotNull CompoundTag cmp) {
		super.addAdditionalSaveData(cmp);
		cmp.putBoolean(TAG_MASTER, isMaster());
		cmp.putBoolean(TAG_CREATIVE, isCreative());
	}

}
