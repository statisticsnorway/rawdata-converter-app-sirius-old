# Sirius Rawdata Converter
[![Build Status](https://dev.azure.com/statisticsnorway/Dapla/_apis/build/status/statisticsnorway.rawdata-converter-app-sirius?branchName=master)](https://dev.azure.com/statisticsnorway/Dapla/_build/latest?definitionId=48&branchName=master)

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
