////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2022 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package com.saxonica.testdriver;


import com.saxonica.ee.domino.DominoTreeModel;
import com.saxonica.ee.stream.Posture;
import com.saxonica.ee.stream.PostureAndSweep;
import com.saxonica.ee.stream.Streamability;
import com.saxonica.ee.stream.Sweep;
import com.saxonica.ee.trans.ContextItemStaticInfoEE;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.om.TreeModel;
import net.sf.saxon.s9api.*;
import net.sf.saxon.testdriver.Environment;
import net.sf.saxon.type.AnyItemType;

import java.util.ArrayList;
import java.util.List;

import static net.sf.saxon.s9api.streams.Steps.path;

/**
 * Created by mike on 2017-06-07.
 */
public class Xslt30TestSuiteDriverEE extends Xslt30TestSuiteDriverPE {


    public Xslt30TestSuiteDriverEE() {
        super();
    }


    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args[0].equals("-?")) {
            usage();
            System.exit(2);
        }

        new Xslt30TestSuiteDriverEE().go(args);
    }

    /**
     * Return the appropriate tree model to use. This adds to the base set available
     *
     * @param s The name of the tree model required
     * @return The tree model - null if model requested is unrecognised
     */
    @Override
    protected TreeModel getTreeModel(String s) {
        if (s.equalsIgnoreCase("DOMINO")) {
            return new DominoTreeModel();
        } else {
            return super.getTreeModel(s);
        }
    }

    @Override
    protected void initPatternOptimization(XsltCompiler compiler) {
        // compiler.getUnderlyingCompilerInfo().setPatternOptimization(new PatternOptimizationEE());
    }


    /**
     * Ensure that a dependency is satisfied, first by checking whether Saxon supports
     * the requested feature, and if necessary by reconfiguring Saxon so that it does;
     * if configuration changes are made, then resetActions should be registered to
     * reverse the changes when the test is complete.
     *
     * @param dependency the dependency to be checked
     * @param env        the environment in which the test runs. The method may modify this
     *                   environment provided the changes are reversed for subsequent tests.
     * @return true if the test can proceed, false if the dependencies cannot be
     * satisfied.
     */

    @Override
    public boolean ensureDependencySatisfied(XdmNode dependency, Environment env) {
        String type = dependency.getNodeName().getLocalName();
        String value = dependency.attribute("value");
        boolean inverse = "false".equals(dependency.attribute("satisfied"));
        boolean needed = !"false".equals(dependency.attribute("satisfied"));

        if (xxCompilerLocation != null) {
            if (type.equals("year_component_values") &&
                    (value.equals("support year above 9999") ||
                             value.equals("support negative year") ||
                             value.equals("support year zero")
                    )) {
                return false; // whether needed or not
            }
            if (unavailableInXX(type, value)) {
                return !needed;
            }
        }

        if ("feature".equals(type)) {
            if ("schema_aware".equals(value)) {
                if (!treeModel.isSchemaAware() && !inverse) {
                    return false; // cannot use the selected tree model for schema-aware tests
                }
                if (xxCompilerLocation != null) {
                    return false; // cannot use the XX compiler for schema-aware tests
                }
//                if (env != null && env.processor.isSchemaAware() != !inverse) {
//                    // DELETED Don't attempt to run non-SA tests with an SA processor; the presence of constructs like
//                    // DELETED import schema will switch on schema-awareness.
//                    return false;
//                }
                if (inverse) {
                    // Saxon-EE cannot be forced to reject an xsl:import-schema instruction
                    return false;
                }
                if (env != null) {
                    env.resetActions.add(new Environment.ResetAction() {
                        @Override
                        public void reset(Environment env) {
                            env.xsltCompiler.setSchemaAware(false);
                        }
                    });
                    env.xsltCompiler.setSchemaAware(true);
                    return true;
                } else {
                    return false;
                }
            } else if ("streaming".equals(value)) {
                if (!"EE".equals(env.processor.getSaxonEdition())) {
                    return inverse;
                }
                final String oldStreamability = env.processor.getConfigurationProperty(Feature.STREAMABILITY);
                final boolean oldFallback = env.processor.getConfigurationProperty(Feature.STREAMING_FALLBACK);
                env.resetActions.add(new Environment.ResetAction() {
                    @Override
                    public void reset(Environment env) {
                        env.processor.setConfigurationProperty(Feature.STREAMABILITY, oldStreamability);
                        env.processor.setConfigurationProperty(Feature.STREAMING_FALLBACK, oldFallback);
                    }
                });
                if (inverse) {
                    env.processor.setConfigurationProperty(Feature.STREAMABILITY, "off");
                    env.processor.setConfigurationProperty(Feature.STREAMING_FALLBACK, false);
                } else {
                    switch (streaming) {
                        case 0: // -streaming:off
                            env.processor.setConfigurationProperty(Feature.STREAMABILITY, "off");
                            env.processor.setConfigurationProperty(Feature.STREAMING_FALLBACK, true);
                            break;
                        case 1: // -streaming:std
                            env.processor.setConfigurationProperty(Feature.STREAMABILITY, "standard");
                            env.processor.setConfigurationProperty(Feature.STREAMING_FALLBACK, false);
                            break;
                        case 2:
                            env.processor.setConfigurationProperty(Feature.STREAMABILITY, "extended");
                            env.processor.setConfigurationProperty(Feature.STREAMING_FALLBACK, false);
                            break;
                        case 3:
                            env.processor.setConfigurationProperty(Feature.STREAMABILITY, "standard");
                            env.processor.setConfigurationProperty(Feature.STRICT_STREAMABILITY, true);
                            env.processor.setConfigurationProperty(Feature.STREAMING_FALLBACK, false);
                            break;
                    }
                }
                return true;

            } else if ("XSD_1.1".equals(value)) {
                String requiredVersion = inverse ? "1.0" : "1.1";
                final String oldVersion = env.processor.getConfigurationProperty(Feature.XSD_VERSION);
                env.resetActions.add(new Environment.ResetAction() {
                    @Override
                    public void reset(Environment env) {
                        env.processor.setConfigurationProperty(Feature.XSD_VERSION, oldVersion);
                    }
                });
                env.processor.setConfigurationProperty(Feature.XSD_VERSION, requiredVersion);
                return true;


            }
        } else if ("sweep_and_posture".equals(type)) {
            if ("supports-sweep-and-posture-assessments".equals(value)) {
                return !inverse;
            }
            return !inverse;
        } else if ("xquery_invocation".equals(type)) {
            return !inverse;
        }
        return super.ensureDependencySatisfied(dependency, env);
    }

    /**
     * Run streamability tests
     */

    @Override
    public void runStreamabilityTests(XPathCompiler xpc, XdmNode testCase) {
        try {
            String //contextPostureStr = xpc.evaluateSingle("test/posture-and-sweep/@context-posture", testCase).getStringValue();
                contextPostureStr = testCase.select(
                    path("test", "posture-and-sweep", "@context-posture")).asString();
            Posture contextPosture = Posture.valueOf(contextPostureStr.toUpperCase());
            String //resultPostureStr = xpc.evaluateSingle("result/assert-posture-and-sweep/@result-posture", testCase).getStringValue();
                    resultPostureStr = testCase.select(path("result", "assert-posture-and-sweep", "@result-posture")).asString();
            Posture resultPosture = Posture.valueOf(resultPostureStr.toUpperCase());
            String //resultSweepStr = xpc.evaluateSingle("result/assert-posture-and-sweep/@result-sweep", testCase).getStringValue();
                    resultSweepStr = testCase.select(path("result", "assert-posture-and-sweep", "@result-sweep")).asString();
            Sweep resultSweep = Sweep.valueOf(resultSweepStr.toUpperCase().replace('-', '_'));
            ContextItemStaticInfoEE info = new ContextItemStaticInfoEE(AnyItemType.getInstance(), true, contextPosture);
            XPathSelector tests = xpc.compile("test/posture-and-sweep/xpath").load();
            tests.setContextItem(testCase);
            Processor testProcessor = new Processor(true);
            XPathCompiler testCompiler = testProcessor.newXPathCompiler();
            testCompiler.declareNamespace("ex", "http://www.example.com");
            List<String> reasons = new ArrayList<String>();
            QName partName = new QName("part");
            for (XdmItem test : tests) {
                try {
                    String part = ((XdmNode)test).getAttributeValue(partName);
                    String testExpression = test.getStringValue();
                    XPathExecutable xExec = testCompiler.compile(testExpression);
                    Expression exp = xExec.getUnderlyingExpression().getInternalExpression();
                    PostureAndSweep ps = Streamability.getStreamability(exp, info, reasons);
                    if (ps.getPosture().equals(resultPosture) && ps.getSweep().equals(resultSweep)) {
                        resultsDoc.writeTestcaseElement(testCase.attribute("name"), part, "pass", "");
                        successes++;
                    } else {
//                        if (Literal.isEmptySequence(exp)) {
//                            // no action: test bug 27075
//                            // TODO: remove this when tests are fixed
//                        } else if (testExpression.contains("namespace")) {
//                            // no action: test bug 27078
//                            // TODO: remove this when tests are fixed
//                        } else {
                        resultsDoc.writeTestcaseElement(testCase.attribute("name"), part, "fail",
                                testExpression + " is " + ps.toString());
                        failures++;
//                        }
                    }
                } catch (SaxonApiException e) {
                    if (e.getMessage().equals("item() is not allowed in a path expression")) {
                        // TODO: remove this when tests are fixed
                        // no action: test bug 27074
                    } else {
                        resultsDoc.writeTestcaseElement(testCase.attribute("name"), "fail", e.getMessage());
                        failures++;
                    }
                }
            }

        } catch (SaxonApiException err) {
            resultsDoc.writeTestcaseElement(testCase.attribute("name"), "crashed", err.toString());
            err.printStackTrace();
        }
    }


}
