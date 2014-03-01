package org.mitratul.plugin.cleanup.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class JavaCodeCleaner {

	/**
	 * TODO: change the key value to proper type parameters, use it to store
	 * cleanup or replacements.
	 */
	// private Map<String, String> mTargetReplacementMap;
	private String target;
	private String replacement;
	private CleanupAction action;

	public JavaCodeCleaner() {
		target = "";
		replacement = "";
	}

	private CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

	public CompilationUnit parse(char[] unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

	private boolean isTargetStatement(Object statement) {
		// return statement.toString().startsWith("System.out.println(")
		// || statement.toString().startsWith("System.err.println(");
		return statement.toString().startsWith(getTarget());
	}

	public void cleanupIJavaElement(IJavaProject selection)
			throws JavaModelException {
		IPackageFragmentRoot[] packageFragmentRoots = selection
				.getAllPackageFragmentRoots();
		for (IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots) {
			if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
				cleanupIJavaElement(packageFragmentRoot);
			}
		}
	}

	public void cleanupIJavaElement(IPackageFragmentRoot packageFragmentRoot)
			throws JavaModelException {
		IJavaElement[] packageFragments = packageFragmentRoot.getChildren();
		for (IJavaElement packageFragment : packageFragments) {
			if (((IPackageFragment) packageFragment).getKind() == IPackageFragmentRoot.K_SOURCE) {
				cleanupIJavaElement((IPackageFragment) packageFragment);
			}
		}

	}

	public void cleanupIJavaElement(IPackageFragment packageFragmentToClean)
			throws JavaModelException {
		for (ICompilationUnit unit : packageFragmentToClean
				.getCompilationUnits()) {
			cleanupIJavaElement(unit);
		}
	}

	public void cleanupIJavaElement(ICompilationUnit compilationUnitToClean)
			throws JavaModelException {
		// * create the objects required for AST traversal / modification
		Document document = new Document(compilationUnitToClean.getSource());
		CompilationUnit astRoot = parse(compilationUnitToClean);
		AST ast = astRoot.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);

		// * turn on recording the modifications
		astRoot.recordModifications();

		// * start traversing AST, if target statement found, do changes in the
		// AST
		for (Object aTypeDeclaration : astRoot.types()) {
			// * clean up individual types
			cleanupType(rewrite, aTypeDeclaration);
		}

		// * Once all the cleanups are executed on the AST,
		// apply the changes in the source file.
		TextEdit edits = rewrite.rewriteAST(document, null);
		try {
			edits.apply(document);
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// * then save the compilation unit
		String newSource = document.get();
		compilationUnitToClean.getBuffer().setContents(newSource);
		compilationUnitToClean.save(null, true);
		// System.out.println("\n============| unit.isOpen() :: "
		// + unitToClean.isOpen() + " |============");
	}

	private void cleanupType(ASTRewrite rewrite, Object typeToClean) {
		for (Object bodyDeclaration : ((TypeDeclaration) typeToClean)
				.bodyDeclarations()) {

			if (bodyDeclaration instanceof Initializer) {
				// * clean up initializer block
				cleanupBlock(rewrite, ((Initializer) bodyDeclaration).getBody());
			} else if (bodyDeclaration instanceof MethodDeclaration) {
				// * clean up method / constructor block
				cleanupBlock(rewrite,
						((MethodDeclaration) bodyDeclaration).getBody());
			} else if (bodyDeclaration instanceof TypeDeclaration) {
				// * clean up nested type
				cleanupType(rewrite, bodyDeclaration);
			} else {
				// * TODO: add support for other block if required
				System.err.println("*** TODO: Handle "
						+ bodyDeclaration.getClass());
				continue;
			}
		}
	}

	private void cleanupBlock(ASTRewrite rewrite, Block blockToClean) {
		// * empty blocks can be skipped
		if (blockToClean == null) {
			return;
		}

		List<Statement> targetStatements = new ArrayList<Statement>();
		for (Object statement : blockToClean.statements()) {
			// * Whenever encounter any target statement, add it to a list.
			// DO NOT change the AST yet, else will mess up the rest of the
			// traversal.
			if (isTargetStatement(statement)) {
				targetStatements.add((Statement) statement);
			}
		}
		// * once the traversal is complete, then change all the listed targets
		ListRewrite listRewrite = rewrite.getListRewrite(blockToClean,
				Block.STATEMENTS_PROPERTY);
		for (Statement targetStatement : targetStatements) {
			cleanupTarget(listRewrite, targetStatement);
		}
	}

	private void cleanupTarget(ListRewrite listRewrite,
			Statement targetStatement) {
		// * decide on the action based on the input
		CleanupAction action = getAction();

		// * perform clean up accordingly
		ASTRewrite rewrite = listRewrite.getASTRewrite();
		if (CleanupAction.REMOVE == action) {
			listRewrite.remove(targetStatement, null);

		} else if (CleanupAction.COMMENT == action) {
			listRewrite.insertBefore(rewrite.createStringPlaceholder("/*",
					ASTNode.EMPTY_STATEMENT), targetStatement, null);
			listRewrite.insertAfter(rewrite.createStringPlaceholder("*/",
					ASTNode.EMPTY_STATEMENT), targetStatement, null);

		} else if (CleanupAction.REPLACE == action) {
			listRewrite.replace(targetStatement, rewrite
					.createStringPlaceholder(
							getReplacementStatement(targetStatement),
							ASTNode.EMPTY_STATEMENT), null);

		} // * else do nothing
	}

	private String getReplacementStatement(Statement targetStatement) {
		return getReplacement()
				+ targetStatement.toString().substring(getTarget().length());
	}

	public String getTarget() {
		return target.trim();
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getReplacement() {
		return replacement.trim();
	}

	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	public CleanupAction getAction() {
		return action;
	}

	public void setAction(CleanupAction action) {
		this.action = action;
	}

	/*
	 * public static void main(String[] args) {
	 * 
	 * 
	 * String src = "System.out.print();";
	 * 
	 * 
	 * ASTParser parser = ASTParser.newParser(AST.JLS4);
	 * parser.setKind(ASTParser.K_STATEMENTS);
	 * parser.setSource(src.toCharArray()); parser.setResolveBindings(true);
	 * Block astBlock = (Block) parser.createAST(null);
	 * 
	 * System.out.println("-\n"); System.out.println("MALFORMED: " +
	 * (astBlock.getFlags() | ASTNode.MALFORMED));
	 * System.out.println("METHOD_INVOCATION: " + (astBlock.getFlags() |
	 * ASTNode.METHOD_INVOCATION)); System.out.println("Statements: " +
	 * astBlock.statements());
	 * 
	 * }
	 */
}
