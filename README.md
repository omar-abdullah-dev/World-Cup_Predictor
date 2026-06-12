# World Cup Predictor

A secure, web-based football match prediction platform built with **Java 17**, **Jakarta EE 10**, and **Jakarta Faces (JSF 4)**. The application is packaged as a **WAR** and runs on **WildFly 27+** with support for both in-memory repositories and persistent **PostgreSQL** storage via JPA.

---

## 🏗️ Architecture

The application is structured into a classic multi-layered enterprise architecture:

```text
       JSF Views (XHTML / JSF 4)
                  │
   Security Filter (Jakarta Servlet Filter)
                  │
  Managed Beans (CDI @Named, @RequestScoped / @SessionScoped)
                  │
  Service Layer (CDI @ApplicationScoped, Transactions)
                  │
 Repository Layer (Interface abstraction)
       ├── JpaUserRepository (Active default, PostgreSQL)
       ├── InMemoryMatchRepository / JpaMatchRepository
       └── InMemoryPredictionRepository / JpaPredictionRepository
```

The repository layer is decoupled from the business logic through Java interfaces, allowing seamless swapping of storage mechanisms (e.g., in-memory map vs. persistent database) via CDI qualifiers/alternatives.

---

## 🌟 Main Features

*   **User Registration & Authentication**: 
    *   Sign-up page with validation (duplicate usernames, password strength).
    *   Secure login process powered by **PBKDF2 password hashing** (`PasswordService`).
    *   Token generation and verification using **JWT (JSON Web Token)** (`JwtTokenService`).
    *   A Servlet `SecurityFilter` that guards pages (only `/login.xhtml` and `/register.xhtml` are public).
*   **Role-Based Access Control**:
    *   `NORMAL_USER`: Can view dashboard, matches, group standings, submit and track score predictions.
    *   `ADMIN`: Access to user administration (`users.xhtml`), schedule matches, and record match results.
*   **Match Management & Seeding**:
    *   Admins can schedule matches specifying teams, groups, and kickoff times.
    *   Admins can record final scores, automatically triggering scoring calculations.
*   **Prediction Submission**:
    *   Users predict match scores (home and away scores) prior to kickoff.
    *   Enforces business logic (e.g., predictions are locked after kickoff).
*   **Live Leaderboard & Group Standings**:
    *   A leaderboard tracking user points sorted descending, using username ascending as a tiebreaker.
    *   Group detailed stats with team records (Wins, Draws, Losses, Goals For/Against, Points).

---

## ⚽ Scoring Rules

When match results are recorded, predictions are automatically evaluated:

| Match Result vs. Prediction | Points Awarded | Example |
| :--- | :---: | :--- |
| **Exact Score** | **2 points** | Prediction: `2-1` \| Result: `2-1` |
| **Correct Outcome** (Win/Loss/Draw) | **1 point** | Prediction: `3-1` \| Result: `1-0` |
| **Incorrect Outcome** | **0 points** | Prediction: `1-1` \| Result: `0-2` |

---

## 🔒 Business & Security Rules

*   **Prediction Timing**: Predictions must be submitted **before kickoff**. Once a match starts or is marked `IN_PROGRESS` / `FINISHED`, predictions are locked.
*   **No Negative Scores**: Score records and predictions cannot have negative values.
*   **Uniqueness constraint**: Each user can only predict a given match once.
*   **Re-scoring**: Modifying or updating match scores recalculates all predictions for that match and updates users' points instantly.

---

## 🗄️ Database Setup (PostgreSQL)

If using the persistent database layer, you can run the PostgreSQL initialization script located at [database_init.sql](file:///c:/Users/moham/Desktop/work%20practice/World_Cup_Prediction_fixed/database_init.sql):

1.  Create a PostgreSQL database named `world_cup_predictor`.
2.  Execute `database_init.sql` to generate tables (`users`, `matches`, `predictions`), indices, and seed default records:
    *   **Admin user**: `admin` (password: `admin123`, PBKDF2 hashed)
    *   **Matches**: Seeded sample group matches.

---

## 📦 Technology Stack

*   **Java 17** & **Maven**
*   **Jakarta EE 10** (Jakarta Faces 4, CDI, JPA, Servlet, Transactions)
*   **Hibernate** (JPA provider)
*   **PostgreSQL JDBC Driver**
*   **JJWT (Java JWT)** for token-based security utilities
*   **WildFly 27+** (WildFly application server)
*   **JUnit 5** for unit and integration testing

---

## 📁 Project Structure

```text
src/main/java/com/worldcup/
  ├── bean/         JSF Backing/Managed Beans (AuthBean, MatchBean, LeaderboardBean, GroupsBean, etc.)
  ├── config/       Application startup hooks and data seeding (DataInitializer)
  ├── model/        Domain Entities (User, Match, Prediction)
  ├── repository/   Data Access layer (JPA and In-Memory repositories)
  ├── security/     Security implementations (Password hashing, JWT Token Service, Security servlet Filter)
  └── service/      Core business logic & workflows (UserService, MatchService, PredictionService)
src/main/resources/
  ├── META-INF/     JPA Configuration (persistence.xml)
  └── messages.properties (Localization bundle)
src/main/webapp/
  ├── WEB-INF/      beans.xml (discovery settings) and faces-config.xml
  ├── templates/    JSF layout templates
  ├── resources/    CSS files and images
  └── *.xhtml       JSF pages (login, register, dashboard, groups, matches, predictions, leaderboard, users)
```

---

## 🚀 Setup & Deployment

### 1. Build the WAR Artifact

Run the clean package command to compile, test, and bundle the app:

```bash
mvn clean package
```

This generates `target/world-cup-predictor.war`.

### 2. WildFly Configuration (JPA Mode)

Configure a JTA Datasource named `WorldCupDS` in WildFly:

Using WildFly CLI:

```bash
$env:WILDFLY_HOME\bin\jboss-cli.bat --connect
[standalone@localhost:9990 /] data-source add --name=WorldCupDS --jndi-name=java:jboss/datasources/WorldCupDS --driver-name=postgresql --connection-url=jdbc:postgresql://localhost:5432/world_cup_predictor --user-name=postgres --password=yourpassword
```

*(Ensure the PostgreSQL JDBC Driver module is registered and active in your WildFly configuration).*

### 3. Deploy to WildFly

Copy the WAR package to the WildFly deployment directory:

```bash
Copy-Item target\world-cup-predictor.war $env:WILDFLY_HOME\standalone\deployments\
```

Or deploy using the CLI:

```bash
$env:WILDFLY_HOME\bin\jboss-cli.bat --connect --command="deploy target/world-cup-predictor.war --force"
```

Once running, navigate to:
```text
http://localhost:8080/world-cup-predictor/
```

---

## 🧪 Testing

Run the JUnit test suite (e.g. testing outcome points and validation constraints) with:

```bash
mvn test
```
