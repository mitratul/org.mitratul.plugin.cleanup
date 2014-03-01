package org.mitratul.plugin.cleanup.handlers;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.mitratul.plugin.cleanup.core.JavaCodeCleaner;
import org.mitratul.plugin.cleanup.ui.FindReplaceDialog;

public class CodeCleanupHandler extends AbstractHandler {

	private JavaCodeCleaner javaCodeCleaner;

	public CodeCleanupHandler() {
		javaCodeCleaner = new JavaCodeCleaner();
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// * get the selection
		IStructuredSelection selections = (IStructuredSelection) HandlerUtil
				.getActiveMenuSelection(event);
		// * ask for input
		IWorkbenchWindow window = HandlerUtil
				.getActiveWorkbenchWindowChecked(event);
		FindReplaceDialog frDialog = new FindReplaceDialog(window.getShell());
		if (frDialog.open() == Window.OK) {
			// * perform the cleanup if user agreed
			javaCodeCleaner.setTarget(frDialog.getTarget());
			javaCodeCleaner.setReplacement(frDialog.getReplacement());
			javaCodeCleaner.setAction(frDialog.getAction());
			performCleanup(selections);
		} // * do nothing if cancelled

		return null;
	}

	private void performCleanup(IStructuredSelection selections) {

		Iterator<?> selectionIterator = selections.iterator();
		while (selectionIterator.hasNext()) {
			Object selection = selectionIterator.next();
			try {
				if (selection instanceof IJavaProject) {
					javaCodeCleaner
							.cleanupIJavaElement((IJavaProject) selection);
				} else if (selection instanceof IPackageFragmentRoot) {
					javaCodeCleaner
							.cleanupIJavaElement((IPackageFragmentRoot) selection);
				} else if (selection instanceof IPackageFragment) {
					javaCodeCleaner
							.cleanupIJavaElement((IPackageFragment) selection);
				} else if (selection instanceof ICompilationUnit) {
					javaCodeCleaner
							.cleanupIJavaElement((ICompilationUnit) selection);
				} else {
					// * TODO: add support for other type if required
					System.err.println("*** TODO: Handle "
							+ selection.getClass());
					continue;
				}
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
