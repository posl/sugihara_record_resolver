package com.simibubi.create.compat.computercraft;

import java.util.function.Function;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.implementation.ComputerBehaviour;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;

public class ComputerCraftProxy {

	public static void register() {
		fallbackFactory = FallbackComputerBehaviour::new;
		Mods.COMPUTERCRAFT.executeIfInstalled(() -> ComputerCraftProxy::registerWithDependency);
	}
	
	private static void registerWithDependency() {
		/* Comment if computercraft.implementation is not in the source set */
		computerFactory = ComputerBehaviour::new;
	}

	private static Function<SmartTileEntity, ? extends AbstractComputerBehaviour> fallbackFactory;
	private static Function<SmartTileEntity, ? extends AbstractComputerBehaviour> computerFactory;

	public static AbstractComputerBehaviour behaviour(SmartTileEntity ste) {
		if (computerFactory == null)
			return fallbackFactory.apply(ste);
		return computerFactory.apply(ste);
	}

}
