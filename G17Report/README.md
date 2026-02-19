
<details>
  <summary><H1> Part 1. Introduction. Set Up. Functional Testing and Partitioning. </H1></summary>

  ## 3. Testing Practices and Frameworks
  The testing architecture is divided into two primary categories: **JUnit** and **AntUnit**

  ### 3.1 JUnit (Java-based Testing)

  #### Purpose: 
  JUnit is used for testing of the internal **Java source code** of Ant

  #### Mechanism: 
  It operates as a traditional unit testing framework to verify that the internal Java API, string manipulation, input/output logic, and core algorithms function correctly

  #### Features:
  * Tests are written in ` .java ` files, typically located in `src/tests/junit`
  * Verifies the **internals** of the system
  * e.g., `DeleteTest.java`
      
  ### 3.2 AntUnit (XML-based Testing)

  #### Purpose:
  AntUnit is an extension library used for **functional or integration testing of Ant Tasks and Build Files**

  #### Mechanism:
  It allows developers to write test cases using **Ant's XML syntax**

  #### Features:
  * Tests are written in ` .xml ` files, typically located in `src/tests/antunit`
  * Verifies the side effects of tasks (e.g., File creation, Directory deletion)
  * e.g., `copy-test.xml`


  ## 4. Execution and Commands
  Tests are executed using the project's bootstrapping script, the script invokes specific **targets** defined in `build.xml`.

  * ```build.sh``` on Unix/Mac  
  * ```build.bat``` on Windows

  ### 4.1 Common Test Targets

  |Command	|Target	|Description|
  | - | -  | -  |
  |`./build.sh test`|	test	|**Full Suite & Reports** - It compiles the source, executes both JUnit and AntUnit tests, and generates HTML reports. If any test fails, the build terminates with BUILD FAILED.|
  |`./build.sh run-tests`|	run-tests	|**Without Reports** - Runs the full test suite but skips HTML report generation. |
  |`./build.sh junit-tests`|	junit-tests	|**Java Only** - Scans `src/tests/junit` and executes all `*Test.java` files.|
  |`./build.sh antunit-tests`|	antunit-tests	|**XML Only** - Scans `src/etc/testcases` and executes `test.xml` or `*-test.xml` scripts.|

  ### 4.2 Running Single Test Case

  To avoid running the entire suite, a specific JUnit test class can be targeted using the `-Dtestcase` property:

  ```
  ./build.sh -Dtestcase=org.apache.tools.ant.taskdefs.DeleteTest test
  ```

  ### 4.3 Test Reporting
  Ant aggregates the results into visual HTML dashboards. These reports provide pass/fail rates, execution times, and detailed stack traces for debugging.

  * JUnit Reports: Located at `build/testcases/reports/index.html`
  * AntUnit Reports: Located at `build/antunit/reports/index.html`

  <div align="center">
    <img src="Image/report.png" width="500">
  </div>

  ## 5. Systematic Functional Testing & Partition Testing

  > **Systematic functional testing** is essential for validating software reliability because testing every possible input is practically impossible for complex systems. Without a systematic approach, testing becomes ad-hoc, leading to redundant test cases and missed edge cases.

  > **Partition Testing** addresses this by dividing the input domain of a program into finite classes of data. The underlying assumption is that the program behaves equivalently for all inputs within a single partition. This method allows us to select a small, representative subset of inputs to achieve high functional coverage with efficiency.

  ### 5.1 Feature 1: Apache Ant \<delete\> Task (Author: Eleanor)

  This feature focus on the input `file` (for single files) and `dir` (for directories). The goal is to verify that the task correctly handles different file system states.

  #### 5.1.1 Partitioning Scheme
  | Partition | Description | Boundary | Representative Input Value | Rationale for Selection |
  | :--- | :--- | :--- | :--- | :--- |
  | **Existing File** | A standard scenario where the input path points to a file that exists and can be deleted. | `.exists() == true` | `testDelete.txt` | Choose a simple text file name. In the test setup, this file is explicitly created before execution to ensure the basic deletion logic is verified. |
  | **Missing File** | An error handling scenario where the input path points to a location that contains no file. | `.exists() == false` | `testDelete.txt` (missing) | Reuse the previous filename but explicitly ensured it was deleted before the test ran. This tests the task's robustness when handling invalid paths or checking if it fails. |
  | **Empty Directory** | The input path points to an existing directory that contains no child. | `IsDirectory() == true` AND `ChildrenCount == 0` | `empty_dir/` | Choose a newly created directory with no content. This serves as a **boundary value test** (size = 0), verifying that the task correctly handles directory removal logic without needing recursion. |
  | **Non-empty Directory** | The input path points to an existing directory that contains at least one file or subdirectory. | `IsDirectory() == true` AND `ChildrenCount >= 1` | `nonempty_dir/`, containing `child.txt` | Choose a newly created directory containing one child file. This represents a complex case requiring recursion. It verifies that the task deletes the child elements first before removing the parent directory. |

  #### 5.1.2 Test Case Implementation

  The test cases were implemented using **JUnit 4** and integrated directly into the Apache Ant source tree. 
  The test instantiates the `Delete` task class directly (via the Java API) rather than parsing an XML build file. 

  * **Setup**:
    * A helper method `createProject()` was implemented to instantiate a fresh `Project` object for each test, ensuring test isolation and correct base directory resolution.
  * **`testExistFile()`**: Validates Partition 1 - Existing File
    * The test creates p1 `testDelete.txt`, configures the task with this file, executes it, and asserts that `p1.exists()` returns `false`.
  * **`testMissFile()`**: Validates Partition 2 - Missing File
    * The test ensures `testDelete.txt` does not exist, then attempts to execute the task. A `try-catch` block is used to handle potential `BuildException`s, verifying that the task handles missing files according to the configuration.
  * **`testEmptyDir()`**: Validates Partition 3 - Empty Directory
    * The test creates a directory `empty_dir`, sets the task's `dir` attribute, executes it, and asserts the directory is removed.
  * **`testNonEmptyDir()`**: Validates Partition 4 - Non-empty Directory
    * The test creates `nonempty_dir` containing `child.txt`. It executes the task and asserts that both the child file and the parent directory are successfully deleted, confirming recursive deletion logic.

  #### 5.1.3 Execution & Results
  The tests are executed using the project's native build script `build.sh`.

  ```
  ./build.sh -Dtestcase=org.apache.tools.ant.taskdefs.DeletePartitionTest test
  ```

  **Result**
  ```
  Testsuite: org.apache.tools.ant.taskdefs.DeletePartitionTest
  Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.07 sec
  BUILD SUCCESSFUL
  ```
  ![Test Results](Image/deleteTestReport.png)


