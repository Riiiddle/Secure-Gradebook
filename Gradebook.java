import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.ByteArrayInputStream;

/**
 * A helper class for your gradebook Some of these methods may be useful for
 * your program You can remove methods you do not need If you do not wiish to
 * use a Gradebook object, don't
 */
public class Gradebook implements Serializable {

  private static final long serialVersionUID = 1L;
  public static final Pattern fileNamePattern =
      Pattern.compile("^[a-zA-Z0-9_.]*$");
  public static final Pattern assignmentNamePattern =
      Pattern.compile("^[a-zA-Z0-9]*$");
  public static final Pattern studentNamePattern =
      Pattern.compile("^[a-zA-Z]*$");

  private static void invalid() {
    System.out.println("invalid");
    System.exit(255);
  }

  public static class Student implements Serializable {

    private static final long serialVersionUID = 1L;
    public String firstName, lastName;

    public Student(String firstName, String lastName) {
      this.firstName = firstName;
      this.lastName = lastName;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof Student)) {
        return false;
      }
      Student otherStudent = (Student)other;
      return firstName.equals(otherStudent.firstName) &&
          lastName.equals(otherStudent.lastName);
    }

    @Override
    public int hashCode() {
      return firstName.hashCode() + lastName.hashCode();
    }
  }

  public static class Assignment implements Serializable {

    private static final long serialVersionUID = 1L;
    public String name;
    public int totalPoints;
    public double weight;

    public Assignment(String name, int points, double weight) {
      this.name = name;
      this.totalPoints = points;
      this.weight = weight;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof Assignment)) {
        return false;
      }
      Assignment otherAssignment = (Assignment)other;
      return name.equals(otherAssignment.name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }

  public static class Grade implements Serializable {

    private static final long serialVersionUID = 1L;
    public Assignment assignment;
    public int points;

    public Grade(Assignment assignment, int points) {
      this.assignment = assignment;
      this.points = points;
    }

    public String getAssignmentName() { return assignment.name; }

    public boolean forAssignment(String name) {
      return assignment.name.equals(name);
    }
  }

  public Map<Student, List<Grade>> gradebook;
  public List<Assignment> assignments;

  /* Read a Gradebook from a file */
  public Gradebook(String filename, SecretKey key) {
    if (!fileNamePattern.matcher(filename).matches()) {
      invalid();
    }

    File file = new File(filename);
    byte[] rawData = new byte[(int)file.length()];
    byte[] initVector = new byte[16];
    byte[] encryptedBook = new byte[rawData.length - initVector.length];
    try (FileInputStream stream = new FileInputStream(file)) {
      // Read the serialized Gradebook object from the file
      stream.read(rawData);

      // separate iv from gradebook
      System.arraycopy(rawData, 0, initVector, 0, initVector.length);
      System.arraycopy(rawData, initVector.length, encryptedBook, 0,
                       encryptedBook.length);

      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key,
                  new GCMParameterSpec(128, initVector));

      encryptedBook = cipher.doFinal(encryptedBook);

      Gradebook decryptedBook;
      try (ByteArrayInputStream bytes = new ByteArrayInputStream(encryptedBook);
           ObjectInputStream objectInput = new ObjectInputStream(bytes)) {
        decryptedBook = (Gradebook)objectInput.readObject();
      }

      this.gradebook = decryptedBook.gradebook;
      this.assignments = decryptedBook.assignments;
    } catch (Exception e) {
      invalid();
    }
  }

  /* Create a new gradebook */
  public Gradebook() {
    gradebook = new HashMap<Student, List<Grade>>();
    assignments = new ArrayList<Assignment>();
  }

  /*Returns true if the student name is valid*/
  public boolean isValidStudentName(String firstName, String lastName) {
    return studentNamePattern.matcher(firstName).matches() &&
        studentNamePattern.matcher(lastName).matches();
  }

  /* Adds a student to the gradebook */
  public void addStudent(String firstName, String lastName) {
    if (!isValidStudentName(firstName, lastName)) {
      invalid();
    }

    Student student = new Student(firstName, lastName);
    if (gradebook.containsKey(student)) {
      invalid();
    }
    gradebook.put(student, new ArrayList<Grade>());
  }

  /* Delete a student from the gradebook */
  public void deleteStudent(String firstName, String lastName) {
    if (!isValidStudentName(firstName, lastName)) {
      invalid();
    }

    Student student = new Student(firstName, lastName);
    if (!gradebook.containsKey(student)) {
      invalid();
    }
    gradebook.remove(student);
  }

  /* Get total weight of assignments */
  public double assignmentWeights() {
    double sum = 0;
    for (Assignment a : assignments) {
      sum += a.weight;
    }
    return sum;
  }

  /*Returns true for a valid assignment name*/
  public boolean isValidAssignmentName(String name) {
    return assignmentNamePattern.matcher(name).matches();
  }

  /* Adds an assinment to the gradebook */
  public void addAssignment(String name, int points, double weight) {
    if (!isValidAssignmentName(name) || points < 0 || weight < 0 ||
        (assignmentWeights() + weight > 1)) {
      invalid();
    }
    Assignment assignment = new Assignment(name, points, weight);
    if (assignments.contains(assignment)) {
      invalid();
    }
    assignments.add(assignment);
  }

  /* Return true if there's an assignment with the given name */
  public boolean containsAssignmentWithName(String name) {
    for (int i = 0; i < assignments.size(); i++) {
      if (assignments.get(i).name.equals(name)) {
        return true;
      }
    }
    return false;
  }

  /* Delete an assignment from the gradebook */
  public void deleteAssignment(String name) {
    if (!isValidAssignmentName(name) || !containsAssignmentWithName(name)) {
      invalid();
    }

    int indexToDelete = 0;
    // Find index of assignment with given name in assignment list
    for (int i = 0; i < assignments.size(); i++) {
      if (assignments.get(i).name.equals(name)) {
        indexToDelete = i;
      }
    }

    // Remove from assignment list
    assignments.remove(indexToDelete);

    // Remove all grades for the assignment
    for (List<Grade> grades : gradebook.values()) {
      for (int i = 0; i < grades.size(); i++) {
        if (grades.get(i).forAssignment(name)) {
          grades.remove(i);
          break;
        }
      }
    }
  }

  /* Return assignment with given name, assumes it is in assignment list */
  public Assignment getAssignmentWithName(String name) {
    Assignment result = null;
    for (Assignment a : assignments) {
      if (a.name.equals(name)) {
        return a;
      }
    }
    return result;
  }

  /* Adds a grade to the gradebook */
  public void addGrade(String firstName, String lastName, String assignmentName,
                       int grade) {
    if (grade < 0 || !isValidStudentName(firstName, lastName) ||
        !isValidAssignmentName(assignmentName)) {
      invalid();
    }
    Student student = new Student(firstName, lastName);
    // If the student or assignment doesn't exist
    if (!gradebook.containsKey(student) ||
        !containsAssignmentWithName(assignmentName)) {
      invalid();
    }

    Assignment assignment = getAssignmentWithName(assignmentName);

    List<Grade> oldGrades = gradebook.get(student);
    boolean updatedExistingGrade = false;
    // Update an existing grade
    for (int i = 0; i < oldGrades.size(); i++) {
      if (oldGrades.get(i).assignment.equals(assignment)) {
        oldGrades.get(i).points = grade;
        updatedExistingGrade = true;
      }
    }

    // If there wasn't already a grade, add a new grade
    if (!updatedExistingGrade) {
      Grade newGrade = new Grade(assignment, grade);
      oldGrades.add(newGrade);
    }
  }

  public static class StudentGradeTuple extends Student {

    private static final long serialVersionUID = 1L;
    public int grade;

    public StudentGradeTuple(String firstName, String lastName, int grade) {
      super(firstName, lastName);
      this.grade = grade;
    }

    public String toString() {
      return "(" + lastName + ", " + firstName + ", " + grade + ")";
    }
  }

  public static class StudentGradeTupleAlphaComparator
      implements Comparator<StudentGradeTuple> {

    @Override
    public int compare(StudentGradeTuple one, StudentGradeTuple two) {
      int lastNameComparison = one.lastName.compareTo(two.lastName);

      // If last names are the same, compare first names
      if (lastNameComparison == 0) {
        return one.firstName.compareTo(two.firstName);
      }

      return lastNameComparison;
    }
  }

  public static class StudentGradeTupleGradeComparator
      implements Comparator<StudentGradeTuple> {

    @Override
    public int compare(StudentGradeTuple one, StudentGradeTuple two) {
      return Integer.compare(two.grade, one.grade);
    }
  }

  /*
   * Prints out grades of all students for a particular assignment, assumes only
   * one boolean is set to true
   */
  public void printAssignment(String name, boolean alphabeticalOrder,
                              boolean gradeOrder) {
    if (!isValidAssignmentName(name) || !containsAssignmentWithName(name)) {
      invalid();
    }

    // Get list of student grades for the given assignment name
    List<StudentGradeTuple> tupleList = new ArrayList<>();
    for (Map.Entry<Student, List<Grade>> e : gradebook.entrySet()) {
      Student student = e.getKey();
      List<Grade> grades = e.getValue();

      for (Grade g : grades) {
        if (g.assignment.name.equals(name)) {
          tupleList.add(new StudentGradeTuple(student.firstName,
                                              student.lastName, g.points));
          break;
        }
      }
    }

    // Sort the list either alphabetically or by grade
    if (alphabeticalOrder) {
      tupleList.sort(new StudentGradeTupleAlphaComparator());
    } else {
      tupleList.sort(new StudentGradeTupleGradeComparator());
    }

    // Print out the grades
    for (StudentGradeTuple t : tupleList) {
      System.out.println(t);
    }
  }

  public static class AssignmentGradeTuple {
    public String name;
    public int grade;

    public AssignmentGradeTuple(String name, int grade) {
      this.name = name;
      this.grade = grade;
    }

    @Override
    public String toString() { return "(" + name + ", " + grade + ")"; }
  }

  /* Prints out all grades for a particular student */
  public void printStudent(String firstName, String lastName) {
    if (!isValidStudentName(firstName, lastName)) {
      invalid();
    }

    Student student = new Student(firstName, lastName);
    // If the student doesn't exist
    if (!gradebook.containsKey(student)) {
      invalid();
    }

    List<AssignmentGradeTuple> tupleList = new ArrayList<>();
    for (Grade g : gradebook.get(student)) {
      tupleList.add(new AssignmentGradeTuple(g.getAssignmentName(), g.points));
    }

    for (AssignmentGradeTuple a : tupleList) {
      System.out.println(a);
    }
  }

  public static class StudentFinalGradeTuple extends Student {

    private static final long serialVersionUID = 1L;
    public double grade;

    public StudentFinalGradeTuple(String firstName, String lastName,
                                  double grade) {
      super(firstName, lastName);
      this.grade = grade;
    }

    public String toString() {
      return "(" + lastName + ", " + firstName + ", " + grade + ")";
    }
  }

  public static class StudentFinalGradeTupleAlphaComparator
      implements Comparator<StudentFinalGradeTuple> {

    @Override
    public int compare(StudentFinalGradeTuple one, StudentFinalGradeTuple two) {
      int lastNameComparison = one.lastName.compareTo(two.lastName);

      // If last names are the same, compare first names
      if (lastNameComparison == 0) {
        return one.firstName.compareTo(two.firstName);
      }

      return lastNameComparison;
    }
  }

  public static class StudentFinalGradeTupleGradeComparator
      implements Comparator<StudentFinalGradeTuple> {

    @Override
    public int compare(StudentFinalGradeTuple one, StudentFinalGradeTuple two) {
      return Double.compare(two.grade, one.grade);
    }
  }

  /* Prints out final grades for all students */
  public void printFinal(boolean alphabeticalOrder, boolean gradeOrder) {
    List<StudentFinalGradeTuple> tupleList = new ArrayList<>();

    for (Map.Entry<Student, List<Grade>> e : gradebook.entrySet()) {
      Student student = e.getKey();
      List<Grade> grades = e.getValue();

      double finalGrade = 0;

      for (Grade g : grades) {
        finalGrade +=
            ((double)g.points / g.assignment.totalPoints) * g.assignment.weight;
      }
      tupleList.add(new StudentFinalGradeTuple(student.firstName,
                                               student.lastName, finalGrade));
    }
    if (alphabeticalOrder) {
      tupleList.sort(new StudentFinalGradeTupleAlphaComparator());
    } else {
      tupleList.sort(new StudentFinalGradeTupleGradeComparator());
    }
    // Print out the final grades
    for (StudentFinalGradeTuple t : tupleList) {
      System.out.println(t);
    }
  }
}
