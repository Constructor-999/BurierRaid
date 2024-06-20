package ch.jdtt.Commands;

import ch.jdtt.BurierRaid.BurierRaid;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scoreboard.*;

import java.util.concurrent.TimeUnit;

public class testCommand implements CommandExecutor {
    BurierRaid plugin;
    public testCommand(BurierRaid plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        String cmdName = command.getName();
        if (!cmdName.equalsIgnoreCase("testCommand")){
            return false;
        }

        return true;
    }
}
