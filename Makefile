SHELL:=/usr/bin/env bash

.PHONY: default start build stop restart start-db start-db restart-db help

default: | help

start: ## Start application
	docker-compose up -d --build

build: ## Build application
	mvn verify test -P ssb-bip

stop: ## Stop application
	docker-compose down

restart: | stop start ## Restart application

start-db: ## Start database
	docker-compose up -d --build postgres

stop-db: ## Stop database and wipe its contents
	docker-compose rm -fsv postgres

restart-db: | stop-db start-db ## Restart database

help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-45s\033[0m %s\n", $$1, $$2}'
