/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.me.helpers;

import java.util.Optional;

import com.google.common.base.Preconditions;

import net.minecraft.world.entity.player.Player;

import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;

public class PlayerSource implements IActionSource {

    private final Player player;
    private final IActionHost via;

    public PlayerSource(final Player p, final IActionHost v) {
        Preconditions.checkNotNull(p);
        this.player = p;
        this.via = v;
    }

    @Override
    public Optional<Player> player() {
        return Optional.of(this.player);
    }

    @Override
    public Optional<IActionHost> machine() {
        return Optional.ofNullable(this.via);
    }

    @Override
    public <T> Optional<T> context(Class<T> key) {
        return Optional.empty();
    }
}
