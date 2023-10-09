/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.item.lens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import vazkii.botania.api.internal.IManaBurst;
import vazkii.botania.api.mana.*;
import vazkii.botania.common.Botania;
import vazkii.botania.common.core.helper.ColorHelper;
import vazkii.botania.common.core.helper.ItemNBTHelper;
import vazkii.botania.common.item.ModItems;

import javax.annotation.Nonnull;

import java.util.List;

public class ItemLens extends Item implements ILensControl, ICompositableLens, ITinyPlanetExcempt {
	public static final int PROP_NONE = 0,
			PROP_POWER = 1,
			PROP_ORIENTATION = 1 << 1,
			PROP_TOUCH = 1 << 2,
			PROP_INTERACTION = 1 << 3,
			PROP_DAMAGE = 1 << 4,
			PROP_CONTROL = 1 << 5;

	private static final String TAG_COLOR = "color";
	private static final String TAG_COMPOSITE_LENS = "compositeLens";

	private final Lens lens;
	private final int props;

	public ItemLens(Item.Properties builder, Lens lens, int props) {
		super(builder);
		this.lens = lens;
		this.props = props;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> stacks, TooltipFlag flags) {
		int storedColor = getStoredColor(stack);
		if (storedColor != -1) {
			TranslatableComponent colorName = new TranslatableComponent(storedColor == 16 ? "botania.color.rainbow" : "color.minecraft." + DyeColor.byId(storedColor));
			TextColor realColor = TextColor.fromRgb(getLensColor(stack));
			stacks.add(new TranslatableComponent("botaniamisc.color", colorName).withStyle(s -> s.withColor(realColor)));
		}

		if (lens instanceof LensStorm) {
			stacks.add(new TranslatableComponent("botaniamisc.creative").withStyle(ChatFormatting.GRAY));
		}
	}

	@Nonnull
	@Override
	public Component getName(@Nonnull ItemStack stack) {
		ItemStack compositeLens = getCompositeLens(stack);
		if (compositeLens.isEmpty()) {
			return super.getName(stack);
		}
		String shortKeyA = stack.getDescriptionId() + ".short";
		String shortKeyB = compositeLens.getDescriptionId() + ".short";
		return new TranslatableComponent("item.botania.composite_lens", new TranslatableComponent(shortKeyA), new TranslatableComponent(shortKeyB));
	}

	@Override
	public void apply(ItemStack stack, BurstProperties props) {
		int storedColor = getStoredColor(stack);
		if (storedColor != -1) {
			props.color = getLensColor(stack);
		}

		getLens(stack).apply(stack, props);

		ItemStack compositeLens = getCompositeLens(stack);
		if (!compositeLens.isEmpty() && compositeLens.getItem() instanceof ILens) {
			((ILens) compositeLens.getItem()).apply(compositeLens, props);
		}
	}

	@Override
	public boolean collideBurst(IManaBurst burst, HitResult pos, boolean isManaBlock, boolean shouldKill, ItemStack stack) {
		shouldKill = getLens(stack).collideBurst(burst, pos, isManaBlock, shouldKill, stack);

		ItemStack compositeLens = getCompositeLens(stack);
		if (!compositeLens.isEmpty() && compositeLens.getItem() instanceof ILens) {
			shouldKill = ((ILens) compositeLens.getItem()).collideBurst(burst, pos, isManaBlock, shouldKill, compositeLens);
		}

		return shouldKill;
	}

	@Override
	public void updateBurst(IManaBurst burst, ItemStack stack) {
		int storedColor = getStoredColor(stack);

		if (storedColor == 16 && burst.entity().level.isClientSide) {
			burst.setColor(getLensColor(stack));
		}

		getLens(stack).updateBurst(burst, stack);

		ItemStack compositeLens = getCompositeLens(stack);
		if (!compositeLens.isEmpty() && compositeLens.getItem() instanceof ILens) {
			((ILens) compositeLens.getItem()).updateBurst(burst, compositeLens);
		}
	}

