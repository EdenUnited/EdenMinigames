package at.haha007.edenminigames.games.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Consumer;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Path {
    private final World world;
    private final List<Vector> path;

    private final List<Consumer<Path>> pathChangedListeners = new ArrayList<>();

    public Path(World world, List<Vector> path) {
        this.world = world;
        if (path.isEmpty()) throw new IllegalArgumentException("Path must not be empty");
        this.path = new ArrayList<>(path);
    }

    public World getWorld() {
        return world;
    }

    public boolean addPathChangedListener(Consumer<Path> listener) {
        if (pathChangedListeners.contains(listener)) return false;
        pathChangedListeners.add(listener);
        return true;
    }

    public boolean removePathChangedListener(Consumer<Path> listener) {
        return pathChangedListeners.remove(listener);
    }

    public void addNode(Vector vector) {
        path.add(vector);
        pathChangedListeners.forEach(c -> c.accept(this));
    }

    public void addNode(int index, Vector vector) {
        path.add(index, vector);
        pathChangedListeners.forEach(c -> c.accept(this));
    }

    public void removeNode(int index) {
        if (path.size() == 1) throw new IllegalArgumentException("Path must not be empty");
        path.remove(index);
        pathChangedListeners.forEach(c -> c.accept(this));
    }

    public Vector get(int index) {
        return path.get(index);
    }

    public int size() {
        return path.size();
    }

    public List<Vector> getPath() {
        return new ArrayList<>(path);
    }

    public void setPath(List<Vector> path) {
        if (path.isEmpty()) throw new IllegalArgumentException("Path must not be empty");
        this.path.clear();
        this.path.addAll(path);
        pathChangedListeners.forEach(c -> c.accept(this));
    }

    public Vector getStart() {
        return path.get(0);
    }

    public Vector getEnd() {
        return path.get(path.size() - 1);
    }

    public Location location(int index) {
        return path.get(index).toLocation(world);
    }

    public Location locationStart() {
        return path.get(0).toLocation(world);
    }

    public Location locationEnd() {
        return path.get(path.size() - 1).toLocation(world);
    }
}
