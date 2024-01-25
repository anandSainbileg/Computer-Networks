FROM alpine:latest

WORKDIR /traffic

RUN apk update
RUN apk add tcpdump

CMD tcpdump -U -w traffic.pcap -i any

# Author Anand Sainbileg 20327050