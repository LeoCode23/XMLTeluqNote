////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2022 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package com.saxonica.testdriver;

import com.saxonica.ee.domino.DominoTree;
import com.saxonica.ee.domino.DominoTreeModel;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.TreeModel;
import net.sf.saxon.s9api.*;
import net.sf.saxon.testdriver.Environment;
import net.sf.saxon.trans.XPathException;
import org.w3c.dom.Document;


/**
 * This class is a test driver for running the QT3 test suite against Saxon-EE.
 */

public class QT3TestDriverEE extends QT3TestDriverPE {
    ;

    public QT3TestDriverEE() {
        super();
    }


    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("-?")) {
            usage();
            return;
        }

        new QT3TestDriverEE().go(args);
    }

    public static void usage() {
        System.err.println("java com.saxonica.testdriver.QT3TestSuiteDriverEE testsuiteDir catalog [-o:resultsdir] [-s:testSetName]" +
                                   " [-t:testNamePattern] [-unfolded] [-bytecode:on|off|debug] [-tree] [-lang:XP20|XP30|XQ10|XQ30]");

    }

    @Override
    public void go(String[] args) throws Exception {
        driverProc = new Processor(true);
        licensor.activate(driverProc);
        super.go(args);
    }

    /**
     * Return the appropriate tree model to use. This adds to the base set available
     * @param s  The name of the tree model required
     * @return  The tree model - null if model requested is unrecognised
     */
    @Override
    protected TreeModel getTreeModel(String s) {
        TreeModel tree = super.getTreeModel(s);
        if (s.equalsIgnoreCase("DOMINO")) {
            tree = new DominoTreeModel();
        }
        return tree;
    }

    @Override
    public XdmNode makeDominoTree(Document doc, Configuration config, String baseUri) throws SaxonApiException {
        XdmNode result = null;
        DominoTree dominoTree = null;
        try {
            dominoTree = DominoTree.makeTree(doc, config, baseUri);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
        NodeInfo root = dominoTree.getRootNode();
        result = (XdmNode) XdmValue.wrap(root);
        return result;
    }

    @Override
    protected boolean makeSchemaAware(Environment env, boolean inverse) {
         if (!treeModel.isSchemaAware() || !env.processor.isSchemaAware()) {
             return false; // cannot use the selected tree model for schema-aware tests
         }

         if (inverse) {
             // force use of a non-schema-aware processor by creating a ProfessionalConfiguration
             final Processor savedProcessor = env.processor;
             env.processor = new Processor(new Configuration());

             com.saxonica.config.ProfessionalConfiguration pConfig = new com.saxonica.config.ProfessionalConfiguration();
             pConfig.setNamePool(env.processor.getUnderlyingConfiguration().getNamePool());
             env.processor = new Processor(pConfig);
             env.processor.setConfigurationProperty(Feature.STABLE_UNPARSED_TEXT, true);


             // Note that at present no variables are copied across
             final boolean xpcState = env.xpathCompiler.isSchemaAware();
             final boolean xqcState = env.xqueryCompiler.isSchemaAware();
             final boolean xtState = env.xsltCompiler != null && env.xsltCompiler.isSchemaAware();
             env.xpathCompiler = env.processor.newXPathCompiler();
             env.xqueryCompiler = env.processor.newXQueryCompiler();
             env.xsltCompiler = env.processor.newXsltCompiler();
             env.xpathCompiler.setSchemaAware(false);
             env.xqueryCompiler.setSchemaAware(false);
             env.xsltCompiler.setSchemaAware(false);
             env.resetActions.add(new Environment.ResetAction() {
                 @Override
                 public void reset(Environment env) {
                     env.xpathCompiler.setSchemaAware(xpcState);
                     env.xqueryCompiler.setSchemaAware(xqcState);
                     if (env.xsltCompiler != null) {
                         env.xsltCompiler.setSchemaAware(xtState);
                     }
                 }
             });
         } else {
             final boolean xpcState = env.xpathCompiler.isSchemaAware();
             final boolean xqcState = env.xqueryCompiler.isSchemaAware();
             final boolean xtState = env.xsltCompiler != null && env.xsltCompiler.isSchemaAware();
             env.xpathCompiler.setSchemaAware(true);
             env.xqueryCompiler.setSchemaAware(true);
             if (env.xsltCompiler != null) {
                env.xsltCompiler.setSchemaAware(true);
             }
             env.resetActions.add(new Environment.ResetAction() {
                 @Override
                 public void reset(Environment env) {
                     env.xpathCompiler.setSchemaAware(xpcState);
                     env.xqueryCompiler.setSchemaAware(xqcState);
                     if (env.xsltCompiler != null) {
                         env.xsltCompiler.setSchemaAware(xtState);
                     }
                 }
             });
         }

         return true;
     }
    
}
