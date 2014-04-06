package org.valich.fsview.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class History<E> {
    private List<E> history = new ArrayList<>();
    private int position;

    public History() {
        position = -1;
    }

    @SuppressWarnings("unused")
    public synchronized E getCurrent() throws IllegalStateException {
        if (position < 0)
            throw new IllegalStateException("History is empty");

        return history.get(position);
    }

    public synchronized int getSize() {
        return history.size();
    }

    public synchronized int getPosition() {
        return position;
    }

    @SuppressWarnings("unused")
    public synchronized List<E> getHistory() {
        return Collections.unmodifiableList(history);
    }

    @SuppressWarnings("unused")
    public synchronized List<E> getHistory(int from, int to) {
        return Collections.unmodifiableList(history.subList(from, to));
    }

    public synchronized void add(E entry) {
        while (history.size() > position + 1) {
            history.remove(history.size() - 1);
        }

        history.add(entry);
        position++;
    }

    public synchronized E back() throws IllegalStateException {
        if (position <= 0)
            throw new IllegalStateException("No earlier entry");

        position--;
        return history.get(position);
    }

    public synchronized E forward() throws IllegalStateException {
        if (position + 1 == history.size())
            throw new IllegalStateException("No next entry");

        position++;
        return history.get(position);
    }
}