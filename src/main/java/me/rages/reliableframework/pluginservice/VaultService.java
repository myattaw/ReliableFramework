package me.rages.reliableframework.pluginservice;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultService implements PluginService<VaultService> {

    private Economy economy;

    @Override
    public VaultService setup(JavaPlugin plugin) {
        RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (registration == null) {
            return null;
        }
        economy = registration.getProvider();
        return this;
    }

    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double amount) {
        return economy.depositPlayer(offlinePlayer, amount);
    }

    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double amount) {
        return economy.withdrawPlayer(offlinePlayer, amount);
    }

    public boolean has(OfflinePlayer offlinePlayer, double amount) {
        return economy.has(offlinePlayer, amount);
    }

    public double getBalance(OfflinePlayer offlinePlayer) {
        return economy.getBalance(offlinePlayer);
    }

    @Override
    public String[] pluginNames() {
        return new String[] {"Vault"};
    }

}
