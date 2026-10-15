package com.cmacfarl.sensei.tutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * One thread: a new sentence plus its follow-ups. The provider and model are fixed when the
 * thread starts. Turns are only ever appended. Plain fields so it can be serialized with Gson
 * if history is added later.
 */
public final class Conversation {

    public final String id;
    public final long createdAt;
    public final String provider;
    public final String model;
    private final List<Turn> turns = new ArrayList<>();

    public Conversation(String provider, String model) {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.provider = provider;
        this.model = model;
    }

    public List<Turn> turns() {
        return Collections.unmodifiableList(turns);
    }

    public boolean isEmpty() {
        return turns.isEmpty();
    }

    /** Mode of the thread's first message, or null if nothing has been asked yet. */
    public Mode mode() {
        return turns.isEmpty() ? null : turns.get(0).mode;
    }

    /** Adds a completed exchange. Both turns go in together so the history never ends on a user turn. */
    public void append(Turn student, Turn tutor) {
        turns.add(student);
        turns.add(tutor);
    }
}
