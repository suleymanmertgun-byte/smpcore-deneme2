package com.smp.core;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Team {
    public final String name;
    public UUID owner;
    public final Set<UUID> members = new LinkedHashSet<>();

    public Team(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        this.members.add(owner);
    }
}
