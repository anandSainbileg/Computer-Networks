FROM openjdk:11-jdk-slim

WORKDIR /app

COPY . .

RUN javac Client.java
RUN apt-get update
RUN apt-get install -y tcpdump
CMD tcpdump -i any -w traffic.pcap -U & java Client

# Author Anand Sainbileg 20327050


