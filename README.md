# TravelOnTheGravel

## Dokumentacja

- [Uruchomienie API](api/README.md) — wymagania, uruchomienie w kontenerach, uruchomienie lokalne, build i testy

Szybki start (Docker):

```bash
cp .env.example .env
docker compose up --build
```

Najczęstsze komendy są w `Makefile` (wpisz `make`, żeby zobaczyć listę).

## Konwencje

**Typy:** `feat`, `fix`, `chore`, `docs`, `test`

**Scope:** `api`, `web`, `mobile`

- **Branche:** `<typ>/<scope>#<numerIssue>`, np. `feat/api#12`
- **Commity:** PR-y mergujemy przez *Squash and merge* z tytułem `<typ>(<scope>): opis`, np. `feat(api): logowanie użytkownika`
- **Szablony:** issue i PR piszemy według przypiętych issues w repozytorium
- **Review:** w każdym PR przypisujemy osoby do *Reviewers*
- **Labele:** tylko na issue, label = typ zmiany
- **Zadania:** każde issue przypisujemy do projektu [TravelOnTheGravel](https://github.com/users/sgorski00/projects/1) (tablica Kanban); w opisie PR dodajemy `Closes #<numer>`
