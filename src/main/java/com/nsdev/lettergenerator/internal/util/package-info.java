/**
 * Collection ownership helpers for immutable model records. {@link
 * com.nsdev.lettergenerator.internal.util.ImmutableLists#copyNullable(java.util.List)} snapshots
 * caller-owned lists without discarding null lists or rejecting null entries. This lets
 * generation-time validation identify the precise offending model-field path instead of failing at
 * record construction with a generic collection error.
 *
 * <p>The copy is shallow and unmodifiable. Records containing nested lists snapshot each level
 * explicitly; image records manage byte-array ownership separately through defensive copies.
 */
package com.nsdev.lettergenerator.internal.util;
