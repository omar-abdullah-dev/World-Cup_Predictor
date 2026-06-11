//package com.worldcup.repository;
//
//import com.worldcup.model.User;
//import jakarta.enterprise.context.ApplicationScoped;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.stream.Collectors;
//
///**
// * Thread-safe in-memory User store.
// * Swap this class for a JDBC / JPA implementation without touching business logic.
// */
//@ApplicationScoped
//public class InMemoryUserRepository implements UserRepository {
//
//    private final ConcurrentHashMap<Long, User> store = new ConcurrentHashMap<>();
//    private final AtomicLong idGenerator = new AtomicLong(1);
//
//    @Override
//    public User save(User user) {
//        if (user.getId() == null) user.setId(idGenerator.getAndIncrement());
//        store.put(user.getId(), user);
//        return user;
//    }
//
//    @Override
//    public Optional<User> findById(Long id) {
//        return Optional.ofNullable(store.get(id));
//    }
//
//    @Override
//    public Optional<User> findByUsername(String username) {
//        return store.values().stream()
//                .filter(u -> u.getUsername().equals(username))
//                .findFirst();
//    }
//
//    @Override
//    public List<User> findAll() {
//        return store.values().stream().collect(Collectors.toList());
//    }
//
//    @Override
//    public User update(User user) {
//        if (user.getId() == null || !store.containsKey(user.getId()))
//            throw new IllegalArgumentException("Cannot update non-existent user. ID: " + user.getId());
//        store.put(user.getId(), user);
//        return user;
//    }
//}
