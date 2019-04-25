#include <jni.h>
#include <bits/sysconf.h>

#include "stdlib.h"

#define PAGE_SIZE   sysconf(_SC_PAGE_SIZE)

JNIEXPORT jlong JNICALL Java_org_thoughtcrime_securesms_service_MemoryWipeService_allocPages
  (JNIEnv *env, jclass clazz, jint order)
{
  if (order == 0)
    return (long) NULL;

  void *p = malloc(order * PAGE_SIZE);

  return (long) p;
}

JNIEXPORT void JNICALL Java_org_thoughtcrime_securesms_service_MemoryWipeService_freePages
        (JNIEnv *env, jclass clazz, jlong p)
{
    free((void *) p);
}

JNIEXPORT void JNICALL Java_org_thoughtcrime_securesms_service_MemoryWipeService_wipePage
        (JNIEnv *env, jclass clazz, jlong p, jint index)
{
    long size = PAGE_SIZE;

    int *x = (void *) p + (index * size);

    do {
        *x++ = rand();
        size -= sizeof(*x);
    } while (size);
}

JNIEXPORT jlong JNICALL Java_org_thoughtcrime_securesms_service_MemoryWipeService_getPageSize
        (JNIEnv *env, jclass clazz)
{
    return PAGE_SIZE;
}