</details>

<details>
  <summary><H1> Part 2. Functional Testing and Finite State Machines. </H1></summary>

  ## 1. Finite Model-Based Testing
  ### The Utility of Finite Models in Testing
  Finite models, such as **Finite State Machines (FSMs)**, provide a mathematical abstraction that transforms complex systems into manageable states and transitions. This shifts the testing focus from simple inputs to **behavioral logic**, allowing testers to design cases systematically.  

  Advantages of FSMs:

  **Systematic Coverage & Logic Validation**
  - **Logical Flaws:** Detects structural issues like unreachable states or dead states by guaranteeing every state and transition is covered.

  **Model Checking & Safety Verification**
  - **Safety Guarantees:** Verifies the system never enters illegal states, does not deadlock, and eventually terminates.
  - **Error Transition Mapping:** Explicitly defines how the system reaches a FAILED state, verifying that it halts safely via exception handling rather than continuing in an unstable state.

  **Test Oracle**
  - **Acting as a Test Oracle:** Provides the expected behavior for any input sequence. Any deviation indicates a fault in either the code or the model.


  ## 2.2 Feature 2 - MailMessage (Author: Eleanor)
  ### 2.2.1 Selection of a Non-trivial Functional Component
  #### Source code
  * [org.apache.tools.mail.MailMessage](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/main/ApacheAnt/src/main/org/apache/tools/mail/MailMessage.java)

  #### Description   
  The [`MailMessage`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/main/ApacheAnt/src/main/org/apache/tools/mail/MailMessage.java) class is a utility in Apache Ant used to send emails via the SMTP protocol. It manages an active network socket (`java.net.Socket`) and implements the client-side state of an SMTP transaction strictly.

  #### Why it lends itself well to a FSM
  - **Strict Sequential Constraints:**   
      The class imposes a rigid execution order dictated by the SMTP protocol. For example, a user cannot define a recipient before defining a sender, nor can they send the message before defining recipients.
  - **Distinct Stages:**   
      The object moves through clear stages: `CONNECTED` → `SENDER_SET` → `RECIPIENT_SET` → `DATA_MODE` → `SENT_CLOSE`
  - **Enforced Transition Logic:**   
      The class contains an internal validation mechanism [`isResponseOK`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/main/ApacheAnt/src/main/org/apache/tools/mail/MailMessage.java#L439-L447). If a method is called out of order, the underlying SMTP server returns an error code, and the `MailMessage` class catches this and throws an `IOException`. This prevents invalid state transitions effectively.
  - **Error State Handling:**   
      If the socket connection fails, the object enters an invalid state where no further commands can be processed.
  - **Terminal States:** 
      The object has a definitive lifecycle. Once [`sendAndClose()`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/main/ApacheAnt/src/main/org/apache/tools/mail/MailMessage.java#L333-L340) is called, the socket is closed, and the object reaches a terminal state that cannot be reused.

  ### 2.2.2 Functional Model Design
  **Model Description:**  
  The functional model represents the lifecycle of an SMTP transaction handled by the `MailMessage` class. The state transitions are guarded by the server's response codes; any out-of-order command results in an IOException, moving the system to an ERROR state.

  **Finite State Diagram**

  <div align="center">
    <img src="Image/MailFSMDiagram.png" width="500">
  </div>

  **States Description**
  | State | Description |
  | ----- | ----------- |
  |**CONNECTED**| Initial state after [`new MailMessage()`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/main/ApacheAnt/src/main/org/apache/tools/mail/MailMessage.java#L149-L177) is constructed. <br> The socket is open, and the `HELO` command accepted by the server.|
  |**SENDER_SET**| Sender address is set via [`from()`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/main/ApacheAnt/src/main/org/apache/tools/mail/MailMessage.java#L194-L197) and `MAIL FROM` command accepted.|
  |**RECIPIENT_SET**| Recipient address is set via [`to()`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/main/ApacheAnt/src/main/org/apache/tools/mail/MailMessage.java#L217-L220) and `RCPT TO` command accepted. <br> Additional recipients or subject can be added here without changing the state.|
  |**DATA_MODE**| [`getPrintStream()`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/main/ApacheAnt/src/main/org/apache/tools/mail/MailMessage.java#L276-L286) method is called, indicating the start of writing the email body. `DATA` command accepted.|
  |**SENT_CLOSE**|Terminal state after [`sendAndClose()`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/main/ApacheAnt/src/main/org/apache/tools/mail/MailMessage.java#L333-L340) is executed. `QUIT` command accepted, and the socket is disconnected.|
  |**ERROR**|Terminal state reached when an IOException occurs due to: <br> **Protocol Violation:** Calling methods out of order (e.g., to() before from()), causing a server error. <br> **Connection Failure:** Network interruption.|

  **Transitions & Rules**
  1. **Sequential Flow:**     
      The model enforces the path: `CONNECTED` → `SENDER_SET` → `RECIPIENT_SET` → `DATA_MODE` → `SENT_CLOSE`

  2. **Self-Transitions:**    
      In the `RECIPIENT_SET` state, methods `to()`, `cc()`, `bcc()` and `setSubject()` are valid self-transitions. They perform actions sending `RCPT TO` or updating internal maps but not change the state.

  3. **Error Handling:**  
      The `MailMessage` class validates every transition by checking the SMTP server's response code (via `isResponseOK()`). If the server returns a error code, the class throw an IOException and transition to the ERROR state.

  ### 2.2.3 Test Implementation & Documentation

  To validate the Finite State Machine model of the `MailMessage` class, I implemented the JUnit test suite `MailMessageFSMTest.java`. And since the `MailMessage` class relies on actual network socket connections, I employed mock servers to verify different state transitions.

  **Test File** 
  * [MailMessageFSMTest.java](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/tests/junit/org/apache/tools/mail/MailMessageFSMTest.java)

  **Test Case Preview**
  ```
  @Test(expected = IOException.class)
  public void testFSMProtocolViolation() throws IOException, InterruptedException {
    OOOMailServer oooServer = new OOOMailServer();
    oooServer.start();
    try {
      MailMessage msg = new MailMessage("localhost", oooServer.getPort());
      msg.to("to@you.com");       
    } finally {
      oooServer.kill();
    }
  }
  ```

  **Test Strategy**
  1.  **Happy Path Verification:**
      Use the Ant project's built-in [`DummyMailServer`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/main/ApacheAnt/src/tests/junit/org/apache/tools/ant/DummyMailServer.java) to simulate a standard SMTP server. This verifies that the client correctly executes the full sequence of state transitions under normal conditions.

  2.  **Error Path Verification:**
      Implement custom mock servers (`OOOMailServer` and `GhostMailServer`) to simulate protocol violations and network interruptions. These verify that the FSM correctly transitions to the **ERROR** state and throws an `IOException`.

  **Test Case**

  | Test Method | Scenario | Covered FSM Path | Description |
  | :--- | :--- | :--- | :--- |
  | [`testFSMHappyPath`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/tests/junit/org/apache/tools/mail/MailMessageFSMTest.java#L41-L87) | **Standard Transaction**<br>Verifies the complete valid sequence of SMTP commands. | `[*] -> CONNECTED -> SENDER_SET -> RECIPIENT_SET -> DATA_MODE -> SENT_CLOSE` | The client completes the transaction without error. The log verifies the correct receipt of `HELO`, `MAIL FROM`, `RCPT TO`, `DATA`, `QUIT` and success codes 220, 250, 354 and 221. |
  | [`testFSMProtocolViolation`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/tests/junit/org/apache/tools/mail/MailMessageFSMTest.java#L95-L112) | **Protocol Violation**<br>Simulates an attempt to set a recipient before a sender is established. | `CONNECTED` --(`to()` skip `from()`)--> `RECIPIENT_SET` (Invalid) -> `ERROR` | The `OOOMailServer` returns a `500 Error` when the out-of-order command is received. The `MailMessage` class detects the error code and throws an `IOException`. |
  | [`testFSMNetworkInterruption`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/tests/junit/org/apache/tools/mail/MailMessageFSMTest.java#L120-L143) | **Network Interruption**<br>Simulates a server crash or connection loss during the transaction. | `CONNECTED` -> `SENDER_SET` -> `RECIPIENT_SET` -> `ERROR` | The `GhostMailServer` accepts the connection but closes the socket after the `RCPT TO` stage. The client throws an `IOException` when attempting to write data to the closed connection. |

  **Mock Servers**
  * **[`OOOMailServer`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/tests/junit/org/apache/tools/mail/MailMessageFSMTest.java#L146-L198)** (Out-of-Order):  
      A mock SMTP server designed to simulate protocol errors. It will return a `500 Error` response when it receives `RCPT TO`.

  * **[`GhostMailServer`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/tests/junit/org/apache/tools/mail/MailMessageFSMTest.java#L201-L255):**
      A mock SMTP server designed to simulate network failures. It closes the socket connection after receiving `RCPT TO`.

  **Test Implementation**

  The tests are executed using the project's native build script `build.sh`.
  ```
  cd ApacheAnt
  ```

  ```
  ./build.sh -Dtestcase=org.apache.tools.mail.MailMessageFSMTest test
  ```

  **Result**
  ```
  Testsuite: org.apache.tools.mail.MailMessageFSMTest
  Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.076 sec
  BUILD SUCCESSFUL
  ```
  ![Test Results](Image/FSMMailMessage.png)

</details>

<details>
  <summary><H1> Part 3. White Box Testing and Coverage. </H1></summary>

  ## 1. Structural Testing
  Structural testing, commonly referred to **White-Box Testing**, is a software testing method to examines the internal code, structure, logic, and design.

  Unlike black-box (functional) testing, which only examines inputs and outputs, structural testing requires testers to possess the ability to read code. The goal of testing is to ensure that all paths, conditions, and statements within the program execute as intended.

  **Key mechanisms**
  * **Control Flow Testing**: Analyzing the sequence of execution instructions. 
  * **Data Flow Testing**: Tracking the lifecycle of variables.
  * **Coverage Metrics**: Ensuring specific criteria are met, like Statement Coverage (each line) or Branch Coverage (each condition).

  **Advantages**
  * **Reveals Hidden Logic Errors**: Many bugs are in edge cases or rarely executed "else" blocks. It forces testers to reach these corners, ensuring no errors occur in overlooked areas.

  * **Validates Complex Decision Logic**: It ensures that errors in conditional expressions (e.g., mistyping `>` as `>=`) are immediately detected by covering specific boundary paths.
  
  * **Eliminates Dead Code**: It helps identify unreachable or "dead" code—segments that can never be executed, allowing developers to clean the code and improve maintainability.

  * **Enhances Code Quality**: By using coverage metrics, teams have a quantitative measurement of how much of the code has actually been tested. This reduces the likelihood of bugs reaching production environments. 

  * **Detects Early Defect**: Bugs are found and fixed early in the lifecycle, which is much more cheaper or more cost-effective than fixing them after deployment.


  ## 2. Coverage of the Existing Test Suite 
  ### 2.1 Test Environment & Method  
  * Tool: **JaCoCo**.
  * Setup:
    1. Downloaded the [JaCoCo package](https://www.jacoco.org/jacoco/).
    2. Placed `jacocoagent.jar` and `jacococli.jar` into `lib/optional`.
    3. Run tests and attach the JaCoCo agent to the Ant build process:
    ```
    ./build.sh test -Dtest.junit.vmargs="-javaagent:lib/optional/jacocoagent.jar=destfile=jacoco.exec"
    ```
  * Generate Report:  
    ```
    java -jar lib/optional/jacococli.jar report jacoco.exec --classfiles build/classes --sourcefiles src/main --html jacoco_report
    ```
    ###### Report will be in `jacoco_report` folder.

  ### 2.2 Overall Coverage Summary
  The initial coverage report reveals that the existing test suite provides moderate code coverage. With **49% Line Coverage** and **44% Branch Coverage**, nearly half of the project's core logic is exercised by the current tests.
  |                  | Total   | Missed  | Covered | Coverage % | Description |
  |:----------------:|:-------:|:-------:|:-------:|:----------:|:------------|
  | **Instructions** | 250,502 | 126,501 | 124,001 | 49%        | Smallest unit of execution <br>(Java byte code instructions) |
  | **Branches**     | 26,307  | 14,482  | 11,825  | 44%        | Control flow branches <br>(if/else/switch) |
  | **Lines**        | 58,046  | 28,749  | 29,297  | 50%      | Source code lines  |
  | **Methods**      | 11,393  | 5,516   | 5,877   | 51%      | Method invocations |
  | **Classes**      | 1,146   | 433     | 713     | 62%      | Class loading      |

  <details>
    <summary>Click to View Detail Overall Report</summary>
    <div align="center">
      <img src="Image/JaCoCoReportAll.png">
    </div>
  </details>

  ### 2.3 \<Delete\> Class Coverage Summary
  The [**Delete**](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/main/ApacheAnt/src/main/org/apache/tools/ant/taskdefs/Delete.java) class is currently a weak point in the test suite, falling below the project average.  
  The method coverage is the most critical metric, indicating that more than half of the methods in this class are never invoked by any test.

  |                  | Total | Missed | Covered | Coverage % | 
  |:----------------:|:-----:|:------:|:-------:|:----------:|
  | **Instructions** | 1,270 | 691 | 579 | 45%     | 
  | **Branches**     | 170   | 91  | 79  | 46%     | 
  | **Lines**        | 290   | 158 | 132 | 45%     | 
  | **Methods**      | 49    | 32  | 17  | 35%     | 

  **Uncovered Part**  
  1. The `removeFiles(File, String[], String[])` method is completely missed, the logic for deleting a list of specific files is never executed during the test.
  2.  Most adding methods for selector list, like `addSelector()`, `addAnd()`, `addOr()` and creating method for configurations, like `createInclude()`, `createIncludesFile()` are untested, resulting in low method coverage.
  3. Some logical branches are missed. For instance, methods like `delete(File)` and `handle(Exception)` have low branch coverage, some of their paths and edge cases are not adequately tested.
  <details>
    <summary>Click to View Detail Report of Delete</summary>
    <div align="center">
      <img src="Image/JaCoCoDeleteBefore.png">
    </div>
  </details>

  ----

  ### 2.4 \<Mail\> Function Coverage Summary
  The [**Mail**](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/tree/main/ApacheAnt/src/main/org/apache/tools/mail) function demonstrates strong test reliability, significantly outperforming the project average. The 85% instruction and 88% method coverage are excellent, showing that the primary functionality is well-tested.
  |                  | Total | Missed | Covered | Coverage % | 
  |:----------------:|:-----:|:------:|:-------:|:----------:|
  | **Instructions** | 697  | 99 | 598 | 85%     | 
  | **Branches**     | 66   | 23 | 43  | 65%     | 
  | **Lines**        | 178  | 27 | 151 | 84%     | 
  | **Methods**      | 43   | 5  | 38  | 88%     | 
  | **Classes**      | 4    | 1  | 3   | 75%     |

  **Uncovered Part**  
  1. The [**ErrorInQuitException**](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/main/ApacheAnt/src/main/org/apache/tools/mail/ErrorInQuitException.java) class is missed. The tests never trigger the error condition that occurs during the SMTP `QUIT` command.
  2. In the [**MailMessage**](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/main/ApacheAnt/src/main/org/apache/tools/mail/MailMessage.java) class, the alternative Constructors `MailMessage()`, `MailMessage(String)` and `setPort(int)` are never used. The tests use the `MailMessage(String, int)` constructor.
  <details>
    <summary>Click to View Detail Report of Mail</summary>
    <div align="center">
      <img src="Image/JaCoCoMailBefore.png">
    </div>
  </details>


### 3.1 \<Delete> Coverage Test  (Author: Eleanor)
White-box testing of Delete class focusing on internal method execution, implicit logical operators, and complex integration with the [`selectors`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/tree/e_branch/ApacheAnt/src/main/org/apache/tools/ant/types/selectors) package.

**Test File** 
  * [`DeleteWhiteBoxTest.java`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/tests/junit/org/apache/tools/ant/taskdefs/DeleteWhiteBoxTest.java)


**Associated Component**
* [`removeFiles(File d, String[] f, String[] d)`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/main/org/apache/tools/ant/taskdefs/Delete.java#L839-L876): Low-level file and directory array processing.
* [`setCaseSensitive(boolean)`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/main/org/apache/tools/ant/taskdefs/Delete.java#L363-L366): Configuration for path-matching case sensitivity.
* [`createInclude()`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/main/org/apache/tools/ant/taskdefs/Delete.java#L250-L253), [`createExclude()`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/main/org/apache/tools/ant/taskdefs/Delete.java#L270-L273): Internal `PatternSet` builder methods.
* Selector Registration Methods: `addFilename()`, `addContains()`, `addNot()`, `addPresent()`, `addDepth()`, `addSize()`, `addOr()`, and `addAnd()`.

**Test Case**
| Test | Scenario | Functionality Tested | Behavioral Verification |
| :--- | :--- | :--- | :--- |
| [`testRemoveFilesDirectly()`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/tests/junit/org/apache/tools/ant/taskdefs/DeleteWhiteBoxTest.java#L52-L84) | Direct Invocation of `removeFiles()` | Internal mechanism for files & directories deletion. | By bypassing the `DirectoryScanner` and injecting hardcoded string arrays (simulating scanner output), this test ensures that the `Delete` task correctly processes isolated arrays of file and directory paths, removing only what is specified and preserving adjacent files. |
| [`testDeleteByContentAndFilename()`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/tests/junit/org/apache/tools/ant/taskdefs/DeleteWhiteBoxTest.java#L95-L131) | Content & Filename Deletion (Multi Selectors) | Implicit logical intersection AND when multiple selectors are added directly. | Ensures a file is deleted only if it meets **all** conditions simultaneously (e.g., content contains "ERROR" AND name matches `*.txt`). |
| [`testDeleteUsingNestedPatterns()`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/tests/junit/org/apache/tools/ant/taskdefs/DeleteWhiteBoxTest.java#L140-L159) | Pattern Builder Integration (`PatternSet`) | Internal handling of standard include/exclude patterns. | Validates that `PatternSet.NameEntry` objects correctly target standard files (e.g., `*.class`) for deletion while strictly protecting excluded patterns (e.g., `*Test*`). |
| [`testCaseInsensitiveAndNotSelector()`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/tests/junit/org/apache/tools/ant/taskdefs/DeleteWhiteBoxTest.java#L168-L196)| Case-Insensitive Deletion with NOT | Interaction between case-blind configuration and `NotSelector`. | Proves `caseSensitive=false` targets files regardless of casing (e.g., `.TXT` vs `.txt`), while a `NotSelector` successfully overrides this to protect specific files. |
| [`testCrossDirectoryPresenceCleanup()`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/tests/junit/org/apache/tools/ant/taskdefs/DeleteWhiteBoxTest.java#L205-L237) | Cross-Directory Cleanup | Advanced filesystem state checks across different directory structures. | Uses `PresentSelector` and `IdentityMapper` to check a secondary `src` directory; ensures files in `build` directory are only deleted if their equivalent counterpart still exists in `src`. |
| [`testDeepSmallLogOrTmpCleanup()`](https://github.com/J-ihsuan/Apache-Ant-Testing-Frameworks-and-Debugging-Practices/blob/e_branch/ApacheAnt/src/tests/junit/org/apache/tools/ant/taskdefs/DeleteWhiteBoxTest.java#L249-L310) | Multi-Dimensional Constraint Filtering | Aggregated selector logic combining structural, physical, and metadata conditions. |  Validates a complex, real-world maintenance scenario. It ensures a file is only deleted if it is buried in a subdirectory (Depth >= 1), falls beneath a specific file size threshold (Size < 50 bytes), and matches at least one of two file extensions (using logical OR). |

 **Test Implementation**

  The tests are executed using the project's native build script `build.sh`.
  ```
  cd ApacheAnt
  ```

  ```
  ./build.sh -Dtestcase=org.apache.tools.ant.taskdefs.DeleteWhiteBoxTest test
  ```

**Test Result**
  ```
  Testsuite: org.apache.tools.ant.taskdefs.DeleteWhiteBoxTest
  Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.117 sec
  BUILD SUCCESSFUL
  ```
  ![Test Results](Image/DeleteCoverageTestResult.png)

**Coverage Improvement Summary**  

  The new test suite yielded improvements across all core metrics. Notably, **line coverage increased by 50+ lines**, reaching 64% overall. And by testing the integration of different add configuration methods, we effectively expanded **method coverage to 59%**. These deeper execution of the task's also enhanced branch coverage to 56% and increased instruction coverage over 200, reducing unverified pathways within the filesystem manipulation logic.

  |                  | Total | Missed <br>(Before → After) | Covered <br>(Before → After) | Coverage % <br>(Before → After) | 
  |:----------------:|:-----:|:------:|:-------:|:----------:|
  | **Instructions** | 1,270 | 691 → 466 | 579 → 804 | 45% → 63% | 
  | **Branches**     | 170   | 91 → 74   | 79 → 96   | 46% → 56% | 
  | **Lines**        | 290   | 158 → 103 | 132 → 187 | 45% → 64% | 
  | **Methods**      | 49    | 32 → 20   | 17 → 29   | 35% → 59% | 

   <details>
    <summary>Click to View Detail Report of Delete After Coverage Testing</summary>
    <div align="center">
      <img src="Image/JaCoCoDeleteAfter.png">
    </div>
  </details>

</details>