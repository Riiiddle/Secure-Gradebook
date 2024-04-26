import java.io.FileOutputStream;
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

/**
 * Allows the user to add a new student or assignment to a gradebook, or add a
 * grade for an existing student and existing assignment
 */
public class gradebookadd {

  private static void invalid() {
    System.out.println("invalid");
    System.exit(255);
  }

  /* parses the cmdline to keep main method simplified */
  private static String[] parse_cmdline(String[] args) {
    if (args.length < 6) {
      invalid();
    }

    String filename = null;
    String key = null;
    String assignmentName = null;
    String points = null;
    String weight = null;
    String firstName = null;
    String lastName = null;
    String grade = null;

    String action = null;

    for (int i = 0; i < args.length; i++) {
      // Filename
      if (i == 0) {
        if (args[i].equals("-N")) {
          filename = args[++i];
        } else {
          invalid();
        }
      }
      // Key
      if (i == 2) {
        if (args[i].equals("-K")) {
          key = args[++i];
        } else {
          invalid();
        }
      }
      // Action
      if (i == 4) {
        action = args[i];
        if (!action.equals("-AA") && !action.equals("-DA") &&
            !action.equals("-AS") && !action.equals("-DS") &&
            !action.equals("-AG")) {
          invalid();
        }
      }
      // {-AN, -FN, -LN, -P, -W, -G}
      if (i > 4) {
        switch (args[i]) {
        case "-AN":
          if (!action.equals("-AA") && !action.equals("-DA") &&
              !action.equals("-AG")) {
            invalid();
          }
          assignmentName = args[++i];
          break;
        case "-FN":
          if (!action.equals("-AS") && !action.equals("-DS") &&
              !action.equals("-AG")) {
            invalid();
          }
          firstName = args[++i];
          break;
        case "-LN":
          if (!action.equals("-AS") && !action.equals("-DS") &&
              !action.equals("-AG")) {
            invalid();
          }
          lastName = args[++i];
          break;
        case "-P":
          if (!action.equals("-AA")) {
            invalid();
          }
          points = args[++i];
          break;
        case "-W":
          if (!action.equals("-AA")) {
            invalid();
          }
          weight = args[++i];
          break;
        case "-G":
          if (!action.equals("-AG")) {
            invalid();
          }
          grade = args[++i];
          break;
        default:
          invalid();
        }
      }
    }

    // Check for valid commands
    switch (action) {
    case "-AA":
      if (assignmentName == null || points == null || weight == null) {
        invalid();
      }
      break;
    case "-DA":
      if (assignmentName == null) {
        invalid();
      }
      break;
    case "-AS":
      if (firstName == null || lastName == null) {
        invalid();
      }
      break;
    case "-DS":
      if (firstName == null || lastName == null) {
        invalid();
      }
      break;
    case "-AG":
      if (firstName == null || lastName == null || assignmentName == null ||
          grade == null) {
        invalid();
      }
      break;
    default:
      invalid();
    }
    /*
     * System.out.println("Filename " + filename); System.out.println("Key " +
     * key); System.out.println("Assignment name " + assignmentName);
     * System.out.println("Points " + points); System.out.println("Weight " +
     * weight); System.out.println("First name " + firstName);
     * System.out.println("Last name " + lastName); System.out.println("Grade "
     * + grade);
     */

    // Stores all the arguments as well as what action
    String[] info = new String[9];

    info[0] = action;
    info[1] = filename;
    info[2] = key;
    info[3] = assignmentName;
    info[4] = points;
    info[5] = weight;
    info[6] = firstName;
    info[7] = lastName;
    info[8] = grade;

    return info;
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
    String[] info = parse_cmdline(args);
    String action = info[0];
    String filename = info[1];
    String key = info[2];
    String assignmentName = info[3];
    int points = 0;
    double weight = 0;
    String firstName = info[6];
    String lastName = info[7];
    int grade = 0;

    // TODO: Check filename is correct and check key == Gradebook.key (?)
    Gradebook gradebook = new Gradebook(filename, convertStringToKey(key));

    switch (action) {
    case "-AA":
      try {
        points = Integer.parseInt(info[4]);
      } catch (NumberFormatException e) {
        invalid();
      }
      try {
        weight = Double.parseDouble(info[5]);
      } catch (NumberFormatException e) {
        invalid();
      }
      gradebook.addAssignment(assignmentName, points, weight);
      break;
    case "-DA":
      gradebook.deleteAssignment(assignmentName);
      break;
    case "-AS":
      gradebook.addStudent(firstName, lastName);
      break;
    case "-DS":
      gradebook.deleteStudent(firstName, lastName);
      break;
    case "-AG":
      try {
        grade = Integer.parseInt(info[8]);
      } catch (NumberFormatException e) {
        invalid();
      }
      gradebook.addGrade(firstName, lastName, assignmentName, grade);
      break;
    default:
      invalid();
    }

    // Serialize and write the Gradebook object to the file
    try (FileOutputStream outputStream = new FileOutputStream(filename)) {
      outputStream.write(encryptObject(gradebook, convertStringToKey(key)));
    } catch (IOException e) {
      invalid();
    } catch (Exception e) {
      invalid();
    }
  }
}
