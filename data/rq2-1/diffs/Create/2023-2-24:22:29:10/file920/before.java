package com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue;

import java.util.List;
import java.util.function.Function;

import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform;

import net.minecraft.network.chat.Component;

public class BulkScrollValueBehaviour extends ScrollValueBehaviour {

	Function<SmartTileEntity, List<? extends SmartTileEntity>> groupGetter;

	public BulkScrollValueBehaviour(Component label, SmartTileEntity te, ValueBoxTransform slot,
									Function<SmartTileEntity, List<? extends SmartTileEntity>> groupGetter) {
		super(label, te, slot);
		this.groupGetter = groupGetter;
	}

	List<? extends SmartTileEntity> getBulk() {
		return groupGetter.apply(tileEntity);
	}

}
