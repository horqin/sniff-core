CC = gcc
CFLAGS = -Og -D_DEBUG
LIBS = -pthread -lcjson -lcurl -lpcap -lsodium -luv -lzookeeper_mt

all:
	$(CC) $(CFLAGS) $(LIBS) -o sniff main.c
clean:
	rm -f sniff
