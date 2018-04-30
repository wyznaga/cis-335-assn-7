package assignment;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;

public class Main {

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
	
	public static void parse(String cur) {
		; // TODO: complete implementation
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
			stmt.setContents(currentLine);
			stmtListTree.addChild(stmt); // add as child of <stmt-list> root node
		}
		
		for (TreeNode currentStmt : stmtListTree.getChildren()) {
			// for each statement (<stmt>), parse it left-recursively
			parse((String) currentStmt.getContents());
		}
		
		// TODO: create blank output SIC file
		// TODO: iterate over id leaves and append them as `[id] RESW 1` lines to SIC output
		// TODO: close all file handles
	}
}
