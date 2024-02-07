////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2002  Oliver Burn
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////
package com.puppycrawl.tools.checkstyle;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import com.puppycrawl.tools.checkstyle.api.AutomaticBean;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.Context;
import com.puppycrawl.tools.checkstyle.api.FileSetCheck;
import com.puppycrawl.tools.checkstyle.api.LocalizedMessage;
import com.puppycrawl.tools.checkstyle.api.MessageDispatcher;

/**
 * This class provides the functionality to check a set of files.
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 * @author <a href="mailto:stephane.bailliez@wanadoo.fr">Stephane Bailliez</a>
 * @author lkuehne
 */
public class Checker extends AutomaticBean
    implements MessageDispatcher
{
    /**
     * An AuditListener that maintains the number of errors.
     */
    private class ErrorCounter implements AuditListener
    {
        /** keeps track of the number of errors */
        private int mCount = 0;

        /** @see AuditListener */
        public void addError(AuditEvent aEvt)
        {
            mCount++;
        }

        /** @see AuditListener */
        public void addException(AuditEvent aEvt, Throwable aThrowable)
        {
            mCount++;
        }

        /** @see AuditListener */
        public void auditStarted(AuditEvent aEvt)
        {
            mCount = 0;
        }

        /** @see AuditListener */
        public void fileStarted(AuditEvent aEvt)
        {
        }

        /** @see AuditListener */
        public void auditFinished(AuditEvent aEvt)
        {
        }

        /** @see AuditListener */
        public void fileFinished(AuditEvent aEvt)
        {
        }

        /**
         * @return the number of errors since audit started.
         */
        private int getCount()
        {
            return mCount;
        }
    }

    /** maintains error count */
    private final ErrorCounter mCounter = new ErrorCounter();

    /** vector of listeners */
    private final ArrayList mListeners = new ArrayList();

    /** vector of fileset checks */
    private final ArrayList mFileSetChecks = new ArrayList();

    /** class loader to resolve classes with. **/
    private ClassLoader mLoader =
            Thread.currentThread().getContextClassLoader();

    /** the basedir to strip off in filenames */
    private String mBasedir;

    /** locale country to report messages  **/
    private String mLocaleCountry = Locale.getDefault().getCountry();
    /** locale language to report messages  **/
    private String mLocaleLanguage = Locale.getDefault().getLanguage();

    /** The factory for instantiating submodules */
    private ModuleFactory mModuleFactory;

    /** the context of all child components */
    private Context mChildContext;

    /**
     * Creates a new <code>Checker</code> instance.
     * The instance needs to be contextualized and configured.
     *
     * @throws CheckstyleException if an error occurs
     */
    public Checker()
        throws CheckstyleException
    {
        addListener(mCounter);
    }

    /** @see AutomaticBean */
    public void finishLocalSetup()
            throws CheckstyleException
    {
        final Locale locale = new Locale(mLocaleLanguage, mLocaleCountry);
        LocalizedMessage.setLocale(locale);

        if (mModuleFactory == null) {
            mModuleFactory = PackageNamesLoader.loadModuleFactory(
                    this.getClass().getClassLoader());
        }

        final DefaultContext context = new DefaultContext();
        context.add("classLoader", mLoader);
        context.add("moduleFactory", mModuleFactory);
        mChildContext = context;
    }

    /**
     * Instantiates, configures and registers a FileSetCheck
     * that is specified in the provided configuration.
     * @see com.puppycrawl.tools.checkstyle.api.AutomaticBean
     */
    protected void setupChild(Configuration aChildConf)
            throws CheckstyleException
    {
        final String name = aChildConf.getName();
        try {
            final Object module = mModuleFactory.createModule(name);
            if (!(module instanceof FileSetCheck)) {
                throw new CheckstyleException(name
                    + " is not allowed as a module in Checker");
            }
            final FileSetCheck fsc = (FileSetCheck) module;
            fsc.contextualize(mChildContext);
            fsc.configure(aChildConf);
            addFileSetCheck(fsc);
        }
        catch (Exception ex) {
            // TODO i18n
            throw new CheckstyleException(
                    "cannot initialize module "
                    + name + " - " + ex.getMessage());
        }
    }

    /**
     * Adds a FileSetCheck to the list of FileSetChecks
     * that is executed in process().
     * @param aFileSetCheck the additional FileSetCheck
     */
    public void addFileSetCheck(FileSetCheck aFileSetCheck)
    {
        aFileSetCheck.setMessageDispatcher(this);
        mFileSetChecks.add(aFileSetCheck);
    }

    /** Cleans up the object **/
    public void destroy()
    {
        mListeners.clear();
    }

    /**
     * Add the listener that will be used to receive events from the audit
     * @param aListener the nosy thing
     */
    public void addListener(AuditListener aListener)
    {
        mListeners.add(aListener);
    }

    /**
     * Processes a set of files with all FileSetChecks.
     * Once this is done, it is highly recommended to call for
     * the destroy method to close and remove the listeners.
     * @param aFiles the list of files to be audited.
     * @return the total number of errors found
     * @see #destroy()
     */
    public int process(File[] aFiles)
    {
        fireAuditStarted();
        for (int i = 0; i < mFileSetChecks.size(); i++) {
            FileSetCheck fileSetCheck = (FileSetCheck) mFileSetChecks.get(i);
            fileSetCheck.process(aFiles);
            fileSetCheck.destroy();
        }
        int errorCount = mCounter.getCount();
        fireAuditFinished();
        return errorCount;
    }


    /**
     * Create a stripped down version of a filename.
     * @param aFileName the original filename
     * @return the filename where an initial prefix of basedir is stripped
     */
    private String getStrippedFileName(final String aFileName)
    {
        final String stripped;
        if ((mBasedir == null) || !aFileName.startsWith(mBasedir)) {
            stripped = aFileName;
        }
        else {
            // making the assumption that there is text after basedir
            final int skipSep = mBasedir.endsWith(File.separator) ? 0 : 1;
            stripped = aFileName.substring(mBasedir.length() + skipSep);
        }
        return stripped;
    }

    /** @param aBasedir the base directory to strip off in filenames */
    public void setBasedir(String aBasedir)
    {
        mBasedir = aBasedir;
    }

    /** notify all listeners about the audit start */
    protected void fireAuditStarted()
    {
        final AuditEvent evt = new AuditEvent(this);
        final Iterator it = mListeners.iterator();
        while (it.hasNext()) {
            final AuditListener listener = (AuditListener) it.next();
            listener.auditStarted(evt);
        }
    }

    /** notify all listeners about the audit end */
    protected void fireAuditFinished()
    {
        final AuditEvent evt = new AuditEvent(this);
        final Iterator it = mListeners.iterator();
        while (it.hasNext()) {
            final AuditListener listener = (AuditListener) it.next();
            listener.auditFinished(evt);
        }
    }

    /**
     * notify all listeners about the beginning of a file audit
     * @param aFileName the file to be audited
     */
    public void fireFileStarted(String aFileName)
    {
        final String stripped = getStrippedFileName(aFileName);
        final AuditEvent evt = new AuditEvent(this, stripped);
        final Iterator it = mListeners.iterator();
        while (it.hasNext()) {
            final AuditListener listener = (AuditListener) it.next();
            listener.fileStarted(evt);
        }
    }

    /**
     * notify all listeners about the end of a file audit
     * @param aFileName the audited file
     */
    public void fireFileFinished(String aFileName)
    {
        final String stripped = getStrippedFileName(aFileName);
        final AuditEvent evt = new AuditEvent(this, stripped);
        final Iterator it = mListeners.iterator();
        while (it.hasNext()) {
            final AuditListener listener = (AuditListener) it.next();
            listener.fileFinished(evt);
        }
    }

    /**
     * notify all listeners about the errors in a file.
     * @param aFileName the audited file
     * @param aErrors the audit errors from the file
     */
    public void fireErrors(String aFileName, LocalizedMessage[] aErrors)
    {
        final String stripped = getStrippedFileName(aFileName);
        for (int i = 0; i < aErrors.length; i++) {
            final AuditEvent evt = new AuditEvent(this, stripped, aErrors[i]);
            final Iterator it = mListeners.iterator();
            while (it.hasNext()) {
                final AuditListener listener = (AuditListener) it.next();
                listener.addError(evt);
            }
        }
    }

    /**
     * Sets the factory for creating submodules.
     *
     * @param aModuleFactory the factory for creating FileSetChecks
     */
    public void setModuleFactory(ModuleFactory aModuleFactory)
    {
        mModuleFactory = aModuleFactory;
    }

    /** @param aLocaleCountry the country to report messages  **/
    public void setLocaleCountry(String aLocaleCountry)
    {
        mLocaleCountry = aLocaleCountry;
    }

    /** @param aLocaleLanguage the language to report messages  **/
    public void setLocaleLanguage(String aLocaleLanguage)
    {
        mLocaleLanguage = aLocaleLanguage;
    }
}
