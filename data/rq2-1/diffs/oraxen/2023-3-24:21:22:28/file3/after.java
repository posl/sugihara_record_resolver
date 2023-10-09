package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenFurnitureBreakEvent extends Event implements Cancellable {

    boolean isCancelled;
    private final FurnitureMechanic mechanic;
    private final Block block;
    private final Player player;
    private final Entity baseEntity;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurnitureBreakEvent(FurnitureMechanic mechanic, Player player, @Nullable Block block, Entity baseEntity) {
        this.block = block;
        this.mechanic = mechanic;
        this.player = player;
        this.baseEntity = baseEntity;
    }

    /**
     * @return The FurnitureMechanic of this Furniture
     */
    public FurnitureMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player that broke the furniture
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the block that was broken.
     * @return block that was broken or null if it was an entity
     */
    @Nullable
    public Block getBlock() {
        return block;
    }

    /**
     * @return The ItemFrame the furniture is inmechanic
     */
    public Entity getBaseEntity() {
        return baseEntity;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
