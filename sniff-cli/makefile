CC = gcc
CFLAGS = -Og
LIBS = -pthread -lcjson -lcurl -lpcap -luv -lzookeeper_mt

all:
	$(CC) -D_DEBUG $(CFLAGS) $(LIBS) main.c -o sniff
clean:
	rm -f sniff
