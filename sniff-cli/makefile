CC = gcc
CFLAGS = -Og
LIBS = -pthread -lcjson -lcurl -lpcap -luv -lzookeeper_mt

all:
	$(CC) -D_DEBUG $(CFLAGS) $(LIBS) -o sniff main.c
clean:
	rm -f sniff
