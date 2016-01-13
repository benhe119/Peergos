package peergos.crypto;

import peergos.util.ArrayOps;

import java.util.*;

public class UserPublicKey implements Comparable<UserPublicKey>
{
    public static final int SIZE = 64;
    public static final int PADDING_LENGTH = 32;
    private static final Random rnd = new Random();

    private final byte[] publicSigningKey, publicBoxingKey;

    public UserPublicKey(byte[] publicSigningKey, byte[] publicBoxingKey)
    {
        this.publicSigningKey = publicSigningKey;
        this.publicBoxingKey = publicBoxingKey;
    }

    public UserPublicKey(byte[] keys) {
        this(Arrays.copyOfRange(keys, 0, 32), Arrays.copyOfRange(keys, 32, 64));
    }

    public byte[] getPublicKeys()
    {
        return ArrayOps.concat(publicSigningKey, publicBoxingKey);
    }

    public byte[] getPublicSigningKey()
    {
        return publicSigningKey;
    }

    public byte[] getPublicBoxingKey()
    {
        return publicBoxingKey;
    }

    public UserPublicKey toUserPublicKey() {
        return new UserPublicKey(publicSigningKey, publicBoxingKey);
    }

    public byte[] encryptMessageFor(byte[] input, byte[] ourSecretBoxingKey)
    {
        byte[] nonce = createNonce();
        return ArrayOps.concat(TweetNaCl.crypto_box(input, nonce, publicBoxingKey, ourSecretBoxingKey), nonce);
    }

    public byte[] createNonce() {
        byte[] nonce = new byte[TweetNaCl.BOX_NONCE_BYTES];
        rnd.nextBytes(nonce);
        return nonce;
    }

    public byte[] unsignMessage(byte[] signed)
    {
        return TweetNaCl.crypto_sign_open(signed, publicSigningKey);
    }

    public boolean equals(Object o)
    {
        if (! (o instanceof UserPublicKey))
            return false;

        return Arrays.equals(publicBoxingKey, ((UserPublicKey) o).publicBoxingKey) && Arrays.equals(publicSigningKey, ((UserPublicKey) o).publicSigningKey);
    }

    public int hashCode()
    {
        return Arrays.hashCode(publicBoxingKey) ^ Arrays.hashCode(publicSigningKey);
    }

    public boolean isValidSignature(byte[] signed, byte[] raw)
    {
        return Arrays.equals(unsignMessage(signed), raw);
    }

    @Override
    public int compareTo(UserPublicKey userPublicKey) {
        int signing = ArrayOps.compare(publicSigningKey, userPublicKey.publicSigningKey);
        if (signing != 0)
            return signing;
        return ArrayOps.compare(publicBoxingKey, userPublicKey.publicBoxingKey);
    }

    public String toString() {
        return new String(Base64.getEncoder().encode(getPublicKeys()));
    }

    public static UserPublicKey fromString(String b64) {
        return new UserPublicKey(Base64.getDecoder().decode(b64));
    }
}
