#ifndef L2W_SHA256_H
#define L2W_SHA256_H
#include <stdint.h>
#include <stddef.h>
typedef struct { uint32_t h[8]; uint64_t len; unsigned char buf[64]; int n; } sha256_ctx;
void sha256_init(sha256_ctx*);
void sha256_update(sha256_ctx*,const void*,size_t);
void sha256_hex(sha256_ctx*,char out[65]);
#endif
