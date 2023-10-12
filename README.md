# Mikrojava Compiler

> Project for **Compiler Construction 1** / **Programski Prevodioci 1** class.

Implementation of a compiler for the Mikrojava programming language. The compiler is designed to convert syntactically and semantically correct Mikrojava programs into Mikrojava bytecode, which can be executed on the Mikrojava Virtual Machine (MJVM).

**JFlex** is used to handle creation of **Java** code that can recognize patterns and tokens used for lexing and the **Constructor of Useful Parsers (CUP)** is used to generate **Java** code that can parse input text based on given rules. Semantic analysis and code generation are done by custom made libraries for Mikrojava.

## Features and Functionalities

### 1. Lexical Analysis

- **Lexer Creation**: A lexical analyzer (scanner) for source programs in the Mikrojava language.
- **Tokenization**: Efficient conversion of source code into recognizable tokens.
- **Handling Various Elements**: The lexer identifies identifiers, constants, keywords, operators, and comments.
- **Error Reporting**: Any unrecognized sequences are reported, detailing the exact line and column of occurrence.

### 2. Syntax Analysis

- **Grammar Implementation**: An LALR(1) grammar consistent with the Mikrojava language specification.
- **AST Creation**: Produces an abstract syntax tree (AST) for valid Mikrojava programs.
- **Error Recovery**: In the event of syntax errors, the parser recovers and provides comprehensive error messages, ensuring continuous parsing.
- **Integration with Lexer**: Ensures smooth transition from lexical to syntax analysis.

### 3. Semantic Analysis

- **AST Traversal**: Uses the AST generated during the syntax analysis phase to perform semantic checks.
- **Symbol Table Integration**: Seamlessly integrates with the provided symbol table, updating and fetching entries as necessary.
- **Contextual Condition Checks**: All contextual conditions as mentioned in the Mikrojava specification are verified.
- **Error Reporting**: Detailed feedback on any detected semantic discrepancies, pinpointing the exact location in the code.

### 4. Code Generation

- **Bytecode Production**: Transforms a semantically validated AST into bytecode for the Mikrojava Virtual Machine (MJVM).
- **Functionalities Handled**: 
  - Basic arithmetic and operations.
  - Class inheritance.
  - Object instantiation for inner classes.
  - Virtual function table creation.
  - Polymorphic method calls.
  - Constructors for inner classes.

### Execution and Output

- On successful compilation, an executable `.obj` file tailored for MJVM is generated.
- Proper error messages and logs are produced in case of compilation failures.

## Tools and Dependencies
**ETF** libraries are custom libraries specifically made for the **Compiler Construction 1** class. All libraries are located in the `lib` folder. 

- **JDK** 1.8
- **Log4J** 1.2.17
- **JFlex** 1.5.0_16-133
- **CUP** 10.0
- **ETF Symboltable** 1.1
- **ETF MJ Runtime** 1.1

---

## How-To Run

### Step 1: Compilation Setup
- Run the `compile` job from the `build.xml` file. This process will:
  - Remove previously generated files ensuring a clean compilation environment.
  - Generate a lexer using the `mjlexer.flex` specification to tokenize the Mikrojava code.
  - Produce a parser from the `mjparser.cup` specification to construct an Abstract Syntax Tree (AST) from the tokenized code.
  - Adjust the project structure if necessary.
  - Compile the source code of the compiler into executable bytecode using necessary dependencies.

### Step 2: Execute Compiler
- Start the `Compiler.java` from `test/rs.ac.bg.etf.pp1`. This action will:
  - Parse the Mikrojava source (`program.mj` from the `test` folder) into an AST.
  - Perform a semantic analysis to ensure the code matches Mikrojava's language specifications.
  - Convert the AST into MJVM-compatible bytecode (`program.obj`).

### Step 3: Run the Mikrojava Bytecode
- Run the `run` job from the `build.xml` file. During this step:
  - The bytecode is disassembled for inspection.
  - The bytecode is executed on the Mikrojava Virtual Machine.
  - Input operations (`read(...)`) will use `input.txt` to read input. Every line counts as one input to be read.
  - Output operations (`print(...)`) will use `output.txt` to print output.

## Mikrojava Code Example
```java
program test301 {
	const int zero = 0;
	const int one = 1;
	const int five = 5;

	int arr[], arr2[], a, b;
	char arr_char[];
	
	void main()	
		int point;
		bool found;
	{
		points = 0;
		points++;
		points = points + one;
		points = points * five;
		points--;

		print(points);
				
		arr = new int[3];
		arr[zero] = one;  
		arr[1] = 2;			
		arr[arr[one]] = arr[arr[0]] * 3; 
		points = arr[2] / arr[0];

		print(points);
		print(arr[2]);
			
		found = arr.findAny(one + five);
		print(found);

		arr_char = new char[3];
		arr_char[0] = 'a';
		arr_char[one] = 'b';
		arr_char[five - 3] = 'c';

		print(arr_char[1]);
		print(arr_char[one * 2]);

		read(points);
		points = -points + (five * a / 2 - one) * points - (3 % 2 + 3 * 2 - 3); 
		print(points);
	}
}
