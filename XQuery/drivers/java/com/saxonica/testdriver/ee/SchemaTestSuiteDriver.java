package com.saxonica.testdriver.ee;


import com.saxonica.config.EnterpriseConfiguration;
import com.saxonica.testdriver.Licensor;
import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.Version;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.lib.*;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.s9api.*;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.testdriver.TestDriverShell;
import net.sf.saxon.trans.Timer;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This class runs the W3C XML Schema Test Suite, driven from the test catalog.
 */
public class SchemaTestSuiteDriver {

    public final static String testNS = "http://www.w3.org/XML/2004/xml-schema-test-suite/";
    public String scmSchemaLocation;
    private final Licensor licensor = new Licensor();
    private String exceptionFilename = null;
    private String reportFilename = null;
    private String errorReportFilename = null;
    private String summarizeResults = null;

    public final static QName QN_bugzilla = new QName("bugzilla");
    public final static QName QN_contributor = new QName("contributor");
    public final static QName QN_group = new QName("group");
    public final static QName QN_name = new QName("name");
    public final static QName QN_set = new QName("set");
    public final static QName QN_status = new QName("status");
    public final static QName QN_targetNamespace = new QName("targetNamespace");
    public final static QName QN_validity = new QName("validity");
    public final static QName QN_version = new QName("version");

    public final static QName QN_xlink_href = new QName("http://www.w3.org/1999/xlink", "href");

    /**
     * Run the testsuite using Saxon.
     *
     * @param args Array of parameters passed to the application
     *             via the command line.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        long startTime = System.nanoTime();
        if (args.length == 0 || args[0].equals("-?")) {
            System.err.println("SchemaTestSuiteDriver testDir -local:dir [-w] [-onwards] [-scm:scmschema.xsd] [-tv] [-dangle] -v:1.0|1.1 -c:contributor? -s:setName? -g:groupName?");
        }
        System.err.println("Testing Saxon " + Version.getProductVersion());
        new SchemaTestSuiteDriver().go(args);
        long endTime = System.nanoTime();
        System.err.println("Execution time: " + Timer.showExecutionTimeNano(endTime-startTime));
    }

    String testSuiteDir;
    String localDir;
    EnterpriseConfiguration catalogConfig;
    Processor catalogProcessor;
    SchemaValidator scmValidator;
    XMLReader parser;
    boolean showWarnings = false;
    boolean onwards = false;
    boolean useSCM = false;
    boolean useTV = false; // Switch to use the experimental validation engine written in XSLT
    boolean allowDangling = false;
    String versionUnderTest = "1.1";
    private TestDriverShell shell = new TestDriverShell();
    private int countSuccesses = 0;
    private int countFailures = 0;
    private int countTests = 0;
    private int queriedCount = 0;

    XMLStreamWriter results;
    int xlinkHref;
    XsltExecutable xsltValidator = null;

    /**
     * Get the document referenced by the xlink:href attribute of a given element node
     *
     * @param element   the element node (a node in the catalog)
     * @param processor the processor used to build the target document (the processor under test)
     * @param validate  true if the document is to be schema-validated
     * @return the constructed document (owned by the supplied Processor)
     * @throws URISyntaxException if the xlink:href attribute is invalid
     * @throws SaxonApiException  if an error occurs building the document
     */

    private XdmNode getLinkedDocument(XdmNode element, Processor processor, boolean validate)
            throws URISyntaxException, SaxonApiException {
        String href = element.getAttributeValue(QN_xlink_href);
        URI target = element.getBaseURI().resolve(href);
        Source ss = new StreamSource(target.toString());

        if (validate) {
            SchemaValidator validator = processor.getSchemaManager().newSchemaValidator();
            StandardErrorReporter reporter = new StandardErrorReporter();
            reporter.setMaximumNumberOfErrors(500);
            validator.setInvalidityHandler(new InvalidityHandlerWrappingErrorReporter(reporter));
            validator.validate(ss);
            return null;
        } else {
            DocumentBuilder builder = processor.newDocumentBuilder();
            builder.setLineNumbering(true);
            return builder.build(ss);
        }
    }

    public void setTestDriverShell(TestDriverShell gui) {
        shell = gui;
    }

    public void println(String data) {
        shell.println(data);
    }


    /**
     * Run the tests
     *
     * @param args command line arguments
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathException
     * @throws IOException
     * @throws URISyntaxException
     */

