package xyz.jpenilla.squaremap.addon.worldguard.hook;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.Nullable;
<<<<<<< HEAD
import org.jetbrains.annotations.NotNull;
=======
>>>>>>> fc913d92079bc21ddd4ca4606eb75d44dd603e4f
import xyz.jpenilla.squaremap.addon.worldguard.SquaremapWorldGuard;
import xyz.jpenilla.squaremap.addon.worldguard.config.WGWorldConfig;
import xyz.jpenilla.squaremap.addon.worldguard.task.SquaremapTask;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;

import static xyz.jpenilla.squaremap.api.Key.key;

public final class SquaremapHook {
    private static final Key WORLDGUARD_LAYER_KEY = key("worldguard");

    private final Map<WorldIdentifier, SquaremapTask> providers = new HashMap<>();
    private final SquaremapWorldGuard plugin;
    private final Squaremap squaremap;

    public SquaremapHook(final SquaremapWorldGuard plugin) {
        this.plugin = plugin;
        this.squaremap = SquaremapProvider.get();
        this.squaremap.mapWorlds().forEach(this::addWorld);
    }

    public void addWorld(final World world) {
        this.squaremap.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world)).ifPresent(this::addWorld);
    }

<<<<<<< HEAD
    private void addWorld(final @NotNull MapWorld world) {
=======
    private void addWorld(final MapWorld world) {
>>>>>>> fc913d92079bc21ddd4ca4606eb75d44dd603e4f
        this.providers.computeIfAbsent(world.identifier(), id -> {
            final WGWorldConfig cfg = this.plugin.config().worldConfig(id);
            SimpleLayerProvider provider = SimpleLayerProvider.builder(cfg.controlLabel)
                .showControls(cfg.controlShow)
                .defaultHidden(cfg.controlHide)
                .build();
            world.layerRegistry().register(WORLDGUARD_LAYER_KEY, provider);
            SquaremapTask task = new SquaremapTask(this.plugin, id, provider);
<<<<<<< HEAD
            task.runAtFixedRate(this.plugin, 0, 20L * cfg.updateInterval);
=======
            task.runTaskTimerAsynchronously(this.plugin, 0, 20L * cfg.updateInterval);
>>>>>>> fc913d92079bc21ddd4ca4606eb75d44dd603e4f
            return task;
        });
    }

    public void removeWorld(final World world) {
        this.removeWorld(BukkitAdapter.worldIdentifier(world));
    }

    public void removeWorld(final WorldIdentifier world) {
        final @Nullable SquaremapTask remove = this.providers.remove(world);
        if (remove != null) {
            remove.disable();
        }
        this.squaremap.getWorldIfEnabled(world).ifPresent(mapWorld -> {
            if (mapWorld.layerRegistry().hasEntry(WORLDGUARD_LAYER_KEY)) {
                mapWorld.layerRegistry().unregister(WORLDGUARD_LAYER_KEY);
            }
        });
    }

    public void disable() {
        Map.copyOf(this.providers).keySet().forEach(this::removeWorld);
    }
}
