package ch.jdtt.BurierRaid;

import ch.jdtt.Commands.*;
import ch.jdtt.autocompeter.startWarTabCompleter;
import ch.jdtt.enchantments.Kevlar;
import com.massivecraft.factions.Faction;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BurierRaid extends JavaPlugin implements Listener {
    @Override
    public void onDisable() {
        // Don't log disabling, Spigot does that for you automatically!
    }

    @Override
    public void onEnable() {
        WarConstructor warConstructor = new WarConstructor();
        String baseDir = "./plugins/BurierRaid/";
        Path baseDirPath = Paths.get(baseDir);
        Path totemList = Paths.get(baseDir+"FactionRaid.json");
        if (Files.notExists(baseDirPath)) {
            try {
                Files.createDirectory(baseDirPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (Files.notExists(totemList)) {
            try {
                Files.createFile(totemList);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        getCommand("placeTotem").setExecutor(new placeTotem(this));
        getCommand("moveTotem").setExecutor(new moveTotem(this));
        getCommand("protectionDensity").setExecutor(new protectionDensity(this));
        getCommand("startWar").setExecutor(new startWar(this, warConstructor));
        getCommand("startWar").setTabCompleter(new startWarTabCompleter());
        getCommand("joinWar").setExecutor(new joinWar(this, warConstructor));
        getCommand("declineWar").setExecutor(new declineWar(this, warConstructor));
        getServer().getPluginManager().registerEvents(warConstructor, this);
        getCommand("joinAsAlly").setExecutor(new joinAsAlly(this, warConstructor));
        registerKevlar();
    }
    public void registerKevlar() {
        try {
            Field f = Enchantment.class.getDeclaredField("acceptingNew");
            f.setAccessible(true);
            f.set(null, true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Kevlar kevlar = new Kevlar();
            Enchantment.registerEnchantment(kevlar);
        }
        catch (IllegalArgumentException e){
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}