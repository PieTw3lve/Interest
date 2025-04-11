package com.github.pietw3lve;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.pietw3lve.utils.ConfigManager;

import net.milkbowl.vault.economy.Economy;

public class Interest extends JavaPlugin {
	
    private static Economy econ = null;

	@Override
	public void onEnable() {
		if (!setupEconomy() ) {
			getLogger().severe(String.format("Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
        reloadConfig();
        scheduleTask();
	}
	
    /**
     * Sets up the economy using Vault.
     *
     * @return true if the economy was successfully set up, false otherwise.
     */
	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

    /**
     * Schedules the task to distribute interest to players' accounts.
     * The task runs daily at the configured target time.
     */
    private void scheduleTask() {
        String timezone = getConfig().getString("timezone", "America/New_York");
        String targetTime = getConfig().getString("target-time", "00:00");
        ZoneId zoneId = ZoneId.of(timezone);

        LocalDateTime now = LocalDateTime.now(zoneId);
        String[] timeParts = targetTime.split(":");
        int targetHour = Integer.parseInt(timeParts[0]);
        int targetMinute = Integer.parseInt(timeParts[1]);

        LocalDateTime target = now.withHour(targetHour).withMinute(targetMinute).withSecond(0).withNano(0);
        if (target.isBefore(now)) {
            target = target.plusDays(1);
        }

        long delay = ChronoUnit.SECONDS.between(now, target) * 20;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (OfflinePlayer player : getServer().getOfflinePlayers()) {
                    if (econ.hasAccount(player)) {
                        boolean notification = getConfig().getBoolean("notification");
                        double interest = getConfig().getDouble("interest");
                        double balance = econ.getBalance(player);
                        double amount = balance * interest;
                        double newBalance = balance + amount;
                        econ.depositPlayer(player, newBalance - balance);
                        if (notification && player.isOnline()) {
                            player.getPlayer().sendMessage(String.format(ChatColor.GREEN + "You have received %.2f Gems from interest!", amount));
                        }
                    }
                }
            }
        }.runTaskTimer(this, delay, 24 * 60 * 60 * 20);
    }
	
    /**
     * Reloads the plugin configuration.
     * Ensures the default configuration is saved and updates the configuration file.
     */
	public void reloadConfig() {
		saveDefaultConfig();
		File configFile = new File(getDataFolder(), "config.yml");
		
		try {
			ConfigManager.update(this, "config.yml", configFile, Arrays.asList());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		super.reloadConfig();
	}
}