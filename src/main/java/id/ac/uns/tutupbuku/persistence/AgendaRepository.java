package id.ac.uns.tutupbuku.persistence;

import id.ac.uns.tutupbuku.domain.AgendaItem;

import java.util.List;

public interface AgendaRepository {
    List<AgendaItem> list(String owner, long semesterId, long courseId);

    AgendaItem find(String owner, long id);

    AgendaItem save(String owner, AgendaItem item);

    void delete(String owner, long id, long version);
}
