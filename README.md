# Sirius Rawdata Converter
[![Build Status](https://drone.prod-bip-ci.ssb.no/api/badges/statisticsnorway/rawdata-converter-app-sirius/status.svg)](https://drone.prod-bip-ci.ssb.no/statisticsnorway/rawdata-converter-app-sirius)
[![Coverage](https://sonarqube.prod-bip-ci.ssb.no/api/project_badges/measure?project=no.ssb.rawdata.converter%3Arawdata-converter-app-sirius&metric=coverage)](https://sonarqube.prod-bip-ci.ssb.no/dashboard?id=no.ssb.rawdata.converter%3Arawdata-converter-app-sirius)
[![Quality Gate Status](https://sonarqube.prod-bip-ci.ssb.no/api/project_badges/measure?project=no.ssb.rawdata.converter%3Arawdata-converter-app-sirius&metric=alert_status)](https://sonarqube.prod-bip-ci.ssb.no/dashboard?id=no.ssb.rawdata.converter%3Arawdata-converter-app-sirius)

Convert Sirius xml (skattemelding) rawdata to [Parquet](https://en.wikipedia.org/wiki/Apache_Parquet).

It is recommended that you work with this repo via the [rawdata-converter-project](https://github.com/statisticsnorway/rawdata-converter-project).


## Development

### Running from within an IDEA

When running the application from your IDEA, make sure to specify the `-Dmicronaut.environments=local`
VM param in order for micronaut to load overridden config from `application-local.properties`.


## Make targets

You can use `make` to execute common tasks:
```
build-all                      Build all and create docker image (requires rawdata-converter-project)
build-all-mvn                  Build all from parent (requires rawdata-converter-project)
build-mvn                      Build the project and install to you local maven repo
build-docker                   Build the docker image
run-local                      Run the app locally (without docker)
release-dryrun                 Simulate a release in order to detect any issues
release                        Release a new version. Update POMs and tag the new version in git. Drone deploys upon tag detection.
```
