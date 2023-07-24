package org.noxet.noxetserver.realm;


import org.bukkit.Location;
import org.noxet.noxetserver.util.ConfigManager;

import javax.annotation.Nullable;

public class RealmDataManager extends ConfigManager {
    @Override
    protected String getFileName() {
        return "realm-data";
    }

    private String getRealmKey(RealmManager.Realm realm, String key) {
        String realmName = realm != null ? realm.name() : "_";
        return realmName + "." + key;
    }

    public void setSoftSpawnLocation(RealmManager.Realm realm, @Nullable Location location) {
        if(location != null) {
            location.setX(Math.round(location.getX() * 2) / 2.0);
            location.setY(Math.round(location.getY() * 2) / 2.0);
            location.setZ(Math.round(location.getZ() * 2) / 2.0);

            location.setYaw(Math.round(Math.abs(location.getYaw()) / 90) * 90 * Math.signum(location.getYaw()));
            location.setPitch(Math.round(Math.abs(location.getPitch()) / 90) * 90 * Math.signum(location.getPitch()));
        }

        config.set(getRealmKey(realm, "spawn"), location);
        save();
    }

    public Location getSpawnLocation(RealmManager.Realm realm) {
        return config.getLocation(getRealmKey(realm, "spawn"));
    }
}
