FROM amazoncorretto:21

USER 1005

VOLUME /config.json
VOLUME /db
VOLUME /static

ENTRYPOINT ["/project_name.server/bin/project_name.server", "/config.json"]

ADD ./build/distributions/project_name.server.tar /
