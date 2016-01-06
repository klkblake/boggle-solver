#include "common.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#define SIZE (1 << 18)
#define MASK (SIZE - 1)

/*
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
*/

typedef struct Node {
	struct Node *children[26];
	u32 offset;
	u32 num_children;
	b32 word_end;
} Node;

void insert(Node *node, u8 *word, u32 len) {
	if (len == 0) {
		node->word_end = true;
		return;
	}
	u32 i = *word - 'a';
	Node *child = node->children[i];
	if (!child) {
		child = node->children[i] = calloc(sizeof(Node), 1);
		node->num_children++;
	}
	insert(child, word + 1, len - 1);
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
		printf("usage: %s words.lst > words.dict\n", argv[0]);
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
	Node root = {};
	u8 *next = file_data;
	u8 *end = file_data + size;
	u32 height = 0;
	while (next < end) {
		u32 len = (u32)strlen((char *)next);
		insert(&root, next, len);
		if (height < len) {
			height = len;
		}
		next += len + 1;
	}
	height++;
	Node *nodes[height*25 + 1];
	nodes[0] = &root;
	u32 count = 1;
	u32 max_offset = 0;
	u32 offset = 0;
	while (count > 0) {
		Node *node = nodes[--count];
		if (node->num_children == 0) {
			continue;
		}
		for (s32 i = 25; i >= 0; i--) {
			if (node->children[i]) {
				nodes[count++] = node->children[i];
			}
		}
		node->offset = offset;
		if (offset > max_offset) {
			max_offset = offset;
		}
		offset += 1 + 4 * node->num_children;
	}
	fprintf(stderr, "Max offset: %d\n", max_offset);
	nodes[0] = &root;
	count = 1;
	max_offset = 0;
	u32 histogram[24] = {};
	u32 child_histogram[26] = {};
	while (count > 0) {
		Node *node = nodes[--count];
		for (s32 i = 25; i >= 0; i--) {
			if (node->children[i] && node->children[i]->num_children != 0) {
				nodes[count++] = node->children[i];
			}
		}
		write_or_die(&node->num_children, 1, stdout);
		for (u8 i = 0; i < 26; i++) {
			if (node->children[i]) {
				u32 word_end = node->children[i]->word_end << 23;
				write_or_die(&i, 1, stdout);
				if (node->children[i]->num_children == 0) {
					write_or_die(&word_end, 3, stdout);
				} else {
					offset = node->children[i]->offset | word_end;
					write_or_die(&offset, 3, stdout);
					offset = node->children[i]->offset -
						(node->offset + 1 + 4 * node->num_children);
					u32 bits;
					if (offset == 0) {
						bits = 0;
					} else {
						bits = 32 - (u32)__builtin_clz(offset);
					}
					histogram[bits]++;
					if (offset > max_offset) {
						max_offset = offset;
					}
				}
			}
		}
	}
	fprintf(stderr, "Max diff offset: %d\n", max_offset);
	fprintf(stderr, "Histogram:\n");
	for (u32 i = 0; i < array_count(histogram); i++) {
		fprintf(stderr, "%u %u\n", i, histogram[i]);
	}
}
