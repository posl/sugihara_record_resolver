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
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

public class ItemReachRing extends ItemBauble {

	public ItemReachRing(Properties props) {
		super(props);
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getEquippedAttributeModifiers(ItemStack stack) {
		Multimap<Attribute, AttributeModifier> attributes = HashMultimap.create();
		attributes.put(ReachEntityAttributes.REACH, new AttributeModifier(getBaubleUUID(stack), "Reach Ring", 3.5, AttributeModifier.Operation.ADDITION));
		return attributes;
	}
}
