package com.worldcup.service;

import com.worldcup.model.User;
import com.worldcup.model.WhitelistEntry;
import com.worldcup.repository.WhitelistRepository;
import com.worldcup.security.SecurityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WhitelistService {

    private WhitelistRepository whitelistRepository;

    protected WhitelistService() {}

    @Inject
    public WhitelistService(WhitelistRepository whitelistRepository) {
        this.whitelistRepository = whitelistRepository;
    }

    public List<WhitelistEntry> getAllEntries(User adminUser) {
        SecurityService.assertAdmin(adminUser, "view whitelist");
        return whitelistRepository.findAll();
    }

    public boolean isUserWhitelisted(String adUsername) {
        return whitelistRepository.findByAdUsername(adUsername)
                .map(WhitelistEntry::isEnabled)
                .orElse(false);
    }

    public WhitelistEntry addEntry(User adminUser, String adUsername, String employeeName, String email) {
        SecurityService.assertAdmin(adminUser, "add to whitelist");
        
        if (whitelistRepository.findByAdUsername(adUsername).isPresent()) {
            throw new IllegalArgumentException("User is already in the whitelist");
        }
        
        WhitelistEntry entry = new WhitelistEntry();
        entry.setAdUsername(adUsername);
        entry.setEmployeeName(employeeName);
        entry.setEmail(email);
        entry.setAddedByUserId(adminUser.getId());
        entry.setAddedAt(LocalDateTime.now());
        entry.setEnabled(true);
        
        return whitelistRepository.save(entry);
    }

    public WhitelistEntry toggleStatus(User adminUser, Long entryId, boolean enabled) {
        SecurityService.assertAdmin(adminUser, "modify whitelist");
        WhitelistEntry entry = whitelistRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Whitelist entry not found"));
        
        entry.setEnabled(enabled);
        return whitelistRepository.update(entry);
    }

    public void removeEntry(User adminUser, Long entryId) {
        SecurityService.assertAdmin(adminUser, "remove from whitelist");
        if (!whitelistRepository.deleteById(entryId)) {
            throw new IllegalArgumentException("Whitelist entry not found");
        }
    }
}
