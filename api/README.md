# TravelOnTheGravel - API

Backend aplikacji TravelOnTheGravel wykonany z użyciem Spring Boot 4.

## Wymagania

- **JDK 25** (np. Eclipse Temurin 25) — `java -version` musi zwracać 25
- **Docker** (uruchomiony) — wymagany do testów integracyjnych i lokalnego uruchomienia z Testcontainers
- Maven nie jest wymagany — projekt używa Maven Wrappera (`./mvnw`, na Windows `mvnw.cmd`)

Wszystkie polecenia poniżej wykonujemy z katalogu `api/`.

## Uruchomienie w kontenerach (Docker Compose)

Aplikacja i baza PostgreSQL uruchamiają się jednym poleceniem. Polecenia wykonujemy
z **głównego katalogu repozytorium** (tam jest `docker-compose.yml`):

```bash
cp .env.example .env
docker compose up --build
```

- zatrzymanie: `docker compose down`
- zatrzymanie i usunięcie danych bazy: `docker compose down -v`
- przebudowanie aplikacji po zmianach w kodzie: `docker compose up --build`

## Uruchomienie lokalne

Najprostszy sposób — aplikacja sama postawi kontener PostgreSQL przez Testcontainers
(klasa `TestTravelonthegravelApplication` w `src/test`):

```bash
./mvnw spring-boot:test-run
```

Alternatywnie, z własną bazą PostgreSQL:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/travelonthegravel \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=postgres \
./mvnw spring-boot:run
```

Po starcie aplikacja jest dostępna pod `http://localhost:8080`:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/actuator/health`

Migracje bazy danych (Liquibase) znajdują się w `src/main/resources/db/changelog/`
i są wykonywane automatycznie przy starcie aplikacji.

## Build

```bash
./mvnw clean package
```

Wynikowy JAR: `target/travelonthegravel-0.0.1-SNAPSHOT.jar`. Aby zbudować bez testów, dodaj `-DskipTests`.

## Testy

Testy jednostkowe (`*Test.java`, `*Tests.java` — surefire):

```bash
./mvnw test
```

Testy jednostkowe + integracyjne (`*IT.java` — failsafe, wymagają Dockera):

```bash
./mvnw verify
```

## Linter

Używamy **Spotless** z formatem **Palantir Java Format** — formatuje kod (wcięcia, klamry, spacje, długość linii, importy).

Autopoprawki (z katalogu głównego repozytorium):

```bash
make lint
```

Samo sprawdzenie, bez zmieniania kodu (tak samo jak w CI):

```bash
make lint-check
```

Bez `make`, z katalogu `api/`: `./mvnw spotless:apply` (poprawki) albo `./mvnw spotless:check` (sprawdzenie).
