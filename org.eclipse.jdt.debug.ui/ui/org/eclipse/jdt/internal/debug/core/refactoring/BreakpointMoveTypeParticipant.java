/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.core.refactoring;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.ltk.core.refactoring.Change;

/**
 * Breakpoint participant for type move.
 *
 * @since 3.2
 */
public class BreakpointMoveTypeParticipant extends BreakpointMoveParticipant {

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.core.refactoring.BreakpointRenameParticipant#accepts(org.eclipse.jdt.core.IJavaElement)
	 */
	@Override
	protected boolean accepts(IJavaElement element) {
		return element instanceof IType && getArguments().getDestination() instanceof IPackageFragment;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.core.refactoring.BreakpointMoveParticipant#gatherChanges(org.eclipse.core.resources.IMarker[], java.util.List)
	 */
	@Override
	protected void gatherChanges(IMarker[] markers, List<Change> changes) throws CoreException, OperationCanceledException {
		IType originalType = (IType) getOriginalElement();
		IPackageFragment destPackage = (IPackageFragment) getDestination();
		for (IMarker marker : markers) {
			IBreakpoint breakpoint = getBreakpoint(marker);
			if (breakpoint instanceof IJavaBreakpoint javaBreakpoint) {
				IType breakpointType = BreakpointUtils.getType(javaBreakpoint);
				if (breakpointType != null && isContained(originalType, breakpointType)) {
					ICompilationUnit cu = destPackage.getCompilationUnit(breakpointType.getCompilationUnit().getElementName());
					IJavaElement element = BreakpointChange.findElement(cu, breakpointType);
					if (element != null) {
						if (element instanceof IType destType) {
							changes.add(createTypeChange(javaBreakpoint, destType, breakpointType));
						}
					}
				}
			}
		}
	}

}
