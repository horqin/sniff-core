CC = gcc
CFLAGS = -Og
LIBS = -pthread -lcjson -lcurl -lpcap -luv -lzookeeper_mt

all:
	$(CC) -D_DEBUG $(CFLAGS) main.c $(LIBS)-o sniff
clean:
	rm -f sniff