    public void go(String[] args) throws SAXException, ParserConfigurationException,
            XPathException, IOException, URISyntaxException {


        testSuiteDir = args[0];
        Pattern testSetPattern = null;
        Pattern testGroupPattern = null;
        String contributor = null;
        HashMap<String, String> exceptions = new HashMap<>();
        String bytecode = "on";

        for (int i = 1; i < args.length; i++) {
            System.err.print(args[i] + " ");
            if (args[i].equals("-w")) {
                showWarnings = true;
            } else if (args[i].equals("-onwards")) {
                onwards = true;
            } else if (args[i].startsWith("-local:")) {
                localDir = args[i].substring(7);
            } else if (args[i].equals("-tv")) {
                // Switch to use the experimental validation engine written in XSLT
                useTV = true;
            } else if (args[i].equals("-dangle")) {
                allowDangling = true;
            } else if (args[i].startsWith("-bytecode:")) {
                bytecode = args[i].substring(10);
            } else if (args[i].startsWith("-exceptions:")) {
                exceptionFilename = args[i].substring(12);
            } else if (args[i].startsWith("-reportfilename:")) {
                reportFilename = args[i].substring(16);
            } else if (args[i].startsWith("-errorfilename:")) {
                errorReportFilename = args[i].substring(15);
            } else if (args[i].startsWith("-summarizeresults:")) {
                summarizeResults = args[i].substring(18);
            } else if (args[i].startsWith("-scm:")) {
                useSCM = true;
                scmSchemaLocation = args[i].substring(5);
                try {
                    Processor scmProcessor = new Processor(true);
                    scmProcessor.setConfigurationProperty(Feature.XSD_VERSION, "1.1");
                    scmProcessor.setConfigurationProperty(Feature.XML_VERSION, "1.1");
                    SchemaManager sm = scmProcessor.getSchemaManager();
                    File scmLocation = new File(scmSchemaLocation);
                    InputStream scmInput = new FileInputStream(scmLocation);
                    StreamSource streamSource = new StreamSource(scmInput);
                    sm.importComponents(streamSource);
                    scmInput.close();
                    scmValidator = sm.newSchemaValidator();
                } catch (SaxonApiException e) {
                    e.printStackTrace();
                    useSCM = false;
                }
            } else if (args[i].startsWith("-c:")) {
                contributor = args[i].substring(3);
            } else if (args[i].startsWith("-s:")) {
                testSetPattern = Pattern.compile(args[i].substring(3));
            } else if (args[i].startsWith("-g:")) {
                testGroupPattern = Pattern.compile(args[i].substring(3));
            } else if (args[i].startsWith("-v:")) {
                versionUnderTest = args[i].substring(3);
            } else {
                System.err.println("Usage: SchemaTestSuiteDriver testDir [-v:1.0|1.1] [-w] [-s:testSetPattern] [-g:testGroupPattern]");
            }
        }

        if (localDir == null) {
            System.err.print("No -local directory specified");
        }

        try {

            NamePool pool = new NamePool();
            catalogConfig = new EnterpriseConfiguration();
            licensor.activate(catalogConfig);
            catalogConfig.setNamePool(pool);
            //catalogConfig.setHostLanguage(Configuration.XML_SCHEMA);
            parser = catalogConfig.getSourceParser();
            catalogProcessor = new Processor(catalogConfig);

            xlinkHref = pool.allocateFingerprint("http://www.w3.org/1999/xlink", "href");


            XdmNode catalog = catalogProcessor.newDocumentBuilder().build(
                    new File(testSuiteDir + "/suite.xml"));

            writeResultFilePreamble(catalogConfig);

            String exfn = exceptionFilename;
            if (exfn == null) {
                exfn = "exceptions" +
                        (versionUnderTest.equals("1.0") ? "" : "11") +
                        (useTV ? "tv" : "");
            }
            if (!exfn.contains(".")) {
                exfn = exfn + ".xml";
            }
            if (!exfn.contains("/")) {
                exfn = localDir + "/" + exfn;
            }

            XdmNode exceptionsDoc = catalogProcessor.newDocumentBuilder().build(new File(exfn));

            XPathCompiler xpath = catalogProcessor.newXPathCompiler();

            for (XdmItem testCaseItem : xpath.evaluate("//exception", exceptionsDoc)) {
                XdmNode testCase = (XdmNode) testCaseItem;
                String set = testCase.attribute("test-set");
                String group = testCase.attribute("test-case");
                String comment = xpath.evaluateSingle("string(reason)", testCase).getStringValue();
                exceptions.put(set + "#" + group, comment);
            }

            xpath.setCaching(true);
            xpath.declareNamespace("", testNS);
            xpath.declareNamespace("saxon", NamespaceConstant.SAXON);

            long startTime = new Date().getTime();

            for (XdmItem testCaseRefItem : xpath.evaluate("//testSetRef", catalog)) {
                XdmNode testSetRef = (XdmNode) testCaseRefItem;
                XdmNode testSetDoc = getLinkedDocument(testSetRef, catalogProcessor, false);
                XdmNode testSetElement = (XdmNode) xpath.evaluateSingle("testSet", testSetDoc);

                if (testSetElement == null) {
                    System.err.println("test set doc has no TestSet child: " + testSetDoc.getBaseURI());
                    continue;
                }

                String testSetName = testSetElement.getAttributeValue(QN_name);
                println("Test set " + testSetName);
                if (testSetPattern != null && !testSetPattern.matcher(testSetName).matches()) {
                    continue;
                }
                String currentContributor = testSetElement.getAttributeValue(QN_contributor);
                if (contributor != null && !contributor.equals(currentContributor)) {
                    continue;
                }

                String testSetVn = testSetElement.getAttributeValue(QN_version);
                if (testSetVn == null) {
                    testSetVn = "";
                }
                if (testSetVn.equals("full-xpath-in-CTA")) {
                    testSetVn = "1.1";
                }
                if (testSetVn.length() != 0 && !testSetVn.contains(versionUnderTest)) {
                    continue;
                }

                boolean xml11TestSet = testSetVn.contains("XML-1.1");

                for (XdmItem testGroupItem : xpath.evaluate("testGroup", testSetElement)) {
                    XdmNode testGroup = (XdmNode) testGroupItem;

                    String testGroupVn = testGroup.getAttributeValue(QN_version);
                    if (testGroupVn == null) {
                        testGroupVn = "";
                    }
                    if (testGroupVn.equals("full-xpath-in-CTA")) {
                        testGroupVn = "1.1";
                    }
                    if (testGroupVn.length() != 0 && !testGroupVn.contains(versionUnderTest)) {
                        continue;
                    }

                    boolean xml11TestGroup = testGroupVn.contains("XML-1.1");

                    String testGroupName = testGroup.getAttributeValue(QN_name);
                    String exception = exceptions.get(testSetName + "#" + testGroupName);

                    if (testGroupPattern != null && !testGroupPattern.matcher(testGroupName).matches()) {
                        continue;
                    }

                    println("-c:" + currentContributor + " -s:" + testSetName + " -g:" + testGroupName);
                    if (onwards) {
                        testGroupPattern = null;
                        testSetPattern = null;
                    }
                    EnterpriseConfiguration testConfig = new EnterpriseConfiguration();
                    testConfig.setTiming(showWarnings);
                    testConfig.setNamePool(catalogConfig.getNamePool());
                    testConfig.setDocumentNumberAllocator(catalogConfig.getDocumentNumberAllocator());
                    //AutoActivate.activate(testConfig);
                    //testConfig.setHostLanguage(Configuration.XML_SCHEMA);
                    //testConfig.setValidationWarnings(true);
                    //testConfig.setConfigurationProperty(Feature.MULTIPLE_SCHEMA_IMPORTS, true);
                    testConfig.setConfigurationProperty(Feature.XSD_VERSION, versionUnderTest);
                    if (xml11TestSet || xml11TestGroup) {
                        testConfig.setConfigurationProperty(Feature.XML_VERSION, "1.1");
                    }
                    if (bytecode.equals("on")) {
                        testConfig.setConfigurationProperty(Feature.GENERATE_BYTE_CODE, true);
                    } else if (bytecode.equals("off")) {
                        testConfig.setConfigurationProperty(Feature.GENERATE_BYTE_CODE, false);
                    } else if (bytecode.equals("debug")) {
                        testConfig.setConfigurationProperty(Feature.GENERATE_BYTE_CODE, true);
                        testConfig.setConfigurationProperty(Feature.DEBUG_BYTE_CODE, true);
                    }
                    Processor testProcessor = new Processor(testConfig);
                    testProcessor.setConfigurationProperty(Feature.ASSERTIONS_CAN_SEE_COMMENTS, false);
                    testProcessor.setConfigurationProperty(Feature.THRESHOLD_FOR_COMPILING_TYPES, 100);
                    testProcessor.setConfigurationProperty(Feature.MAX_COMPILED_CLASSES, 100000);
                    testProcessor.setConfigurationProperty(Feature.ALLOW_UNRESOLVED_SCHEMA_COMPONENTS,allowDangling);

                    for (XdmItem feature : xpath.evaluate("saxon:Configuration/feature", testGroup)) {
                        String name = ((XdmNode) feature).attribute("name");
                        String value = ((XdmNode) feature).attribute("value");
                        testProcessor.setConfigurationProperty(name, value);
                    }

                    boolean schemaQueried = false;
                    boolean schemaFailed = false;
                    String bugzillaRef = null;
                    String scm = null;
                    for (XdmItem schemaTestItem : xpath.evaluate("schemaTest", testGroup)) {
                        countTests++;
                        XdmNode schemaTest = (XdmNode) schemaTestItem;
                        bugzillaRef = null;
                        String testName = schemaTest.getAttributeValue(QN_name);
                        if (exception != null) {
                            results.writeEmptyElement(testNS, "testResult");
                            results.writeAttribute("set", testSetName);
                            results.writeAttribute("group", testGroupName);
                            results.writeAttribute("test", testName);
                            results.writeAttribute("actualValidity", "notKnown");
                            results.writeAttribute("saxon:outcome", "notRun");
                            results.writeAttribute("saxon:comment", exception);
                            continue;
                        }

                        String schemaTestVn = schemaTest.getAttributeValue(QN_version);
                        if (schemaTestVn != null && !schemaTestVn.contains(versionUnderTest)) {
                            continue;
                        }

                        boolean queried = false;
                        XdmNode statusElement = (XdmNode) xpath.evaluateSingle("current", schemaTest);
                        if (statusElement != null) {
                            String status = statusElement.getAttributeValue(QN_status);
                            queried = "queried".equals(status) || "disputed-test".equals(status);
                            bugzillaRef = statusElement.getAttributeValue(QN_bugzilla);
                        }
                        if (queried) {
                            schemaQueried = true;
                        }
                        println("TEST SCHEMA " + testName + (queried ? " (queried)" : ""));
                        boolean success = true;
                        List<String> scmList = new ArrayList<String>(1); // Used as an output parameter
                        success = loadSchemas(xpath, testProcessor, schemaTest, success, scmList);
                        if (!scmList.isEmpty()) {
                            scm = scmList.get(0);
                        }
                        XdmNode expected = (XdmNode) xpath.evaluateSingle(
                                "expected[not(@version) or contains(@version, '" + versionUnderTest + "') or contains(@version, 'Unicode_6.0.0')]", schemaTest);
                        String expectedOutcome = (expected == null ? "indeterminate" :
                                                          expected.getAttributeValue(QN_validity));
                        boolean ok = "indeterminate".equals(expectedOutcome) ||
                                "implementation-defined".equals(expectedOutcome) ||
                                "implementation-dependent".equals(expectedOutcome) ||
                                success == "valid".equals(expectedOutcome);

                        results.writeEmptyElement(testNS, "testResult");
                        results.writeAttribute("set", testSetName);
                        results.writeAttribute("group", testGroupName);
                        results.writeAttribute("test", testName);
                        results.writeAttribute("expectedValidity", expectedOutcome);
                        results.writeAttribute("actualValidity", (success ? "valid" : "invalid"));
                        if (queried) {
                            results.writeAttribute("saxon:queried", "true");
                            results.writeAttribute("saxon:bugzilla", bugzillaRef);
                            queriedCount++;
                        } else {
                            results.writeAttribute("saxon:outcome", (ok ? "same" : "different"));
                            if (ok) {
                                countSuccesses++;
                            } else {
                                countFailures++;
                            }
                        }
                        if (!success) {
                            schemaFailed = true;
                        }
                    }

                    if (!schemaFailed) {
                        //XsltExecutable xsltValidator = null;
                        for (XdmItem instanceTestItem : xpath.evaluate("instanceTest", testGroup)) {
                            countTests++;
                            XdmNode instanceTest = (XdmNode) instanceTestItem;
                            String testName = instanceTest.getAttributeValue(QN_name);

                            if (exception != null) {
                                results.writeEmptyElement(testNS, "testResult");
                                results.writeAttribute("set", testSetName);
                                results.writeAttribute("group", testGroupName);
                                results.writeAttribute("test", testName);
                                results.writeAttribute("actualValidity", "notKnown");
                                results.writeAttribute("saxon:outcome", "notRun");
                                results.writeAttribute("saxon:comment", exception);
                                continue;
                            }

                            String instanceTestVn = instanceTest.getAttributeValue(QN_version);
                            if (instanceTestVn != null && !instanceTestVn.contains(versionUnderTest)) {
                                continue;
                            }

                            boolean queried = false;
                            XdmNode statusElement = (XdmNode) xpath.evaluateSingle("current", instanceTest);
                            if (statusElement != null) {
                                String status = statusElement.getAttributeValue(QN_status);
                                queried = "queried".equals(status);
                                String instanceBug = statusElement.getAttributeValue(QN_bugzilla);
                                if (instanceBug != null) {
                                    bugzillaRef = instanceBug;
                                }
                            }
                            queried |= schemaQueried;

                            println("TEST INSTANCE " + testName + (queried ? " (queried)" : ""));

                            XdmNode instanceDocument = (XdmNode) xpath.evaluateSingle("instanceDocument", instanceTest);

                            boolean success = true;
                            if (useSCM && useTV) {
                                // Use the new/experimental validator written in XSLT (TV)
                                if (scm == null) {
                                    // no schema, test probably relies on xsi:schemaLocation. Try to load it
                                    // from the first instance document
                                    List<String> scmList = new ArrayList<String>(1); // Used as an output parameter
                                    success = loadSchemasViaInstanceDocument(xpath, testProcessor, instanceTest, success, scmList);
                                    if (!scmList.isEmpty()) {
                                        scm = scmList.get(0);
                                    }
                                }
                                if (scm != null) {
                                    try {
                                        if (xsltValidator == null) {
                                            if (showWarnings) {
                                                System.err.println(scm);
                                            }
                                            // TODO: adjust path to the current repository location (if we ever productise this code)
                                            Source xsltVal = new StreamSource(new File("/Users/mike/GitHub/tools/xslt-val/validator.xsl"));
                                            testProcessor.setConfigurationProperty(Feature.OPTIMIZATION_LEVEL, 0);
                                            XsltCompiler xsltCompiler = testProcessor.newXsltCompiler();
                                            if (showWarnings) {
                                                xsltCompiler.setCompileWithTracing(true);
                                                xsltCompiler.setAssertionsEnabled(true);
                                            }
                                            xsltValidator = xsltCompiler.compile(xsltVal);
                                        }

                                        XdmNode instance = getLinkedDocument(instanceDocument, testProcessor, false);
                                        XdmNode scmTree = testProcessor.newDocumentBuilder().build(new StreamSource(new StringReader(scm)));
                                        Xslt30Transformer t = xsltValidator.load30();
                                        t.setAssertionsEnabled(true);
                                        //                                    if (showWarnings) {
                                        //                                        t.setTraceListener(new XSLTTraceListener());
                                        //                                    }
                                        Map<QName, XdmValue> params = new HashMap<QName, XdmValue>();
                                        params.put(new QName("", "scm"), scmTree);
                                        t.setStylesheetParameters(params);
                                        t.setGlobalContextItem(instance);
                                        XdmNode report = (XdmNode) t.applyTemplates(instance);
                                        success = !report.axisIterator(Axis.DESCENDANT, new QName("http://saxon.sf.net/ns/validation", "error")).hasNext();
                                        System.err.println("VALIDATION " + (success ? "SUCCEEDED" : "FAILED"));
                                    } catch (SaxonApiException err) {
                                        if (showWarnings) {
                                            throw new AssertionError("xslt-val transformation failed");
                                        }
                                        System.err.println(err.getMessage());
                                        success = false;
                                    }
                                }
                            } else {
                                // Use the standard Java validator
                                try {
                                    getLinkedDocument(instanceDocument, testProcessor, true);
                                } catch (SaxonApiException err) {
                                    System.err.println(err.getMessage());
                                    success = false;
                                }
                            }

                            XdmNode expected = (XdmNode) xpath.evaluateSingle(
                                    "expected[not(@version) or contains(@version, '" + versionUnderTest + "')]", instanceTest);
                            String expectedOutcome = (expected == null ? "indeterminate" :
                                                              expected.getAttributeValue(QN_validity));
                            boolean ok = "indeterminate".equals(expectedOutcome) ||
                                    "implementation-defined".equals(expectedOutcome) ||
                                    "implementation-dependent".equals(expectedOutcome) ||
                                    success == "valid".equals(expectedOutcome);

                            results.writeEmptyElement(testNS, "testResult");
                            results.writeAttribute("set", testSetName);
                            results.writeAttribute("group", testGroupName);
                            results.writeAttribute("test", testName);
                            results.writeAttribute("expectedValidity", expectedOutcome);
                            results.writeAttribute("actualValidity", (success ? "valid" : "invalid"));
                            if (queried) {
                                results.writeAttribute("saxon:queried", "true");
                                results.writeAttribute("saxon:bugzilla", bugzillaRef);
                                queriedCount++;
                            } else {
                                results.writeAttribute("saxon:outcome", (ok ? "same" : "different"));
                                if (ok) {
                                    countSuccesses++;
                                } else {
                                    countFailures++;
                                }

                            }
                        }
                    }
                }
            }
            shell.printResults("Result: " + countSuccesses + " passed, " +
                                       countFailures + " failed, " +
                                       queriedCount + " queried", testSuiteDir + "/saxon/SaxonResults"
                                       + Version.getProductVersion() + ".xml", testSuiteDir + "/saxon");
            writeResultFilePostamble();

            System.err.println("Time: " + (new Date().getTime() - startTime) + " ms");

            generateFailureReport();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean loadSchemas(XPathCompiler xpath, Processor testProcessor, XdmNode schemaTest, boolean success, List<String> scmList)
            throws URISyntaxException, SaxonApiException {
        if (useSCM) {
            return loadSchemasViaSCM(xpath, testProcessor, schemaTest, success, scmList);
        } else {
            return loadSchemasDirect(xpath, testProcessor, schemaTest, success);
        }
    }

    private boolean loadSchemasDirect(XPathCompiler xpath, Processor processor, XdmNode schemaTest, boolean success)
            throws SaxonApiException, URISyntaxException {
        for (XdmItem schemaDocRefItem : xpath.evaluate("schemaDocument", schemaTest)) {
            XdmNode schemaDocumentRef = (XdmNode) schemaDocRefItem;
            println("Loading schema at " + schemaDocumentRef.getAttributeValue(QN_xlink_href));
            XdmNode schemaDoc = getLinkedDocument(schemaDocumentRef, processor, false);
            XdmSequenceIterator si = schemaDoc.axisIterator(Axis.CHILD);
            XdmNode schemaElement = (si.hasNext() ? (XdmNode) si.next() : null);
            String targetNamespace = (schemaElement == null ? null : schemaElement.getAttributeValue(QN_targetNamespace));
            if (targetNamespace != null && processor.getUnderlyingConfiguration().isSchemaAvailable(targetNamespace)) {
                // do nothing
                // TODO: this is the only way I can get MS additional test addB132 to work.
                // This test has two schema documents, A and B; A imports B, and the test catalog
                // requests both A and B to be loaded.
                // It's not ideal: addSchemaSource() ought to be a no-op if the schema components
                // are already loaded, but in fact recompiling the imported schema document on its
                // own is losing the substitution group membership that was defined in the
                // importing document.
            } else {
                try {
                    processor.getSchemaManager().load(schemaDoc.asSource()); // TODO: use an ErrorListener
                } catch (SaxonApiException err) {
                    println(err.getMessage());
                    success = false;
                }
            }
        }
        return success;
    }

    private boolean loadSchemasViaSCM(XPathCompiler xpath, Processor testProcessor, XdmNode schemaTest, boolean success, List<String> scmList)
            throws SaxonApiException, URISyntaxException {
        Processor tempProcessor = new Processor(true);
        tempProcessor.setConfigurationProperty(Feature.XSD_VERSION, versionUnderTest);
        tempProcessor.setConfigurationProperty(Feature.XML_VERSION,
                                               testProcessor.getConfigurationProperty(Feature.XML_VERSION));
        tempProcessor.setConfigurationProperty(Feature.ALLOW_UNRESOLVED_SCHEMA_COMPONENTS, allowDangling);
        EnterpriseConfiguration tempConfig = (EnterpriseConfiguration) tempProcessor.getUnderlyingConfiguration();
        //AutoActivate.activate(tempConfig);
        XPathCompiler tempXPath = tempProcessor.newXPathCompiler();
        for (XdmItem schemaDocRefItem : xpath.evaluate("schemaDocument", schemaTest)) {
            XdmNode schemaDocumentRef = (XdmNode) schemaDocRefItem;
            println("Loading schema at " + schemaDocumentRef.getAttributeValue(QN_xlink_href));
            XdmNode schemaDoc = getLinkedDocument(schemaDocumentRef, tempProcessor, false);
            XdmNode schemaElement =
                    (XdmNode) tempXPath.evaluateSingle("*", schemaDoc);
            String targetNamespace = schemaElement.getAttributeValue(QN_targetNamespace);
            if (targetNamespace != null && tempConfig.isSchemaAvailable(targetNamespace)) {
                // do nothing
            } else {
                try {
                    tempProcessor.getSchemaManager().load(schemaDoc.asSource()); // TODO: use an ErrorListener
                } catch (SaxonApiException err) {
                    return false;
                }
            }
        }
        try {
            StringWriter sw = new StringWriter();
            Result result = new StreamResult(sw);
            SerializationProperties props = new SerializationProperties();
            props.setProperty(OutputKeys.METHOD, "xml");
            props.setProperty(OutputKeys.INDENT, "yes");
            Receiver serializer = tempConfig.getSerializerFactory().getReceiver(result, props);

            tempConfig.exportComponents(serializer);
            String scm = sw.toString();
            try {
                scmValidator.validate(new StreamSource(new StringReader(scm)));
            } catch (SaxonApiException e) {
                System.err.println("*** SCM does not validate against schema for SCM ***");
                System.err.println(scm);
                System.exit(2);
            }
            scmList.add(scm); // used as a return parameter
            try {
                testProcessor.getSchemaManager().importComponents(new StreamSource(new StringReader(scm)));
            } catch (SaxonApiException e) {
                e.printStackTrace();
                System.exit(2);
            }
        } catch (XPathException e) {
            if (!e.hasBeenReported()) {
                System.err.println(e.getMessage());
            }
            return false;
        }
        return success;
    }

    private boolean loadSchemasViaInstanceDocument(XPathCompiler xpath, Processor testProcessor, XdmNode instanceTest, boolean success, List<String> scmList)
            throws SaxonApiException, URISyntaxException {
        Processor tempProcessor = new Processor(true);
        tempProcessor.setConfigurationProperty(Feature.XSD_VERSION, versionUnderTest);
        tempProcessor.setConfigurationProperty(Feature.XML_VERSION,
                                               testProcessor.getConfigurationProperty(Feature.XML_VERSION));
        EnterpriseConfiguration tempConfig = (EnterpriseConfiguration) tempProcessor.getUnderlyingConfiguration();
        //AutoActivate.activate(tempConfig);
        XPathCompiler tempXPath = tempProcessor.newXPathCompiler();
        for (XdmItem schemaDocRefItem : xpath.evaluate("instanceDocument", instanceTest)) {
            XdmNode instanceDocumentRef = (XdmNode) schemaDocRefItem;
            println("Loading instance at " + instanceDocumentRef.getAttributeValue(QN_xlink_href));
            XdmNode instanceDoc = getLinkedDocument(instanceDocumentRef, tempProcessor, false);
            XdmValue schemaUris = tempXPath.evaluate(
                    "//@Q{http://www.w3.org/2001/XMLSchema-instance}noNamespaceSchemaLocation/resolve-uri(., base-uri(..)), "
                            + "//@Q{http://www.w3.org/2001/XMLSchema-instance}schemaLocation/"
                            + "(for $t in tokenize(.)[position() mod 2 = 0] return resolve-uri($t, base-uri(..)))", instanceDoc);
            for (XdmItem uriItem : schemaUris) {
                String uri = uriItem.getStringValue();
                System.err.println("Loading schema from schemaLocation " + uri);
                try {
                    tempProcessor.getSchemaManager().load(new StreamSource(uri));
                } catch (SaxonApiException err) {
                    System.err.println("Failed to load schema: " + err.getMessage());
                    return false;
                }
            }
        }
        try {
            StringWriter sw = new StringWriter();
            Result result = new StreamResult(sw);
            SerializationProperties props = new SerializationProperties();
            props.setProperty(OutputKeys.METHOD, "xml");
            props.setProperty(OutputKeys.INDENT, "yes");
            Receiver serializer = tempConfig.getSerializerFactory().getReceiver(result, props);

            tempConfig.exportComponents(serializer);
            String scm = sw.toString();
            try {
                scmValidator.validate(new StreamSource(new StringReader(scm)));
            } catch (SaxonApiException e) {
                System.err.println("*** SCM does not validate against schema for SCM ***");
                System.err.println(scm);
                System.exit(2);
            }
            scmList.add(scm); // used as a return parameter
            try {
                testProcessor.getSchemaManager().importComponents(new StreamSource(new StringReader(scm)));
            } catch (SaxonApiException e) {
                e.printStackTrace();
                System.exit(2);
            }
        } catch (XPathException e) {
            if (!e.hasBeenReported()) {
                System.err.println(e.getMessage());
            }
            return false;
        }
        return success;
    }

    protected boolean isExcluded(String testName) {
        return false;
    }


    private void writeResultFilePreamble(Configuration config) throws IOException, XPathException, XMLStreamException {
        String rfn = reportFilename;
        if (rfn == null) {
            rfn = "SaxonResults" + Version.getProductVersion() + "-" + versionUnderTest + ".xml";
        }
        if (!rfn.contains(".")) {
            rfn = rfn + ".xml";
        }
        if (!rfn.contains("/")) {
            rfn = localDir + "/" + rfn;
        }
        reportFilename = rfn; // We need this later

        Writer resultWriter = new BufferedWriter(new FileWriter(new File(rfn)));
        Properties resultProperties = new Properties();
        resultProperties.setProperty(OutputKeys.METHOD, "xml");
        resultProperties.setProperty(OutputKeys.INDENT, "yes");
        resultProperties.setProperty(SaxonOutputKeys.LINE_LENGTH, "120");
        results = config.getSerializerFactory().getXMLStreamWriter(
                new StreamResult(resultWriter), resultProperties);

        results.writeStartElement(testNS, "testSuiteResults");
        results.writeDefaultNamespace(testNS);
        results.writeNamespace("saxon", "http://saxon.sf.net/");
        results.writeAttribute("suite", "XS_2006");
        results.writeAttribute("processor", "Saxon " + Version.getProductVariantAndVersion(config.getEditionCode()) + " with xsdVersion=" + versionUnderTest);
        results.writeAttribute("submitDate", new Controller(config).getCurrentDateTime().getStringValue().substring(0, 10));
        results.writeAttribute("publicationPermission", "public");
        results.writeAttribute("xsdVersion", versionUnderTest);
    }

    private void writeResultFilePostamble() throws IOException, XMLStreamException {
        results.writeEndElement();
        results.close();
    }

    private void generateFailureReport() throws SaxonApiException, FileNotFoundException {
        Processor processor = new Processor(true);
        processor.setConfigurationProperty(Feature.GENERATE_BYTE_CODE, false); //TODO: temporary
        XsltCompiler c = processor.newXsltCompiler();

        String xslfn = summarizeResults;
        if (xslfn == null) {
            xslfn = "summarize-results.xsl";
        }
        if (!xslfn.contains("/")) {
            xslfn = localDir + "/" + xslfn;
        }
        XsltTransformer t = c.compile(new StreamSource(new File(xslfn))).load();

        String errfn = errorReportFilename;
        if (errfn == null) {
            errfn = "SaxonFailures" + Version.getProductVersion() + ".xml";
        }
        if (!errfn.contains("/")) {
            errfn = localDir + "/" + errfn;
        }

        Serializer ser = processor.newSerializer(new FileOutputStream(errfn));
        XdmNode resultDoc = processor.newDocumentBuilder().build(new File(reportFilename));
        t.setParameter(new QName("in"), resultDoc);
        t.setInitialTemplate(new QName("all-diffs"));
        t.setDestination(ser);
        t.transform();
    }
}

// Copyright (c) 2011-2022 Saxonica Limited