	@Override
	public int getLensColor(ItemStack stack) {
		int storedColor = getStoredColor(stack);

		if (storedColor == -1) {
			return 0xFFFFFF;
		}

		if (storedColor == 16) {
			return Mth.hsvToRgb(Botania.proxy.getWorldElapsedTicks() * 2 % 360 / 360F, 1F, 1F);
		}

		return ColorHelper.getColorValue(DyeColor.byId(storedColor));
	}

	public static int getStoredColor(ItemStack stack) {
		return ItemNBTHelper.getInt(stack, TAG_COLOR, -1);
	}

	public static void setLensColor(ItemStack stack, int color) {
		ItemNBTHelper.setInt(stack, TAG_COLOR, color);
	}

	@Override
	public boolean doParticles(IManaBurst burst, ItemStack stack) {
		return true;
	}

	public static boolean isBlacklisted(ItemStack lens1, ItemStack lens2) {
		ICompositableLens item1 = (ICompositableLens) lens1.getItem();
		ICompositableLens item2 = (ICompositableLens) lens2.getItem();
		return (item1.getProps(lens1) & item2.getProps(lens2)) != 0;
	}

	public static Lens getLens(ItemStack stack) {
		if (stack.getItem() instanceof ItemLens) {
			return ((ItemLens) stack.getItem()).lens;
		} else {
			return new Lens();
		}
	}

	@Override
	public boolean canCombineLenses(ItemStack sourceLens, ItemStack compositeLens) {
		ICompositableLens sourceItem = (ICompositableLens) sourceLens.getItem();
		ICompositableLens compositeItem = (ICompositableLens) compositeLens.getItem();
		if (sourceItem == compositeItem) {
			return false;
		}

		if (!sourceItem.isCombinable(sourceLens) || !compositeItem.isCombinable(compositeLens)) {
			return false;
		}

		if (isBlacklisted(sourceLens, compositeLens)) {
			return false;
		}

		return true;
	}

	@Override
	public ItemStack getCompositeLens(ItemStack stack) {
		CompoundTag cmp = ItemNBTHelper.getCompound(stack, TAG_COMPOSITE_LENS, true);
		if (cmp == null) {
			return ItemStack.EMPTY;
		} else {
			return ItemStack.of(cmp);
		}
	}

	@Override
	public ItemStack setCompositeLens(ItemStack sourceLens, ItemStack compositeLens) {
		if (compositeLens.isEmpty()) {
			ItemNBTHelper.removeEntry(sourceLens, TAG_COMPOSITE_LENS);
		} else {
			CompoundTag cmp = compositeLens.save(new CompoundTag());
			ItemNBTHelper.setCompound(sourceLens, TAG_COMPOSITE_LENS, cmp);
		}
		return sourceLens;
	}

	@Override
	public int getManaToTransfer(IManaBurst burst, ItemStack stack, IManaReceiver receiver) {
		return getLens(stack).getManaToTransfer(burst, stack, receiver);
	}

	@Override
	public boolean shouldPull(ItemStack stack) {
		return !stack.is(ModItems.lensStorm);
	}

	@Override
	public boolean isControlLens(ItemStack stack) {
		return (getProps(stack) & PROP_CONTROL) != 0;
	}

	@Override
	public boolean allowBurstShooting(ItemStack stack, IManaSpreader spreader, boolean redstone) {
		return getLens(stack).allowBurstShooting(stack, spreader, redstone);
	}

	@Override
	public void onControlledSpreaderTick(ItemStack stack, IManaSpreader spreader, boolean redstone) {
		getLens(stack).onControlledSpreaderTick(stack, spreader, redstone);
	}

	@Override
	public void onControlledSpreaderPulse(ItemStack stack, IManaSpreader spreader) {
		getLens(stack).onControlledSpreaderPulse(stack, spreader);
	}

	@Override
	public int getProps(ItemStack stack) {
		return props;
	}

	@Override
	public boolean isCombinable(ItemStack stack) {
		return !stack.is(ModItems.lensNormal);
	}

}
