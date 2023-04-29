package at.haha007.edenminigames.games.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class PathVisualizer {
    private final Path path;
    private final List<UUID> viewers = new ArrayList<>();
    private final List<PacketContainer> displayPackets = new ArrayList<>();
    private PacketContainer destroyPacket;

    public PathVisualizer(Path path) {
        this.path = path;
        path.addPathChangedListener(this::onPathChanged);
        buildPath();
    }

    public void show(Player player) {
        viewers.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        if (viewers.contains(player.getUniqueId())) hide(player);
        viewers.add(player.getUniqueId());
        displayPackets.forEach(packet -> ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet));
    }

    public void hide(Player player) {
        viewers.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        if (!viewers.contains(player.getUniqueId())) return;
        viewers.remove(player.getUniqueId());
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, destroyPacket);
    }

    private void onPathChanged(Path path) {
        viewers.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        List<Player> players = viewers.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();
        players.forEach(this::hide);
        buildPath();
        players.forEach(this::show);
    }

    private void buildPath() {
        List<Integer> entityIds = new ArrayList<>();
        List<UUID> uuids = new ArrayList<>();
        List<PacketContainer> displayPackets = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < path.size(); i++) {
            entityIds.add(random.nextInt());
            uuids.add(new UUID(random.nextLong(), random.nextLong()));
        }

        for (int i = 0; i < path.size(); i++) {
            Vector position = path.get(i);
            int entityId = entityIds.get(i);
            UUID uuid = uuids.get(i);
            displayPackets.add(createSpawnPacket(entityId, uuid, position));
        }

        for (int i = path.size() - 1; i >= 1; i--) {
            int to = entityIds.get(i);
            int from = entityIds.get(i - 1);
            displayPackets.add(createMetadataPacket(from, to));
        }
        int lastId = entityIds.get(entityIds.size() - 1);
        displayPackets.add(createMetadataPacket(lastId, -1));

        displayPackets.add(deleteTeamPacket());
        displayPackets.add(createTeamPacket(uuids));

        this.displayPackets.clear();
        this.displayPackets.addAll(displayPackets);

        PacketContainer destroyPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        destroyPacket.getModifier().write(0, new IntArrayList(entityIds.stream().mapToInt(Integer::intValue).toArray()));
        this.destroyPacket = destroyPacket;
    }

    private PacketContainer createMetadataPacket(int id, int target) {
        PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, id);

        final List<WrappedDataValue> metadata = Lists.newArrayList();
        //shooting the next entity
        metadata.add(new WrappedDataValue(17, WrappedDataWatcher.Registry.get(Integer.class), target));
        //invisible
        metadata.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x20));
        metadataPacket.getDataValueCollectionModifier().write(0, metadata);

        return metadataPacket;
    }

    private PacketContainer deleteTeamPacket() {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, "path_team");
        packet.getIntegers().write(0, 1);
        return packet;
    }

    private PacketContainer createTeamPacket(List<UUID> uuids) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
        //team name
        packet.getStrings().write(0, "path_team");
        //new team
        packet.getIntegers().write(0, 0);
        packet.getStructures().write(1, InternalStructure.getConverter().getSpecific(uuids.stream().map(UUID::toString).toList()));

        Optional<InternalStructure> optional = packet.getOptionalStructures().read(0);
        if (optional.isPresent()) {
            InternalStructure structure = optional.get();
            structure.getChatComponents().write(0, WrappedChatComponent.fromText("path_team"));
            structure.getChatComponents().write(1, WrappedChatComponent.fromText(""));
            structure.getChatComponents().write(2, WrappedChatComponent.fromText(""));
            structure.getStrings().write(0, "never");
            structure.getStrings().write(1, "never");
            structure.getIntegers().write(0, 0);
        }

        return packet;
    }

    private PacketContainer createSpawnPacket(int id, UUID uuid, Vector position) {
        PacketContainer spawnPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
        spawnPacket.getIntegers().write(0, id);
        spawnPacket.getUUIDs().write(0, uuid);
        spawnPacket.getEntityTypeModifier().write(0, EntityType.GUARDIAN);
        spawnPacket.getDoubles().write(0, position.getX());
        spawnPacket.getDoubles().write(1, position.getY());
        spawnPacket.getDoubles().write(2, position.getZ());
        displayPackets.add(spawnPacket);
        return spawnPacket;
    }

    public void hideAll() {
        viewers.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        List<Player> players = viewers.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();
        players.forEach(this::hide);
    }
}
