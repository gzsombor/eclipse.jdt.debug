/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.jdt.internal.debug.eval.ast.engine;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaPrimitiveValue;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaVariable;

public class JavaPrimitiveRuntimeContext extends AbstractRuntimeContext {
	/**
	 * <code>this</code> object or this context.
	 */
	private final IJavaPrimitiveValue fThisPrimitive;

	/**
	 * The thread for this context.
	 */
	private final IJavaThread fThread;

	/**
	 * ObjectValueRuntimeContext constructor.
	 *
	 * @param thisObject
	 *            <code>this</code> object of this context.
	 * @param javaProject
	 *            the project for this context.
	 * @param thread
	 *            the thread for this context.
	 */
	public JavaPrimitiveRuntimeContext(IJavaPrimitiveValue thisObject, IJavaProject javaProject, IJavaThread thread) {
		super(javaProject);
		fThisPrimitive = thisObject;
		fThread = thread;
	}

	/**
	 * @see IRuntimeContext#getVM()
	 */
	@Override
	public IJavaDebugTarget getVM() {
		return (IJavaDebugTarget) fThisPrimitive.getDebugTarget();
	}

	/**
	 * @see IRuntimeContext#getThis()
	 */
	@Override
	public IJavaPrimitiveValue getThisPrimitive() {
		return fThisPrimitive;
	}

	/**
	 * @see IRuntimeContext#getReceivingType()
	 */
	@Override
	public IJavaReferenceType getReceivingType() throws CoreException {
		return (IJavaReferenceType) getThisPrimitive().getJavaType();
	}

	/**
	 * @see IRuntimeContext#getLocals()
	 */
	@Override
	public IJavaVariable[] getLocals() {
		return new IJavaVariable[0];
	}

	/**
	 * @see IRuntimeContext#getThread()
	 */
	@Override
	public IJavaThread getThread() {
		return fThread;
	}

	/**
	 * @see IRuntimeContext#isConstructor()
	 */
	@Override
	public boolean isConstructor() {
		return false;
	}

	@Override
	public IJavaObject getThis() throws CoreException {
		return null;
	}

}
