package net.aruneko.chainblockbreaker

import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.Plugin

class ChainBlockBreakerListeners(private val plugin: Plugin, private val server: Server) : Listener {
    private fun isLog(block: Block): Boolean {
        return when(block.type) {
            Material.ACACIA_LOG,
            Material.BIRCH_LOG,
            Material.DARK_OAK_LOG,
            Material.JUNGLE_LOG,
            Material.OAK_LOG,
            Material.SPRUCE_LOG,
            Material.STRIPPED_ACACIA_LOG,
            Material.STRIPPED_BIRCH_LOG,
            Material.STRIPPED_DARK_OAK_LOG,
            Material.STRIPPED_JUNGLE_LOG,
            Material.STRIPPED_OAK_LOG,
            Material.STRIPPED_SPRUCE_LOG -> true
            else -> false
        }
    }

    private fun isAxe(item: ItemStack): Boolean {
        return when(item.type) {
            Material.DIAMOND_AXE,
            Material.GOLDEN_AXE,
            Material.IRON_AXE,
            Material.STONE_AXE,
            Material.WOODEN_AXE -> true
            else -> false
        }
    }

    private fun isOre(block: Block): Boolean {
        return when(block.type) {
            Material.COAL_ORE,
            Material.DIAMOND_ORE,
            Material.EMERALD_ORE,
            Material.GOLD_ORE,
            Material.IRON_ORE,
            Material.LAPIS_ORE,
            Material.NETHER_QUARTZ_ORE,
            Material.REDSTONE_ORE,
            Material.GLOWSTONE -> true
            else -> false
        }
    }

    private fun isPickAxe(item: ItemStack): Boolean {
        return when(item.type) {
            Material.DIAMOND_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.IRON_PICKAXE,
            Material.STONE_PICKAXE,
            Material.WOODEN_PICKAXE -> true
            else -> false
        }
    }

    private fun isDiggable(block: Block): Boolean {
        return when(block.type) {
            Material.GRAVEL, Material.CLAY -> true
            else -> false
        }
    }

    private fun isShovel(item: ItemStack): Boolean {
        return when(item.type) {
            Material.DIAMOND_SHOVEL,
            Material.GOLDEN_SHOVEL,
            Material.IRON_SHOVEL,
            Material.STONE_SHOVEL,
            Material.WOODEN_SHOVEL -> true
            else -> false
        }
    }

    private fun getBoolMetadata(block: Block, key: String): Boolean {
        val metadata = block.getMetadata(key).filter { it.owningPlugin.name == plugin.name }
        return when(metadata.size) {
            0 -> false
            else -> metadata.first().asBoolean()
        }
    }

    @EventHandler
    fun onBlockBreakEvent(event: BlockBreakEvent) {
        val targetBlock = event.block
        val player = event.player
        val mainHandItem = player.inventory.itemInMainHand

        if (!player.isSneaking) {
            return
        }

        when {
            isLog(targetBlock) and isAxe(mainHandItem) -> breakChain(
                    0, targetBlock, player
            ) { block -> isLog(block) }
            isOre(targetBlock) and isPickAxe(mainHandItem) -> breakChain(
                    -1, targetBlock, player
            ) { block -> isOre(block) }
            isDiggable(targetBlock) and isShovel(mainHandItem) -> breakChain(
                    -1, targetBlock, player
            ) { block -> isDiggable(block) }
        }
    }

    private fun breakChain(minY: Int, targetBlock: Block, player: Player, blockCondition: (Block) -> Boolean) {
        val metadataKey = "isTarget"
        if (getBoolMetadata(targetBlock, metadataKey)) {
            return
        }
        targetBlock.setMetadata(metadataKey, FixedMetadataValue(plugin, true))

        for (y in minY..1) {
            for (x in -1..1) {
                for (z in -1..1) {
                    val block = targetBlock.getRelative(x, y, z)
                    if (targetBlock.location == block.location || !blockCondition(block) || targetBlock.type != block.type) {
                        continue
                    }
                    server.scheduler.scheduleSyncDelayedTask(
                            plugin,
                            {
                                if (player.isValid) {
                                    val blockBreakEvent = BlockBreakEvent(block, player)
                                    server.pluginManager.callEvent(blockBreakEvent)
                                }
                            },
                            1
                    )
                }
            }
        }
        val mainHandItem = player.inventory.itemInMainHand
        targetBlock.removeMetadata(metadataKey, plugin)
        targetBlock.breakNaturally(mainHandItem)
    }
}
