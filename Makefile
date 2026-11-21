.PHONY: help build lint lint-check utest itest test up down restart logs clean

MVN = cd api && ./mvnw -B

help:
	@echo "make build    - budowanie API"
	@echo "make lint     - formatowanie kodu (Spotless)"
	@echo "make lint-check - samo sprawdzenie, bez poprawek (jak w CI)"
	@echo "make utest    - testy jednostkowe"
	@echo "make itest    - testy integracyjne (wymagają Dockera)"
	@echo "make test     - wszystkie testy"
	@echo "make up       - uruchomienie aplikacji i bazy w Dockerze"
	@echo "make down     - zatrzymanie kontenerów"
	@echo "make restart  - przebudowanie i ponowne uruchomienie"
	@echo "make logs     - logi aplikacji"
	@echo "make clean    - zatrzymanie kontenerów i usunięcie danych bazy"

build:
	$(MVN) package -DskipTests

lint:
	$(MVN) spotless:apply

lint-check:
	$(MVN) spotless:check

utest:
	$(MVN) test

itest:
	$(MVN) test-compile failsafe:integration-test failsafe:verify

test:
	$(MVN) verify

up:
	docker compose up -d --build

down:
	docker compose down

restart: down up

logs:
	docker compose logs -f api

clean:
	docker compose down -v
