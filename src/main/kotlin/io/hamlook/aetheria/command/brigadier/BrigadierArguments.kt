package io.hamlook.aetheria.command.brigadier

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType

object BrigadierArguments {
    fun string(): ArgumentType<String> = StringArgumentType.word()
    fun greedyString(): ArgumentType<String> = StringArgumentType.greedyString()
    fun integer(): ArgumentType<Int> = IntegerArgumentType.integer()
    fun integer(min: Int): ArgumentType<Int> = IntegerArgumentType.integer(min)
    fun integer(min: Int, max: Int): ArgumentType<Int> = IntegerArgumentType.integer(min, max)
    fun double(): ArgumentType<Double> = DoubleArgumentType.doubleArg()
    fun double(min: Double): ArgumentType<Double> = DoubleArgumentType.doubleArg(min)
    fun double(min: Double, max: Double): ArgumentType<Double> = DoubleArgumentType.doubleArg(min, max)
    fun bool(): ArgumentType<Boolean> = BoolArgumentType.bool()
}
