////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2022 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package com.saxonica.testdriver.ee;

import com.saxonica.config.EnterpriseConfiguration;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.Version;
import net.sf.saxon.event.Builder;
import net.sf.saxon.lib.*;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.sxpath.XPathDynamicContext;
import net.sf.saxon.sxpath.XPathEvaluator;
import net.sf.saxon.sxpath.XPathExpression;
import net.sf.saxon.testdriver.CanonicalXML;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.tiny.TinyBuilder;

import net.sf.saxon.type.Type;
import net.sf.saxon.value.AnyURIValue;
import net.sf.saxon.value.DateTimeValue;
import org.w3c.tidy.Tidy;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

//import com.saxonica.testdriver.gui.TestDriverForm;

/**
 * This class runs the W3C XQuery Update Test Suite, driven from the test catalog. It includes options to
 * run the tests interpretively or by compilation (not yet supported).
 */
public class UpdateTestSuiteDriver {
    /**
     * Run the testsuite using Saxon.
     *
     * @param args Array of parameters passed to the application
     *             via the command line.
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("-?")) {
            System.err.println(
                    "UpdateTestSuiteDriver testsuiteDir saxonDir [testNamePattern] [-compile:off] [-runcomp] [-w] [-onwards] [-unfold] [-pull]");
        }

        System.err.println("Testing Saxon " + Version.getProductVersion());
        new UpdateTestSuiteDriver().go(args);
    }

    protected int notrun = 0;
    private String testSuiteDir;
    private String saxonDir;
    private EnterpriseConfiguration saConfig;
    private XMLReader resultParser;
    private XMLReader fragmentParser;
    private Pattern testPattern = null;
    private boolean showWarnings = false;
    private boolean compile = true;
    private boolean onwards = false;
    private boolean unfolded = false;
    //private boolean runCompiled = false;
    private final HashMap<String, NodeInfo> sourceIndex = new HashMap<String, NodeInfo>(50);
    private final HashMap uriMap = new HashMap(50);

    private final TransformerFactory tfactory = new TransformerFactoryImpl();

    private Writer results;
    private final Writer compileScript = null;
    private final PrintStream monitor = System.err;
    private Logger log;
    private Object guiForm;
    private int successes = 0;
    private int failures = 0;
    private int wrongErrorResults = 0;
    private final List<String> failingTests = new ArrayList<>();

    /**
     * Some tests use schemas that conflict with others, so they can't use the common schema cache.
     * These tests are run in a Configuration of their own. (Ideally we would put this list in a
     * catalogue file of some kind).
     */

    static HashSet noCacheTests = new HashSet(30);

    static {
        noCacheTests.add("schemainline20_005_01");

    }

    public final static String TEST_NS = "http://www.w3.org/2005/02/query-test-update";

    private NameTest elementNameTest(NamePool pool, String local) {
        int nameFP = pool.allocateFingerprint(TEST_NS, local) & NamePool.FP_MASK;
        return new NameTest(Type.ELEMENT, nameFP, pool);
    }

    private NodeInfo getChildElement(NodeInfo parent, NameTest child) {
        return parent.iterateAxis(AxisInfo.CHILD, child).next();
    }

//    public void setTestDriverForm(TestDriverForm gui){
//        guiForm =gui;
//    }

    public void println(String data) {
        if (guiForm != null) {
            //guiForm.setResultTextArea(data);
        } else {
            monitor.println(data);
        }
    }

    public void printError(String error, Exception e) {
        if (guiForm != null) {
            //guiForm.errorPopup(error);
            e.printStackTrace();
        } else {
            e.printStackTrace();
        }
    }

    public void go(String[] args) throws SAXException, ParserConfigurationException {


        testSuiteDir = args[0];
        saxonDir = args[1];
        HashSet<String> exceptions = new HashSet<String>();

        for (int i = 2; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if (args[i].equals("-w")) {
                    showWarnings = true;
                } else if (args[i].equals("-compile:off")) {
                    compile = false;
                } else if (args[i].equals("-onwards")) {
                    onwards = true;
                } else if (args[i].equals("-unfold")) {
                    unfolded = true;
                }
            } else {
                if (args[i].length() > 0) {
                    testPattern = Pattern.compile(args[i]);
                }
            }
        }


