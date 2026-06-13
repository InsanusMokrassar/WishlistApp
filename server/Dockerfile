FROM amazoncorretto:21

USER 1005

VOLUME /config.json
VOLUME /db
#VOLUME /static

ENTRYPOINT ["/wishlist.server/bin/wishlist.server", "/config.json"]

ADD ./build/productionExecutable.tar /static/
ADD ./build/distributions/wishlist.server.tar /
