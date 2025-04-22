package com.MrRabbitson.RabbitDropShulker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public final class RabbitDropShulker extends JavaPlugin implements Listener {

    private final Map<Player, BossBar> bossBars = new HashMap<>();
    private FileConfiguration config;

    private String bossBarTitle;
    private BarColor bossBarColor;
    private String quitMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadConfigValues();

        getServer().getPluginManager().registerEvents(this, this);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkInventory(player);
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void loadConfigValues() {
        bossBarTitle = config.getString("bossbar-title", "У тебя в инвентаре есть шалкер! Выйдешь - шалкер выпадет");
        quitMessage = config.getString("quit-message", "Игрок %player% вышел с шалкерами в инвентаре!");

        try {
            bossBarColor = BarColor.valueOf(config.getString("bossbar-color", "RED").toUpperCase());
        } catch (IllegalArgumentException e) {
            bossBarColor = BarColor.RED;
        }
    }

    private void checkInventory(Player player) {
        boolean hasShulker = false;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType().toString().endsWith("SHULKER_BOX")) {
                hasShulker = true;
                break;
            }
        }

        if (hasShulker) {
            if (!bossBars.containsKey(player)) {
                BossBar bossBar = Bukkit.createBossBar(
                        bossBarTitle.replace("%player%", player.getName()),
                        bossBarColor,
                        BarStyle.SOLID,
                        BarFlag.PLAY_BOSS_MUSIC
                );
                bossBar.addPlayer(player);
                bossBars.put(player, bossBar);
            }
        } else {
            removeBossBar(player);
        }
    }

    private void removeBossBar(Player player) {
        if (bossBars.containsKey(player)) {
            BossBar bossBar = bossBars.get(player);
            bossBar.removeAll();
            bossBars.remove(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkInventory(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (bossBars.containsKey(player)) {
            String message = quitMessage.replace("%player%", player.getName());
            getLogger().info(message);

            // Удаляем шалкеры старым способом (совместимым с 1.16.5)
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType().toString().endsWith("SHULKER_BOX")) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
                    player.getInventory().remove(item);
                }
            }

            removeBossBar(player);
        }
    }

    @Override
    public void onDisable() {
        for (BossBar bossBar : bossBars.values()) {
            bossBar.removeAll();
        }
        bossBars.clear();
    }
}