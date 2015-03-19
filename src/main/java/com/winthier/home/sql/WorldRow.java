package com.winthier.home.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(
    name = "worlds",
    uniqueConstraints = @UniqueConstraint(columnNames={"name"})
    )
@Getter
@Setter
public class WorldRow {
    private static final Map<String, WorldRow> cache = new HashMap<>();
    public static final int MAX_WORLD_NAME_LENGTH = 32;

    @Id
    private Integer id;

    @NotEmpty
    @Length(max = MAX_WORLD_NAME_LENGTH)
    private String name;

    @Version
    private Integer version;

    public static WorldRow find(@NonNull String name) {
        WorldRow result;
        result = cache.get(name);
        if (result != null) return result;
        result = DB.get().find(WorldRow.class).where().eq("name", name).findUnique();
        cache.put(name, result); // May put null;
        return result;
    }

    public static WorldRow findOrCreate(@NonNull String name) {
        if (name.length() == 0) throw new IllegalArgumentException("World name cannot be empty");
        if (name.length() > MAX_WORLD_NAME_LENGTH) throw new IllegalArgumentException("World name cannot be longer than " + MAX_WORLD_NAME_LENGTH);
        WorldRow result;
        result = find(name);
        if (result == null) {
            result = new WorldRow();
            result.setName(name);
            DB.get().save(result);
            cache.put(name.toLowerCase(), result);
        }
        return result;
    }

    public static void clearCache() {
        cache.clear();
    }
}
