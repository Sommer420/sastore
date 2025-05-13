package hundeklemmen.nikolaialex05;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hundeklemmen.nikolaialex05.Commands.SuperAwesomeStoreCommand;
import hundeklemmen.nikolaialex05.classes.betaling;
import hundeklemmen.nikolaialex05.classes.id;
import hundeklemmen.nikolaialex05.classes.vote;
import hundeklemmen.nikolaialex05.events.players;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class SAStore extends JavaPlugin {

    private static DecimalFormat format = new DecimalFormat("##.##");

    public static SAStore instance;
    public static String prefix = "&8[&dSA&fStore &7// &fArcMC.dk&8]&r";
    public static File configFile;
    public static FileConfiguration config;
    public static File spigotFile;
    public static FileConfiguration spigot;
    public static long lastPaymentFetch = 0;
    public static long lastVoteFetch = 0;

    public static String authorization = "";

    public static Logger log;



    @Override
    public void onEnable(){
        instance = this;
        log = getLogger();
        loadSpigot();
        getCommand("sastore").setExecutor(new SuperAwesomeStoreCommand());
        getCommand("sastore").setTabCompleter(new TabCompleteListener());
        getServer().getPluginManager().registerEvents(new players(), this);

        createConfig();

        if(config.get("serverkey") != null) {
            overwrite();
        }
        if(spigot.getBoolean("settings.bungeecord") == true && (config.getString("apikey") != null && !config.getString("apikey").equalsIgnoreCase(""))) {


            new BukkitRunnable() {
                @Override
                public void run() {
                    fetchPayments(false);
                    fetchVotes(false);
                }
            }.runTaskTimer(SAStore.instance, 20L, 20L * 30);
        }
    }


    @Override
    public void onDisable(){

    }

    public static void fetchPayments(boolean isSocket) {
        if(SAStore.lastPaymentFetch > new Date().getTime() - 5000) return;
        SAStore.lastPaymentFetch = new Date().getTime();

        Bukkit.getScheduler().runTaskAsynchronously(SAStore.instance, new Runnable() {
            @Override
            public void run() {
                String svar = Utils.get("https://api.superawesome.dk/storeapi/v2/purchases", SAStore.authorization);

                Gson gson = new Gson();
                Type listType = new TypeToken<List<betaling>>(){}.getType();
                List<betaling> betalinger = gson.fromJson(svar, listType);

                Bukkit.getScheduler().scheduleSyncDelayedTask(SAStore.instance, new Runnable() {
                    @Override
                    public void run() {
                        for (betaling betal : betalinger) {
                            Bukkit.getServer().getPluginManager().callEvent(
                                    new betalingEvent(
                                            Bukkit.getOfflinePlayer(
                                                    UUID.fromString(betal.getUuid())
                                            ),
                                            betal.getProduct(),
                                            betal.getAmount(),
                                            new id(betal.getId())
                                    )
                            );
                        };
                    }
                }, 0);
            }
        });
    }

    public static void fetchVotes(boolean isSocket) {
        if(SAStore.lastVoteFetch > new Date().getTime() - 5000) return;
        SAStore.lastVoteFetch = new Date().getTime();

        Bukkit.getScheduler().runTaskAsynchronously(SAStore.instance, new Runnable() {
            @Override
            public void run() {
                String svar = Utils.get("https://api.superawesome.dk/storeapi/v2/votes", SAStore.authorization);

                Gson gson = new Gson();
                Type listType = new TypeToken<List<vote>>(){}.getType();
                List<vote> votes = gson.fromJson(svar, listType);

                Bukkit.getScheduler().scheduleSyncDelayedTask(SAStore.instance, new Runnable() {
                    @Override
                    public void run() {
                        for (vote vote : votes) {
                            Bukkit.getServer().getPluginManager().callEvent(
                                    new voteEvent(
                                            Bukkit.getOfflinePlayer(
                                                    UUID.fromString(vote.getUuid())
                                            ),
                                            new ch.njol.skript.util.Date(vote.getDate()),
                                            new id(vote.getId())
                                    )
                            );
                        };
                    }
                }, 0);
            }
        });
    }
    public static void createConfig() {
        configFile = new File(SAStore.instance.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            SAStore.instance.saveResource("config.yml", false);
        }
        config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        if(config.contains("apikey") && config.getString("apikey").toString().length() != 0) {
            authorization = config.getString("apikey");
        }
    }

    public static void overwrite() {
        configFile = new File(SAStore.instance.getDataFolder(), "config.yml");
        SAStore.instance.saveResource("config.yml", true);
    }

    public static void loadSpigot() {
        spigotFile = new File("./", "spigot.yml");
        spigot = new YamlConfiguration();
        try {
            spigot.load(spigotFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
}
