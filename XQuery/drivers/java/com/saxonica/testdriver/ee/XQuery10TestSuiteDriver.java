package com.saxonica.testdriver.ee;


import com.saxonica.config.EnterpriseConfiguration;
import net.sf.saxon.Configuration;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.Version;
import net.sf.saxon.lib.*;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.pattern.NodeKindTest;
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
import net.sf.saxon.type.SchemaException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AnyURIValue;
import net.sf.saxon.value.DateTimeValue;
import net.sf.saxon.value.StringValue;
import org.w3c.tidy.Tidy;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * This class runs the W3C XQuery Test Suite, driven from the test catalog. It includes options to
 * run the tests intepretively or by compilation.
 */
public class XQuery10TestSuiteDriver {
    /**
     * Run the testsuite using Saxon.
     *
     * @param args Array of parameters passed to the application
     *             via the command line.
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("-?")) {
            System.err.println(
                    "XQueryTestSuiteDriver testsuiteDir saxonDir [testNamePattern] [-cat:catalogfile] [-compile] [-runcomp] [-debugcomp] [-w] [-onwards] [-unfold] [-nosrc] [-pull] [-indent:yes|no] [-spec:1.0|3.0]");
        }

        System.err.println("Testing Saxon " + Version.getProductVersion());
        new XQuery10TestSuiteDriver().go(args);
    }

    private String testSuiteDir;
    private String saxonDir;
    private EnterpriseConfiguration eeConfig;
    private XMLReader resultParser;
    private XMLReader fragmentParser;
    private Pattern testPattern = null;
    private boolean showWarnings = false;
    private boolean compile = false;
    private boolean onwards = false;
    private boolean unfolded = false;
    private boolean generateByteCode = false;
    private boolean debugBytecode = false;
    private boolean noSource = false;
    private final HashMap documentCache = new HashMap(50);
    private String catalogfile = "XQTSCatalog.xml";

    private final TransformerFactory tfactory = new TransformerFactoryImpl();

    private XMLStreamWriter results;
    private final Writer compileScript = null;
    private final PrintStream monitor = System.err;
    private Logger log;
    private String indent = "yes";
    private String specVersion = "3.0";
    private String catalogVersion;

    /**
     * Some tests use schemas that conflict with others, so they can't use the common schema cache.
     * These tests are run in a Configuration of their own. (Ideally we would put this list in a
     * catalogue file of some kind).
     */

    static HashSet noCacheTests = new HashSet(30);

    static {
        noCacheTests.add("schemainline20_005_01");

    }

    private NameTest elementNameTest(NamePool pool, String local) {
        int nameFP = pool.allocateFingerprint("http://www.w3.org/2005/02/query-test-XQTSCatalog", local) & NamePool.FP_MASK;
        return new NameTest(Type.ELEMENT, nameFP, pool);
    }

    private NodeInfo getChildElement(NodeInfo parent, NameTest child) {
        return parent.iterateAxis(AxisInfo.CHILD, child).next();
    }

    public void go(String[] args) throws SAXException, ParserConfigurationException {


        testSuiteDir = args[0];
        saxonDir = args[1];
        HashSet exceptions = new HashSet();

        for (int i = 2; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if (args[i].equals("-w")) {
                    showWarnings = true;
                } else if (args[i].equals("-compile")) {
                    compile = true;
                } else if (args[i].equals("-runcomp")) {
                    generateByteCode = true;
                } else if (args[i].equals("-debugcomp")) {
                    debugBytecode = true;
                } else if (args[i].equals("-onwards")) {
                    onwards = true;
                } else if (args[i].equals("-unfold")) {
                    unfolded = true;
                } else if (args[i].equals("-nosrc")) {
                    noSource = true;
                } else if (args[i].startsWith("-indent:")) {
                    indent = args[i].substring(8);
                } else if (args[i].startsWith("-spec:")) {
                    specVersion = args[i].substring(6);
                } else if (args[i].startsWith("-cat:")) {
                    catalogfile = args[i].substring(5);
                }
            } else {
                testPattern = Pattern.compile(args[i]);
            }
        }


