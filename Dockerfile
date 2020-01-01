FROM darecosystem/java8-deb

MAINTAINER Alikhanm<dbom12360@gmail.com@gmail.com>


ENV APP_NAME app.zip
ENV APP_DIR app
ENV JAVA_OPTS -server -Xms128M -Xmx512M -Xss1M -XX:+CMSClassUnloadingEnabled
ENV RUN_SCRIPT bhle-lecture-demos
ENV LOG_DIR /dar/logs/bhle-lecture-demos
ENV LOG_ARCHIVE_DIR /dar/logs/archive/bhle-lecture-demos
ENV config  application.conf
ENV logback logback.xml

# logs
RUN mkdir -p /root/config/ \
    && mkdir -p $LOG_DIR \
    && mkdir -p $LOG_ARCHIVE_DIR

COPY ./src/main/resources/*logback.xml /root/config/
COPY ./src/main/resources/*.conf /root/config/

WORKDIR /root
COPY ./target/universal/$APP_NAME /root/
RUN unzip -q $APP_NAME
WORKDIR /root/$APP_DIR/bin
CMD chmod +x $RUN_SCRIPT
EXPOSE 8080
CMD ./$RUN_SCRIPT