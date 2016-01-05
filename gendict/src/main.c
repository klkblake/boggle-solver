#include "common.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#define SIZE (1 << 18)
#define MASK (SIZE - 1)

typedef struct {
	u32 hashes[SIZE];
	u32 keys[SIZE];
} Table;

u32 hash(u8 *key, usize len) {
	u32 h = 7;
	for (u32 i = 0; i < len; i++) {
		h = h * 31 + key[i];
	}
	return (h & MASK) | (u32)len << 18;
}

// Assume no duplicates
void insert(Table *table, u8 *key, usize len, u32 key_offset) {
	u32 h = hash(key, len);
	u32 bucket = h & MASK;
	while (table->hashes[bucket] != 0) {
		bucket = (bucket + 1) & MASK;
	}
	table->hashes[bucket] = h;
	table->keys[bucket] = key_offset;
}

global_variable char *prog_name;

__attribute__((noreturn))
void die() {
	perror(prog_name);
	exit(1);
}

u64 do_or_die(s64 result) {
	if (result == -1) {
		die();
	}
	return (u64) result;
}

void write_or_die(void *ptr, usize size, FILE *file) {
	usize result = fwrite(ptr, 1, size, file);
	if (result != size) {
		die();
	}
}

int main(int argc, char **argv) {
	prog_name = argv[0];
	if (argc != 2) {
		printf("usage: %s words.lst > dict\n", argv[0]);
		return 1;
	}
	FILE *file = fopen(argv[1], "r");
	if (file == NULL) {
		perror(argv[0]);
		return 1;
	}
	do_or_die(fseek(file, 0, SEEK_END));
	u32 size = (u32) do_or_die(ftell(file));
	do_or_die(fseek(file, 0, SEEK_SET));
	u8 *file_data = malloc(size);
	usize result = fread(file_data, 1, size, file);
	if (result != size) {
		die();
	}
	for (u32 i = 0; i < size; i++) {
		if (file_data[i] == '\n') {
			file_data[i] = 0;
		}
	}
	Table table = {};
	u8 *next = file_data;
	u8 *end = file_data + size;
	u32 offset = 0;
	while (next < end) {
		usize len = strlen((char *)next);
		insert(&table, next, len, offset);
		memmove(file_data + offset, next, len);
		next += len + 1;
		offset += len;
		size--;
	}
	u32 tmp = SIZE;
	write_or_die(&tmp, sizeof(tmp), stdout);
	u32 words_start = sizeof(tmp) + SIZE * 6;
	u32 written = sizeof(tmp);
	for (u32 i = 0; i < SIZE; i++) {
		write_or_die(&table.hashes[i], 3, stdout);
		tmp = (u32) (table.keys[i] - words_start);
		write_or_die(&tmp, 3, stdout);
		written += 6;
	}
	assert(words_start == written);
	write_or_die(file_data, size, stdout);
}