        try {
//            parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
//            if (!parser.getFeature("http://xml.org/sax/features/xml-1.1")) {
//                System.err.println("Warning: XML parser does not support XML 1.1 - " + parser.getClass());
//            };
            NamePool pool = new NamePool();
            eeConfig = new EnterpriseConfiguration();
            eeConfig.setNamePool(pool);
            //eeConfig.setHostLanguage(Configuration.XQUERY);
            eeConfig.setBooleanProperty(Feature.XQUERY_SCHEMA_AWARE, true);
            eeConfig.setBooleanProperty(Feature.GENERATE_BYTE_CODE, generateByteCode);
            eeConfig.setConfigurationProperty(Feature.DEBUG_BYTE_CODE, debugBytecode);

            //saConfig.setSourceParserClass("com.sun.org.apache.xerces.internal.parsers.SAXParser");
            XMLReader parser = eeConfig.getSourceParser();

            boolean supports11 = false;
            try {
                supports11 = parser.getFeature("http://xml.org/sax/features/xml-1.1");
            } catch (Exception err) {
            }

            if (!supports11) {
                monitor.println("Warning: XML parser does not support XML 1.1 - " + parser.getClass());
            }
            resultParser = eeConfig.getSourceParser();
            resultParser.setEntityResolver(
                    new EntityResolver() {
                        @Override
                        public InputSource resolveEntity(String publicId, String systemId) {
                            return new InputSource(new StringReader(""));
                        }
                    }
            );
            fragmentParser = eeConfig.getSourceParser();

            //Configuration config11 = new Configuration();
            //config11.setXMLVersion(Configuration.XML11);
            //config11.setNamePool(pool);

            log = new StandardLogger(new File(saxonDir, "/results" + Version.getProductVersion() + ".log"));

            MyErrorListener errorListener = new MyErrorListener(log);
            //eeConfig.setErrorListener(errorListener);

            NameTest testCaseNT = elementNameTest(pool, "test-case");
            NameTest inputUriNT = elementNameTest(pool, "input-URI");
            NameTest inputFileNT = elementNameTest(pool, "input-file");
            NameTest queryNT = elementNameTest(pool, "query");
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

            TreeInfo exceptionsDoc = eeConfig.buildDocumentTree(
                    new StreamSource(new File(saxonDir + "/exceptions.xml"))
            );


            boolean attSet = true;
            NameTest exceptionTestsNT = new NameTest(Type.ELEMENT, pool.allocateFingerprint("", "tests"), pool);
            AxisIterator exceptionTestCases = exceptionsDoc.getRootNode().iterateAxis(AxisInfo.DESCENDANT, exceptionTestsNT);
            while (true) {
                NodeInfo testCase = exceptionTestCases.next();
                String attVal = null;

//                if(testCase!=null && (attVal = Navigator.getAttributeValue(testCase, "", "apply-to")) != null ){
//                	attSet = attVal.equals("runcomp")&& runCompiled;
//                }

                if (testCase == null || !attSet) {
                    break;
                }
                String name = testCase.getStringValue();
                StringTokenizer tok = new StringTokenizer(name);
                while (tok.hasMoreTokens()) {
                    exceptions.add(tok.nextToken());
                }
                attSet = true;
            }


            TreeInfo catalog = eeConfig.buildDocumentTree(
                    new StreamSource(new File(testSuiteDir + "/" + catalogfile))
            );

            NodeInfo catalogTop = catalog.getRootNode().iterateAxis(AxisInfo.CHILD, NodeKindTest.ELEMENT).next();
            catalogVersion = catalogTop.getAttributeValue("", "version");


            String date = DateTimeValue.getCurrentDateTime(null).getStringValue().substring(0, 10);
            writeResultFilePreamble(eeConfig, date);

            Properties outputProps = new Properties();
            outputProps.setProperty("method", "xml");
            outputProps.setProperty("indent", indent);
            outputProps.setProperty("omit-xml-declaration", "yes");


            /**
             * Load all the schemas
             */

            AxisIterator schemas = catalog.getRootNode().iterateAxis(AxisInfo.DESCENDANT, schemaNT);
            while (!noSource) {
                NodeInfo schemaElement = schemas.next();
                if (schemaElement == null) {
                    break;
                }
                String fileName = schemaElement.getAttributeValue("", "FileName");
                monitor.println("Loading schema " + fileName);
                FileInputStream ssIStream = new FileInputStream(new File(new File(testSuiteDir), fileName));
                StreamSource ss = new StreamSource(ssIStream);
                try {
                    eeConfig.addSchemaSource(ss);
                } catch (SchemaException e) {
                    monitor.println("** Failed to load schema " + fileName + ": " + e.getMessage());
                }
                ss.getInputStream().close();
            }

            /**
             * Load the source documents
             */

            AxisIterator sources = catalog.getRootNode().iterateAxis(AxisInfo.DESCENDANT, sourceNT);
            while (!noSource) {
                NodeInfo sourceElement = sources.next();
                if (sourceElement == null) {
                    break;
                }
                String schema = sourceElement.getAttributeValue("", "schema");
                String id = sourceElement.getAttributeValue("", "ID");
                String fileName = sourceElement.getAttributeValue("", "FileName");
                monitor.println("Loading source " + fileName);
                File sFile = new File(new File(testSuiteDir), fileName);
                FileInputStream inStream = new FileInputStream(sFile.getCanonicalPath());
                Source ss = new StreamSource(inStream);
                ss.setSystemId(sFile.toURI().toString());
                ParseOptions options = new ParseOptions();
                if (schema != null) {
                    options.setSchemaValidationMode(Validation.STRICT);
                }
                try {
                    TreeInfo doc = eeConfig.buildDocumentTree(ss, options);
                    documentCache.put(id, doc);

                } catch (XPathException e) {
                    monitor.println("** invalid source document: " + e.getMessage());
                }
                inStream.close();
            }


            AxisIterator testCases = catalog.getRootNode().iterateAxis(AxisInfo.DESCENDANT, testCaseNT);
            while (true) {
                NodeInfo testCase = testCases.next();
                if (testCase == null) {
                    break;
                }

                String testName = testCase.getAttributeValue("", "name");
                boolean optimizationOK = true;
                if (testPattern != null && !testPattern.matcher(testName).matches()) {
                    continue;
                }
                if (onwards) {
                    testPattern = null;
                }
                if (exceptions.contains(testName)) {
                    continue;
                }
                if (isExcluded(testName)) {
                    continue;
                }

                String filePath = testCase.getAttributeValue("", "FilePath");
                if (filePath.startsWith("StaticTyping")) {
                    continue;
                }

                monitor.println("Test " + testName + " (" + filePath + ")");
                log.info("Test " + testName);


                //NodeInfo testInput = getChildElement(testCase, inputFileNT);

                NodeInfo query = getChildElement(testCase, queryNT);
                String queryName = query.getAttributeValue("", "name");
                String languageVersion = query.getAttributeValue("", "version");
                if (languageVersion == null) {
                    languageVersion = specVersion;
                }

                String absQueryName;
                if (unfolded) {
                    //absQueryName = saxonDir + "/XQUnfolded/" + filePath + queryName + ".xq";
                    absQueryName = testSuiteDir + "/Queries/XQUnfolded/" + filePath + queryName + ".xq";
                } else {
                    absQueryName = testSuiteDir + "/Queries/XQuery/" + filePath + queryName + ".xq";
                }

                String outputFile;

                StaticQueryContext env = eeConfig.newStaticQueryContext();
                env.setModuleURIResolver(new XQTSModuleURIResolver(testCase));
                env.setErrorListener(errorListener);
                File file1 = new File(absQueryName);
                FileInputStream fileInput1 = null;
                try {
                    fileInput1 = new FileInputStream(file1.getCanonicalPath());
                } catch (FileNotFoundException e) {
                    continue;
                }
                env.setBaseURI(file1.toURI().toString());
                fileInput1.close();
                int vn = 10;
                if (languageVersion.equals("3.0")) {
                    vn = 30;
                } else if (languageVersion.equals("3.1")) {
                    vn = 31;
                }
                env.setLanguageVersion(vn);

                XQueryExpression xqe;
                FileInputStream stream = new FileInputStream(absQueryName);
                try {

                    xqe = env.compileQuery(stream, "UTF-8");
                    stream.close();
                } catch (XPathException err) {
                    processError(err, testCase, testName, filePath + queryName + ".xq", expectedErrorNT);
                    stream.close();
                    continue;
                } catch (Throwable e) {
                    e.printStackTrace();
                    stream.close();
                    continue;
                }

                NodeInfo optElement = getChildElement(testCase, optimizationNT);
                if (optElement != null) {
                    String explain = optElement.getAttributeValue("", "explain");
                    if ("true".equals(explain) || "1".equals(explain)) {
                        ExpressionPresenter presenter = new ExpressionPresenter(eeConfig);
                        xqe.explain(presenter);
                        presenter.close();
                    }
                    String assertion = optElement.getAttributeValue("", "assert");
                    if (assertion != null) {
                        TinyBuilder builder = new TinyBuilder(eeConfig.makePipelineConfiguration());
                        builder.setStatistics(eeConfig.getTreeStatistics().ASSERTION_TREE_STATISTICS);
                        ExpressionPresenter presenter = new ExpressionPresenter(eeConfig, builder);
                        xqe.explain(presenter);
                        presenter.close();
                        NodeInfo expressionTree = builder.getCurrentRoot();
                        XPathEvaluator xpe = new XPathEvaluator(eeConfig);
                        XPathExpression exp = xpe.createExpression(assertion);
                        XPathDynamicContext c = exp.createDynamicContext(expressionTree);
                        try {
                            boolean bv = exp.effectiveBooleanValue(c);
                            if (!bv) {
                                log.info("** Optimization assertion failed");
                                optimizationOK = false;
                            }
                        } catch (Exception e) {
                            log.info("** Optimization assertion result is not a boolean: " + assertion +
                                             "(" + e.getMessage() + ")");

                        }
                    }
                }

                DynamicQueryContext dqc = new DynamicQueryContext(eeConfig);

                NodeInfo contextItemElement = getChildElement(testCase, contextItemNT);
                if (contextItemElement != null) {
                    NodeInfo contextNode = loadDocument(contextItemElement.getStringValue()).getRootNode();
                    dqc.setContextItem(contextNode);
                }

                processInputQueries(testCase, inputQueryNT, filePath, dqc);

                processInputDocuments(testCase, inputFileNT, dqc);

                setQueryParameters(catalog, testCase, dqc, inputUriNT, collectionNT);
                if (unfolded) {
                    dqc.setParameter(new StructuredQName("", "", "zlsJJ"), StringValue.ZERO_LENGTH_UNTYPED);
                }

                NodeInfo defaultCollection = getChildElement(testCase, defaultCollectionNT);
                if (defaultCollection != null) {
                    String docName = defaultCollection.getStringValue();
                    NodeInfo collectionElement = getCollectionElement(catalog, docName, collectionNT);
                    // Ignore collection tests for now
//                    CollectionURIResolver r =
//                            new XQTSCollectionURIResolver(catalog, collectionElement, true);
//                    eeConfig.setCollectionURIResolver(r);
                }


                // Run the query

                String outputDir = saxonDir + "/results/" + filePath;
                if (outputDir.endsWith("/")) {
                    outputDir = outputDir.substring(0, outputDir.length() - 1);
                }
                new File(outputDir).mkdirs();
                outputFile = outputDir + "/" + testName + ".out";
                File outputFileF = new File(outputFile);
                outputFileF.createNewFile();
                StreamResult result = new StreamResult(outputFileF);
                try {
                    xqe.run(dqc, result, outputProps);
                } catch (XPathException err) {
                    processError(err, testCase, testName, filePath + queryName + ".xq", expectedErrorNT);
                    continue;
                } catch (Throwable e) {
                    e.printStackTrace();
                    continue;
                }

                // Compare the results

                boolean resultsMatched = false;
                String possibleMatch = null;
                SequenceIterator expectedResults = testCase.iterateAxis(AxisInfo.CHILD, outputFileNT);
                boolean multipleResults = false;
                SequenceIterator ccc = testCase.iterateAxis(AxisInfo.CHILD, outputFileNT);
                ccc.next();
                if (ccc.next() != null) {
                    multipleResults = true;
                }
                while (true) {
                    NodeInfo outputFileElement = (NodeInfo) expectedResults.next();
                    if (outputFileElement == null) {
                        break;
                    }
                    String appliesTo = outputFileElement.getAttributeValue("", "spec-version");
                    if (appliesTo != null && !appliesTo.contains(specVersion)) {
                        continue; // results apply to a different version
                    }
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
                        results.writeEmptyElement("test-case");
                        results.writeAttribute("name", testName);
                        results.writeAttribute("result", "pass");
                        if (!optimizationOK) {
                            results.writeAttribute("comment", "check optimization");
                        }
                        resultsMatched = true;
                        break;
                    } else if (comparisonResult.equals("false")) {
                        //continue;
                    } else {
                        possibleMatch = comparisonResult;
                        //continue;
                    }
                }

                if (!resultsMatched) {
                    if (multipleResults) {
                        log.info("*** Failed to match any of the permitted results");
                    }
                    NodeInfo expectedError = null;
                    SequenceIterator eit = testCase.iterateAxis(AxisInfo.CHILD, expectedErrorNT);
                    while (true) {
                        NodeInfo e = (NodeInfo) eit.next();
                        if (e == null) {
                            break;
                        }
                        String appliesTo = e.getAttributeValue("", "spec-version");
                        if (appliesTo != null && !appliesTo.contains(specVersion)) {
                            continue; // results apply to a different version
                        }
                        expectedError = e;
                        break;
                    }

                    results.writeEmptyElement("test-case");
                    results.writeAttribute("name", testName);
                    if (possibleMatch != null) {
                        results.writeAttribute("result", "pass");
                        results.writeAttribute("comment", possibleMatch);
                    } else if (expectedError != null) {
                        results.writeAttribute("result", "fail");
                        results.writeAttribute("comment", "expected " + expectedError.getUnicodeStringValue() + ", got success");
                    } else {
                        results.writeAttribute("result", "fail");
                    }
                    results.writeProcessingInstruction("file", filePath + queryName + ".xq");
                }
            }

