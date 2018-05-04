import java.util.ArrayList;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;

public class Main {

	public ArrayList<String> outputLines;
	public ArrayList<String> varLines;
	public int tempVarCounter;
	
	public Main() {
		this.outputLines = new ArrayList<String>();
		this.varLines = new ArrayList<String>();
		this.tempVarCounter = 0;
	}
	
	// define usable tree node class for modeling recursive-descent parse tree
	class TreeNode {
		private ArrayList<TreeNode> children;
		private TreeNode parent;
		private Object contents;
		public TreeNode() {
			children = new ArrayList<TreeNode>();
			contents = new Object();
		}
		public ArrayList<TreeNode> getChildren() {
			return children;
		}
		public void setChildren(ArrayList<TreeNode> newChildren) {
			children = newChildren;
		}
		public void addChild(TreeNode newChild) {
			children.add(newChild);
		}
		public void removeChild(TreeNode childToRemove) {
			children.remove(childToRemove);
		}
		public TreeNode getParent() {
			return parent;
		}
		public void setParent(TreeNode newParent) {
			parent = newParent;
		}
		public Object getContents() {
			return contents;
		}
		public void setContents(Object newContents) {
			contents = newContents;
		}
	}
	
	public static void main(String[] args) {
		// allocate a new Main object for auxiliary purposes
		Main prog = new Main();
		
		String inputFilename = args[0];
		String inputFileLine = null;
		ArrayList<String> stmtArrList = new ArrayList<String>();
		
		try { // try to read in the file line by line
			FileReader inputFileReader = new FileReader(inputFilename);
			BufferedReader inputBuf = new BufferedReader(inputFileReader);
			while (((inputFileLine = inputBuf.readLine()) != null) && inputFileLine != "") {
				stmtArrList.add(inputFileLine);
			}
			inputBuf.close();
			inputFileReader.close();
		}
		catch (Exception ex) {
			System.out.println("Could not read in file " + inputFilename + "; exiting due to exception:");
			System.out.println(ex.toString());
		}
		
		TreeNode stmtListTree = prog.new TreeNode(); // make <stmt-list> root node
		
		for (String currentLine : stmtArrList) {
			currentLine = currentLine.replace(";", ""); // strip semicolon from end of line if present
			TreeNode stmt = prog.new TreeNode(); // create <stmt> node with proper parent and contents
			stmt.setParent(stmtListTree);
			String[] contentsArray = {"stmt", currentLine};
			stmt.setContents(contentsArray);
			stmtListTree.addChild(stmt); // add as child of <stmt-list> root node
		}
		
		for (TreeNode currentStmt : stmtListTree.getChildren()) {
			// for each statement (<stmt>), parse it left-recursively
			parse(currentStmt, prog);
		}
		
		File outputFile = null;
		try { // try to create a blank output file
			// make the output filename equal to [inputFilename - '.[extension]] + ".asm"
			outputFile = new File(inputFilename.replaceAll("\\..*", ".asm"));
			outputFile.createNewFile();
		}
		catch (Exception ex) {
			System.out.println("Could not create file " + inputFilename.replaceAll("\\..*", ".asm") + "; exiting due to exception:");
			System.out.println(ex.toString());
		}
		
		for (TreeNode currentStmt : stmtListTree.getChildren()) {
			// for each statement (<stmt>), compile it left-recursively
			compile(currentStmt, prog);
		}
		
		try { // try to write to output file line by line
			FileWriter outputFileWriter = new FileWriter(outputFile.getName());
			BufferedWriter outputBuf = new BufferedWriter(outputFileWriter);
			for (String currentCodeLine : prog.outputLines) {
				outputBuf.write(currentCodeLine);
				outputBuf.write("\n");
			}
			outputBuf.write("\n");
			for (String varLine : prog.varLines) {
				outputBuf.write(varLine + "    RESW 1");
				outputBuf.write("\n");
			}
			outputBuf.close();
			outputFileWriter.close();
		}
		catch (Exception ex) {
			System.out.println("Could not write to file " + outputFile.getName() + "; exiting due to exception:");
			System.out.println(ex.toString());
		}
		
		// execution finished
	}
	
