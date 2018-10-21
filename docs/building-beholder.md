# Building Beholder

A ready-made Docker image is available at https://hub.docker.com/r/johnnywoo/beholder

To use the Docker container, mount your config into it as `/etc/beholder/beholder.conf`.

To build the jar file with all dependencies from source:

    $ ./gradlew jar
    $ ls build/libs/beholder*.jar

To build a Docker container with the jar:

    $ docker build -t beholder .
    $ docker run -ti beholder beholder
    usage: beholder
    ...
