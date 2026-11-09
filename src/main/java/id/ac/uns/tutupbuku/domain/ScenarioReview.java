package id.ac.uns.tutupbuku.domain;

import java.util.List;

public record ScenarioReview(List<String> changes) {
    public ScenarioReview {
        changes = List.copyOf(changes);
    }

    public boolean outdated() {
        return !changes.isEmpty();
    }

    public String message() {
        return outdated()
                ? "Perlu ditinjau / snapshot kedaluwarsa:\n" + String.join("\n", changes)
                : "Acuan skenario sesuai data terkini.";
    }
}