	public static void parse(TreeNode curNode, Main program) {
		switch(((String[]) curNode.getContents())[0]) {
		case "stmt":
			// parse id as first substring of current <stmt> before '='
			String id = ((String[]) curNode.getContents())[1].substring(0, (((String[]) curNode.getContents())[1].indexOf('='))).trim();
			// parse expr as substring from after " = " to end of string of <stmt>
			String expr = ((String[]) curNode.getContents())[1].substring(((String[]) curNode.getContents())[1].indexOf('=') + 2).trim();
			// create subtrees as appropriate, linking children to parents and setting proper contents
			TreeNode stmtId = program.new TreeNode();
			stmtId.setParent(curNode);
			String[] idArray = {"id", id};
			stmtId.setContents(idArray);
			TreeNode stmtExpr = program.new TreeNode();
			curNode.addChild(stmtId);
			curNode.addChild(stmtExpr);
			stmtExpr.setParent(curNode);
			String[] exprArray = {"expr", expr};
			stmtExpr.setContents(exprArray);
			// recursively parse <expr> substring of current <stmt>
			parse(stmtExpr, program);
			break;
		case "expr":
			// first determine if expr **DOES NOT** contain '+' or '-'
			// if not (if the negation is the true case), assume a single <term> is present and parse it alone
			// if so (if the negation of the negation is the true case), split on those individual terms and parse each
			if (!(((String[]) curNode.getContents())[1].matches("^.*[\\+\\-]+.*$"))) {
				String termString = ((String[]) curNode.getContents())[1];
				String[] termArray = {"term", termString};
				TreeNode singleTerm = program.new TreeNode();
				curNode.addChild(singleTerm);
				singleTerm.setParent(curNode);
				singleTerm.setContents(termArray);
				// recursively parse <term> of current <expr>
				parse(singleTerm, program);
			}
			else { // else assume this is a group of terms being added or subtracted
				// first discover whether addition or subtraction is involved
				String opString;
				if (((String[]) curNode.getContents())[1].contains("+")) {
					opString = "+";
				}
				else { // the only other operation it could contain is '-'
					opString = "-";
				}
				String[] opArray = {"op", opString};
				TreeNode opNode = program.new TreeNode();
				curNode.addChild(opNode);
				opNode.setParent(curNode);
				opNode.setContents(opArray);
				// and *now* split on '+' or '-' as delimiter(s) and parse them multiple-y
					// (since we have now saved the information of what the actual op used was)
				String[] terms = ((String[]) curNode.getContents())[1].split("\\+|\\-");
				for (String curTerm : terms) {
					// trim leading and trailing whitespace around terms
					curTerm = curTerm.trim();
					// and add as subtree, linking to parent and recursively parsing
					String currentTermString = curTerm;
					String[] currentTermArray = {"term", currentTermString};
					TreeNode currentTerm = program.new TreeNode();
					curNode.addChild(currentTerm);
					currentTerm.setParent(curNode);
					currentTerm.setContents(currentTermArray);
					// recursively parse <term> of current <expr>
					parse(currentTerm, program);
				}
			}
			break;
		case "term":
			// first determine if term **DOES NOT** contain '*' or '/'
			// if not (if the negation is the true case), assume a single <factor> is present and parse it alone
			// if so (if the negation of the negation is the true case), split on those individual factors and parse each
			if (!(((String[]) curNode.getContents())[1].matches("^.*[\\*\\/]+.*$"))) {
				String factorString = ((String[]) curNode.getContents())[1];
				String[] factorArray = {"factor", factorString};
				TreeNode singleFactor = program.new TreeNode();
				curNode.addChild(singleFactor);
				singleFactor.setParent(curNode);
				singleFactor.setContents(factorArray);
				// recursively parse <factor> of current <term>
				parse(singleFactor, program);
			}
			else { // else assume this is a group of factors being multiplied or divided
				// first discover whether multiplication or division is involved
				String opString;
				if (((String[]) curNode.getContents())[1].contains("*")) {
					opString = "*";
				}
				else { // the only other operation it could contain is '/'
					opString = "/";
				}
				String[] opArray = {"op", opString};
				TreeNode opNode = program.new TreeNode();
				curNode.addChild(opNode);
				opNode.setParent(curNode);
				opNode.setContents(opArray);
				// and *now* split on '*' or '/' as delimiter(s) and parse them multiple-y
					// (since we have now saved the information of what the actual op used was)
				String[] factors = ((String[]) curNode.getContents())[1].split("\\*|\\/");
				for (String curFac : factors) {
					// trim leading and trailing whitespace around factors
					curFac = curFac.trim();
					// and add as subtree, linking to parent and recursively parsing
					String currentFactorString = curFac;
					String[] currentFactorArray = {"factor", currentFactorString};
					TreeNode currentFactor = program.new TreeNode();
					curNode.addChild(currentFactor);
					currentFactor.setParent(curNode);
					currentFactor.setContents(currentFactorArray);
					// recursively parse <factor> of current <term>
					parse(currentFactor, program);
				}
			}
			break;
		case "factor":
			if (((String[]) curNode.getContents())[1].matches("^[0-9]+$")) { // if factor's actual contents contain *only* numerical digits
				// create intnum subtree as appropriate, linking child to parent and setting proper contents
				int intnum = Integer.parseInt(((String[]) curNode.getContents())[1]);
				Object[] intArray = {"int", intnum};
				TreeNode intNode = program.new TreeNode();
				curNode.addChild(intNode);
				intNode.setParent(curNode);
				intNode.setContents(intArray);
			}
			else if (!(((String[]) curNode.getContents())[1].matches("^.*(?:\\(|\\)).*$"))) { // if factor's actual contents contain **NO** parentheses
				// create id subtree as appropriate, linking child to parent and setting proper contents
				String idName = ((String[]) curNode.getContents())[1];
				String[] idNameArray = {"id", idName};
				TreeNode idNameNode = program.new TreeNode();
				curNode.addChild(idNameNode);
				idNameNode.setParent(curNode);
				idNameNode.setContents(idNameArray);
			}
			else { // else this must be another <expr> surrounded by parentheses
				// make the new <expr> string into itself except with parentheses and extraneous spaces stripped
				String exprString = ((String[]) curNode.getContents())[1].replaceAll("\\(", "").replaceAll("\\)", "").trim();
				// create subtree as appropriate, linking child to parent and setting proper contents
				TreeNode nestedExpr = program.new TreeNode();
				curNode.addChild(nestedExpr);
				nestedExpr.setParent(curNode);
				String[] nestedExprArray = {"expr", exprString};
				nestedExpr.setContents(nestedExprArray);
				// recursively parse nested <expr> substring
				parse(nestedExpr, program);
			}
			break;
		default:
			System.out.println("");
			System.out.println("Syntax error parsing contents:");
			for (String elem : ((String[]) curNode.getContents())) {
				System.out.print(elem + " ");
			}
		}
	}
	