        try {
//            parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
//            if (!parser.getFeature("http://xml.org/sax/features/xml-1.1")) {
//                System.err.println("Warning: XML parser does not support XML 1.1 - " + parser.getClass());
//            };
            NamePool pool = new NamePool();
            saConfig = new EnterpriseConfiguration();
            //AutoActivate.activate(saConfig);
            saConfig.setNamePool(pool);
            //saConfig.setHostLanguage(Configuration.XQUERY);
            saConfig.setTreeModel(Builder.LINKED_TREE);
            saConfig.setBooleanProperty(Feature.XQUERY_SCHEMA_AWARE, true);
            saConfig.setBooleanProperty(Feature.GENERATE_BYTE_CODE, compile);

            XMLReader parser = saConfig.getSourceParser();

            boolean supports11 = false;
            try {
                supports11 = parser.getFeature("http://xml.org/sax/features/xml-1.1");
            } catch (Exception err) {
            }

            if (!supports11) {
                println("Warning: XML parser does not support XML 1.1 - " + parser.getClass());
            }
            resultParser = saConfig.getSourceParser();
            resultParser.setEntityResolver(
                    new EntityResolver() {
                        @Override
                        public InputSource resolveEntity(String publicId, String systemId) {
                            return new InputSource(new StringReader(""));
                        }
                    }
            );
            fragmentParser = saConfig.getSourceParser();
            fragmentParser.setEntityResolver(new StandardEntityResolver(saConfig));

            //Configuration config11 = new Configuration();
            //config11.setXMLVersion(Configuration.XML11);
            //config11.setNamePool(pool);

            results = new FileWriter(
                    new File(saxonDir + "/results" + Version.getProductVersion() + ".xml"));
            log = new StandardLogger(
                    new File(saxonDir + "/results" + Version.getProductVersion() + ".log"));
            

            NameTest testCaseNT = elementNameTest(pool, "test-case");
            NameTest inputUriNT = elementNameTest(pool, "input-URI");
            NameTest inputFileNT = elementNameTest(pool, "input-file");
            NameTest queryNT = elementNameTest(pool, "query");
            NameTest stateNT = elementNameTest(pool, "state");
            NameTest inputQueryNT = elementNameTest(pool, "input-query");
            NameTest contextItemNT = elementNameTest(pool, "contextItem");
            NameTest outputFileNT = elementNameTest(pool, "output-file");
            NameTest sourceNT = elementNameTest(pool, "source");
            NameTest schemaNT = elementNameTest(pool, "schema");
            NameTest expectedErrorNT = elementNameTest(pool, "expected-error");
            NameTest collectionNT = elementNameTest(pool, "collection");
            NameTest defaultCollectionNT = elementNameTest(pool, "defaultCollection");
            NameTest optimizationNT = elementNameTest(pool, "optimization");


            /**
             * Look for an exceptions.xml document with the general format:
             *
             * <exceptions>
             *   <exception>
             *     <tests>testname1 testname2 ...</tests>
             *     <decription>text explanation</description>
             *   </exception>
             * </exceptions>
             *
             * Tests listed in this file will not be run.
             */

            TreeInfo exceptionsDoc = saConfig.buildDocumentTree(
                    new StreamSource(new File(saxonDir + "/exceptions.xml"))
            );

            NameTest exceptionTestsNT = new NameTest(Type.ELEMENT, pool.allocateFingerprint("", "exception"), pool);
            AxisIterator exceptionTestCases = exceptionsDoc.getRootNode().iterateAxis(AxisInfo.DESCENDANT, exceptionTestsNT);
            while (true) {
                NodeInfo exceptionCase = exceptionTestCases.next();
                if (exceptionCase == null) {
                    break;
                }
                String testCase = exceptionCase.getAttributeValue("", "test-case");
                StringTokenizer tok = new StringTokenizer(testCase);
                while (tok.hasMoreTokens()) {
                    exceptions.add(tok.nextToken());
                }
            }


            TreeInfo catalog = saConfig.buildDocumentTree(
                    new StreamSource(new File(testSuiteDir + "/XQUTSCatalog.xml"))
            );


            Properties outputProps = new Properties();
            outputProps.setProperty("method", "xml");
            outputProps.setProperty("indent", "no");
            outputProps.setProperty("omit-xml-declaration", "yes");
            results.write("<test-suite-result xmlns='http://www.w3.org/2005/02/query-test-XQTSResult'>\n");
            results.write(" <implementation name='Saxon-EE' version='" + Version.getProductVersion() +
                    "' anonymous-result-column='false'>\n");
            results.write("  <organization name='Saxonica' website='http://www.saxonica.com/' anonymous='false'/>\n");
            results.write("  <submittor name='Michael Kay' title='Director' email='mike@saxonica.com'/>\n");
            results.write("  <description><p>Saxon-EE implements XQuery Update from release 9.1 onwards</p></description>\n");
            outputDiscretionaryItems();
            results.write(" </implementation>\n");
            results.write(" <syntax>XQuery</syntax>");
            results.write(" <test-run dateRun='" + currentDate() + "'>\n");
            results.write("   <test-suite version='CVS'/>\n");
            results.write("   <transformation><p>Standard</p></transformation>\n");
            results.write("   <comparison><p>Standard</p></comparison>\n");
            results.write("   <otherComments><p>None</p></otherComments>\n");
            results.write(" </test-run>\n");

            /**
             * Load all the schemas
             */

            AxisIterator schemas = catalog.getRootNode().iterateAxis(AxisInfo.DESCENDANT, schemaNT);
            while (true) {
                NodeInfo schemaElement = schemas.next();
                if (schemaElement == null) {
                    break;
                }
                String fileName = schemaElement.getAttributeValue("", "FileName");
                println("Loading schema " + fileName);
                File schemaFile = new File(testSuiteDir + "/" + fileName);
                StreamSource ss = new StreamSource(schemaFile);
                saConfig.addSchemaSource(ss);


            }

            /**
             * Load the source documents
             */


            AxisIterator sources = catalog.getRootNode().iterateAxis(AxisInfo.DESCENDANT, sourceNT);
            while (true) {
                NodeInfo sourceElement = sources.next();
                if (sourceElement == null) {
                    break;
                }
                String id = sourceElement.getAttributeValue("", "ID");
                sourceIndex.put(id, sourceElement);

            }


            String exclusionTest = "exists(../GroupInfo/depends-on/feature[@supported='false' and starts-with(., 'revalidation')])";
            XPathEvaluator xe = new XPathEvaluator(saConfig);
            xe.getStaticContext().setDefaultElementNamespace(TEST_NS);
            XPathExpression exclusionTestExp = xe.createExpression(exclusionTest);
            XPathDynamicContext dyn = exclusionTestExp.createDynamicContext();

            AxisIterator testCases = catalog.getRootNode().iterateAxis(AxisInfo.DESCENDANT, testCaseNT);
            while (true) {
                NodeInfo testCase = testCases.next();
                if (testCase == null) {
                    break;
                }

                String testName = testCase.getAttributeValue("", "name");

                dyn.setContextItem(testCase);


                boolean optimizationOK = true;
                if (testPattern != null && !testPattern.matcher(testName).matches()) {
                    continue;
                }
                if (onwards) {
                    testPattern = null;
                }
                if (exceptions.contains(testName)) {
                    println("Test " + testName + " not run (in exceptions list)");
                    log.info("Test " + testName + " not run (in exceptions list)");
                    continue;
                }
                if (isExcluded(testName)) {
                    println("Test " + testName + " not run (excluded)");
                    log.info("Test " + testName + " not run (excluded)");
                    continue;
                }
                boolean excluded = exclusionTestExp.effectiveBooleanValue(dyn);
                if (excluded) {
                    println("Test " + testName + " not run (not applicable)");
                    log.info("Test " + testName + " not run (not applicable)");
                    continue;
                }


                println("Test " + testName);
                log.info("Test " + testName);

                String filePath = testCase.getAttributeValue("", "FilePath");
//                if (filePath.startsWith("StaticTyping")) {
//                    continue;
//                }
                //NodeInfo testInput = getChildElement(testCase, inputFileNT);

                File putOutput = new File(testSuiteDir + "/TestSources/putOutput.xml");
                if (putOutput.exists() && !putOutput.delete()) {
                    System.err.println("Failed to delete file " + putOutput.getAbsolutePath());
                }
                File putOutput2 = new File(testSuiteDir + "/TestSources/putOutput2.xml");
                if (putOutput2.exists() && !putOutput2.delete()) {
                    System.err.println("Failed to delete file " + putOutput2.getAbsolutePath());
                }

                AxisIterator states = testCase.iterateAxis(AxisInfo.CHILD, stateNT);
                HashMap updatedDocuments = new HashMap();

                int numberOfStates = 0;
                NodeInfo state;

                while ((state = states.next()) != null) {
                    numberOfStates++;
                }

                states = testCase.iterateAxis(AxisInfo.CHILD, stateNT);
                int stateNumber = 0;
                while ((state = states.next()) != null) {
                    stateNumber++;
                    NodeInfo query = getChildElement(state, queryNT);
                    String queryName = query.getAttributeValue("", "name");

                    String absQueryName = testSuiteDir + "/Queries/" +
                            (unfolded ? "XQUnfolded/" : "XQuery/") +
                            filePath + queryName + ".xq";

                    String outputFile = null;

                    StaticQueryContext env = saConfig.newStaticQueryContext();
                    env.setUpdatingEnabled(true);
                    env.setModuleURIResolver(new XQTSModuleURIResolver(testCase));
                    env.setBaseURI(new File(absQueryName).toURI().toString());
                    env.setErrorReporter(new StandardErrorReporter());  // TODO: use custom error reporter
                    XQueryExpression xqe;

                    try {
                        xqe = env.compileQuery(new FileInputStream(absQueryName), "UTF-8");
                    } catch (XPathException err) {
                        processError(err, state, testName, expectedErrorNT, stateNumber == numberOfStates);
                        continue;
                    }

                    NodeInfo optElement = getChildElement(state, optimizationNT);
                    if (optElement != null) {
                        String explain = optElement.getAttributeValue("", "explain");
                        if ("true".equals(explain) || "1".equals(explain)) {
                            ExpressionPresenter presenter = new ExpressionPresenter(saConfig);
                            xqe.explain(presenter);
                            presenter.close();
                        }
                        String assertion = optElement.getAttributeValue("", "assert");
                        if (assertion != null) {
                            TinyBuilder builder = new TinyBuilder(saConfig.makePipelineConfiguration());
                            builder.setStatistics(saConfig.getTreeStatistics().TEMPORARY_TREE_STATISTICS);
                            ExpressionPresenter presenter = new ExpressionPresenter(saConfig, builder);
                            xqe.explain(presenter);
                            presenter.close();
                            NodeInfo expressionTree = builder.getCurrentRoot();
                            XPathEvaluator xpe = new XPathEvaluator(saConfig);
                            XPathExpression exp = xpe.createExpression(assertion);
                            XPathDynamicContext c = exp.createDynamicContext(expressionTree);
                            boolean bv = exp.effectiveBooleanValue(c);
                            if (!bv) {
                                log.info("** Optimization assertion failed");
                                optimizationOK = false;
                            }
                        }
                    }

                    DynamicQueryContext dqc = new DynamicQueryContext(saConfig);

                    NodeInfo contextItemElement = getChildElement(state, contextItemNT);
                    if (contextItemElement != null) {
                        String docName = contextItemElement.getStringValue();
                        if (!docName.startsWith("putOutput")) {
                            TreeInfo contextNode = loadDocument(docName);
                            dqc.setContextItem(contextNode.getRootNode());
                        }
                    }

                    processInputQueries(state, inputQueryNT, filePath, dqc);

                    processInputDocuments(state, inputFileNT, dqc, updatedDocuments);

                    setQueryParameters(catalog, state, dqc, inputUriNT, collectionNT);

                    NodeInfo defaultCollection = getChildElement(state, defaultCollectionNT);
                    if (defaultCollection != null) {
                        String docName = defaultCollection.getStringValue();
                        NodeInfo collectionElement = getCollectionElement(catalog, docName, collectionNT);
                        CollectionFinder r =
                                new XQTSCollectionFinder(catalog, collectionElement, true);
                        saConfig.setCollectionFinder(r);
                    }

                    // Allocate a filename for the Saxon results. Use the name of the test, with a suffix
                    // based on the suffix used for the expected results file

                    String suffix = null;
                    SequenceIterator expectedResults = state.iterateAxis(AxisInfo.CHILD, outputFileNT);
                    NodeInfo outputFileElement = (NodeInfo) expectedResults.next();
                    if (outputFileElement != null) {
                        String outputFileName = outputFileElement.getStringValue();
                        int lastDot = outputFileName.lastIndexOf('.');
                        suffix = outputFileName.substring(lastDot);
                    }
                    if (suffix == null) {
                        suffix = ".out";
                    }
                    String localOutputFile = testName + suffix;

                    // Run the query

                    String outputDir = saxonDir + "/results/" + filePath;
                    if (outputDir.endsWith("/")) {
                        outputDir = outputDir.substring(0, outputDir.length() - 1);
                    }
                    new File(outputDir).mkdirs();
                    outputFile = outputDir + '/' + localOutputFile;
                    File outputFileF = new File(outputFile);
                    outputFileF.createNewFile();
                    StreamResult result = new StreamResult(outputFileF);
                    Set<MutableNodeInfo> affected;
                    try {
                        if (xqe.isUpdateQuery()) {
                            affected = xqe.runUpdate(dqc);
                        } else {
                            xqe.run(dqc, result, outputProps);
                            affected = Collections.EMPTY_SET;
                        }
                    } catch (XPathException err) {
                        processError(err, state, testName, expectedErrorNT, stateNumber == numberOfStates);
                        continue;
                    }
                    for (MutableNodeInfo affectedDoc : affected) {
                        String uri = affectedDoc.getSystemId();
                        if (uri != null) {
                            updatedDocuments.put(uri, affectedDoc);
                        }
                    }

                    // Compare the results

                    boolean resultsMatched = false;
                    String possibleMatch = null;
                    expectedResults = state.iterateAxis(AxisInfo.CHILD, outputFileNT);
                    boolean multipleResults = false;
                    SequenceIterator ccc = state.iterateAxis(AxisInfo.CHILD, outputFileNT);
                    ccc.next();
                    if (ccc.next() != null) {
                        multipleResults = true;
                    }
                    boolean foundExpectedResults = false;
                    while (true) {
                        outputFileElement = (NodeInfo) expectedResults.next();
                        if (outputFileElement == null) {
                            break;
                        }
                        foundExpectedResults = true;
                        String resultsDir = testSuiteDir + "/ExpectedTestResults/" + filePath;
                        String resultsPath = resultsDir + outputFileElement.getUnicodeStringValue();
                        String comparisonMethod = outputFileElement.getAttributeValue("", "compare");
                        String comparisonResult;
                        if (comparisonMethod.equals("Ignore")) {
                            comparisonResult = "true";
                        } else {
                            comparisonResult = compare(outputFile, resultsPath, comparisonMethod, multipleResults);
                        }
                        if (comparisonResult.equals("true")) {
                            // exact match
                            resultsMatched = true;
                            break;
                        } else if (comparisonResult.equals("false")) {
                            //continue;
                        } else {
                            possibleMatch = comparisonResult;
                            //continue;
                        }
                    }

                    if (stateNumber == numberOfStates) {
                        if (resultsMatched) {
                            successes++;
                            results.write("  <test-case name='" + testName + "' result='pass'" +
                                    (optimizationOK ? "" : " comment='check optimization'") +
                                    "/>\n");
                        } else {
                            if (multipleResults) {
                                log.info("*** Failed to match any of the permitted results");
                            }
                            NodeInfo expectedError = getChildElement(state, expectedErrorNT);
                            if (possibleMatch != null) {
                                wrongErrorResults++;
                                successes++;
                                results.write("  <test-case name='" + testName + "' result='pass' comment='" +
                                        possibleMatch + "'/>\n");
                            } else if (expectedError != null) {
                                failures++;
                                results.write("  <test-case name='" + testName + "' result='fail' comment='" +
                                        "expected " + expectedError.getUnicodeStringValue() + ", got success'/>\n");
                                failingTests.add(testName);
                            } else if (foundExpectedResults) {
                                failures++;
                                results.write("  <test-case name='" + testName + "' result='fail'/>\n");
                                failingTests.add(testName);
                            }
                        }

//                        File putOutput = new File(testSuiteDir + "/TestSources/putOutput.xml");
//                        putOutput.delete();
//                        File putOutput2 = new File(testSuiteDir + "/TestSources/putOutput2.xml");
//                        putOutput2.delete();
                    }
                }
            }

            results.write("</test-suite-result>");
            results.close();
            if (guiForm != null) {
                //guiForm.printResults("Results: "+ successes + " passed, " + failures + " failed, "+ wrongErrorResults + " incorrect error code", saxonDir + "/results" + Version.getProductVersion() + ".xml", saxonDir);
            }
            monitor.println("Results: " + successes + " passed, " + failures + " failed, " +
                    wrongErrorResults + " wrong error code");
            monitor.println("Failing tests (max 30):");
            for (int i=0; i<Integer.min(failures, failingTests.size()); i++) {
                monitor.println("  " + failingTests.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String currentDate() {
        return DateTimeValue.getCurrentDateTime(null).getStringValue().substring(0, 10);
    }

    private void setQueryParameters(TreeInfo catalog, NodeInfo testCase, DynamicQueryContext dqc, NameTest inputUriNT, NameTest collectionNT) throws XPathException {
        SequenceIterator inputURIs = testCase.iterateAxis(AxisInfo.CHILD, inputUriNT);
        NodeInfo inputURI;
        while ((inputURI = (NodeInfo) inputURIs.next()) != null) {
            String variableName = inputURI.getAttributeValue("", "variable");
            if (variableName != null) {
                String docName = inputURI.getStringValue();
                if (docName.startsWith("putOutput")) {
                    String uri = new File(testSuiteDir + "/TestSources/" + docName + ".xml").toURI().toString();
                    dqc.setParameter(new StructuredQName("", "", variableName), new AnyURIValue(uri));
                } else if (docName.startsWith("collection")) {
                    NodeInfo collectionElement = getCollectionElement(catalog, docName, collectionNT);
                    CollectionFinder r =
                            new XQTSCollectionFinder(catalog, collectionElement, false);
                    saConfig.setCollectionFinder(r);
                    dqc.setParameter(new StructuredQName("", "", variableName), new AnyURIValue(docName));
                } else {
                    String uri = loadDocument(docName).getRootNode().getSystemId();
                    dqc.setParameter(new StructuredQName("", "", variableName), new AnyURIValue(uri));
                }
            }
        }
    }

    private void processInputDocuments(NodeInfo state, NameTest inputFileNT,
                                       DynamicQueryContext dqc, HashMap updatedDocuments) throws XPathException {
        SequenceIterator inputFiles = state.iterateAxis(AxisInfo.CHILD, inputFileNT);
        while (true) {
            NodeInfo inputFile = (NodeInfo) inputFiles.next();
            if (inputFile == null) {
                break;
            }
            String fileId = inputFile.getStringValue();
            String variableName = inputFile.getAttributeValue("", "variable");
            if (variableName != null) {
                TreeInfo inputDoc = null;
                String uri = (String) uriMap.get(fileId);
                if (uri != null) {
                    inputDoc = (TreeInfo) updatedDocuments.get(uri);
                }
                if (inputDoc == null) {
                    inputDoc = loadDocument(fileId);
                }
                dqc.setParameter(new StructuredQName("", "", variableName),
                                 inputDoc.getRootNode().materialize());
                //System.err.println("Set parameter " + variableName + " := " + inputDoc.getSystemId());
            }
        }
    }

    private void processInputQueries(NodeInfo testCase, NameTest inputQueryNT, String filePath, DynamicQueryContext dqc) throws XPathException, IOException {
        SequenceIterator inputQueries = testCase.iterateAxis(AxisInfo.CHILD, inputQueryNT);
        while (true) {
            NodeInfo inputQuery = (NodeInfo) inputQueries.next();
            if (inputQuery == null) {
                break;
            }
            String variableName = inputQuery.getAttributeValue("", "variable");
            if (variableName != null) {
                String preQueryName = inputQuery.getAttributeValue("", "name");
                String subQueryFile = testSuiteDir + "/Queries/XQuery/" + filePath + preQueryName + ".xq";
                StaticQueryContext sqc2 = saConfig.newStaticQueryContext();
                XQueryExpression subQuery = sqc2.compileQuery(new FileReader(subQueryFile));
                SequenceIterator subQueryResult = subQuery.iterator(new DynamicQueryContext(saConfig));
                dqc.setParameter(new StructuredQName("", "", variableName), SequenceTool.toGroundedValue(subQueryResult));
            }
        }
    }

    private NodeInfo getCollectionElement(TreeInfo catalog, String docName, NameTest collectionNT) {
        NodeInfo collectionElement = null;
        AxisIterator colls = catalog.getRootNode().iterateAxis(AxisInfo.DESCENDANT, collectionNT);
        while (true) {
            NodeInfo coll = colls.next();
            if (coll == null) {
                break;
            }
            if (docName.equals(coll.getAttributeValue("", "ID"))) {
                collectionElement = coll;
            }
        }
        return collectionElement;
    }

//    protected String getResultDirectoryName() {
//        return "SaxonDriver";
//    }

    protected boolean isExcluded(String testName) {
        // tests that require unsupported features
        return testName.startsWith("stf") ||
                testName.equals("fn-put-003") || testName.equals("fn-put-004") ||
                testName.equals("fn-put-005") || testName.equals("fn-put-006");
    }

    private static String toClarkName(String variableName) {
        // Crude handling of QName-valued variables (there aren't many in the catalog!)
        if (variableName == null) {
            return null;
        }
        if (variableName.startsWith("local:")) {
            return "{http://www.w3.org/2005/xquery-local-functions}" + variableName.substring(6);
        } else {
            return variableName;
        }

    }


    /**
     * Construct source object. This method allows subclassing e.g. to build a DOM or XOM source.
     *
     * @param id the short name of the document
     */

    private TreeInfo loadDocument(String id) throws XPathException {
        NodeInfo sourceElement = sourceIndex.get(id);
        String schema = sourceElement.getAttributeValue("", "schema");
        String fileName = sourceElement.getAttributeValue("", "FileName");
        Source ss = new StreamSource(new File(testSuiteDir + "/" + fileName));
        ParseOptions options = new ParseOptions();
        if (schema != null) {
            options.setSchemaValidationMode(Validation.STRICT);
        }
        TreeInfo doc = saConfig.buildDocumentTree(ss, options);
        uriMap.put(id, doc.getRootNode().getSystemId());
        return doc;
    }

    /**
     * Process a static or dynamic error
     */

    private void processError(XPathException err, NodeInfo state, String testName, NameTest expectedErrorNT, boolean reportResult)
            throws IOException {
        String actualError = err.getErrorCodeLocalPart();
        AxisIterator expectedErrors = state.iterateAxis(AxisInfo.CHILD, expectedErrorNT);
        StringBuilder expected = new StringBuilder(20);
        while (true) {
            NodeInfo expectedError = expectedErrors.next();
            if (expectedError == null) {
                break;
            }
            if (expectedError.getStringValue().equals(actualError)) {
                if (reportResult) {
                    successes++;
                    results.write("  <test-case name='" + testName + "' result='pass'/>\n");
                }
                return;
            }
            expected.append(expectedError.getStringValue());
            expected.append(" ");
        }
        if (!reportResult) {
            StringBuilder fsb = new StringBuilder(20);
            fsb.append("-");
            state.generateId(fsb);
            testName += fsb.toString();
        }
        if (expected.length() > 0) {
            successes++;
            wrongErrorResults++;
            results.write("  <test-case name='" + testName + "' result='pass'" +
                    " comment='expected " + expected + ", got " + actualError + "'/>\n");
        } else {
            failures++;
            results.write("  <test-case name='" + testName + "' result='fail'" +
                    " comment='expected success, got " + actualError + "'/>\n");
            failingTests.add(testName);
        }

    }


    static CanonicalXML canon = new CanonicalXML();

    private String compare(String outfile, String reffile, String comparator, boolean silent) {
        if (reffile == null) {
            log.info("*** No reference results available");
            return "false";
        }
        File outfileFile = new File(outfile);
        File reffileFile = new File(reffile);

        if (!reffileFile.exists()) {
            log.info("*** No reference results available");
            return "false";
        }

        // try direct comparison first

        String refResult = null;
        String actResult = null;

        try {
            // This is decoding bytes assuming the platform default encoding
            FileReader reader1 = new FileReader(outfileFile);
            FileReader reader2 = new FileReader(reffileFile);
            char[] contents1 = new char[(int) outfileFile.length()];
            char[] contents2 = new char[(int) reffileFile.length()];
            int size1 = reader1.read(contents1, 0, (int) outfileFile.length());
            int size2 = reader2.read(contents2, 0, (int) reffileFile.length());
            int offset1 = 0;
            int offset2 = 0;
            if (contents1[0] == '\u00ef' && contents1[1] == '\u00bb' && contents1[2] == '\u00bf') {
                offset1 += 3;
            }
            if (contents2[0] == '\u00ef' && contents2[1] == '\u00bb' && contents2[2] == '\u00bf') {
                offset2 += 3;
            }
            actResult = (size1 == -1 ? "" : new String(contents1, offset1, size1 - offset1));
            refResult = (size2 == -1 ? "" : new String(contents2, offset2, size2 - offset2));

            actResult = normalizeNewlines(actResult);
            refResult = normalizeNewlines(refResult);
            if (actResult.equals(refResult)) {
                return "true";
            }
            if (size1 == 0) {
                if (!silent) {
                    log.info("** ACTUAL RESULTS EMPTY; REFERENCE RESULTS LENGTH " + size2);
                }
                return "false";
            }
            if (size2 == 0) {
                if (!silent) {
                    log.info("** REFERENCED RESULTS EMPTY; ACTUAL RESULTS LENGTH " + size2);
                }
                return "false";
            }
        } catch (Exception e) {
        }

        // HTML: can't do logical comparison

        if (comparator.equals("html-output")) {
            // TODO: Tidy gets upset by byte-order-marks. Use the strings constructed above as input.
            try {
                Tidy tidy = new Tidy();
                tidy.setXmlOut(true);
                tidy.setQuiet(true);
                tidy.setShowWarnings(false);
                tidy.setCharEncoding(org.w3c.tidy.Configuration.UTF8);
                InputStream in1 = new FileInputStream(outfile);
                File xml1 = new File(outfile + ".xml");
                xml1.createNewFile();
                OutputStream out1 = new FileOutputStream(xml1);
                tidy.parse(in1, out1);
                InputStream in2 = new FileInputStream(reffile);
                File xml2 = new File(reffile + ".xml");
                xml2.createNewFile();
                OutputStream out2 = new FileOutputStream(xml2);
                tidy.parse(in2, out2);
                return compare(xml1.toString(), xml2.toString(), "xml", silent);
            } catch (IOException e) {
                e.printStackTrace();
                return "false";
            }
        } else if (comparator.equals("xhtml-output")) {
            refResult = canonizeXhtml(refResult);
            actResult = canonizeXhtml(actResult);
            return Boolean.toString((actResult.equals(refResult)));

        } else if (comparator.equals("Fragment") || comparator.equals("Text")) {
            try {
                // try two comparison techniques hoping one will work...
                boolean b = false;
                try {
                    b = compareFragments2(actResult, refResult, outfile, silent);
                } catch (Exception err1) {
                    log.info("XQTS: First comparison attempt failed, trying again");
                }
                if (!b) b = compareFragments(outfileFile, reffileFile, outfile, silent);
                return Boolean.toString(b);
            } catch (Exception err2) {
                log.info("Failed to compare results for: " + outfile);
                err2.printStackTrace();
                return "false";
            }
        } else if (comparator.equals("Inspect")) {
            log.info("** Inspect results by hand");
            return "true";
        } else {
            // convert both files to Canonical XML and compare them again
            try {
                InputSource out = new InputSource(outfileFile.toURI().toString());
                InputSource ref = new InputSource(reffileFile.toURI().toString());
                String outxml = canon.toCanonicalXML2(resultParser, out, false);
                String refxml = canon.toCanonicalXML2(resultParser, ref, false);
//                String outxml = canon.toCanonicalXML3(factory, resultParser, actResult, false);
//                String refxml = canon.toCanonicalXML3(factory, resultParser, refResult, false);
                if (!outxml.equals(refxml)) {
                    // try comparing again, this time without whitespace nodes
                    outxml = canon.toCanonicalXML2(resultParser, out, true);
                    refxml = canon.toCanonicalXML2(resultParser, ref, true);
//                    outxml = canon.toCanonicalXML3(factory, resultParser, actResult, true);
//                    refxml = canon.toCanonicalXML3(factory, resultParser, refResult, true);
                    if (outxml.equals(refxml)) {
                        log.info("*** Match after stripping whitespace nodes: " + outfile);
                        return "*** Match after stripping whitespace nodes";
                    } else {
                        if (!silent) {
                            log.info("Mismatch with reference results: " + outfile);
                            log.info("REFERENCE RESULTS:");
                            log.info(truncate(refxml));
                            log.info("ACTUAL RESULTS:");
                            log.info(truncate(outxml));
                            findDiff(refxml, outxml);
                        }
                        return "false";
                    }
                } else {
                    return "true";
                }
            } catch (Exception err) {
                try {
                    log.info("Failed to compare results for: " + outfile + ": " + err.getMessage());
                    log.info("** Attempting XML Fragment comparison");
                    //boolean b = compareFragments(outfileFile, reffileFile, outfile, silent);
                    boolean b = compareFragments2(actResult, refResult, outfile, silent);
                    log.info("** " + (b ? "Success" : "Still different"));
                    return Boolean.toString(b);
                } catch (Exception err2) {
                    log.info("Again failed to compare results for: " + outfile);
                    err2.printStackTrace();
                }
                return "false";
            }
        }
    }

    Templates xhtmlCanonizer;

    private String canonizeXhtml(String input) {
        try {
            Templates canonizer = getXhtmlCanonizer();
            Transformer t = canonizer.newTransformer();
            StringWriter sw = new StringWriter();
            StreamResult r = new StreamResult(sw);
            InputSource is = new InputSource(new StringReader(input));
            SAXSource ss = new SAXSource(resultParser, is);
            t.transform(ss, r);
            return sw.toString();
        } catch (TransformerConfigurationException err) {
            log.info("*** Failed to compile XHTML canonicalizer stylesheet");
        } catch (TransformerException err) {
            log.info("*** Failed while running XHTML canonicalizer stylesheet");
        }
        return "";
    }

    private Templates getXhtmlCanonizer() throws TransformerConfigurationException {
        if (xhtmlCanonizer == null) {
            Source source = new StreamSource(new File(saxonDir + "/canonizeXhtml.xsl"));
            xhtmlCanonizer = tfactory.newTemplates(source);
        }
        return xhtmlCanonizer;
    }

    private boolean compareFragments(File outfileFile, File reffileFile, String outfile, boolean silent) {
        // if we can't parse the output as a document, try it as an external entity, with space stripping
        String outurl = outfileFile.toURI().toString();
        String refurl = reffileFile.toURI().toString();
        String outdoc = "<?xml version='1.1'?><!DOCTYPE doc [ <!ENTITY e SYSTEM '" + outurl + "'>]><doc>&e;</doc>";
        String refdoc = "<?xml version='1.1'?><!DOCTYPE doc [ <!ENTITY e SYSTEM '" + refurl + "'>]><doc>&e;</doc>";
        InputSource out2 = new InputSource(new StringReader(outdoc));
        InputSource ref2 = new InputSource(new StringReader(refdoc));
        String outxml2 = canon.toCanonicalXML(fragmentParser, out2, true);
        String refxml2 = canon.toCanonicalXML(fragmentParser, ref2, true);
        if (outxml2 != null && refxml2 != null && !outxml2.equals(refxml2)) {
            if (!silent) {
                log.info("Mismatch with reference results: " + outfile);
                log.info("REFERENCE RESULTS:");
                log.info(truncate(refxml2));
                log.info("ACTUAL RESULTS:");
                log.info(truncate(outxml2));
                findDiff(refxml2, outxml2);
            }
            return false;
        } else if (outxml2 == null) {
            log.info("Cannot canonicalize actual results");
            return false;
        } else if (refxml2 == null) {
            log.info("Cannot canonicalize reference results");
            return false;
        }
        return true;
    }

    /**
     * With this method of fragment comparison we build the wrapper document ourselves. This is
     * mainly to circumvent a Java XML parsing bug
     *
     * @param outFragment
     * @param refFragment
     * @param outfile
     * @param silent
     * @return
     */

    private boolean compareFragments2(String outFragment, String refFragment, String outfile, boolean silent) {
        if (outFragment == null) {
            outFragment = "";
        }
        if (outFragment.startsWith("<?xml")) {
            int x = outFragment.indexOf("?>");
            outFragment = outFragment.substring(x + 2);
        }
        if (refFragment == null) {
            refFragment = "";
        }
        if (refFragment.startsWith("<?xml")) {
            int x = refFragment.indexOf("?>");
            refFragment = refFragment.substring(x + 2);
        }

        String outdoc = "<?xml version='1.1'?><doc>" + outFragment.trim() + "</doc>";
        String refdoc = "<?xml version='1.1'?><doc>" + refFragment.trim() + "</doc>";
        InputSource out2 = new InputSource(new StringReader(outdoc));
        InputSource ref2 = new InputSource(new StringReader(refdoc));
        String outxml2 = canon.toCanonicalXML(fragmentParser, out2, true);
        String refxml2 = canon.toCanonicalXML(fragmentParser, ref2, true);
        if (outxml2 != null && refxml2 != null && !outxml2.equals(refxml2)) {
            if (!silent) {
                log.info("Mismatch with reference results: " + outfile);
                log.info("REFERENCE RESULTS:");
                log.info(truncate(refxml2));
                log.info("ACTUAL RESULTS:");
                log.info(truncate(outxml2));
                findDiff(refxml2, outxml2);
            }
            return false;
        } else if (outxml2 == null) {
            log.info("Cannot canonicalize actual results");
            return false;
        } else if (refxml2 == null) {
            log.info("Cannot canonicalize reference results");
            return false;
        }
        return true;
    }


    private static String truncate(String s) {
        if (s.length() > 200) return s.substring(0, 200);
        return s;
    }

    private void findDiff(String s1, String s2) {
        StringBuilder sb1 = new StringBuilder(s1.length());
        sb1.append(s1);
        StringBuilder sb2 = new StringBuilder(s2.length());
        sb2.append(s2);
        int i = 0;
        while (true) {
            if (s1.charAt(i) != s2.charAt(i)) {
                int j = (i < 50 ? 0 : i - 50);
                int k = (i + 50 > s1.length() || i + 50 > s2.length() ? i + 1 : i + 50);
                log.info("Different at char " + i + "\n+" + s1.substring(j, k) +
                        "\n+" + s2.substring(j, k));
                break;
            }
            if (i >= s1.length()) break;
            if (i >= s2.length()) break;
            i++;
        }
    }

    private void outputDiscretionaryItems() throws IOException {
        results.write("  <implementation-defined-items>\n");
        results.write("    <implementation-defined-item name='supportedRevalidationModes' value='strict lax skip'/>\n");
        results.write("    <implementation-defined-item name='defaultRevalidationMode' value='skip'/>\n");
        results.write("    <implementation-defined-item name='externalFunctionXDMPUL' value='external functions cannot return a PUL'/>\n");
        results.write("    <implementation-defined-item name='fnPutSemantics' value='supported for file URIs; node kinds element and document only'/>\n");
        results.write("  </implementation-defined-items>\n");
        results.write("  <features>\n");
        results.write("    <feature name='Minimal Conformance' supported='true'/>\n");
        results.write("    <feature name='Update Facility Static Typing Feature' supported='false'/>\n");
        results.write("  </features>\n");
        results.write("  <context-properties>\n");
        results.write("    <context-property  context-type=\"static\" name=\"Revalidation mode\" value='defaulted'/>\n");
        results.write("  </context-properties>\n");

    }


    private class MyErrorListener extends StandardErrorListener {

        public String errorCode;

        public MyErrorListener(Logger log) {
            setLogger(log);
        }

        /**
         * Receive notification of a recoverable error.
         */

        @Override
        public void error(TransformerException exception) {
            if (exception instanceof XPathException) {
                String code = ((XPathException) exception).getErrorCodeLocalPart();
                if (code != null) {
                    errorCode = code;
                }
                if ("FODC0005".equals(errorCode) || "FODC0002".equals(errorCode)) {
                    fatalError(exception);
                }
            }
            super.error(exception);
        }

        /**
         * Receive notification of a non-recoverable error.
         */

        @Override
        public void fatalError(TransformerException exception) {
            if (exception instanceof XPathException) {
                String code = ((XPathException) exception).getErrorCodeLocalPart();
                if (code != null) {
                    errorCode = code;
                }
            }
            super.fatalError(exception);
        }

        /**
         * Receive notification of a warning.
         */

        @Override
        public void warning(TransformerException exception) {
            if (showWarnings) {
                super.warning(exception);
            }
        }

        /**
         * Make a clean copy of this ErrorListener. This is necessary because the
         * standard error listener is stateful (it remembers how many errors there have been)
         */

        public StandardErrorListener makeAnother(int hostLanguage) {
            return new UpdateTestSuiteDriver.MyErrorListener(log);
        }

    }

    private String normalizeNewlines(String in) {
        int cr = in.indexOf('\r');
        if (cr >= 0 && in.indexOf(cr + 1) == '\n') {
            return in.substring(0, cr) + normalizeNewlines(in.substring(cr + 1));
        } else {
            return in;
        }
    }


}

