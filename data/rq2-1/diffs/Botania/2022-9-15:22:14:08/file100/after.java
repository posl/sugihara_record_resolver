/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.item.equipment.bauble;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import vazkii.botania.xplat.XplatAbstractions;

public class RingOfFarReachItem extends BaubleItem {

	public RingOfFarReachItem(Properties props) {
		super(props);
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getEquippedAttributeModifiers(ItemStack stack) {
		Multimap<Attribute, AttributeModifier> attributes = HashMultimap.create();
		attributes.put(XplatAbstractions.INSTANCE.getReachDistanceAttribute(),
				new AttributeModifier(getBaubleUUID(stack), "Reach Ring", 3.5, AttributeModifier.Operation.ADDITION));
		return attributes;
	}
}
