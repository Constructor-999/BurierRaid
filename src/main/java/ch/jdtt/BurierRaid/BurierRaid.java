package ch.jdtt.BurierRaid;

import ch.jdtt.Commands.moveTotem;
import org.bukkit.plugin.java.JavaPlugin;
import ch.jdtt.Commands.placeTotem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BurierRaid extends JavaPlugin {
    @Override
    public void onDisable() {
        // Don't log disabling, Spigot does that for you automatically!
    }

    @Override
    public void onEnable() {
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
    }
}
