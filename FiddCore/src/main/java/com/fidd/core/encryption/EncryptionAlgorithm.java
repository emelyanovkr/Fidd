package com.fidd.core.encryption;

import com.fidd.core.NamedEntry;
import com.fidd.core.random.RandomGeneratorType;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface EncryptionAlgorithm extends NamedEntry {
    default long plaintextLengthToCiphertextLength(long plaintextLength) { return plaintextLength; }

    // TODO: ciphertextLengthToPlaintextLength defaults to returning the ciphertext size, but registered padded
    //  algorithms such as AES-256-CBC produce ciphertext that includes PKCS#5 padding.
    //  For those files this sets Content-Length/range bounds larger than the decrypted stream, which can break
    //  full downloads. Store the plaintext length in the Fidd key/metadata or otherwise avoid advertising a decoded
    //  length that cannot be derived from the ciphertext length.
    default long ciphertextLengthToPlaintextLength(long ciphertextLength) { return ciphertextLength; }

    String UNENCRYPTED = "UNENCRYPTED";

    interface CrcCallback {
        void write(byte[] b);
        void write(int b);
        byte[] getCrc();
    }

    byte[] generateNewKeyData(RandomGeneratorType random);

    byte[] encrypt(byte[] keyData, byte[] plaintext);
    byte[] decrypt(byte[] keyData, byte[] ciphertext);

    /** @return Bytes written to output stream ciphertext */
    long encrypt(byte[] keyData, List<InputStream> plaintext, OutputStream ciphertext, List<CrcCallback> ciphertextCrcCallbacks);
    /** @return Bytes written to output stream plaintext */
    default long decrypt(byte[] keyData, InputStream ciphertext, OutputStream plaintext) {
        return decrypt(keyData, ciphertext, plaintext, false);
    }
    long decrypt(byte[] keyData, InputStream ciphertext, OutputStream plaintext, boolean allowPartial);

    InputStream getDecryptedStream(byte[] keyData, InputStream stream);
}
