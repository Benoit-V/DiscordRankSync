package fr.bebedlastreat.discord.common.interfaces;

import java.util.UUID;

public interface IDatabaseFetch {

    /**
     * @return true if the link was stored
     */
    boolean insert(UUID uuid, String name, String discord);

    void update(UUID uuid, String name);

    boolean firstLink(UUID uuid);

    void insertFirstLink(UUID uuid);

    /**
     * @return true if a link was deleted
     */
    boolean delete(UUID uuid);

    /**
     * @return true if a link was deleted
     */
    boolean delete(String discord);

    boolean exist(String discord);

    boolean exist(UUID uuid);

    String discord(UUID uuid);

    String discord(String name);

    String uuid(String discord);

    String name(UUID uuid);

    int count();

    int allTimeCount();

    void insertBoost(UUID uuid, long time);

    void deleteBoost(UUID uuid);

    boolean canBoost(UUID uuid);

    long getNextBoost(UUID uuid);

    void close();
}
