/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package com.archimatetool.modelrepository;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.archimatetool.editor.FileLogger;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.modelrepository.preferences.IPreferenceConstants;
import com.archimatetool.modelrepository.treemodel.RepositoryTreeModel;



/**
 * Activator
 * 
 * @author Phillip Beauvoir
 */
@SuppressWarnings("nls")
public class ModelRepositoryPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.archimatetool.modelrepository";
    
    // General Help ID for contextual help
    // If we ever ship a full set of help pages then each View, Dialog, or Page can add their own unique Help ID and context
    public static String HELP_ID = ModelRepositoryPlugin.PLUGIN_ID + ".modelRepositoryHelp"; //$NON-NLS-1$

    // The shared instance
    private static ModelRepositoryPlugin instance;
    
    private FileLogger fileLogger;
    
    /**
     * @return the shared instance
     */
    public static ModelRepositoryPlugin getInstance() {
        return instance;
    }
    
    public ModelRepositoryPlugin() {
        instance = this;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        
        // Set these first
        setSystemProperties();
        
        // Start logging
        createLogger();
        
        // If user changes repo folder location, close old logger and create a new one and reset RepositoryTreeModel
        getPreferenceStore().addPropertyChangeListener(event -> {
            if(IPreferenceConstants.PREFS_REPOSITORY_FOLDER.equals(event.getProperty())) {
                if(fileLogger != null) {
                    fileLogger.close();
                    createLogger();
                }

                RepositoryTreeModel.getInstance().reset();
            }
        });
    }
    
    /**
     * Create and register a new FileLogger
     */
    private void createLogger() {
        try {
            fileLogger = FileLogger.create("com.archimatetool.modelrepository",
                              getBundle().getEntry("logging.properties"),
                              new File(getUserModelRepositoryFolder(), "log-%g.txt"));
        }
        catch(IOException ex) {
            ILog.of(getBundle()).error("Could not start logger!", ex);
        }
    }
    
    private void setSystemProperties() {
        // This needs to be set in order to avoid this exception when using a Proxy:
        // "Unable to tunnel through proxy. Proxy returns "HTTP/1.1 407 Proxy Authentication Required""
        // It needs to be set before any JGit operations, because it can't be set again
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        
        // Added this one too for Proxy. I think it's for HTTP
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
    }
    
    /**
     * @return The File Location of this plugin
     */
    public File getPluginFolder() {
        try {
            URL url = FileLocator.resolve(getBundle().getEntry("/"));
            return new File(url.getPath());
        }
        catch(IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    /**
     * @return The folder where we store repositories
     */
    public File getUserModelRepositoryFolder() {
        // Get from preferences
        String path = getPreferenceStore().getString(IPreferenceConstants.PREFS_REPOSITORY_FOLDER);
        
        if(StringUtils.isSet(path)) {
            return normalizedFile(path);
        }
        
        // Default
        return normalizedFile(getPreferenceStore().getDefaultString(IPreferenceConstants.PREFS_REPOSITORY_FOLDER));
    }
    
    /**
     * @return File normalized so that any ".." is removed
     */
    private File normalizedFile(String filePath) {
        return Path.of(filePath).toAbsolutePath().normalize().toFile();
    }
}
