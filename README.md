# World Cup Predictor

A web-based football match prediction platform built with **Java 17**, **Jakarta EE 10**, and **Jakarta Faces (JSF 4)**.
The application is packaged as a **WAR** and is designed to run on **WildFly 27+**.

## Overview

Users can:

- create participants for the competition
- schedule football matches
- submit score predictions before kickoff
- record final results
- view a live-updated leaderboard

The application uses an **in-memory repository layer** backed by `ConcurrentHashMap`, so it is easy to run locally and can be swapped for JDBC/JPA later without changing the service layer.

## Architecture

```text
JSF Views (XHTML)
      |
Managed Beans (CDI @RequestScoped)
      |
Service Layer (CDI @ApplicationScoped)
      |
Repository Layer (interface abstraction)
      |
In-Memory Store (ConcurrentHashMap)
```

## Main Features

- User management with duplicate-name validation
- Match scheduling with kickoff date/time support
- Prediction submission with business-rule validation
- Automatic scoring when a match result is recorded
- Leaderboard sorted by total points descending, then username ascending
- Auto-seeded sample data on application startup

## Scoring Rules

| Result | Points |
|---|---:|
| Exact score | 2 |
| Correct outcome | 1 |
| Incorrect | 0 |

`ScoringServiceTest` covers the scoring rules and the unfinished-match validation.

## Business Rules

- Predictions must be submitted **before kickoff**.
- Predictions cannot be submitted for matches that are already finished.
- Match results cannot be recorded with negative scores.
- Each user can predict a match only once.
- When a match result is saved, all predictions for that match are re-scored and user totals are recalculated.

## Seed Data

On startup, `DataInitializer` creates sample data:

- **8 users**: `MessiFan2026`, `RonaldoCR7`, `NeymarJr11`, `MbappeStar`, `Modric10`, `Lewandowski9`, `ViniciusJr`, `Bellingham22`
- **12 group-stage matches** with kickoff dates in **June 2027**

## Pages

- `index.xhtml` – home dashboard
- `users.xhtml` – add and view users
- `matches.xhtml` – schedule matches and record results
- `predictions.xhtml` – submit predictions and review entries
- `leaderboard.xhtml` – view standings

## Technology Stack

- Java 17
- Maven
- Jakarta EE 10
- Jakarta Faces (JSF 4)
- CDI
- WildFly 27+
- JUnit 5

## Project Structure

```text
src/main/java/com/worldcup/
  bean/         JSF backing beans
  config/       application startup seeding
  model/        domain objects
  repository/   in-memory persistence layer
  service/      business logic
src/main/webapp/
  *.xhtml       JSF views
  resources/    static assets such as CSS
```

## Prerequisites

- Java 17 or later
- Maven 3.8+
- WildFly 27+ or JBoss EAP 8+

## Build

```bash
mvn clean package
```

This produces `target/world-cup-predictor.war`.

## Run / Deploy to WildFly

Copy the WAR to your WildFly deployments folder:

```bash
Copy-Item target\world-cup-predictor.war $env:WILDFLY_HOME\standalone\deployments\
```

Or deploy with the WildFly CLI:

```bash
$env:WILDFLY_HOME\bin\jboss-cli.bat --connect "deploy target/world-cup-predictor.war"
```

After deployment, open:

```text
http://localhost:8080/world-cup-predictor/
```

## Notes

- `src/main/resources/messages.properties` provides the application resource bundle.
- The project is intentionally configured for a full Jakarta EE runtime, so APIs such as JSF and CDI are provided by the server.
- `maven-war-plugin` is configured to build a deployable WAR without requiring `web.xml`-based CDI bootstrap entries.

## Testing

Run the test suite with:

```bash
mvn test
```

