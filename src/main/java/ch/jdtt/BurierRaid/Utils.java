package ch.jdtt.BurierRaid;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public double getDensity(Chunk chunk, Location loc, Material blockType) {
        int blockCounter = 0;
        for (int i = 0; i <= 15; i++) {
            for (int j = 0; j <= 15; j++) {
                for (int k = loc.getBlockY()-2; k <= loc.getBlockY()+3; k++) {
                    if (chunk.getBlock(i, k, j).getType().equals(blockType)){
                        blockCounter++;
                    }
                }
            }
        }
        return (double) blockCounter / (16 * 16 * 6 - 2);
    }
}