package io.hamlook.aetheria.utils.compat

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
//#if FORGE
import net.minecraft.client.resources.AbstractResourcePack
import net.minecraft.client.resources.IReloadableResourceManager
import net.minecraft.client.resources.IResourcePack
import net.minecraft.client.resources.ResourcePackRepository
import net.minecraft.util.ResourceLocation
//#else
//$$ import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
//$$ import net.fabricmc.fabric.api.resource.ResourceManagerHelper
//$$ import net.minecraft.resource.Resource
//$$ import net.minecraft.resource.ResourceManager
//$$ import net.minecraft.resource.ResourceReloader
//$$ import net.minecraft.resource.ResourceType
//$$ import net.minecraft.util.Identifier
//$$ import net.minecraft.util.profiler.Profiler
//#endif

object ResourceCompat {

    @JvmStatic
    fun registerReloadListener(name: String, onReload: Runnable) {
        //#if FORGE
        val manager = MinecraftCompat.getMinecraft().resourceManager
        (manager as IReloadableResourceManager).registerReloadListener { onReload.run() }
        //#else
        //$$ ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(object : IdentifiableResourceReloadListener {
        //$$     override fun getFabricId(): Identifier = Identifier.of("aetheria", name)
        //$$     override fun reload(synchronizer: ResourceReloader.Synchronizer, manager: ResourceManager, prepareProfiler: Profiler, applyProfiler: Profiler, prepareExecutor: Executor, applyExecutor: Executor): CompletableFuture<Void> {
        //$$         return CompletableFuture.runAsync(onReload, applyExecutor).thenCompose(synchronizer::whenPrepared)
        //$$     }
        //$$ })
        //#endif
    }

    @JvmStatic
    fun openResource(id: ResourceLocation): ByteArray? {
        //#if FORGE
        return try {
            MinecraftCompat.getMinecraft().resourceManager.getResource(id)?.inputStream?.let { readAllBytes(it) }
        } catch (e: Exception) {
            null
        }
        //#else
        //$$ return try {
        //$$     MinecraftCompat.getMinecraft().resourceManager.getResource(id).orElse(null)?.inputStream?.let { readAllBytes(it) }
        //$$ } catch (e: Exception) {
        //$$     null
        //$$ }
        //#endif
    }

    @JvmStatic
    fun openAllResources(id: ResourceLocation): List<ByteArray> {
        return try {
            MinecraftCompat.getMinecraft().resourceManager.getAllResources(id).mapNotNull { res ->
                try {
                    readAllBytes(res.inputStream)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @JvmStatic
    fun findResources(namespace: String, prefix: String, suffix: String): Map<String, ByteArray> {
        //#if FORGE
        val result = LinkedHashMap<String, ByteArray>()
        val packs = MinecraftCompat.getMinecraft().resourcePackRepository.repositoryEntries
        for (entry in packs) {
            try {
                scanPack(entry.resourcePack, namespace, prefix, suffix, result)
            } catch (e: Exception) {
            }
        }
        return result
        //#else
        //$$ val result = LinkedHashMap<String, ByteArray>()
        //$$ val manager = MinecraftCompat.getMinecraft().resourceManager
        //$$ for ((id, resource) in manager.findResources(namespace, { it.path.startsWith(prefix) && it.path.endsWith(suffix) })) {
        //$$     try {
        //$$         result[id.path] = readAllBytes(resource.inputStream)
        //$$     } catch (e: Exception) {
        //$$     }
        //$$ }
        //$$ return result
        //#endif
    }

    //#if FORGE
    private fun scanPack(pack: IResourcePack, namespace: String, prefix: String, suffix: String, out: MutableMap<String, ByteArray>) {
        val base = getPackFile(pack) ?: return
        val root = "assets/$namespace/"
        val fullPrefix = root + prefix
        if (base.isDirectory) {
            val target = java.io.File(base, root)
            if (!target.exists()) return
            target.walkTopDown().filter { it.isFile }.forEach { file ->
                val rel = base.toPath().relativize(file.toPath()).toString().replace("\\", "/")
                if (rel.startsWith(fullPrefix) && rel.endsWith(suffix)) {
                    try {
                        file.inputStream().use { out[rel.substring(root.length)] = readAllBytes(it) }
                    } catch (e: Exception) {
                    }
                }
            }
        } else {
            try {
                java.util.zip.ZipFile(base).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val name = entries.nextElement().name
                        if (name.startsWith(fullPrefix) && name.endsWith(suffix)) {
                            try {
                                zip.getInputStream(zip.getEntry(name)).use { out[name.substring(root.length)] = readAllBytes(it) }
                            } catch (e: Exception) {
                            }
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun getPackFile(pack: IResourcePack): java.io.File? {
        return try {
            val field = AbstractResourcePack::class.java.getDeclaredField("resourcePackFile")
            field.isAccessible = true
            field.get(pack) as java.io.File
        } catch (e: Exception) {
            null
        }
    }
    //#endif

    private fun readAllBytes(input: InputStream): ByteArray {
        val buffer = ByteArray(8192)
        val out = ByteArrayOutputStream()
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}
