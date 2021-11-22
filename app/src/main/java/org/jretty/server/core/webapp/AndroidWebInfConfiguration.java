package org.jretty.server.core.webapp;

import java.util.List;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;

public class AndroidWebInfConfiguration extends WebInfConfiguration {
    private static final long serialVersionUID = 314977241413L;

    public void preConfigure(WebAppContext context)
            throws Exception {
        context.setClassLoader(new AndroidClassLoader(this.getClass().getClassLoader(), context));
        super.preConfigure(context);
        Log.debug("Setting classloader parent=" + this.getClass().getClassLoader() + " for context: " + context);
    }

    public void configure(WebAppContext context)
            throws Exception {
        //cannot configure if the context is already started
        if (context.isStarted()) {
            if (Log.isDebugEnabled()) {
                Log.debug("Cannot configure webapp " + context + " after it is started");
            }
            return;
        }
        Resource web_inf = context.getWebInf();
        String paths = "";
        //Make a loader containing all .zip files in WEB-INF/lib
        if ((web_inf != null) && web_inf.isDirectory()) {
            Resource lib = web_inf.addPath("lib/");

            ((AndroidClassLoader)context.getClassLoader()).addJars(lib);
            /*
            if (lib.exists() || lib.isDirectory()) 
            {
                for (String dex : lib.list()) 
                {
                    if (dex.endsWith("zip") || dex.endsWith("apk")) 
                    {
                        String fullpath = web_inf.addPath("lib/").addPath(dex).getFile().getAbsolutePath();
                        if (!"".equals(paths)) 
                        {
                            paths += ":";
                        }

                        paths += fullpath;
                    }
                }
            }
            */
        }

        //initialize the paths for the dex class loader
        ((AndroidClassLoader)context.getClassLoader()).init();

        // Look for extra resource
        List<Resource> resources = (List<Resource>)context.getAttribute(RESOURCE_URLS);
        if (resources!=null) {
            Resource[] collection=new Resource[resources.size()+1];
            int i=0;
            collection[i++]=context.getBaseResource();
            for (Resource resource : resources)
                collection[i++]=resource;
            context.setBaseResource(new ResourceCollection(collection));
        } 
    }
}
