package io.hamlook.aetheria.utils.compat

import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer

object WorldCompat {

    @JvmStatic
    fun getLoadedPlayers(world: WorldClient): List<EntityPlayer> =
        //#if MC < 1.14
        world.playerEntities
        //#else
        //$$ world.players
        //#endif

    @JvmStatic
    fun getAllEntities(world: WorldClient): List<Entity> =
        //#if MC < 1.14
        world.loadedEntityList
        //#else
        //$$ world.entities.toList()
        //#endif
}
