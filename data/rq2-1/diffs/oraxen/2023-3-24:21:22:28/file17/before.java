package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import io.papermc.paper.event.entity.EntityInsideBlockEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenStringBlockBreakEvent;
import io.th0rgal.oraxen.api.events.OraxenStringBlockInteractEvent;
import io.th0rgal.oraxen.api.events.OraxenStringBlockPlaceEvent;
import io.th0rgal.oraxen.compatibilities.provided.lightapi.WrappedLightAPI;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import io.th0rgal.oraxen.utils.limitedplacing.LimitedPlacing;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class StringBlockMechanicListener implements Listener {

    private final MechanicFactory factory;

    public StringBlockMechanicListener(final StringBlockMechanicFactory factory) {
        this.factory = factory;
        BreakerSystem.MODIFIERS.add(getHardnessModifier());
        if (OraxenPlugin.get().isPaperServer)
            Bukkit.getPluginManager().registerEvents(new StringBlockMechanicPaperListener(), OraxenPlugin.get());
    }

    public class StringBlockMechanicPaperListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEnteringTripwire(EntityInsideBlockEvent event) {
            if (event.getBlock().getType() == Material.TRIPWIRE)
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void callInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock().getType() != Material.NOTE_BLOCK) return;
        StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
        if (mechanic == null) return;
        OraxenStringBlockInteractEvent oraxenEvent = new OraxenStringBlockInteractEvent(mechanic, block, event.getItem(), event.getPlayer(), event.getBlockFace(), event.getHand());
        Bukkit.getPluginManager().callEvent(oraxenEvent);
        if (oraxenEvent.isCancelled()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void tripwireEvent(BlockPhysicsEvent event) {
        if (event.getChangedType() == Material.TRIPWIRE)
            event.setCancelled(true);

        for (BlockFace f : BlockFace.values()) {
            if (!f.isCartesian() || f.getModY() != 0 || f == BlockFace.SELF) continue; // Only take N/S/W/E
            final Block changed = event.getBlock().getRelative(f);
            if (changed.getType() != Material.TRIPWIRE) continue;

            final BlockData data = changed.getBlockData().clone();
            Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), Runnable ->
                    changed.setBlockData(data, false), 1L);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlacingString(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType() != Material.STRING) return;
        if (StringBlockMechanicFactory.getInstance().disableVanillaString) {
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlacingString(final BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        if (placedBlock.getType() != Material.TRIPWIRE || OraxenItems.exists(event.getItemInHand()))
            return;
        // Placing string, meant for the first blockstate as invisible string
        placedBlock.setBlockData(Bukkit.createBlockData(Material.TRIPWIRE), false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        List<Block> tripwireList = event.getBlocks().stream().filter(block -> block.getType().equals(Material.TRIPWIRE)).toList();

        for (Block block : tripwireList) {
            final StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
            if (mechanic == null) return;

            block.setType(Material.AIR, false);

            OraxenStringBlockBreakEvent stringBlockBreakEvent = new OraxenStringBlockBreakEvent(mechanic, block, null);
            OraxenPlugin.get().getServer().getPluginManager().callEvent(stringBlockBreakEvent);
            if (stringBlockBreakEvent.isCancelled()) return;

            if (mechanic.hasLight())
                WrappedLightAPI.removeBlockLight(block.getLocation());
            mechanic.getDrop().spawns(block.getLocation(), new ItemStack(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLimitedPlacing(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        ItemStack item = event.getItem();

        if (item == null || block == null || event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (block.getType().isInteractable() && block.getType() != Material.NOTE_BLOCK) return;

        StringBlockMechanic mechanic = (StringBlockMechanic) factory.getMechanic(OraxenItems.getIdByItem(item));
        if (mechanic == null || !mechanic.hasLimitedPlacing()) return;

        LimitedPlacing limitedPlacing = mechanic.getLimitedPlacing();
        Block belowPlaced = block.getRelative(blockFace).getRelative(BlockFace.DOWN);

        if (limitedPlacing.isNotPlacableOn(belowPlaced, blockFace)) {
            event.setCancelled(true);
        } else if (limitedPlacing.getType() == LimitedPlacing.LimitedPlacingType.ALLOW) {
            if (!limitedPlacing.checkLimitedMechanic(belowPlaced))
                event.setCancelled(true);
        } else if (limitedPlacing.getType() == LimitedPlacing.LimitedPlacingType.DENY) {
            if (limitedPlacing.checkLimitedMechanic(belowPlaced))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrePlacingCustomBlock(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final ItemStack item = event.getItem();
        final String itemID = OraxenItems.getIdByItem(item);
        final Block placedAgainst = event.getClickedBlock();
        final Player player = event.getPlayer();

        if (placedAgainst == null) return;
        if (placedAgainst.getType().isInteractable() && !player.isSneaking()) {
            if (placedAgainst.getType() == Material.NOTE_BLOCK && OraxenBlocks.getNoteBlockMechanic(placedAgainst) == null)
                return;
            else if (placedAgainst.getType() != Material.NOTE_BLOCK) return;
        }

        if (item != null && item.getType().isBlock() && !factory.isNotImplementedIn(itemID)) {
            for (BlockFace face : BlockFace.values()) {
                if (!face.isCartesian() || face.getModZ() != 0) continue;
                final Block relative = placedAgainst.getRelative(face);
                if (relative.getType() == Material.NOTE_BLOCK)
                    if (OraxenBlocks.getNoteBlockMechanic(relative) == null) continue;
                if (relative.getType() == Material.TRIPWIRE)
                    if (OraxenBlocks.getStringMechanic(relative) == null) continue;
                if (item.getItemMeta() instanceof BlockStateMeta) continue;
                if (item.getType().hasGravity()) continue;
                if (item.getType().toString().endsWith("SLAB")) continue;

                makePlayerPlaceBlock(player, event.getHand(), item, placedAgainst, event.getBlockFace(), Bukkit.createBlockData(item.getType()));
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), Runnable ->
                        fixClientsideUpdate(placedAgainst.getLocation()), 1L);
            }
        }

        if (factory.isNotImplementedIn(itemID)) return;
        // determines the new block data of the block
        StringBlockMechanic mechanic = (StringBlockMechanic) factory.getMechanic(itemID);

        int customVariation = mechanic.getCustomVariation();

        if (mechanic.hasRandomPlace()) {
            List<String> randomList = mechanic.getRandomPlaceBlock();
            String randomBlock = randomList.get(new Random().nextInt(randomList.size()));
            customVariation = ((StringBlockMechanic) factory.getMechanic(randomBlock)).getCustomVariation();
        }

        Block placedBlock = makePlayerPlaceBlock(player, event.getHand(), event.getItem(),
                placedAgainst, event.getBlockFace(),
                StringBlockMechanicFactory.createTripwireData(customVariation));
        if (placedBlock == null) return;

        if (mechanic.isTall())
            placedBlock.getRelative(BlockFace.UP).setType(Material.TRIPWIRE, false);
        if (mechanic.getLight() != -1)
            WrappedLightAPI.createBlockLight(placedBlock.getLocation(), mechanic.getLight());
        if (mechanic.isSapling()) {
            SaplingMechanic sapling = mechanic.getSaplingMechanic();
            if (mechanic.getSaplingMechanic().canGrowNaturally())
                BlockHelpers.getPDC(placedBlock).set(SaplingMechanic.SAPLING_KEY, PersistentDataType.INTEGER, sapling.getNaturalGrowthTime());
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent event) {
        final Block block = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && block != null
                && block.getType() == Material.TRIPWIRE) {
            ItemStack clicked = event.getItem();
            // Call the event
            StringBlockMechanic stringBlockMechanic = OraxenBlocks.getStringMechanic(block);
            if (stringBlockMechanic == null) return;
            OraxenStringBlockInteractEvent wireBlockInteractEvent = new OraxenStringBlockInteractEvent(stringBlockMechanic, block, event.getItem(), event.getPlayer(), event.getBlockFace(), event.getHand());
            OraxenPlugin.get().getServer().getPluginManager().callEvent(wireBlockInteractEvent);
            if (wireBlockInteractEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            if (clicked == null)
                return;
            Material type = clicked.getType();
            if (clicked.getType().isInteractable())
                return;
            if (type == Material.LAVA_BUCKET)
                type = Material.LAVA;
            if (type == Material.WATER_BUCKET)
                type = Material.WATER;
            if (type.isBlock())
                makePlayerPlaceBlock(event.getPlayer(), event.getHand(), event.getItem(), block, event.getBlockFace(), Bukkit.createBlockData(type));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomBlock(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Block blockAbove = block.getRelative(BlockFace.UP);
        final Block blockBelow = block.getRelative(BlockFace.DOWN);
        final Player player = event.getPlayer();

        for (BlockFace face : BlockFace.values()) {
            if (face == BlockFace.SELF && !face.isCartesian()) continue;
            if (block.getType() == Material.TRIPWIRE || block.getType() == Material.NOTE_BLOCK) break;
            if (block.getRelative(face).getType() == Material.TRIPWIRE) {
                if (player.getGameMode() != GameMode.CREATIVE)
                    for (ItemStack item : block.getDrops())
                        if (item.getType() != Material.AIR)
                            player.getWorld().dropItemNaturally(block.getLocation(), item);
                block.setType(Material.AIR, false);
                if (BlockHelpers.REPLACEABLE_BLOCKS.contains(blockAbove.getType())) blockAbove.breakNaturally();
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), Runnable ->
                        fixClientsideUpdate(block.getLocation()), 1);
            }
        }

        if (block.getType() == Material.TRIPWIRE) {
            StringBlockMechanic mechanicBelow = OraxenBlocks.getStringMechanic(blockBelow);
            if (OraxenBlocks.isOraxenStringBlock(block)) {
                event.setCancelled(true);
                OraxenBlocks.remove(block.getLocation(), player);
                event.setDropItems(false);
            } else if (mechanicBelow != null && mechanicBelow.isTall()) {
                event.setCancelled(true);
                OraxenBlocks.remove(blockBelow.getLocation(), player);
                event.setDropItems(false);
            }
        } else if (blockAbove.getType() == Material.TRIPWIRE) {
            if (!OraxenBlocks.isOraxenStringBlock(blockAbove)) return;
            event.setCancelled(true);

            OraxenBlocks.remove(blockAbove.getLocation(), player);
            block.setType(Material.AIR); // This doesn't affect furniture and noteblock as they are handled by other functions
            event.setDropItems(false);
        }
    }

    @EventHandler
    public void onExplosionDestroy(EntityExplodeEvent event) {
        List<Block> blockList = event.blockList().stream().filter(block -> block.getType().equals(Material.TRIPWIRE)).toList();
        blockList.forEach(block -> {
            final StringBlockMechanic stringBlockMechanic = OraxenBlocks.getStringMechanic(block);
            if (stringBlockMechanic == null) return;

            OraxenStringBlockBreakEvent wireBlockBreakEvent = new OraxenStringBlockBreakEvent(stringBlockMechanic, block, null);
            OraxenPlugin.get().getServer().getPluginManager().callEvent(wireBlockBreakEvent);
            if (wireBlockBreakEvent.isCancelled()) {
                return;
            }

            final Block blockAbove = block.getRelative(BlockFace.UP);
            final Block blockBelow = block.getRelative(BlockFace.DOWN);
            if (block.getType() == Material.TRIPWIRE) {
                StringBlockMechanic mechanicBelow = OraxenBlocks.getStringMechanic(blockBelow);
                if (OraxenBlocks.isOraxenStringBlock(block)) {
                    event.setCancelled(true);
                    OraxenBlocks.remove(block.getLocation(), null);
                } else if (mechanicBelow != null && mechanicBelow.isTall()) {
                    event.setCancelled(true);
                    OraxenBlocks.remove(blockBelow.getLocation(), null);
                }
            } else if (blockAbove.getType() == Material.TRIPWIRE) {
                if (!OraxenBlocks.isOraxenStringBlock(blockAbove)) return;
                event.setCancelled(true);

                OraxenBlocks.remove(blockAbove.getLocation(), null);
                block.setType(Material.AIR); // This doesn't affect furniture and noteblock as they are handled by other functions
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWaterUpdate(final BlockFromToEvent event) {
        if (!event.getBlock().isLiquid()) return;
        for (BlockFace f : BlockFace.values()) {
            if (!f.isCartesian() || f == BlockFace.SELF) continue; // Only take N/S/W/E
            final Block changed = event.getToBlock().getRelative(f);
            final Block changedBelow = changed.getRelative(BlockFace.DOWN);

            if (changed.getType() == Material.TRIPWIRE) {
                StringBlockMechanic mechanicBelow = OraxenBlocks.getStringMechanic(changedBelow);
                if (OraxenBlocks.isOraxenStringBlock(changed)) {
                    OraxenBlocks.remove(changed.getLocation(), null);
                } else if (mechanicBelow != null && mechanicBelow.isTall()) {
                    OraxenBlocks.remove(changedBelow.getLocation(), null);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMiddleClick(final InventoryCreativeEvent event) {
        if (event.getClick() != ClickType.CREATIVE) return;
        final Player player = (Player) event.getInventory().getHolder();
        if (player == null) return;
        if (event.getCursor().getType() == Material.STRING) {
            final RayTraceResult rayTraceResult = player.rayTraceBlocks(6.0);
            if (rayTraceResult == null) return;
            final Block block = rayTraceResult.getHitBlock();
            if (block == null) return;

            StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
            if (mechanic == null) {
                StringBlockMechanic mechanicBelow = OraxenBlocks.getStringMechanic(block.getRelative(BlockFace.DOWN));
                if (mechanicBelow == null || !mechanicBelow.isTall()) return;
                mechanic = mechanicBelow;
            }
            ItemStack item = OraxenItems.getItemById(mechanic.getItemID()).build();
            for (int i = 0; i <= 8; i++) {
                if (player.getInventory().getItem(i) == null) continue;
                if (Objects.equals(OraxenItems.getIdByItem(player.getInventory().getItem(i)), mechanic.getItemID())) {
                    player.getInventory().setHeldItemSlot(i);
                    event.setCancelled(true);
                    return;
                }
            }
            event.setCursor(item);
        }
    }

    /**
     * Scheduled for removal in a future update. As of 1.147.0 API has been entirely redone.<br>
     * See {@link io.th0rgal.oraxen.api.OraxenBlocks#getStringMechanic(Block)} for the new method
     */
    @Deprecated(forRemoval = true)
    public static StringBlockMechanic getStringMechanic(Block block) {
        if (block.getType() == Material.TRIPWIRE) {
            final Tripwire tripwire = (Tripwire) block.getBlockData();
            return StringBlockMechanicFactory.getBlockMechanic(StringBlockMechanicFactory.getCode(tripwire));
        } else return null;
    }

    /**
     * Scheduled for removal in a future update. As of 1.147.0 API has been entirely redone.<br>
     * See {@link io.th0rgal.oraxen.api.OraxenBlocks#getStringMechanic(BlockData)} for the new method
     */
    @Deprecated(forRemoval = true)
    public static StringBlockMechanic getStringMechanic(Tripwire tripwire) {
        return StringBlockMechanicFactory.getBlockMechanic(StringBlockMechanicFactory.getCode(tripwire));
    }

    private HardnessModifier getHardnessModifier() {
        return new HardnessModifier() {

            @Override
            public boolean isTriggered(final Player player, final Block block, final ItemStack tool) {
                if (block.getType() != Material.TRIPWIRE)
                    return false;
                final StringBlockMechanic tripwireMechanic = OraxenBlocks.getStringMechanic(block);
                return tripwireMechanic != null && tripwireMechanic.hasHardness;
            }

            @Override
            public void breakBlock(final Player player, final Block block, final ItemStack tool) {
                block.setType(Material.AIR);
            }

            @Override
            public long getPeriod(final Player player, final Block block, final ItemStack tool) {
                final StringBlockMechanic tripwireMechanic = OraxenBlocks.getStringMechanic(block);
                if (tripwireMechanic == null) return 0;
                final long period = tripwireMechanic.getPeriod();
                double modifier = 1;
                if (tripwireMechanic.getDrop().canDrop(tool)) {
                    modifier *= 0.4;
                    final int diff = tripwireMechanic.getDrop().getDiff(tool);
                    if (diff >= 1)
                        modifier *= Math.pow(0.9, diff);
                }
                return (long) (period * modifier);
            }
        };
    }

    private Block makePlayerPlaceBlock(final Player player, final EquipmentSlot hand, final ItemStack item,
                                       final Block placedAgainst, final BlockFace face, final BlockData newBlock) {
        final Block target;
        final Material type = placedAgainst.getType();
        if (BlockHelpers.REPLACEABLE_BLOCKS.contains(type))
            target = placedAgainst;
        else {
            target = placedAgainst.getRelative(face);
            if (!target.getType().isAir() && target.getType() != Material.WATER && target.getType() != Material.LAVA)
                return null;
        }
        if (BlockHelpers.isStandingInside(player, target) || !ProtectionLib.canBuild(player, target.getLocation()))
            return null;

        // determines the old information of the block
        final BlockData curentBlockData = target.getBlockData();
        target.setBlockData(newBlock, false);
        final BlockState currentBlockState = target.getState();

        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, currentBlockState, placedAgainst, item, player, true, hand);
        final OraxenStringBlockPlaceEvent oraxenBlockPlaceEvent = new OraxenStringBlockPlaceEvent(OraxenBlocks.getStringMechanic(target), target, player);
        Bukkit.getPluginManager().callEvent(blockPlaceEvent);
        Bukkit.getPluginManager().callEvent(oraxenBlockPlaceEvent);

        if (player.getGameMode() == GameMode.ADVENTURE || BlockHelpers.correctAllBlockStates(target, player, face, item))
            blockPlaceEvent.setCancelled(true);

        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled() || oraxenBlockPlaceEvent.isCancelled()) {
            target.setBlockData(curentBlockData, false); // false to cancel physic
            return null;
        }

        if (!player.getGameMode().equals(GameMode.CREATIVE))
            item.setAmount(item.getAmount() - 1);

        return target;
    }

    public static void fixClientsideUpdate(Location loc) {
        List<Entity> players =
                Objects.requireNonNull(loc.getWorld()).getNearbyEntities(loc, 20, 20, 20)
                        .stream().filter(entity -> entity.getType() == EntityType.PLAYER).toList();

        for (double x = loc.getX() - 10; x < loc.getX() + 10; x++) {
            for (double y = loc.getY() - 4; y < loc.getY() + 4; y++) {
                for (double z = loc.getZ() - 10; z < loc.getZ() + 10; z++) {
                    if (loc.getBlock().getType() == Material.TRIPWIRE) {
                        Location newLoc = new Location(loc.getWorld(), x, y, z);
                        for (Entity e : players) {
                            ((Player) e).sendBlockChange(newLoc, newLoc.getBlock().getBlockData());
                        }
                    }
                }
            }
        }
    }
}
