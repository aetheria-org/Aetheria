package io.hamlook.aetheria.utils.compat

/**
 * Bridges inventory array types across MC versions. On 1.8.9 mainInventory is ItemSTack[];
 * on 1.21+ it is NonNullList which needs .toArray(). This is a no-op on arrays (passthrough)
 * and converts lists to typed arrays. Use around mainInventory accesses for forward compatibility.
 */
inline fun <reified T> List<T>.normalizeAsArray(): Array<T> = this.toTypedArray()
fun <T> Array<T>.normalizeAsArray(): Array<T> = this
