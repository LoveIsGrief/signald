export VERSION = $(shell git describe --always --tags)
export CI_PROJECT_NAME ?= signald

all: installDist tar deb

installDist:
	./gradlew installdist

tar:
	./gradlew tar

deb:
	./gradlew deb
