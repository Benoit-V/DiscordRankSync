package fr.bebedlastreat.discord.common.mongodb;

import com.mongodb.MongoException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import fr.bebedlastreat.discord.common.interfaces.IDatabaseFetch;
import fr.bebedlastreat.discord.common.DiscordCommon;
import org.bson.Document;

import java.util.UUID;
import java.util.logging.Level;

public class MongoDBFetch implements IDatabaseFetch {

    private final MongoDBHandler mongodb;

    public MongoDBFetch(MongoDBHandler mongodb) {
        this.mongodb = mongodb;
    }

    @Override
    public boolean insert(UUID uuid, String name, String discord) {
        Document doc = new Document("uuid", uuid.toString())
                .append("name", name)
                .append("discord", discord);
        try {
            mongodb.getBaseCollection().insertOne(doc);
            return true;
        } catch (MongoException e) {
            DiscordCommon.getLogger().log(Level.SEVERE, "MongoDB insert failed: " + e.getMessage());
            return false;
        }
    }

    private static String field(Document doc, String key) {
        if (doc == null) return "";
        String value = doc.getString(key);
        return value == null ? "" : value;
    }

    @Override
    public void update(UUID uuid, String name) {
        mongodb.getBaseCollection().updateOne(
                Filters.eq("uuid", uuid.toString()),
                Updates.set("name", name)
        );
    }

    @Override
    public boolean firstLink(UUID uuid) {
        return mongodb.getAllTimeCollection().find(Filters.eq("uuid", uuid.toString())).first() == null;
    }

    @Override
    public void insertFirstLink(UUID uuid) {
        Document doc = new Document("uuid", uuid.toString());
        mongodb.getAllTimeCollection().insertOne(doc);
    }

    @Override
    public boolean delete(UUID uuid) {
        return mongodb.getBaseCollection().deleteOne(Filters.eq("uuid", uuid.toString())).getDeletedCount() > 0;
    }

    @Override
    public boolean delete(String discord) {
        return mongodb.getBaseCollection().deleteOne(Filters.eq("discord", discord)).getDeletedCount() > 0;
    }

    @Override
    public boolean exist(String discord) {
        return !uuid(discord).isEmpty();
    }

    @Override
    public boolean exist(UUID uuid) {
        return !discord(uuid).isEmpty();
    }

    @Override
    public String discord(UUID uuid) {
        return field(mongodb.getBaseCollection().find(Filters.eq("uuid", uuid.toString())).first(), "discord");
    }

    @Override
    public String discord(String name) {
        return field(mongodb.getBaseCollection().find(Filters.eq("name", name)).first(), "discord");
    }

    @Override
    public String uuid(String discord) {
        return field(mongodb.getBaseCollection().find(Filters.eq("discord", discord)).first(), "uuid");
    }

    @Override
    public String name(UUID uuid) {
        return field(mongodb.getBaseCollection().find(Filters.eq("uuid", uuid.toString())).first(), "name");
    }

    @Override
    public int count() {
        return (int) mongodb.getBaseCollection().countDocuments();
    }

    @Override
    public int allTimeCount() {
        return (int) mongodb.getAllTimeCollection().countDocuments();
    }

    @Override
    public void insertBoost(UUID uuid, long time) {
        Document doc = new Document("uuid", uuid.toString())
                .append("finish_time", time);
        mongodb.getBoostCollection().insertOne(doc);
    }

    @Override
    public void deleteBoost(UUID uuid) {
        mongodb.getBoostCollection().deleteOne(Filters.eq("uuid", uuid.toString()));
    }

    @Override
    public boolean canBoost(UUID uuid) {
        Document doc = mongodb.getBoostCollection().find(Filters.eq("uuid", uuid.toString())).first();
        if (doc == null) return true;
        Number time = doc.get("finish_time", Number.class);
        return time == null || System.currentTimeMillis() > time.longValue();
    }

    @Override
    public long getNextBoost(UUID uuid) {
        Document doc = mongodb.getBoostCollection().find(Filters.eq("uuid", uuid.toString())).first();
        if (doc == null) return 0;
        Number time = doc.get("finish_time", Number.class);
        return time == null ? 0 : time.longValue();
    }

    @Override
    public void close() {
        mongodb.closeConnection();
    }
}
