package io.ark.core.crypto;

import io.ark.core.model.Transaction;
import io.ark.core.util.StringUtils;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;

public class Crypto {

  private static ECNamedCurveParameterSpec curve = ECNamedCurveTable.getParameterSpec("secp256k1");

  public static List<String> getPassphrase() {
    byte[] entropy = new byte[16];
    List<String> passphrase = new ArrayList<>();
    try {
      SecureRandom.getInstanceStrong().nextBytes(entropy);
      passphrase = MnemonicCode.INSTANCE.toMnemonic(entropy);
    } catch (NoSuchAlgorithmException | MnemonicLengthException e) {
      e.printStackTrace();
    }
    return passphrase;
  }

  public static void sign(Transaction tx, ECKey keyPair) {
    Sha256Hash data = getHash(tx, true, true);
    ECDSASignature signature = keyPair.sign(data);
    byte[] derEncodedSignature = signature.encodeToDER();
    String hexSignature = StringUtils.toHexString(derEncodedSignature);
    tx.setSignatureBytes(derEncodedSignature);
    tx.setSignature(hexSignature);
  }

  public static void secondSign(Transaction tx, ECKey keyPair) {
    Sha256Hash data = getHash(tx, false, true);
    ECDSASignature signature = keyPair.sign(data);
    byte[] derEncodedSignature = signature.encodeToDER();
    String hexSignature = StringUtils.toHexString(derEncodedSignature);
    tx.setSignSignatureBytes(derEncodedSignature);
    tx.setSignSignature(hexSignature);
  }

  public static void setId(final Transaction tx) {
    Sha256Hash data;
    if (tx.getSignSignatureBytes() != null) {
      data = getHash(tx, false, false);
    } else {
      data = getHash(tx, false, true);
    }
    tx.setId(StringUtils.toHexString(data.getBytes()));
  }

  public static byte[] getBytes(Transaction tx, boolean skipSignature, boolean skipSecondSignature) {
    int assetSize = 0;
    byte[] assetBytes;

    switch (tx.getType()) {
      case 0:
        if (!skipSignature) {
          assetSize += tx.getSignatureBytes().length;
        }
        if (!skipSecondSignature) {
          assetSize += tx.getSignatureBytes().length;
        }
        break;
      case 1:
        break;
      case 2:
        break;
      case 3:
        break;
      case 4:
        break;
      default:
        break;
    }

    ByteBuffer bb = ByteBuffer.allocate(266 + assetSize);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.put(tx.getType());
    bb.putInt(tx.getTimestamp());

    byte[] senderPublicKey = tx.getKeyPair().getPubKey();
    bb.put(senderPublicKey);

    byte[] recipientId = Base58Check.base58ToRawBytes(tx.getRecipientId());
    byte[] copiedId = new byte[21];
    System.arraycopy(recipientId, 0, copiedId, 0, 21);
    bb.put(copiedId);

    if (tx.getVendorField() != null) {
      byte[] vendorBytes = new byte[64];
      byte[] vendorField = tx.getVendorField().getBytes();
      System.arraycopy(vendorField, 0, vendorBytes, 0, Math.min(vendorField.length, 64));
      for(int j = 0; j < 64; j++) {
        bb.put(vendorBytes[j]);
      }
    } else {
      for (int i = 0; i < 64; i++) {
        bb.put((byte) 0);
      }
    }
    
    bb.putLong(tx.getAmount());
    bb.putLong(tx.getFee());

    if (!skipSignature) {
      bb.put(tx.getSignatureBytes());
    }
    if (!skipSecondSignature) {
      bb.put(tx.getSignSignatureBytes());
    }

    bb.flip();

    byte[] buffer = new byte[bb.remaining()];
    bb.get(buffer);

    return buffer;
  }

  public static Sha256Hash getHash(Transaction tx, boolean skipSignature, boolean skipSecondSignature) {
    byte[] txBytes = getBytes(tx, skipSignature, skipSecondSignature);
    return Sha256Hash.of(txBytes);
  }

  public static ECKey getKeys(String secret) {
    MessageDigest digest;
    BigInteger d;
    try {
      digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
      d = new BigInteger(1, hash);
      if (d.signum() <= 0 || d.compareTo(curve.getN()) >= 0) {
        throw new IllegalArgumentException("seed cannot resolve to a compatible private key");
      }
    } catch (NoSuchAlgorithmException | IllegalArgumentException e) {
      return null;
    }
    return ECKey.fromPrivate(d);
  }

  public static String getAddress(ECKey keyPair, byte version) {
    RIPEMD160Digest digest = new RIPEMD160Digest();
    byte[] publicKey = keyPair.getPubKey();
    digest.update(publicKey, 0, publicKey.length);
    byte[] out = new byte[20];
    digest.doFinal(out, 0);

    byte[] data = new byte[21];
    data[0] = version;

    for (int i = 0; i < 20; i++) {
      data[i + 1] = out[i];
    }

    return Base58Check.bytesToBase58(data);
  }

  public static boolean validateAddress(String address, byte version) {
    byte[] base58Decode = Base58Check.base58ToRawBytes(address);
    return version == base58Decode[0];
  }

  public static byte[] getDoubleHash(byte[] data) {
    SHA256Digest digest = new SHA256Digest();
    digest.update(data, 0, data.length);
    byte[] out = new byte[32];
    digest.doFinal(out, 0);
    digest.reset();
    digest.update(out, 0, out.length);
    byte[] ret = new byte[32];
    digest.doFinal(ret, 0);
    return ret;
  }
}
