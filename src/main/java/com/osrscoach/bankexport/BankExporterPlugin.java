package com.osrscoach.bankexport;

import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Local-only bank snapshots. Never sends network traffic or controls gameplay. */
@PluginDescriptor(name = "OSRS Coach Bank Export", description = "Export bank snapshots for a local read-only progression planner", tags = {"bank", "ironman", "export"})
public class BankExporterPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(BankExporterPlugin.class);
    @Inject private Client client;

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() != InventoryID.BANK || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
        {
            return;
        }
        final String account = client.getLocalPlayer().getName();
        final ItemContainer bank = event.getItemContainer();
        if (bank == null) return;

        // Snapshot all slots, not only visible bank tabs; ignore empty slots and placeholders.
        final Map<Integer, Long> quantities = new TreeMap<>();
        for (Item item : bank.getItems())
        {
            if (item == null || item.getId() <= 0 || item.getQuantity() <= 0) continue;
            ItemComposition composition = client.getItemDefinition(item.getId());
            if (composition != null && composition.getPlaceholderTemplateId() != -1) continue;
            quantities.merge(item.getId(), (long) item.getQuantity(), Long::sum);
        }
        String safeAccount = account.replaceAll("[^a-zA-Z0-9 _-]", "_").trim();
        if (safeAccount.isEmpty()) return;
        // Same default folder convention as Character Export, for automatic pickup by V11.1.
        Path directory = RuneLite.RUNELITE_DIR.toPath().resolve("character-exporter").resolve(safeAccount);
        StringBuilder json = new StringBuilder(128 + quantities.size() * 32);
        json.append("{\n  \"schema_version\": 1,\n  \"account_name\": \"")
            .append(escapeJson(account))
            .append("\",\n  \"exported_at\": \"").append(Instant.now())
            .append("\",\n  \"items\": [\n");
        boolean first = true;
        for (Map.Entry<Integer, Long> entry : quantities.entrySet())
        {
            if (!first) json.append(",\n");
            first = false;
            json.append("    {\"id\": ").append(entry.getKey())
                .append(", \"quantity\": ").append(entry.getValue()).append("}");
        }
        json.append("\n  ]\n}\n");
        try
        {
            Files.createDirectories(directory);
            Path target = directory.resolve("bank.json");
            Path temporary = Files.createTempFile(directory, "bank-", ".tmp");
            try
            {
                Files.write(temporary, json.toString().getBytes(StandardCharsets.UTF_8));
                try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (java.nio.file.AtomicMoveNotSupportedException e)
                { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
            }
            finally { Files.deleteIfExists(temporary); }
            log.debug("Updated bank snapshot for {} ({} item types)", account, quantities.size());
        }
        catch (IOException | SecurityException e)
        {
            log.warn("Bank export failed. Check RuneLite plugin file-access permissions: {}", e.toString());
        }
    }

    private static String escapeJson(String s)
    {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