            results.writeEndElement(); // test-suite-result
            results.close();
            if (compile) {
                compileScript.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setQueryParameters(TreeInfo catalog, NodeInfo testCase, DynamicQueryContext dqc, NameTest inputUriNT, NameTest collectionNT) throws XPathException {
        SequenceIterator inputURIs = testCase.iterateAxis(AxisInfo.CHILD, inputUriNT);
        while (true) {
            NodeInfo inputURI = (NodeInfo) inputURIs.next();
            if (inputURI == null) {
                break;
            }
            String variableName = inputURI.getAttributeValue("", "variable");
            if (variableName != null) {
                String docName = inputURI.getStringValue();
                if (docName.startsWith("collection")) {
                    NodeInfo collectionElement = getCollectionElement(catalog, docName, collectionNT);
                    // Ignore collection tests for now
//                    CollectionURIResolver r =
//                            new XQTSCollectionURIResolver(catalog, collectionElement, false);
//                    eeConfig.setCollectionURIResolver(r);
//                    dqc.setParameter(new StructuredQName("", "", variableName), new AnyURIValue(docName));
                } else {
                    TreeInfo doc = loadDocument(docName);
                    if (doc == null) {
                        dqc.setParameter(new StructuredQName("", "", variableName), new AnyURIValue("error-document" + docName));
                    } else {
                        String uri = doc.getRootNode().getSystemId();
                        dqc.setParameter(new StructuredQName("", "", variableName), new AnyURIValue(uri));
                    }
                }
            }
        }
    }

    private void processInputDocuments(NodeInfo testCase, NameTest inputFileNT, DynamicQueryContext dqc) throws XPathException {
        SequenceIterator inputFiles = testCase.iterateAxis(AxisInfo.CHILD, inputFileNT);
        while (true) {
            NodeInfo inputFile = (NodeInfo) inputFiles.next();
            if (inputFile == null) {
                break;
            }
            String variableName = inputFile.getAttributeValue("", "variable");
            if (variableName != null) {
                TreeInfo inputDoc = loadDocument(inputFile.getStringValue());
                dqc.setParameter(new StructuredQName("", "", variableName), inputDoc.getRootNode().materialize());
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
                StaticQueryContext sqc2 = eeConfig.newStaticQueryContext();
                FileReader subQueryFileReader = new FileReader(subQueryFile);
                XQueryExpression subQuery = sqc2.compileQuery(subQueryFileReader);
                subQueryFileReader.close();
                SequenceIterator subQueryResult = subQuery.iterator(new DynamicQueryContext(eeConfig));
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
        return testName.startsWith("dotnet");
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
     * @param xml
     */

    private TreeInfo loadDocument(String xml) {
        return (TreeInfo) documentCache.get(xml);
    }

    /**
     * Process a static or dynamic error
     */

    private void processError(XPathException err, NodeInfo testCase, String testName, String queryPath, NameTest expectedErrorNT)
            throws java.io.IOException, XMLStreamException {
        String actualError = err.getErrorCodeLocalPart();
        AxisIterator expectedErrors = testCase.iterateAxis(AxisInfo.CHILD, expectedErrorNT);
        StringBuilder expected = new StringBuilder(20);
        while (true) {
            NodeInfo expectedError = expectedErrors.next();
            if (expectedError == null) {
                break;
            }
            String appliesTo = expectedError.getAttributeValue("", "spec-version");
            if (appliesTo != null && !appliesTo.contains(specVersion)) {
                continue; // results apply to a different version
            }
            if (expectedError.getStringValue().equals(actualError) ||
                    expectedError.getStringValue().equals("*")) {
                results.writeEmptyElement("test-case");
                results.writeAttribute("name", testName);
                results.writeAttribute("result", "pass");
                return;
            }
            expected.append(expectedError.getStringValue());
            expected.append(" ");
        }
        if (expected.length() > 0) {
            results.writeEmptyElement("test-case");
            results.writeAttribute("name", testName);
            results.writeAttribute("result", "pass");
            results.writeAttribute("comment", "expected " + expected + ", got " + actualError);
        } else {
            results.writeEmptyElement("test-case");
            results.writeAttribute("name", testName);
            results.writeAttribute("result", "fail");
            results.writeAttribute("comment", "expected success, got " + actualError);
        }
        results.writeProcessingInstruction("file", queryPath);

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
            reader1.close();
            reader2.close();
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
                in1.close();
                in2.close();
                out2.close();
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
                    log.info("XQTS: First comparison attempt failed " + err1.getMessage() + ", trying again");
                }
                if (!b) {
                    log.info("XQTS: First comparison attempt failed, trying again");
                    b = compareFragments(outfileFile, reffileFile, outfile, silent);
                }
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
//                out.getByteStream().close();
//                ref.getByteStream().close();
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
        try {
            out2.getByteStream().close();
            ref2.getByteStream().close();
        } catch (Exception e) {
        }
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
        String outxml2 = canon.toCanonicalXML(fragmentParser, out2, false);
        String refxml2 = canon.toCanonicalXML(fragmentParser, ref2, false);
        try {
            out2.getByteStream().close();
            ref2.getByteStream().close();
        } catch (Exception e) {
        }
        if (outxml2 != null && refxml2 != null && !outxml2.equals(refxml2)) {
            // Try again with whitespace stripping
            InputSource out3 = new InputSource(new StringReader(outdoc));
            InputSource ref3 = new InputSource(new StringReader(refdoc));
            String outxml3 = canon.toCanonicalXML(fragmentParser, out3, true);
            String refxml3 = canon.toCanonicalXML(fragmentParser, ref3, true);
            if (outxml3 != null && refxml3 != null && !outxml3.equals(refxml3)) {
                if (!silent) {
                    log.info("Mismatch with reference results: " + outfile);
                    log.info("REFERENCE RESULTS:");
                    log.info(truncate(refxml2));
                    log.info("ACTUAL RESULTS:");
                    log.info(truncate(outxml2));
                    findDiff(refxml2, outxml2);
                }
                return false;
            } else {
                log.info("Matches after stripping whitespace");
                return true;
            }

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

    private void writeResultFilePreamble(Configuration config, String date) throws IOException, XPathException, XMLStreamException {
        Writer resultWriter = new BufferedWriter(new FileWriter(
                new File(saxonDir, "/results" + Version.getProductVersion() + ".xml")));

        Properties resultProperties = new Properties();
        resultProperties.setProperty(OutputKeys.METHOD, "xml");
        resultProperties.setProperty(OutputKeys.INDENT, "yes");
        resultProperties.setProperty(SaxonOutputKeys.LINE_LENGTH, "120");
        results = config.getSerializerFactory().getXMLStreamWriter(
                new StreamResult(resultWriter), resultProperties);


        results.writeStartElement("test-suite-result");
        results.writeDefaultNamespace("http://www.w3.org/2005/02/query-test-XQTSResult");

        outputImplementationDetails();

        results.writeStartElement("syntax");
        results.writeCharacters("XQuery");
        results.writeEndElement(); // syntax

        outputRunDetails(date);
    }

    private void outputImplementationDetails() throws IOException, XMLStreamException {
        results.writeStartElement("implementation");
        results.writeAttribute("name", "Saxon-EE");
        results.writeAttribute("version", Version.getProductVersion());
        results.writeAttribute("anonymous-result-column", "false");
        results.writeEmptyElement("organization");
        results.writeAttribute("name", "Saxonica");
        results.writeAttribute("website", "http://www.saxonica.com/");
        results.writeAttribute("anonymous", "false");
        results.writeEmptyElement("submittor");
        results.writeAttribute("name", "Michael Kay");
        results.writeAttribute("title", "Director");
        results.writeAttribute("email", "mike@saxonica.com");
        results.writeEmptyElement("description");
        outputImplementationDefinedItems();
        outputFeatures();
        results.writeEndElement(); //implementation
    }

    private void outputImplementationDefinedItems() throws XMLStreamException {

        results.writeStartElement("implementation-defined-items");

        outputIDI("expressionUnicode",
                  "Whatever is supported by the Java JDK in use");
        outputIDI("collations",
                  "URIs corresponding to RuleBasedCollators that can be constructed by the Java VM, plus" +
                          " any user-defined collations whose implementation is on the Java classpath");
        outputIDI("implicitTimezone",
                  "Taken from the system clock. For this test run, +01:00");
        outputIDI("warningsMethod",
                  "Controlled using the JAXP-defined ErrorListener interface.");
        outputIDI("errorsMethod",
                  "Errors are reported using the JAXP ErrorListener interface.");
        outputIDI("XMLVersion",
                  "XML 1.0 or 1.1 can be selected under user control.");
        outputIDI("overwrittenContextComponents",
                  "None");
        outputIDI("axes",
                  "All the axes are supported.");
        outputIDI("defaultOrderEmpty",
                  "By default, empty collates least.");
        outputIDI("pragmas",
                  "The saxon:validate-type pragma validates an expression against a named simple or complex type");
        outputIDI("optionDeclarations", "saxon:default declares default values for external variables." +
                " saxon:memo-function declares functions that are to be implemented as memo-functions. " +
                " saxon:output declares serialization parameters." +
                " For detailed semantics, see the user documentation.");
        outputIDI("externalFunctionProtocols",
                  "Saxon supports calls on external Java methods." +
                          " There is a plug-in architecture allowing additional external function libraries and binding mechanisms" +
                          " to be supported by third parties.");
        outputIDI("moduleLocationHints",
                  "An actual URI for the location of the module source code must be provided in the import declaration," +
                          " or via a ModuleURIResolver nominated using the Java API");
        outputIDI("staticTypingExtensions",
                  "Saxon does not support strict static typing, although it does report type errors at compile time" +
                          " if they can be detected at compile time.");
        outputIDI("serializationInvocation",
                  "Saxon supports a Java API that gives full access to serialization, reusing parts of the" +
                          " JAXP API where appropriate.");
        outputIDI("serializationDefaults",
                  "As described in the XQuery specification (but if running from the command line, indent=yes is" +
                          " set by default)");
        outputIDI("externalFunctionCall",
                  "An unsuccessful call to an external function results in a dynamic error");
        outputIDI("limits", "Documented in the detailed conformance documentation");
        outputIDI("traceDestination",
                  "If a TraceListener is registered, the trace output is sent to the TraceListener. Otherwise it is" +
                          " sent as text to System.err.");
        outputIDI("integerOperations", "Saxon supports unlimited-precision integer arithmetic");
        outputIDI("decimalDigits",
                  "Saxon implements arbitrary-precision decimal arithmetic. The default precision for division is" +
                          " 18 digits, but this is configurable.");
        outputIDI("roundOrTruncate",
                  "Not applicable, since Saxon supports indefinite precision.");
        outputIDI("Unicode",
                  "Saxon validates that characters in strings and names are as permitted by XML 1.0" +
                          " (or XML 1.1 at user option), unless the value" +
                          " thas already been validated by an XML parser, in which case it depends on the user's choice of parser");
        outputIDI("normalizationForms",
                  "Saxon supports NFC, NFD, NFKC, and NFKD.");
        outputIDI("collationUnits",
                  "All collations based on Java's RuleBasedCollator have this property. User-defined collations may or may non" +
                          " have this property.");
        outputIDI("secondsDigits",
                  "Saxon supports 4 digits for the year and 6 digits for fractional seconds on dateTimes and durations.");
        outputIDI("stringToDecimal",
                  "Not applicable, since Saxon supports arbitrary-precision decimal numbers");
        outputIDI("weakenStable",
                  "Saxon provides an extension function saxon:discard-document() that removes a document" +
                          " from the stable set. It also allows a user-defined collection URI resolver to return an" +
                          " unstable collection.");
        outputIDI("additionalTypes",
                  "Saxon allows any Java object to be wrapped as an XPath item, and defines a mapping of Java class" +
                          " names to XPath type names so that such objects carry full dynamic type information with them.");
        outputIDI("undefinedProperties",
                  "Access to undefined values is an error.");
        outputIDI("sequenceNormalization",
                  "Sequences are always normalized to documents before the serializer is invoked. However, the Java" +
                          " application may change the way in which a result sequence is converted to a document before invoking" +
                          " the serializer.");
        outputIDI("outputMethods",
                  "Saxon allows a user-specified serialization class to be nominated using this mechanism. The class may be a SAX" +
                          " ContentHandler or a Saxon Receiver.");
        outputIDI("normalizationFormBehavior",
                  "Any normalization form other than NFC, NFD, NFKC, or NFKD is treated as an error.");
        outputIDI("additionalParams",
                  "Saxon supports a number of additional serialization parameters in the namespace http://saxon.sf.net/." +
                          "  These include saxon:indent-spaces, saxon:character-representation, saxon:require-well-formed, and saxon:next-in-chain");
        outputIDI("encodingPhase",
                  "Saxon allows the serialization destination to be a Java Writer (a character stream) rather than a byte  stream.");
        outputIDI("CDATASerialization",
                  "Saxon provides the standard cdata-section-elements mechanism only.");

        results.writeEndElement(); //  implementation-defined-items
    }

    private void outputIDI(String name, String value) throws XMLStreamException {
        results.writeEmptyElement("implementation-defined-item");
        results.writeAttribute("name", name);
        results.writeAttribute("value", value);
    }

    private void outputFeatures() throws XMLStreamException {
        results.writeStartElement("features");
        outputFeature("MinimalConformance", true);
        outputFeature("Schema Import", true);
        outputFeature("Schema Validation", true);
        outputFeature("Static Typing", false);
        outputFeature("Static Typing Extensions", false);
        outputFeature("Full Axis", true);
        outputFeature("Module", true);
        results.writeEndElement(); //features

    }

    private void outputFeature(String name, boolean supported) throws XMLStreamException {
        results.writeEmptyElement("feature");
        results.writeAttribute("name", name);
        results.writeAttribute("supported", supported ? "true" : "false");
    }

    private void outputRunDetails(String date) throws XMLStreamException {
        results.writeStartElement("test-run");
        results.writeAttribute("dateRun", date);
        results.writeEmptyElement("test-suite");
        results.writeAttribute("version", catalogVersion);
        results.writeEmptyElement("transformation");
        results.writeEmptyElement("comparison");
        results.writeEmptyElement("otherComments");
        results.writeEndElement(); // test-run
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
                if ("FODC0005".equals(errorCode)) {
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
            return new MyErrorListener(log);
        }

    }

    private String lowercase(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        name = name.toLowerCase();
        for (int p = 0; p < name.length(); p++) {
            char c = name.charAt(p);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(""+c);
            }
        }
        return sb.toString();
    }

    private String makeClassName(String groupName, String mainName) {
        StringBuilder sb = new StringBuilder(mainName.length());
        sb.append(groupName);
        sb.append(".");
        mainName = mainName.substring(0, 1).toUpperCase() + mainName.substring(1).toLowerCase();
        sb.append(mainName);
        return sb.toString();
    }

    private String normalizeNewlines(String in) {
        return in.replace("\r\n", "\n");
    }
//        int cr = in.indexOf('\r');
//        if (cr >= 0 && in.charAt(cr + 1) == '\n') {
//            return in.substring(0, cr) + normalizeNewlines(in.substring(cr + 1));
//        } else {
//            return in;
//        }
//    }


}
