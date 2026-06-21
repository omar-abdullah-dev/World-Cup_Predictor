# Deprecated Files

This folder contains source files that have been superseded by the JDBC migration.
They are preserved here for reference and rollback purposes.

## Contents

- `InMemoryMatchRepository.java` — original in-memory match store (already commented out in source)
- `InMemoryUserRepository.java` — original in-memory user store (already commented out in source)
- `InMemoryPredictionRepository.java` — original in-memory prediction store

These files were NOT deleted. They remain in the main source tree (commented out) and are
additionally archived here for safety.

## Migration Branch

All Hibernate/JPA removal changes are in the `oracle-jdbc-migration` branch.
