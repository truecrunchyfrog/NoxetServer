package org.noxet.noxetserver.messaging.channels;

import org.bukkit.command.CommandSender;
import org.noxet.noxetserver.RealmManager;

import java.util.HashSet;
import java.util.Set;

public class MessagingRealmChannel extends MessagingChannel {
    private final RealmManager.Realm realm;

    public MessagingRealmChannel(RealmManager.Realm realm) {
        this.realm = realm;
    }

    @Override
    public Set<CommandSender> getRecipients() {
        return new HashSet<>(realm.getPlayers());
    }
}
