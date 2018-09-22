export VERSION = $(shell git describe --always --tags)
export CI_PROJECT_NAME ?= signald

all: installDist tar deb

installDist distTar deb:
	./gradlew $<
