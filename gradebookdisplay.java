// import ...

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;

/** Prints out a gradebook in a few ways Some skeleton functions are included */
public class gradebookdisplay {
  static boolean verbose = false;

  private static void invalid() {
    System.out.println("invalid");
    System.exit(255);
  }

  private static SecretKey convertStringToKey(String keyAsString) {
    byte[] keyBytes = Base64.getDecoder().decode(keyAsString);
    return new SecretKeySpec(keyBytes, "AES");
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

  public static void main(String[] args) {
    // Check if there are enough command line arguments
    if (args.length < 6) {
      invalid();
    }

    // Parse command line arguments
    String gradebookName = null;
    String key = null;
    String action = null;
    String assignmentName = null;
    String studentFirstName = null;
    String studentLastName = null;
    boolean alphabeticalOrder = false;
    boolean gradeOrder = false;

    for (int i = 0; i < args.length; i++) {
      // gradebook name
      if (i == 0) {
        if (args[i].equals("-N")) {
          gradebookName = args[++i];
        } else {
          invalid();
        }
        // key
      } else if (i == 2) {
        if (args[i].equals("-K")) {
          key = args[++i];
        } else {
          invalid();
        }
        // action
      } else if (i == 4) {
        if (args[i].equals("-PA") || args[i].equals("-PS") ||
            args[i].equals("-PF")) {
          action = args[i];
        } else {
          invalid();
        }
        //  from the set {-AN, -FN, -LN, -A, -G}
      } else {
        switch (args[i]) {
        case "-AN":
          if (!action.equals("-PA")) {
            invalid();
          }
          assignmentName = args[++i];
          break;
        case "-FN":
          if (!action.equals("-PS")) {
            invalid();
          }
          studentFirstName = args[++i];
          break;
        case "-LN":
          if (!action.equals("-PS")) {
            invalid();
          }
          studentLastName = args[++i];
          break;
        case "-A":
          if (action.equals("-PS")) {
            invalid();
          }
          alphabeticalOrder = true;
          break;
        case "-G":
          if (action.equals("-PS")) {
            invalid();
          }
          gradeOrder = true;
          break;
        default:
          invalid();
        }
      }
    }
    // Perform additional validation
    // Check that you're ordering by either grade or assignment
    if (action.equals("-PA") || action.equals("-PF")) {
      if ((!alphabeticalOrder && !gradeOrder) ||
          (alphabeticalOrder && gradeOrder)) {
        invalid();
      }
    }
    // Check for assignment name
    if (action.equals("-PA")) {
      if (assignmentName == null) {
        invalid();
      }
    }
    // Check for student names
    if (action.equals("-PS")) {
      if (studentFirstName == null || studentLastName == null) {
        invalid();
      }
    }

    // Print out the parsed values (for testing purposes)
    //  System.out.println("Gradebook Name: " + gradebookName);
    //  System.out.println("Key: " + key);
    //  System.out.println("Action: " + action);
    //  System.out.println("Assignment Name: " + assignmentName);
    //  System.out.println("Student First Name: " + studentFirstName);
    //  System.out.println("Student Last Name: " + studentLastName);
    //  System.out.println("Alphabetical Order: " + alphabeticalOrder);
    //  System.out.println("Grade Order: " + gradeOrder);

    // Implement the logic based on the parsed values
    Gradebook gradebook = new Gradebook(gradebookName, convertStringToKey(key));

    if (action.equals("-PA")) {
      gradebook.printAssignment(assignmentName, alphabeticalOrder, gradeOrder);
    } else if (action.equals("-PS")) {
      gradebook.printStudent(studentFirstName, studentLastName);
    } else {
      gradebook.printFinal(alphabeticalOrder, gradeOrder);
    }

    // TODO: Memory could contain gradebook at this point in time!

    // Exit with success
    System.exit(0);
  }
}
