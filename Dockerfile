FROM java
WORKDIR /usr/src/app
ENV CONSUL_IP http://40.114.79.33:8500
COPY ./elastic-apm-agent-0.7.0.jar ./
COPY ./target/SabbioProxy-0.0.1-SNAPSHOT-jar-with-dependencies.jar ./
EXPOSE 5555
CMD java -javaagent:./elastic-apm-agent-0.7.0.jar -Delastic.apm.service.name=clietn_prova -Delastic.apm.server_urls=http://88.147.126.145:8010 -Delastic.apm.application_packages=it.imolinfo -jar ./SabbioProxy-0.0.1-SNAPSHOT-jar-with-dependencies.jar
