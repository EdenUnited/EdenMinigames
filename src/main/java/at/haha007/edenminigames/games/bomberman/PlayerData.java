package at.haha007.edenminigames.games.bomberman;

import at.haha007.edenminigames.EdenMinigames;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

final class PlayerData {
    private int lives = 1;
    private int blastDistance = 1;
    private int bombs = 1;

    public int getLives() {
        return lives;
    }

    public int getBlastDistance() {
        return blastDistance;
    }

    public int getBombs() {
        return bombs;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public void setBlastDistance(int blastDistance) {
        this.blastDistance = blastDistance;
    }

    public void setBombs(int bombs) {
        this.bombs = bombs;
    }

    public ItemStack getBlastDistanceItem() {
        return new ItemStack(Material.BLAZE_POWDER, blastDistance);
    }

    public ItemStack getBombsItem() {
        return new ItemStack(Material.TNT, bombs);
    }

    public ItemStack getLivesItem() {
        return new ItemStack(Material.GOLDEN_APPLE, lives);
    }

    public void placedBomb(Runnable runnable) {
        bombs--;
        Bukkit.getScheduler().runTaskLater(EdenMinigames.instance(), () -> {
            bombs++;
            if(runnable != null)
                runnable.run();
        }, 80);
    }

    public void damage() {
        lives--;
    }

    public boolean isAlive() {
        return lives > 0;
    }
}
