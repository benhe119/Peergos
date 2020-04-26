package peergos.shared.storage;

import peergos.shared.cbor.*;
import peergos.shared.crypto.hash.*;
import peergos.shared.io.ipfs.cid.*;
import peergos.shared.io.ipfs.multiaddr.*;
import peergos.shared.io.ipfs.multihash.*;
import peergos.shared.util.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

public class HashVerifyingStorage implements ContentAddressedStorage {

    private final ContentAddressedStorage source;
    private final Hasher hasher;

    public HashVerifyingStorage(ContentAddressedStorage source, Hasher hasher) {
        this.source = source;
        this.hasher = hasher;
    }

    private <T> CompletableFuture<T> verify(byte[] data, Multihash claimed, Supplier<T> result) {
        switch (claimed.type) {
            case sha2_256:
                return hasher.sha256(data)
                        .thenApply(hash -> {
                            Multihash computed = new Multihash(Multihash.Type.sha2_256, hash);
                            if (claimed instanceof Cid)
                                computed = Cid.build(((Cid) claimed).version, ((Cid) claimed).codec, computed);

                            if (computed.equals(claimed))
                                return result.get();

                            throw new IllegalStateException("Incorrect hash! Are you under attack? Expected: " + claimed + " actual: " + computed);
                        });
            case id:
                if (Arrays.equals(data, claimed.getHash()))
                    return Futures.of(result.get());
                throw new IllegalStateException("Incorrect identity hash! This shouldn't ever  happen.");
            default: throw new IllegalStateException("Unimplemented hash algorithm: " + claimed.type);
        }
    }

    @Override
    public CompletableFuture<Multihash> id() {
        return source.id();
    }

    @Override
    public CompletableFuture<List<PresignedUrl>> authWrites(PublicKeyHash owner,
                                                            PublicKeyHash writer,
                                                            List<byte[]> signedHashes,
                                                            List<Integer> blockSizes,
                                                            boolean isRaw,
                                                            TransactionId tid) {
        return source.authWrites(owner, writer, signedHashes, blockSizes, isRaw, tid);
    }

    @Override
    public CompletableFuture<TransactionId> startTransaction(PublicKeyHash owner) {
        return source.startTransaction(owner);
    }

    @Override
    public CompletableFuture<Boolean> closeTransaction(PublicKeyHash owner, TransactionId tid) {
        return source.closeTransaction(owner, tid);
    }

    @Override
    public CompletableFuture<Boolean> gc() {
        return source.gc();
    }

    @Override
    public CompletableFuture<List<Multihash>> put(PublicKeyHash owner,
                                                  PublicKeyHash writer,
                                                  List<byte[]> signatures,
                                                  List<byte[]> blocks,
                                                  TransactionId tid) {
        return source.put(owner, writer, signatures, blocks, tid)
                .thenCompose(hashes -> Futures.combineAllInOrder(hashes.stream()
                        .map(h -> verify(blocks.get(hashes.indexOf(h)), h, () -> h))
                        .collect(Collectors.toList())));
    }

    @Override
    public CompletableFuture<Optional<CborObject>> get(Multihash hash) {
        return source.get(hash)
                .thenCompose(cborOpt -> cborOpt.map(cbor -> verify(cbor.toByteArray(), hash, () -> cbor)
                        .thenApply(Optional::of))
                        .orElseGet(() -> Futures.of(Optional.empty())));
    }

    @Override
    public CompletableFuture<List<Multihash>> putRaw(PublicKeyHash owner,
                                                     PublicKeyHash writer,
                                                     List<byte[]> signatures,
                                                     List<byte[]> blocks,
                                                     TransactionId tid) {
        return source.putRaw(owner, writer, signatures, blocks, tid)
                .thenCompose(hashes -> Futures.combineAllInOrder(hashes.stream()
                        .map(h -> verify(blocks.get(hashes.indexOf(h)), h, () -> h))
                        .collect(Collectors.toList())));
    }

    @Override
    public CompletableFuture<Optional<byte[]>> getRaw(Multihash hash) {
        return source.getRaw(hash)
                .thenCompose(arrOpt -> arrOpt.map(bytes -> verify(bytes, hash, () -> bytes)
                        .thenApply(Optional::of))
                        .orElseGet(() -> Futures.of(Optional.empty())));
    }

    @Override
    public CompletableFuture<List<Multihash>> pinUpdate(PublicKeyHash owner, Multihash existing, Multihash updated) {
        return source.pinUpdate(owner, existing, updated);
    }

    @Override
    public CompletableFuture<List<Multihash>> recursivePin(PublicKeyHash owner, Multihash h) {
        return source.recursivePin(owner, h);
    }

    @Override
    public CompletableFuture<List<Multihash>> recursiveUnpin(PublicKeyHash owner, Multihash h) {
        return source.recursiveUnpin(owner, h);
    }

    @Override
    public CompletableFuture<List<Multihash>> getLinks(Multihash root) {
        return source.getLinks(root);
    }

    @Override
    public CompletableFuture<Optional<Integer>> getSize(Multihash block) {
        return source.getSize(block);
    }
}
