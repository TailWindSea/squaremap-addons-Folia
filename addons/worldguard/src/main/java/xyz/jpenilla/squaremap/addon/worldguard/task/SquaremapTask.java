package xyz.jpenilla.squaremap.addon.worldguard.task;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import com.sk89q.worldguard.util.profile.cache.ProfileCache;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.addon.common.config.ListMode;
import xyz.jpenilla.squaremap.addon.worldguard.SquaremapWorldGuard;
import xyz.jpenilla.squaremap.addon.worldguard.config.StyleSettings;
import xyz.jpenilla.squaremap.addon.worldguard.config.WGWorldConfig;
import xyz.jpenilla.squaremap.addon.worldguard.hook.WGHook;
import xyz.jpenilla.squaremap.addon.worldguard.task.schedulers.FoliaRunnable;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

public final class SquaremapTask extends FoliaRunnable {
    private final WorldIdentifier world;
    private final SimpleLayerProvider provider;
    private final SquaremapWorldGuard plugin;

    private boolean stop;

    public SquaremapTask(SquaremapWorldGuard plugin, WorldIdentifier world, SimpleLayerProvider provider) {
        super(Bukkit.getGlobalRegionScheduler());
        this.plugin = plugin;
        this.world = world;
        this.provider = provider;
    }

    @Override
    public void run() {
        if (this.stop) {
            this.cancel();
        }
        this.updateClaims();
    }

    void updateClaims() {
        this.provider.clearMarkers(); // TODO track markers instead of clearing them
        Map<String, ProtectedRegion> regions = WGHook.getRegions(this.world);
        if (regions == null) {
            return;
        }
        final WGWorldConfig cfg = this.plugin.config().worldConfig(this.world);
        final ListMode listMode = cfg.listMode;
        final List<String> list = cfg.regionList;
        regions.forEach((id, region) -> {
            if (!listMode.allowed(list, id)) {
                return;
            }
            this.handleClaim(region);
        });
    }

    private void handleClaim(ProtectedRegion region) {
        final StateFlag.State state = region.getFlag(this.plugin.visibleFlag);
        if (state == StateFlag.State.DENY) {
            return;
        }

        Marker marker;

        if (region.getType() == RegionType.CUBOID) {
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            marker = Marker.rectangle(
                Point.of(min.getX(), min.getZ()),
                Point.of(max.getX() + 1, max.getZ() + 1)
            );
        } else if (region.getType() == RegionType.POLYGON) {
            List<Point> points = region.getPoints().stream()
                .map(point -> Point.of(point.getX(), point.getZ()))
                .collect(Collectors.toList());
            marker = Marker.polygon(points);
        } else {
            // do not draw global region
            return;
        }

        ProfileCache pc = WorldGuard.getInstance().getProfileCache();
        Map<Flag<?>, Object> flags = region.getFlags();

        final WGWorldConfig cfg = this.plugin.config().worldConfig(this.world);
        final StyleSettings defaults = StyleSettings.fromFlags(this.plugin, region).defaulted(cfg.defaultStyle);
        final @Nullable StyleSettings override = cfg.styleOverrides.get(region.getId());
        final StyleSettings style = override == null ? defaults : override.defaulted(defaults);
        MarkerOptions.Builder options = MarkerOptions.builder()
            .strokeColor(style.stroke.color)
            .strokeWeight(style.stroke.weight)
            .strokeOpacity(style.stroke.opacity)
            .fillColor(style.fill.color)
            .fillOpacity(style.fill.opacity);
        if (style.clickTooltip != null && !style.clickTooltip.isBlank()) {
            options.clickTooltip(
                style.clickTooltip
                    .replace("{world}", Bukkit.getWorld(BukkitAdapter.namespacedKey(this.world)).getName()) // use names for now
                    .replace("{id}", region.getId())
                    .replace("{owner}", region.getOwners().toPlayersString())
                    .replace("{regionname}", region.getId())
                    .replace("{playerowners}", region.getOwners().toPlayersString(pc))
                    .replace("{groupowners}", region.getOwners().toGroupsString())
                    .replace("{playermembers}", region.getMembers().toPlayersString(pc))
                    .replace("{groupmembers}", region.getMembers().toGroupsString())
                    .replace("{parent}", region.getParent() == null ? "" : region.getParent().getId())
                    .replace("{priority}", String.valueOf(region.getPriority()))
                    .replace(
                        "{flags}",
                        flags.keySet().stream()
                            .filter(flag -> cfg.flagListMode.allowed(cfg.flagList, flag.getName()))
                            .map(flag -> flag.getName() + ": " + flags.get(flag) + "<br/>")
                            .collect(Collectors.joining())
                    )
            );
        }

        marker.markerOptions(options);

        String markerid = "worldguard_region_" + region.getId().hashCode();
        this.provider.addMarker(Key.of(markerid), marker);
    }

    public void disable() {
        this.cancel();
        this.stop = true;
        this.provider.clearMarkers();
    }
}

