package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.CalculatorUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;

@RegisterCommand
public class CalcCommand extends ASMCommand {

    private static final String PREFIX = EnumChatFormatting.AQUA + "[ATHR] " + EnumChatFormatting.RESET;

    @Override
    public String getName() {
        return "athrcalc";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("aetheriacalc", "jefcalc", "asmcalc", "calc");
    }

    @Override
    public String getUsage() {
        return "/athrcalc <expression>";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Usage: /athrcalc <expression>");
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GRAY + "Basic Examples:");
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GRAY + "  /athrcalc 2 + 2");
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GRAY + "  /athrcalc 100k * 5");
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GRAY + "  /athrcalc (10 + 5) * 2");
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GRAY + "  /athrcalc 2^10");
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GRAY + "Advanced Examples:");
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GRAY + "  /athrcalc sin(pi/2)");
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GRAY + "  /athrcalc sqrt(16)");
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GRAY + "  /athrcalc log(100)");
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GRAY + "  /athrcalc max(5, 10)");
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GRAY + "Multipliers: k, m, b, t (numbers), s (stack/64)");
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GRAY + "Functions: sin, cos, tan, sqrt, log, ln, abs, ceil, floor, pow, max, min");
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GRAY + "Constants: pi, e");
            return;
        }

        String expression = String.join(" ", args);

        try {
            java.math.BigDecimal result = CalculatorUtils.calculate(expression);
            String formatted = CalculatorUtils.FORMAT.format(result);
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.YELLOW + expression + EnumChatFormatting.GREEN + " = " + EnumChatFormatting.AQUA + formatted);
        } catch (CalculatorUtils.CalculatorException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Invalid expression";
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Error: " + msg);
        } catch (java.util.NoSuchElementException e) {
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Error: Not enough values (check parentheses and operators)");
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Error: " + msg);
            e.printStackTrace();
        }
    }
}