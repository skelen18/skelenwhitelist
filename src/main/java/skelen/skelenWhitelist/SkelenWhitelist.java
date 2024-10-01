package skelen.skelenWhitelist;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public final class SkelenWhitelist extends JavaPlugin implements TabCompleter {

    private Set<WhitelistEntry> whitelist;
    private File whitelistFile;
    private Gson gson = new Gson();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        whitelistFile = new File(getDataFolder(), "whitelist.json");
        loadWhitelist();
        getCommand("skelenwl").setTabCompleter(this);
        Bukkit.getConsoleSender().sendMessage("Plugin SkelenWhitelist byl aktivován.");
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("Plugin SkelenWhitelist byl deaktivován.");
    }

    private void loadWhitelist() {
        if (whitelistFile.exists()) {
            try (FileReader reader = new FileReader(whitelistFile)) {
                Type listType = new TypeToken<HashSet<WhitelistEntry>>() {}.getType();
                whitelist = gson.fromJson(reader, listType);
                if (whitelist == null) {
                    whitelist = new HashSet<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            whitelist = new HashSet<>();
        }
    }

    private void saveWhitelist() {
        try (FileWriter writer = new FileWriter(whitelistFile)) {
            gson.toJson(whitelist, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("skelenwl")) {

            if (!(sender.isOp())) {
                sender.sendMessage("Tento příkaz nemůžeš použít.");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage("§6===== SkelenWhitelist Nápověda =====");
                sender.sendMessage("§e/skelenwl add <hráč> §7- Přidá hráče na whitelist.");
                sender.sendMessage("§e/skelenwl remove <hráč> §7- Odebere hráče z whitelistu.");
                sender.sendMessage("§e/skelenwl all §7- Přidá všechny hráče z configu na whitelist serveru.");
                sender.sendMessage("§e/skelenwl list §7- Zobrazí seznam hráčů na whitelistu i s UUID.");
                sender.sendMessage("§e/skelenwl removeall §7- Odebere všechny hráče z whitelistu.");
                sender.sendMessage("§e/skelenwl load §7- Načte whitelist z JSON a config souboru.");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "add":
                    if (args.length == 2) {
                        String playerName = args[1];
                        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                        String uuid = player.getUniqueId().toString();

                        WhitelistEntry entry = new WhitelistEntry(uuid, playerName);
                        if (!whitelist.contains(entry)) {
                            whitelist.add(entry);
                            player.setWhitelisted(true);
                            sender.sendMessage("Hráč " + playerName + " byl přidán na whitelist.");
                            saveWhitelist();
                            updateServerWhitelist();
                            addPlayerToConfig(playerName);
                        } else {
                            sender.sendMessage("Hráč " + playerName + "již je na whitelistu,.");
                        }
                    } else {
                        sender.sendMessage("Použití: /skelenwl add <jméno hráče>");
                    }
                    return true;

                case "remove":
                    if (args.length == 2) {
                        String playerName = args[1];
                        WhitelistEntry toRemove = null;
                        for (WhitelistEntry entry : whitelist) {
                            if (entry.getName().equalsIgnoreCase(playerName)) {
                                toRemove = entry;
                                break;
                            }
                        }

                        if (toRemove != null) {
                            whitelist.remove(toRemove);
                            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                            player.setWhitelisted(false);
                            sender.sendMessage("Hráč " + playerName + " byl odebrán z whitelistu.");
                            saveWhitelist();
                            updateServerWhitelist();
                            removePlayerFromConfig(playerName);
                        } else {
                            sender.sendMessage("Hráč " + playerName + "není na whitelistu.");
                        }
                    } else {
                        sender.sendMessage("Použití: /skelenwl remove <jméno hráče>");
                    }
                    return true;

                case "all":
                    loadPlayersFromConfig();
                    for (WhitelistEntry entry : whitelist) {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getName());
                        if (!offlinePlayer.isWhitelisted()) {
                            offlinePlayer.setWhitelisted(true);
                        }
                    }
                    sender.sendMessage("Všichni hráči z config.yml byli přidáni na whitelist serveru.");
                    updateServerWhitelist();
                    return true;

                case "list":
                    sender.sendMessage("§6=====Hráči na whitelistu=====:");
                    for (WhitelistEntry entry : whitelist) {
                        sender.sendMessage(entry.getName() + " (UUID: " + entry.getUuid() + ")");
                    }
                    return true;

                case "removeall":
                    for (WhitelistEntry entry : whitelist) {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getName());
                        if (offlinePlayer.isWhitelisted()) {
                            offlinePlayer.setWhitelisted(false);
                            Bukkit.getConsoleSender().sendMessage("Hráč " + entry.getName() + " byl odebrán z whitelistu.");
                        }
                    }
                    whitelist.clear();
                    saveWhitelist();
                    sender.sendMessage("Všichni hráči byli odebráni z whitelistu.");
                    updateServerWhitelist();
                    return true;

                case "load":
                    loadWhitelist();
                    sender.sendMessage("Whitelist byl načten z whitelist.json.");
                    return true;

                default:
                    sender.sendMessage("Neznámý příkaz. Pro nápovědu použijte /skelenwl.");
                    return true;
            }
        }
        return false;
    }

    private void updateServerWhitelist() {
        Bukkit.reloadWhitelist();
    }

    private void loadPlayersFromConfig() {
        List<String> playersFromConfig = getConfig().getStringList("whitelist");

        for (String playerName : playersFromConfig) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            String uuid = player.getUniqueId().toString();

            WhitelistEntry entry = new WhitelistEntry(uuid, playerName);
            if (!whitelist.contains(entry)) {
                whitelist.add(entry);
                player.setWhitelisted(true);
                Bukkit.getConsoleSender().sendMessage("Hráč " + playerName + "byl přidán na whitelist.");
            }
        }
        saveWhitelist();
        updateServerWhitelist();
    }

    private void addPlayerToConfig(String playerName) {
        List<String> playersFromConfig = getConfig().getStringList("whitelist");
        if (!playersFromConfig.contains(playerName)) {
            playersFromConfig.add(playerName);
            getConfig().set("whitelist", playersFromConfig);
            saveConfig();
        }
    }

    private void removePlayerFromConfig(String playerName) {
        List<String> playersFromConfig = getConfig().getStringList("whitelist");
        if (playersFromConfig.contains(playerName)) {
            playersFromConfig.remove(playerName);
            getConfig().set("whitelist", playersFromConfig);
            saveConfig();
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (cmd.getName().equalsIgnoreCase("skelenwl")) {
            if (args.length == 1) {
                List<String> subcommands = Arrays.asList("add", "remove", "all", "list", "removeall", "load");
                return filterSuggestions(subcommands, args[0]);
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
                List<String> playerNames = new ArrayList<>();
                for (WhitelistEntry entry : whitelist) {
                    playerNames.add(entry.getName());
                }
                return filterSuggestions(playerNames, args[1]);
            }
        }
        return null;
    }

    private List<String> filterSuggestions(List<String> options, String input) {
        List<String> suggestions = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(input.toLowerCase())) {
                suggestions.add(option);
            }
        }
        return suggestions;
    }

    private class WhitelistEntry {
        private String uuid;
        private String name;

        public WhitelistEntry(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            WhitelistEntry that = (WhitelistEntry) obj;
            return Objects.equals(uuid, that.uuid) && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid, name);
        }
    }
}