	public static void compile(TreeNode curNode, Main program) {
		switch(((String[]) curNode.getContents())[0]) {
		case "stmt":
			// recursively compile child <expr> (should only be one <expr>-type child, at index 1)
			// then write to assembly the storing of the result in this <stmt>'s id
			TreeNode exprChild = curNode.getChildren().get(1);
			Main.compile(exprChild, program);
			String stmtId = "";
			for (TreeNode childNode : curNode.getChildren()) {
				if (((String[]) childNode.getContents())[0] == "id") {
					stmtId = ((String[]) childNode.getContents())[1];
				}
			}
			program.outputLines.add("STA " + stmtId);
			break;
		case "expr":
			// if curNode multiple children (parent's children array size > 1):
				// then recursively compile all <term> subtrees/elements
			// else just recursively compile the one current factor
			
			if (curNode.getChildren().size() > 1) {
				int curNodeIdx = 0;
				for (TreeNode currentTerm : curNode.getChildren()) {
					if (curNodeIdx < (curNode.getChildren().size() - 1)) {
						// if not on last term in expr:
							// recursively compile <term>
							// then insert appropriate operation symbol between terms
						for (TreeNode currentSibling : curNode.getChildren()) {
							if (((String[])currentSibling.getContents())[0] == "op") {
								switch (((String[])currentSibling.getContents())[1]) {
								case "+":
									Main.compile(currentTerm, program);
									// TODO: complete ADD actually having an operand
									program.outputLines.add("ADD ");
									break;
								case "-":
									Main.compile(currentTerm, program);
									// TODO: complete SUB actually having an operand
									program.outputLines.add("SUB ");
									break;
								}
							}
						}
					}
					curNodeIdx++;
				}
			}
			else {
				Main.compile(curNode.getChildren().get(0), program);
			}
			break;
		case "term":
			// if curNode has multiple children (parent's children array size > 1):
				// then recursively compile all <factor> subtrees/elements
			// else just recursively compile the one current factor
			
			if (curNode.getChildren().size() > 1) {
				int curNodeIdx = 0;
				for (TreeNode currentFactor : curNode.getChildren()) {
					if (curNodeIdx < (curNode.getChildren().size() - 1)) {
						// if not on last factor in term:
							// recursively compile <factor>
							// then insert appropriate operation symbol between factors
						for (TreeNode currentSibling : curNode.getChildren()) {
							if (((String[])currentSibling.getContents())[0] == "op") {
								switch (((String[])currentSibling.getContents())[1]) {
								case "*":
									Main.compile(currentFactor, program);
									// TODO: complete MUL actually having an operand
									program.outputLines.add("MUL ");
									break;
								case "/":
									Main.compile(currentFactor, program);
									// TODO: complete DIV actually having an operand
									program.outputLines.add("DIV ");
									break;
								}
							}
						}
					}
					curNodeIdx++;
				}
			}
			else {
				Main.compile(curNode.getChildren().get(0), program);
			}
			break;
		case "factor":
		case "id":
		case "int":
		case "op":
			if (!(curNode.getChildren().isEmpty())) {
				Object[] baseContents = (Object[]) curNode.getChildren().get(0).getContents();
				String baseContentsNodeType = (String) baseContents[0];
				if (baseContentsNodeType == "id") { // id leaf node
					// <id>s need only be loaded
					program.outputLines.add("LDA " + (String) baseContents[1]);
					// also, if id not yet present in varLines, add this id to it
					if (!(program.varLines.contains(((String[]) curNode.getContents())[1]))) {
						program.varLines.add((String) baseContents[1]);
					}
				}
				else if (baseContentsNodeType == "int") { // int(num) leaf node
					// intnum literals need only be loaded
					String intnumString = ((Integer) baseContents[1]).toString();
					program.outputLines.add("LDA #" + intnumString);
				}
				else if (baseContentsNodeType == "expr") { // new <expr> subtree
					// add another T<N> temporary variable to what will be the final output
					program.varLines.add("T" + (new Integer(program.tempVarCounter)).toString());
					program.tempVarCounter++;
					// recursively compile
					Main.compile(curNode.getChildren().get(0), program);
				}
				else if (baseContentsNodeType == "op") {
					;; // do nothing with "op" nodes as their contents are already handled above
						// to determine whether ADD, SUB, MUL, or DIV will be added to the compiled
						// SIC/XE (pseudo)code
				}
			}
			break;
		default:
			System.out.println("");
			System.out.println("Syntax error compiling input code:");
			for (String elem : ((String[]) curNode.getContents())) {
				System.out.print(elem + " ");
			}
		}
	}
}