////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2022 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package com.saxonica.testdriver;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.expr.parser.CodeInjector;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.TreeModel;
import net.sf.saxon.option.axiom.AxiomObjectModel;
import net.sf.saxon.option.dom4j.DOM4JObjectModel;
import net.sf.saxon.option.jdom2.JDOM2ObjectModel;
import net.sf.saxon.option.xom.XOMObjectModel;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.testdriver.QT3TestDriverHE;


/**
 * This class is a test driver for running the QT3 test suite against Saxon-PE and Saxon-EE.
 */

public class QT3TestDriverPE extends QT3TestDriverHE {

    protected Licensor licensor = new Licensor();

    public QT3TestDriverPE() {
        super();
    }


    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("-?")) {
            usage();
            return;
        }

        new QT3TestDriverPE().go(args);
    }

    public static void usage() {
        System.err.println("java com.saxonica.testdriver.QT3TestSuiteDriverPE testsuiteDir catalog [-o:resultsdir] [-s:testSetName]" +
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
        if (s.equalsIgnoreCase("jdom2")) {
            tree = new JDOM2ObjectModel();
        } else if (s.equalsIgnoreCase("dom4j")) {
            tree = new DOM4JObjectModel();
        } else if (s.equalsIgnoreCase("xom")) {
            tree = new XOMObjectModel();
        } else if (s.equalsIgnoreCase("axiom")) {
            tree = new AxiomObjectModel();
        }
        return tree;
    }



    @Override
    protected boolean isSupportedLanguage(String language) {
        return "en".equals(language) || "fr".equals(language) || "de".equals(language) || "it".equals(language);
    }


    @Override
    public void addInjection(XQueryCompiler compiler) {
        compiler.getUnderlyingStaticContext().setCodeInjector(new LazyLiteralInjector());
    }

    /**
     * The LazyLiteralInjector wraps literals appearing in a query-under-test by a call to an extension
     * function that delivers the same value as the literal. This is designed to suppress the "constant
     * unfolding" optimization whereby expressions are evaluated at compile time; as such it forces
     * greater coverage of code paths in the interpreter and in the byte code generator.
     */

    private static class LazyLiteralInjector implements CodeInjector {

        @Override
        public Expression inject(Expression exp) {
            if (exp instanceof Literal) {
                if (exp instanceof StringLiteral && ((StringLiteral)exp).getString().toString().startsWith("http://www.w3.org/")) {
                    // This is to avoid dynamic computation of Collation uris, which tends to disable bytecode generation
                    return exp;
                }
                StructuredQName name = new StructuredQName("saxon", NamespaceConstant.SAXON, "lazy-literal");
                com.saxonica.expr.JavaExtensionFunctionCall wrapper = new com.saxonica.expr.JavaExtensionFunctionCall();
                try {
                    wrapper.init(name,
                                 QT3TestDriverHE.class,
                                 QT3TestDriverHE.class.getMethod("lazyLiteral", Sequence.class),
                                 exp.getConfiguration()
                    );
                    wrapper.setArguments(new Expression[]{exp});
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                }
                return wrapper;
            } else {
                return exp;
            }
        }

    }
}
