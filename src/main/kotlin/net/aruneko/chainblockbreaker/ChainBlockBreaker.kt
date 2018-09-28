package net.aruneko.chainblockbreaker

import org.bukkit.plugin.java.JavaPlugin


class ChainBlockBreaker : JavaPlugin() {
    override fun onEnable() {
        server.pluginManager.registerEvents(ChainBlockBreakerListeners(this, server), this)
    }

    override fun onDisable() {}
}
