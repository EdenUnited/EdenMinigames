package at.haha007.edenminigames;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class SqliteDatabase {

    private final String path;
    protected Connection connection;


    public SqliteDatabase(JavaPlugin plugin, String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) throw new IOException("The file should not exist!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.path = plugin.getName() + "/" + path;
    }

    public PreparedStatement prepareStatement(String statement) throws SQLException {
        return connection.prepareStatement(statement);
    }


    public void disconnect() throws SQLException {
        if (isConnected()) connection.close();
    }

    public boolean isConnected() throws SQLException {
        return connection != null && !connection.isClosed();
    }

    public void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        connection = DriverManager.getConnection(String.format("jdbc:sqlite:plugins/%s", path));
    }

    public static byte[] bytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID uuid(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();
        return new UUID(high, low);
    }
}
