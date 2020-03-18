.PHONY: default
default: | help

.PHONY: build-all
build-all: build-all-mvn build-docker ## Build all and create docker image (requires rawdata-converter-project)

.PHONY: build-all-mvn
build-all-mvn: ## Build all from parent (requires rawdata-converter-project)
	./mvnw -f ../pom.xml clean install

.PHONY: build-mvn
build-mvn: ## Build the project and install to you local maven repo
	./mvnw clean install

.PHONY: build-docker
build-docker: ## Build the docker image
	docker build -t rawdata-converter-app-sirius:dev -f Dockerfile .

.PHONY: run-local
run-local: ## Run the app locally (without docker)
	java -Dcom.sun.management.jmxremote -noverify ${JAVA_OPTS} -Dmicronaut.environments=local -jar target/rawdata-converter-app-*.jar

.PHONY: release-dryrun
release-dryrun: ## Simulate a release in order to detect any issues
	./mvnw release:prepare release:perform -Darguments="-Dmaven.deploy.skip=true" -DdryRun=true -DtagNameFormat=@{project.version}

.PHONY: release
release: ## Release a new version. Update POMs and tag the new version in git. Drone deploys upon tag detection.
	./mvnw release:prepare release:perform -Darguments="-Dmaven.deploy.skip=true -Dmaven.javadoc.skip=true" -DtagNameFormat=@{project.version}

.PHONY: help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
