import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.ByteArrayOutputStream;

/** Initialize gradebook with specified name and generate a key. */
public class setup {

  public static final Pattern fileNamePattern =
      Pattern.compile("^[a-zA-Z0-9_.]*$");

  private static void invalid() {
    System.out.println("invalid");
    System.exit(255);
  }

  public static void main(String[] args) {
    // Check if the correct number of arguments is provided
    if (args.length != 2 || !"-N".equals(args[0])) {
      invalid();
    }

    // Get the name of the gradebook from the command line arguments
    String gradebookName = args[1];

    if (!fileNamePattern.matcher(gradebookName).matches()) {
      invalid();
    }

    // Check if the file already exists
    File file = new File(gradebookName);
    if (file.exists()) {
      invalid();
    }

    // Create a new Gradebook object
    Gradebook gradebook = new Gradebook();
    try {
      SecretKey key = generateKey();
      System.out.println(Base64.getEncoder().encodeToString(key.getEncoded()));
      // Serialize and write the Gradebook object to the file
      try (FileOutputStream outputStream =
               new FileOutputStream(gradebookName)) {
        outputStream.write(encryptObject(gradebook, key));
      }
    } catch (IOException e) {
      invalid();
    } catch (Exception e) {
      invalid();
    }
  }

  private static SecretKey generateKey() throws NoSuchAlgorithmException {
    // AES256
    KeyGenerator keygen = KeyGenerator.getInstance("AES");
    keygen.init(256);
    SecretKey key = keygen.generateKey();
    return key;
  }

  private static byte[] encryptObject(Serializable object, SecretKey key)
      throws BadPaddingException, NoSuchAlgorithmException,
             InvalidAlgorithmParameterException, InvalidKeyException,
             IOException, IllegalBlockSizeException, NoSuchPaddingException {
    byte[] initVector = new byte[16];
    byte[] gradebookByte;
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         ObjectOutputStream objectOutput = new ObjectOutputStream(bytes)) {
      objectOutput.writeObject(object);
      gradebookByte = bytes.toByteArray();
    }

    SecureRandom rng = new SecureRandom();
    rng.nextBytes(initVector);
    final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    // GCM128 with AES key
    cipher.init(Cipher.ENCRYPT_MODE, key,
                new GCMParameterSpec(128, initVector));
    gradebookByte = cipher.doFinal(gradebookByte);

    // if the iv gets guessed we're screwed, hoping SecureRandom is sufficently
    // random
    byte[] finalCipher = new byte[initVector.length + gradebookByte.length];
    // copy iv in
    System.arraycopy(initVector, 0, finalCipher, 0, initVector.length);
    // copy gradebook right after
    System.arraycopy(gradebookByte, 0, finalCipher, initVector.length,
                     gradebookByte.length);

    return finalCipher;
  }
}
