package org.eclipse.jdt.internal.launching;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallChangedListener;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.sourcelookup.ArchiveSourceLocation;

public class LaunchingPlugin extends Plugin implements Preferences.IPropertyChangeListener, IVMInstallChangedListener {
	
	/**
	 * Identifier for 'vmConnectors' extension point
	 */
	public static final String ID_EXTENSION_POINT_VM_CONNECTORS = getUniqueIdentifier() + ".vmConnectors"; //$NON-NLS-1$
	
	private static LaunchingPlugin fgLaunchingPlugin;
	
	private HashMap fVMConnectors = null;
	
	public LaunchingPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		fgLaunchingPlugin= this;
	}
	
	/**
	 * Convenience method which returns the unique identifier of this plugin.
	 */
	public static String getUniqueIdentifier() {
		if (getDefault() == null) {
			// If the default instance is not yet initialized,
			// return a static identifier. This identifier must
			// match the plugin id defined in plugin.xml
			return "org.eclipse.jdt.launching"; //$NON-NLS-1$
		}
		return getDefault().getDescriptor().getUniqueIdentifier();
	}

	public static LaunchingPlugin getDefault() {
		return fgLaunchingPlugin;
	}
	
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}
	
	public static void log(String message) {
		log(new Status(IStatus.ERROR, getUniqueIdentifier(), IStatus.ERROR, message, null));
	}	
		
	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getUniqueIdentifier(), IStatus.ERROR, e.getMessage(), e));
	}	
	
	/**
	 * Clears zip file cache.
	 * Shutdown the launch config helper.
	 * 
	 * @see Plugin#shutdown()
	 */
	public void shutdown() throws CoreException {
		ArchiveSourceLocation.shutdown();
		getPluginPreferences().removePropertyChangeListener(this);
		JavaRuntime.removeVMInstallChangedListener(this);
		super.shutdown();
	}
		
	/**
	 * @see Plugin#startup()
	 */
	public void startup() throws CoreException {
		super.startup();
		
		//exclude launch configurations from being copied to the output directory
		Hashtable optionsMap = JavaCore.getOptions();
		String filters= (String)optionsMap.get("org.eclipse.jdt.core.builder.resourceCopyExclusionFilter"); //$NON-NLS-1$
		if (filters == null || filters.length() ==0) {
			filters= "*." + ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION; //$NON-NLS-1$;;
		} else {
			filters= filters + ",*." + ILaunchConfiguration.LAUNCH_CONFIGURATION_FILE_EXTENSION; //$NON-NLS-1$;
		}

		optionsMap.put("org.eclipse.jdt.core.builder.resourceCopyExclusionFilter", filters);  //$NON-NLS-1$
		JavaCore.setOptions(optionsMap);
		
		// set default preference values
		getPluginPreferences().setDefault(JavaRuntime.PREF_CONNECT_TIMEOUT, JavaRuntime.DEF_CONNECT_TIMEOUT);
		getPluginPreferences().addPropertyChangeListener(this);
		JavaRuntime.addVMInstallChangedListener(this);
	}
	
	/**
	 * Returns the VM connetor with the specified id, or <code>null</code>
	 * if none. 
	 * 
	 * @param id connector identifier
	 * @return VM connector
	 */
	public IVMConnector getVMConnector(String id) {
		if (fVMConnectors == null) {
			initializeVMConnectors();
		}
		return (IVMConnector)fVMConnectors.get(id); 
	}
	
	/**
	 * Returns all VM connector extensions.
	 *
	 * @return VM connectors
	 */
	public IVMConnector[] getVMConnectors() {
		if (fVMConnectors == null) {
			initializeVMConnectors();
		}
		return (IVMConnector[])fVMConnectors.values().toArray(new IVMConnector[fVMConnectors.size()]); 
	}	
	
	/**
	 * Loads VM connector extensions
	 */
	private void initializeVMConnectors() {
		IExtensionPoint extensionPoint= Platform.getPluginRegistry().getExtensionPoint(ID_EXTENSION_POINT_VM_CONNECTORS);
		IConfigurationElement[] configs= extensionPoint.getConfigurationElements(); 
		MultiStatus status= new MultiStatus(getUniqueIdentifier(), IStatus.OK, LaunchingMessages.getString("LaunchingPlugin.Exception_occurred_reading_vmConnectors_extensions_1"), null); //$NON-NLS-1$
		fVMConnectors = new HashMap(configs.length);
		for (int i= 0; i < configs.length; i++) {
			try {
				IVMConnector vmConnector= (IVMConnector)configs[i].createExecutableExtension("class"); //$NON-NLS-1$
				fVMConnectors.put(vmConnector.getIdentifier(), vmConnector);
			} catch (CoreException e) {
				status.add(e.getStatus());
			}
		}
		if (!status.isOK()) {
			LaunchingPlugin.log(status);
		}			
	}
	
	/**
	 * Save preferences whenever they change.
	 * 
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(JavaRuntime.PREF_CONNECT_TIMEOUT)) {
			savePluginPreferences();
		}
	}

	/**
	 * Update any classpaths that reference the default VM install
	 * 
	 * @see IVMInstallChangedListener#defaultVMInstallChanged(IVMInstall, IVMInstall)
	 */
	public void defaultVMInstallChanged(IVMInstall previous, IVMInstall current) {
		IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		try {
			IJavaProject[] projects = model.getJavaProjects();
			List affectedProjects = new ArrayList(projects.length);
			for (int i = 0; i < projects.length; i++) {
				IClasspathEntry[] classpath = projects[i].getRawClasspath();
				for (int j = 0; j < classpath.length; j++) {
					IClasspathEntry entry = classpath[j];
					if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
						IPath path = entry.getPath();
						if (path.segmentCount() == 1 && path.segment(0).equals(JavaRuntime.JRE_CONTAINER)) {
							// references default JRE
							affectedProjects.add(projects[i]);
						}
					}
				}
			}
			if (!affectedProjects.isEmpty()) {
				IJavaProject[] projArray = (IJavaProject[])affectedProjects.toArray(new IJavaProject[affectedProjects.size()]);
				IPath containerPath = new Path(JavaRuntime.JRE_CONTAINER);
				IVMInstall vm = JREContainerInitializer.resolveVM(containerPath);
				JREContainer container = new JREContainer(vm, containerPath);
				IClasspathContainer[] containers = new IClasspathContainer[projArray.length];
				Arrays.fill(containers, container);
				JavaCore.setClasspathContainer(containerPath, projArray, containers, null);
			}
		} catch (JavaModelException e) {
			LaunchingPlugin.log(e);
		}
	}

	/**
	 * Update classpaths that reference the changed VM, as required.
	 * 
	 * @see IVMInstallChangedListener#vmChanged(PropertyChangeEvent)
	 */
	public void vmChanged(org.eclipse.jdt.launching.PropertyChangeEvent event) {
	}

	/**
	 * @see IVMInstallChangedListener#vmAdded(IVMInstall)
	 */
	public void vmAdded(IVMInstall vm) {
	}

	/**
	 * @see IVMInstallChangedListener#vmRemoved(IVMInstall)
	 */
	public void vmRemoved(IVMInstall vm) {
	}

}

