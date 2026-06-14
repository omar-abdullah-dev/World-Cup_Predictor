package com.worldcup.repository;

import com.worldcup.model.WhitelistEntry;
import java.util.List;
import java.util.Optional;

public interface WhitelistRepository {
    WhitelistEntry save(WhitelistEntry entry);
    Optional<WhitelistEntry> findById(Long id);
    Optional<WhitelistEntry> findByAdUsername(String adUsername);
    List<WhitelistEntry> findAll();
    WhitelistEntry update(WhitelistEntry entry);
    boolean deleteById(Long id);
}
