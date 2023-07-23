package org.noxet.noxetserver.util;

import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

public class ControllableTaskSet {
    private final Set<BukkitTask> tasks = new HashSet<>();

    public void push(BukkitTask task) {
        tasks.add(task);
    }

    public void abortAll() {
        tasks.forEach(BukkitTask::cancel);
    }
}
