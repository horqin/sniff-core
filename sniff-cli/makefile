CC = gcc
CFLAGS = -Og -D_DEBUG
LIBS = -pthread -lcjson -lcurl -lpcap -lsodium -lzookeeper_mt

all:
	$(CC) $(CFLAGS) $(LIBS) -o sniff main.c utils/*.c
clean:
	rm -f sniff
