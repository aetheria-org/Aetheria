package io.hamlook.aetheria.features.misc.protect

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.hamlook.aetheria.utils.compat.MinecraftCompat
import net.minecraft.util.ChatComponentText
import java.io.File

object ProtectedItemStorage {

    private val gson = Gson()
    private lateinit var file: File
    val protectedUuids: MutableSet<String> = mutableSetOf()

    fun init(configDir: File) {
        file = File(configDir, "protected_items.json")
        load()
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val type = object : TypeToken<Set<String>>() {}.type
            val loaded: Set<String> = gson.fromJson(file.readText(), type) ?: return
            protectedUuids.addAll(loaded)
        } catch (e: Exception) {
            MinecraftCompat.getLocalPlayer()?.addChatMessage(
                ChatComponentText("§c[ATHR] Failed to load protected items: ${e.message}")
            )
        }
    }

    fun save() {
        try {
            file.writeText(gson.toJson(protectedUuids))
        } catch (e: Exception) {
            MinecraftCompat.getLocalPlayer()?.addChatMessage(
                ChatComponentText("§c[ATHR] Failed to save protected items: ${e.message}")
            )
        }
    }

    fun add(uuid: String) {
        protectedUuids.add(uuid); save()
    }

    fun remove(uuid: String) {
        protectedUuids.remove(uuid); save()
    }

    fun contains(uuid: String) = protectedUuids.contains(uuid)
}