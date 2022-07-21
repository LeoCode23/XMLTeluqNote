using System;
using System.IO;
using System.Xml;
using System.Net;
using Saxon.Api;
using System.Collections.Generic;
using System.Linq;
using ExtensionFunctionCall = Saxon.Api.ExtensionFunctionCall;
using ExtensionFunctionDefinition = Saxon.Api.ExtensionFunctionDefinition;

namespace SaxonEE
{
    class Examples
    {
        /// <summary>
        /// Run Saxon XSLT and XQuery sample applications in Saxon Enterprise Edition on .NET
        /// </summary>
        /// <param name="argv">
        /// <para>Options:</para>
        /// <list>
        /// <item>-test:testname  run a specific test</item>
        /// <item>-dir:samplesdir directory containing the sample data files (default %SAXON_HOME%/samples)</item>
        /// <item>-ask:yes|no     indicates whether to prompt for confirmation after each test (default yes)</item>
        /// </list>
        /// </param>


        public static void Main(string[] argv)
        {
            Example[] examples = {
                new XdmNavigation(),
                new XPathSimple(),
                new XPathSimple2(),
                new XPathVariables(),
                new XPathUndeclaredVariables(),
                new XPathWithStaticError(),
                new XPathWithDynamicError(),
                new XsltSimple1(),
                new XsltSimple2(),
                new XsltSimple3(),
                new XsltStripSpace(),
                new XsltReuseExecutable(),
                new XsltReuseTransformer(),
                new XsltFilterChain(),
                new XsltDomToDom(),
                new XsltXdmToXdm(),
                new XsltXdmElementToXdm(),
                new XsltUsingSourceResolver(),
                new XsltSettingOutputProperties(),
                new XsltDisplayingErrors(),
                new XsltCapturingErrors(),
                new XsltCapturingMessages(),
                new XsltProcessingInstruction(),
                new XsltShowingLineNumbers(),
                new XsltStreamDoc(),
                new XsltMultipleOutput(),
                new XsltUsingResultHandler(),
                new XsltUsingIdFunction(),
                new XsltUsingCollectionFinder(),
                new XsltUsingDirectoryCollection(),
                new XsltIntegratedExtension(),
                new XsltSimpleExtension(),
                new XQueryToStream(),
                new XQueryToAtomicValue(),
                new XQueryToSequence(),
                new XQueryToDom(),
                new XQueryToXdm(),
                new XQueryCallFunction(),
                new XQueryFromXmlReader(),
                new XQueryToSerializedSequence(),
                new XQueryUsingParameter(),
                new XQueryMultiModule(),
                new XQueryTryCatch(),
                new XQueryExtensibility(),
                new XQueryUpdate(),
                new XQuerySchemaAware(),
                new XPathSchemaAware(),
                new Validate()
            };

            bool ask = true;
            string test = "all";

            string samplesPath = null;
            Uri samplesDir;

            foreach (String s in argv)
            {
                if (s.StartsWith("-test:"))
                {
                    test = s[6..];
                }
                else if (s.StartsWith("-dir:"))
                {
                    samplesPath = s[5..];
                }
                else if (s == "-ask:yes")
                {
                    // no action
                }
                else if (s == "-ask:no")
                {
                    ask = false;
                }
                else if (s == "-?")
                {
                    Console.WriteLine("ExamplesEE -dir:samples -test:testname -ask:yes|no");
                }
                else
                {
                    Console.WriteLine("Unrecognized Argument: " + s);
                    return;
                }
            }
            if (samplesPath != null)
            {
                if (samplesPath.StartsWith("file:///"))
                {
                    samplesPath = samplesPath[8..];
                }
                else if (samplesPath.StartsWith("file:/"))
                {
                    samplesPath = samplesPath[6..];
                }

            }
            else
            {
                string home = Environment.GetEnvironmentVariable("SAXON_HOME");
                if (home == null)
                {
                    Console.WriteLine("No input directory supplied, and SAXON_HOME is not set");
                    return;
                }
                else
                {
                    if (!(home.EndsWith("/") || home.EndsWith("\\")))
                    {
                        home = home + "/";
                    }
                    samplesPath = home + "samples/";
                }
            }

            if (!(samplesPath.EndsWith("/") || samplesPath.EndsWith("\\")))
            {
                samplesPath = samplesPath + "/";
            }

            if (!File.Exists(samplesPath + "data/books.xml"))
            {
                Console.WriteLine("Supplied samples directory " + samplesPath + " does not contain the Saxon sample data files");
                return;
            }

            try
            {
                samplesDir = new Uri(samplesPath);
            }
            catch
            {
                Console.WriteLine("Invalid URI for samples directory: " + samplesPath);
                return;
            }

            Boolean found = false;
            foreach (Example ex in examples)
            {
                if (test == "all" || test == ex.testName)
                {
                    Console.WriteLine("\n\n===== " + ex.testName + " =======\n");
                    found = true;
                    try
                    {
                        ex.run(samplesDir);
                    }
                    catch (Saxon.Api.SaxonApiException se)
                    {
                        Console.WriteLine("Test failed with error " + (se.ErrorCode!= null ? se.ErrorCode.LocalName : "") + ": " + se.Message);
                    }
                    catch (Exception exc)
                    {
                        Console.WriteLine("Test failed unexpectedly (" + exc.GetType() + "): " + exc.Message);
                        Console.WriteLine(exc.StackTrace);
                    }
                    if (ask) {
                        Console.WriteLine("\n\nContinue? - type (Y(es)/N(o)/A(ll))");
                        string answer = Console.ReadLine();
                        if (answer is "N" or "n")
                        {
                            break;
                        }
                        else if (answer is "A" or "a")
                        {
                            ask = false;
                        }
                    }
                }
            }
            if (!found) {
                Console.WriteLine("Please supply a valid test name, or 'all' ('" + test + "' is invalid)");
            }
            Console.WriteLine("\n==== done! ====");
        }
    }

    ///<summary>
    /// Each of the example programs is implemented as a subclass of the abstract class Example
    ///</summary> 


    public abstract class Example
    {
        /// <summary>
        /// Read-only property: the name of the test example
        /// </summary>
        public abstract string testName { get; }
        /// <summary>
        /// Entry point for running the example
        /// </summary>
        public abstract void run(Uri samplesDir);
    }
    

    /// <summary>
    /// Build an XDM Document and navigate it directly, without using XPath
    /// </summary>

    public class XdmNavigation : Example
    {

        public override string testName => "XdmNavigation";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Navigate to the outermost element
            XdmNode outermost = input.Select(Steps.Child("*")).First();
            
            // Check its name
            if (!outermost.NodeName.Equals(new QName("BOOKLIST"))) {
                throw new InvalidDataException();
            }
            
            // Find the categories
            if (outermost.Children("CATEGORIES").First().Children("CATEGORY").Count() != 3) {
                throw new InvalidDataException();
            }
            
            // Count the ITEM descendants
            if (outermost.Select(Steps.Descendant("ITEM")).Count() != 6) {
                throw new InvalidDataException();
            }
            
            // Get the average price 
            XdmValue prices =
                new XdmValue(outermost.Select(Steps.Descendant("PRICE"))).Atomize();
            XdmFunctionItem average = processor.GetSystemFunction(new QName(NamespaceConstant.FN, "avg"), 1);
            XdmValue overallAverage = average.Invoke(new XdmValue[] { prices }, processor);
            Console.WriteLine("Average book price for all books = " + overallAverage[0].StringValue);
            
            // Get the average price of books by Thomas Hardy
            XdmValue selectedPrices =
                new XdmValue(outermost.Select(
                    Steps.Descendant("ITEM")
                        .Where(Predicates.Eq(Steps.Child("AUTHOR"), "Thomas Hardy"))
                        .Then(Steps.Child("PRICE"))));
            XdmValue authorAverage = average.Invoke(new XdmValue[] { selectedPrices.Atomize() }, processor);
            Console.WriteLine("Average book price for Hardy's books = " + authorAverage[0].StringValue);

        }
    }

    /// <summary>
    /// Evaluate an XPath expression selecting from a source document supplied as a URI
    /// </summary>

    public class XPathSimple : Example
    {

        public override string testName => "XPathSimple";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Create an XPath compiler
            XPathCompiler xpath = processor.NewXPathCompiler();

            // Enable caching, so each expression is only compiled once
            xpath.Caching = true;

            // Compile and evaluate some XPath expressions
            foreach (XdmItem item in xpath.Evaluate("//ITEM", input))
            {
                Console.WriteLine("TITLE: " + xpath.EvaluateSingle("string(TITLE)", item));
                Console.WriteLine("PRICE: " + xpath.EvaluateSingle("string(PRICE)", item));
            }
        }
    }

    /// <summary>
    /// Evaluate an XPath expression against a source document, returning its effective boolean value
    /// </summary>

    public class XPathSimple2 : Example
    {

        public override string testName => "XPathSimple2";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Create an XPath compiler
            XPathCompiler xpath = processor.NewXPathCompiler();

            // Enable caching, so each expression is only compiled once
            xpath.Caching = true;
            
            // Compile and evaluate an XPath expression
            XPathSelector selector = xpath.Compile("//ITEM").Load();
            selector.ContextItem = input;
            Console.WriteLine(selector.EffectiveBooleanValue());

        }
    }

    /// <summary>
    /// Evaluate an XPath expression using variables (and no source document)
    /// </summary>

    public class XPathVariables : Example
    {

        public override string testName => "XPathVariables";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Create the XPath expression.
            XPathCompiler compiler = processor.NewXPathCompiler();
            compiler.DeclareVariable(new QName("", "a"));
            compiler.DeclareVariable(new QName("", "b"));
            XPathSelector selector = compiler.Compile("$a + $b").Load();

            // Set the values of the variables
            selector.SetVariable(new QName("", "a"), new XdmAtomicValue(2));
            selector.SetVariable(new QName("", "b"), new XdmAtomicValue(3));

            // Evaluate the XPath expression
            Console.WriteLine(selector.EvaluateSingle().ToString());
        }
    }

    /// <summary>
    /// Evaluate an XPath expression using variables without explicit declaration
    /// </summary>

    public class XPathUndeclaredVariables : Example
    {

        public override string testName => "XPathUndeclaredVariables";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Create the XPath expression.
            XPathCompiler compiler = processor.NewXPathCompiler();
            compiler.AllowUndeclaredVariables = true;
            XPathExecutable expression = compiler.Compile("$a + $b");
            XPathSelector selector = expression.Load();

            // Set the values of the variables
            foreach(QName var in expression.ExternalVariables) {
                selector.SetVariable(var, new XdmAtomicValue(10));
            }

            // Evaluate the XPath expression
            Console.WriteLine(selector.EvaluateSingle().ToString());
        }
    }

    /// <summary>
    /// Evaluate an XPath expression throwing a static error
    /// </summary>

    public class XPathWithStaticError : Example
    {

        public override string testName => "XPathWithStaticError";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Create the XPath expression.
            XPathCompiler compiler = processor.NewXPathCompiler();
            compiler.AllowUndeclaredVariables = true;
            XPathExecutable expression = compiler.Compile("1 + unknown()");
            XPathSelector selector = expression.Load();

            // Evaluate the XPath expression
            Console.WriteLine(selector.EvaluateSingle().ToString());
        }
    }

    /// <summary>
    /// Evaluate an XPath expression throwing a dynamic error
    /// </summary>

    public class XPathWithDynamicError : Example
    {

        public override string testName => "XPathWithDynamicError";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Create the XPath expression.
            XPathCompiler compiler = processor.NewXPathCompiler();
            compiler.AllowUndeclaredVariables = true;
            XPathExecutable expression = compiler.Compile("$a gt $b");
            XPathSelector selector = expression.Load();

            // Set the values of the variables
            selector.SetVariable(new QName("", "a"), new XdmAtomicValue(10));
            selector.SetVariable(new QName("", "b"), new XdmAtomicValue("Paris"));

            // Evaluate the XPath expression
            Console.WriteLine(selector.EvaluateSingle().ToString());
        }
    }

    /// <summary>
    /// XSLT 2.0 transformation with source document and stylesheet supplied as URIs
    /// </summary>

    public class XsltSimple1 : Example
    {

        public override string testName => "XsltSimple1";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = processor.NewXsltCompiler().Compile(new Uri(samplesDir, "styles/books.xsl")).Load30();

            // Set the root node of the source document to be the global context item
            transformer.GlobalContextItem = input;

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.OutputWriter = Console.Out;

            // Transform the source XML and serialize the result document
            transformer.ApplyTemplates(input, serializer);
        }
    }

    /// <summary>
    /// Run a transformation, sending the serialized output to a file
    /// </summary>

    public class XsltSimple2 : Example
    {

        public override string testName => "XsltSimple2";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = processor.NewXsltCompiler().Compile(new Uri(samplesDir, "styles/identity.xsl")).Load30();

            // Create a serializer
            const string outfile = "OutputFromXsltSimple2.xml";
            Serializer serializer = processor.NewSerializer();
            serializer.OutputStream = new FileStream(outfile, FileMode.Create, FileAccess.Write);

            // Transform the source XML and serialize the result to the output file.
            transformer.ApplyTemplates(input, serializer);

            Console.WriteLine("\nOutput written to " + outfile + "\n");
        }
    }

    /// <summary>
    /// XSLT 2.0 transformation with source document and stylesheet supplied as files
    /// </summary>

    public class XsltSimple3 : Example
    {

        public override string testName => "XsltSimple3";

        public override void run(Uri samplesDir)
        {
            if (samplesDir.Scheme != Uri.UriSchemeFile)
            {
                Console.WriteLine("Supplied URI must be a file directory");
            }
            string dir = samplesDir.AbsolutePath;
            string sourceFile = dir + "data/books.xml";
            string styleFile = dir + "styles/books.xsl";

            // Create a Processor instance.
            Processor processor = new();

            // Load the source document
            DocumentBuilder builder = processor.NewDocumentBuilder();
            builder.BaseUri = new Uri(samplesDir, "data/books.xml");

            XdmNode input = builder.Build(File.OpenRead(sourceFile));

            // Create a transformer for the stylesheet.
            XsltCompiler compiler = processor.NewXsltCompiler();
            compiler.BaseUri = new Uri(samplesDir, "styles/books.xsl");
            Xslt30Transformer transformer = compiler.Compile(File.OpenRead(styleFile)).Load30();

            // Set the root node of the source document to be the global context item
            transformer.GlobalContextItem = input;

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.OutputWriter = Console.Out;

            // Transform the source XML and serialize the result document
            transformer.ApplyTemplates(input, serializer);
        }
    }


    /// <summary>
    /// XSLT 2.0 transformation showing stripping of whitespace controlled by the stylesheet
    /// </summary>

    public class XsltStripSpace : Example
    {

        public override string testName => "XsltStripSpace";

        public override void run(Uri samplesDir)
        {
            Processor processor = new();

            // Load the source document
            DocumentBuilder builder = processor.NewDocumentBuilder();
            builder.BaseUri = samplesDir;

            String doc = "<doc>  <a>  <b>text</b>  </a>  <a/>  </doc>";
            MemoryStream ms = new();
            StreamWriter tw = new(ms);
            tw.Write(doc);
            tw.Flush();
            Stream instr = new MemoryStream(ms.GetBuffer(), 0, (int)ms.Length);
            XdmNode input = builder.Build(instr);

            // Create a transformer for the stylesheet.
            String stylesheet =
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>" +
                "<xsl:strip-space elements='*'/>" +
                "<xsl:template match='/'>" +
                "  <xsl:copy-of select='.'/>" +
                "</xsl:template>" +
                "</xsl:stylesheet>";

            XsltCompiler compiler = processor.NewXsltCompiler();
            compiler.BaseUri = samplesDir;
            Xslt30Transformer transformer = compiler.Compile(new XmlTextReader(new StringReader(stylesheet))).Load30();

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.OutputWriter = Console.Out;

            // Transform the source XML and serialize the result document
            transformer.ApplyTemplates(input, serializer);
        }
    }


    /// <summary>
    /// Run a transformation, compiling the stylesheet once (into an XsltExecutable) and using it to transform two 
    /// different source documents
    /// </summary>

    public class XsltReuseExecutable : Example
    {

        public override string testName => "XsltReuseExecutable";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Create a compiled stylesheet
            XsltExecutable templates = processor.NewXsltCompiler().Compile(new Uri(samplesDir, "styles/summarize.xsl"));

            // Note: we could actually use the same Xslt30Transformer in this case.
            // But in principle, the two transformations could be done in parallel in separate threads.

            const string sourceFile1 = "data/books.xml";
            const string sourceFile2 = "data/othello.xml";

            // Do the first transformation
            Console.WriteLine("\n\n----- transform of " + sourceFile1 + " -----");
            Xslt30Transformer transformer1 = templates.Load30();
            XdmNode input1 = processor.NewDocumentBuilder().Build(new Uri(samplesDir, sourceFile1));
            transformer1.ApplyTemplates(input1, processor.NewSerializer(Console.Out));     // default destination is Console.Out

            // Do the second transformation
            Console.WriteLine("\n\n----- transform of " + sourceFile2 + " -----");
            Xslt30Transformer transformer2 = templates.Load30();
            XdmNode input2 = processor.NewDocumentBuilder().Build(new Uri(samplesDir, sourceFile2));
            transformer2.ApplyTemplates(input2, processor.NewSerializer(Console.Out));     // default destination is Console.Out
        }
    }

    /// <summary>
    /// Show that the Xslt30Transformer is serially reusable; run a transformation twice using the same stylesheet
    /// and the same stylesheet parameters, but with a different input document.
    /// </summary>

    public class XsltReuseTransformer : Example
    {

        public override string testName => "XsltReuseTransformer";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Compile the stylesheet
            XsltExecutable exec = processor.NewXsltCompiler().Compile(new Uri(samplesDir, "styles/summarize.xsl"));

            // Create a transformer 
            Xslt30Transformer transformer = exec.Load30();
            
            // Set the stylesheet parameters
            Dictionary<QName, XdmValue> params1 = new Dictionary<QName, XdmValue>();
            params1.Add(new QName("", "", "include-attributes"), new XdmAtomicValue(false));
            transformer.SetStylesheetParameters(params1);

            // Load the 1st source document, building a tree
            XdmNode input1 = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Run the transformer once
            XdmDestination results = new();
            transformer.ApplyTemplates(input1, results);
            Console.WriteLine("1: " + results.XdmNode.OuterXml);

            // Load the 2nd source document, building a tree
            XdmNode input2 = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/more-books.xml"));

            // Run the transformer again
            results.Reset();
            transformer.ApplyTemplates(input2, results);
            Console.WriteLine("2: " + results.XdmNode.OuterXml);
        }
    }

    /// <summary>
    /// Run a sequence of transformations in a pipeline, each one acting as a filter
    /// </summary>

    public class XsltFilterChain : Example
    {

        public override string testName => "XsltFilterChain";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Create a compiler
            XsltCompiler compiler = processor.NewXsltCompiler();

            // Compile all three stylesheets
            Xslt30Transformer transformer1 = compiler.Compile(new Uri(samplesDir, "styles/identity.xsl")).Load30();
            Xslt30Transformer transformer2 = compiler.Compile(new Uri(samplesDir, "styles/books.xsl")).Load30();
            Xslt30Transformer transformer3 = compiler.Compile(new Uri(samplesDir, "styles/summarize.xsl")).Load30();

            // Now run them in series
            XdmDestination results1 = new();
            transformer1.ApplyTemplates(input, results1);
            //Console.WriteLine("After phase 1:");
            //Console.WriteLine(results1.XdmNode.OuterXml);

            XdmDestination results2 = new();
            transformer2.GlobalContextItem = results1.XdmNode;
            transformer2.ApplyTemplates(results1.XdmNode, results2);
            //Console.WriteLine("After phase 2:");
            //Console.WriteLine(results2.XdmNode.OuterXml);

            XdmDestination results3 = new();
            transformer3.ApplyTemplates(results2.XdmNode, results3);
            Console.WriteLine("After phase 3:");
            Console.WriteLine(results3.XdmNode.OuterXml);
        }
    }

    /// <summary>
    /// Transform from an XDM tree to an XDM tree
    /// </summary>

    public class XsltXdmToXdm : Example
    {

        public override string testName => "XsltXdmToXdm";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Create a compiler
            XsltCompiler compiler = processor.NewXsltCompiler();

            // Compile the stylesheet
            Xslt30Transformer transformer = compiler.Compile(new Uri(samplesDir, "styles/summarize.xsl")).Load30();

            // Run the transformation
            XdmDestination result = new();
            transformer.ApplyTemplates(input, result);

            // Serialize the result so we can see that it worked
            StringWriter sw = new();
            result.XdmNode.WriteTo(new XmlTextWriter(sw));
            Console.WriteLine(sw.ToString());

            // Note: we don't do 
            //   result.XdmNode.WriteTo(new XmlTextWriter(Console.Out));
            // because that results in the Console.out stream being closed, 
            // with subsequent attempts to write to it being rejected.
        }
    }

    /// <summary>
    /// Run an XSLT transformation from an XDM tree, starting at a node that is not the document node
    /// </summary>

    public class XsltXdmElementToXdm : Example
    {

        public override string testName => "XsltXdmElementToXdm";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/othello.xml"));

            // Navigate to the first grandchild
            XPathSelector eval = processor.NewXPathCompiler().Compile("/PLAY/FM[1]").Load();
            eval.ContextItem = input;
            input = (XdmNode)eval.EvaluateSingle();

            // Create an XSLT compiler
            XsltCompiler compiler = processor.NewXsltCompiler();

            // Compile the stylesheet
            Xslt30Transformer transformer = compiler.Compile(new Uri(samplesDir, "styles/summarize.xsl")).Load30();

            // Run the transformation
            XdmDestination result = new();
            transformer.ApplyTemplates(input, result);

            // Serialize the result so we can see that it worked
            Console.WriteLine(result.XdmNode.OuterXml);
        }
    }

    /// <summary>
    /// Run a transformation from a DOM (System.Xml.Document) input to a DOM output
    /// </summary>

    public class XsltDomToDom : Example
    {

        public override string testName => "XsltDomToDom";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Load the source document (in practice, it would already exist as a DOM)
            XmlDocument doc = new();
            doc.Load(new XmlTextReader(samplesDir.AbsolutePath + "data/othello.xml"));
            XdmNode input = processor.NewDocumentBuilder().Wrap(doc);

            // Create a compiler
            XsltCompiler compiler = processor.NewXsltCompiler();

            // Compile the stylesheet
            Xslt30Transformer transformer = compiler.Compile(new Uri(samplesDir, "styles/summarize.xsl")).Load30();

            // Run the transformation
            DomDestination result = new();
            transformer.ApplyTemplates(input, result);

            // Serialize the result so we can see that it worked
            Console.WriteLine(result.XmlDocument.OuterXml);
        }
    }


    /// <summary>
    /// Run a transformation driven by an xml-stylesheet processing instruction in the source document
    /// </summary>

    public class XsltProcessingInstruction : Example
    {

        public override string testName => "XsltProcessingInstruction";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();
            XsltExecutable exec;

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));
            //Console.WriteLine("=============== source document ===============");
            //Console.WriteLine(input.OuterXml);
            //Console.WriteLine("=========== end of source document ============");

            // Navigate to the xml-stylesheet processing instruction having the pseudo-attribute type=text/xsl;
            // then extract the value of the href pseudo-attribute if present

            const string path = @"/processing-instruction(xml-stylesheet)[matches(.,'type\s*=\s*[''""]text/xsl[''""]')]" +
                                @"/replace(., '.*?href\s*=\s*[''""](.*?)[''""].*', '$1')";

            XPathSelector eval = processor.NewXPathCompiler().Compile(path).Load();
            eval.ContextItem = input;
            XdmAtomicValue hrefval = (XdmAtomicValue)eval.EvaluateSingle();
            string href = hrefval?.ToString();

            if (string.IsNullOrEmpty(href))
            {
                Console.WriteLine("No suitable xml-stylesheet processing instruction found");
                return;

            }
            else if (href[0] == '#')
            {

                // The stylesheet is embedded in the source document and identified by a URI of the form "#id"

                Console.WriteLine("Locating embedded stylesheet with href = " + href);
                string idpath = "id('" + href[1..] + "')";
                eval = processor.NewXPathCompiler().Compile(idpath).Load();
                eval.ContextItem = input;
                XdmNode node = (XdmNode)eval.EvaluateSingle();
                if (node == null) {
                    Console.WriteLine("No element found with ID " + href[1..]);
                    return;
                }
                exec = processor.NewXsltCompiler().Compile(node);

            }
            else
            {

                // The stylesheet is in an external document

                Console.WriteLine("Locating stylesheet at uri = " + new Uri(input.BaseUri, href));

                // Fetch and compile the referenced stylesheet
                exec = processor.NewXsltCompiler().Compile(new Uri(input.BaseUri, href));
            }

            // Create a transformer 
            Xslt30Transformer transformer = exec.Load30();

            // Set the root node of the source document to be the global context item
            transformer.GlobalContextItem = input;

            // Run it       
            XdmDestination results = new();
            transformer.ApplyTemplates(input, results);
            Console.WriteLine(results.XdmNode.OuterXml);

        }
    }

    /// <summary>
    /// Run an XSLT transformation setting serialization properties from the calling application
    /// </summary>

    public class XsltSettingOutputProperties : Example
    {

        public override string testName => "XsltSettingOutputProperties";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = processor.NewXsltCompiler().Compile(new Uri(samplesDir, "styles/summarize.xsl")).Load30();

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.SetOutputProperty(Serializer.METHOD, "xml");
            serializer.SetOutputProperty(Serializer.INDENT, "no");
            serializer.OutputWriter = Console.Out;

            // Transform the source XML and serialize the result document
            transformer.ApplyTemplates(input, serializer);
        }

    }

    /// <summary>
    /// Run an XSLT transformation making use of an XmlResolver to resolve URIs at document build time, at stylesheet compile time 
    /// and at transformation run-time
    /// </summary>

    public class XsltUsingSourceResolver : Example
    {

        public override string testName => "XsltUsingSourceResolver";

        public override void run(Uri samplesDir)
        {

            // Create a Processor instance.
            Processor processor = new();

            // Load the source document
            DocumentBuilder builder = processor.NewDocumentBuilder();
            UserXmlResolver buildTimeResolver = new();
            buildTimeResolver.Message = "** Calling build-time XmlResolver: ";
            builder.XmlDocumentResolver = buildTimeResolver.GetResourceResolver();
            builder.BaseUri = samplesDir;

            String doc = "<!DOCTYPE doc [<!ENTITY e SYSTEM 'flamingo.txt'>]><doc>&e;</doc>";
            MemoryStream ms = new();
            StreamWriter tw = new(ms);
            tw.Write(doc);
            tw.Flush();
            Stream instr = new MemoryStream(ms.GetBuffer(), 0, (int)ms.Length);
            XdmNode input = builder.Build(instr);

            // Create a transformer for the stylesheet.
            const string stylesheet = "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>" +
                                      "<xsl:import href='empty.xslt'/>" +
                                      "<xsl:template match='/'>" +
                                      "<out note=\"{doc('heron.txt')}\" ><xsl:copy-of select='.'/></out>" +
                                      "</xsl:template>" +
                                      "</xsl:stylesheet>";

            XsltCompiler compiler = processor.NewXsltCompiler();
            UserXmlResolver compileTimeResolver = new() {Message = "** Calling compile-time XmlResolver: "};
            compiler.StylesheetModuleResolver = compileTimeResolver.GetResourceResolver();
            compiler.BaseUri = samplesDir;
            Xslt30Transformer transformer = compiler.Compile(new XmlTextReader(new StringReader(stylesheet))).Load30();

            // Set the user-written XmlResolver
            UserXmlResolver runTimeResolver = new() {Message = "** Calling transformation-time XmlResolver: "};
            transformer.XmlDocumentResolver = runTimeResolver.GetResourceResolver();

            // Create a serializer
            Serializer serializer = processor.NewSerializer();
            serializer.OutputWriter = Console.Out;

            // Transform the source XML and serialize the result document
            transformer.ApplyTemplates(input, serializer);

        }
    }

    /// <summary>
    /// Run an XSLT transformation displaying compile-time errors to the console
    /// </summary>

    public class XsltDisplayingErrors : Example
    {

        public override string testName => "XsltDisplayingErrors";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Create the XSLT Compiler
            XsltCompiler compiler = processor.NewXsltCompiler();


            // Define a stylesheet containing errors
            const string stylesheet = "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>\n" +
                                      "<xsl:template name='eee:template'>\n" +
                                      "  <xsl:value-of select='32'/>\n" +
                                      "</xsl:template>\n" +
                                      "<xsl:template name='main'>\n" +
                                      "  <xsl:value-of select='$var'/>\n" +
                                      "</xsl:template>\n" +
                                      "</xsl:stylesheet>";


            // Attempt to compile the stylesheet and display the errors
            try
            {
                compiler.BaseUri = new Uri("http://localhost/stylesheet");
                compiler.Compile(new XmlTextReader(new StringReader(stylesheet)));
                Console.WriteLine("Stylesheet compilation succeeded");
            }
            catch (Exception)
            {
                Console.WriteLine("Stylesheet compilation failed");
            }


        }
    }

    /// <summary>
    /// Run an XSLT transformation capturing compile-time errors within the application
    /// </summary>

    public class XsltCapturingErrors : Example
    {

        public override string testName => "XsltCapturingErrors";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Create the XSLT Compiler
            XsltCompiler compiler = processor.NewXsltCompiler();

            // Create a list to hold the error information
            List<Error> errorList = new();
            compiler.ErrorReporter = error => errorList.Add(error);

            // Define a stylesheet containing errors
            string stylesheet =
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>\n" +
                "<xsl:template name='fff:template'>\n" +
                "  <xsl:value-of select='32'/>\n" +
                "</xsl:template>\n" +
                "<xsl:template name='main'>\n" +
                "  <xsl:value-of select='$var'/>\n" +
                "</xsl:template>\n" +
                "</xsl:stylesheet>";


            // Attempt to compile the stylesheet and display the errors
            try
            {
                compiler.BaseUri = new Uri("http://localhost/stylesheet");
                compiler.Compile(new StringReader(stylesheet));
                Console.WriteLine("Stylesheet compilation succeeded");
            }
            catch (Exception)
            {
                Console.WriteLine("Stylesheet compilation failed with " + errorList.Count + " errors");
                foreach (Error error in errorList) {
                    Console.WriteLine("At line " + error.Location.LineNumber + ": " + error.Message);
                }
            }
        }
    }

    /// <summary>
    /// Run an XSLT transformation capturing run-time messages within the application
    /// </summary>

    public class XsltCapturingMessages : Example
    {

        public override string testName => "XsltCapturingMessages";

        public override void run(Uri samplesDir)
        {

            // Create a Processor instance.
            Processor processor = new();

            // Create the XSLT Compiler
            XsltCompiler compiler = processor.NewXsltCompiler();

            // Define a stylesheet that generates messages
            const string stylesheet = "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>\n" +
                                      "<xsl:template name='main'>\n" +
                                      "  <xsl:message><a>starting</a></xsl:message>\n" +
                                      "  <out><xsl:value-of select='current-date()'/></out>\n" +
                                      "  <xsl:message><a>finishing</a></xsl:message>\n" +
                                      "</xsl:template>\n" +
                                      "</xsl:stylesheet>";

            compiler.BaseUri = new Uri("http://localhost/stylesheet");
            XsltExecutable exec = compiler.Compile(new StringReader(stylesheet));


            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = exec.Load30();

            // Create a Listener to which messages will be written
            transformer.MessageListener = message => {
                Console.Out.WriteLine("MESSAGE terminate=" + (message.Terminate ? "yes" : "no") + " at " + DateTime.Now);
                Console.Out.WriteLine("From instruction at line " + message.Location.LineNumber +
                                      " of " + message.Location.SystemId);
                Console.Out.WriteLine(">>" + message.Content.StringValue);
            };

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.OutputWriter = Console.Out;

            // Transform the source XML, calling a named initial template, and serialize the result document
            transformer.CallTemplate(new QName("", "main"), serializer);
        }

    }
    

    /// <summary>
    /// Run an XSLT transformation showing source line numbers
    /// </summary>

    public class XsltShowingLineNumbers : Example
    {

        public override string testName => "XsltShowingLineNumbers";

        public override void run(Uri samplesDir)
        {

            // Create a Processor instance.
            Processor processor = new(true);
            
            // Load the source document
            DocumentBuilder builder = processor.NewDocumentBuilder();
            builder.LineNumbering = true;
            XdmNode input = builder.Build(new Uri(samplesDir, "data/othello.xml"));

            // Create the XSLT Compiler
            XsltCompiler compiler = processor.NewXsltCompiler();

            // Define a stylesheet that shows line numbers of source elements
            const string stylesheet = "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0' xmlns:saxon='http://saxon.sf.net/'>\n" +
                                      "<xsl:template match='/'>\n" +
                                      "<out>\n" +
                                      "  <xsl:for-each select='//ACT'>\n" +
                                      "  <out><xsl:value-of select='saxon:line-number(.)'/></out>\n" +
                                      "  </xsl:for-each>\n" +
                                      "</out>\n" +
                                      "</xsl:template>\n" +
                                      "</xsl:stylesheet>";

            compiler.BaseUri = new Uri("http://localhost/stylesheet");
            XsltExecutable exec = compiler.Compile(new StringReader(stylesheet));


            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = exec.Load30();

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.OutputWriter = Console.Out;

            // Transform the source XML and serialize the result document
            transformer.ApplyTemplates(input, serializer);
        }

    }

    /// <summary>
    /// Run an XSLT transformation producing multiple output documents
    /// </summary>

    public class XsltMultipleOutput : Example
    {

        public override string testName => "XsltMultipleOutput";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();
            processor.SetProperty(Saxon.Api.Feature.TIMING, true);

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/othello.xml"));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = processor.NewXsltCompiler().Compile(new Uri(samplesDir, "styles/play.xsl")).Load30();

            // Set the required stylesheet parameter
            Dictionary<QName, XdmValue> parameters = new() {
                {new QName("dir"), new XdmAtomicValue(samplesDir + "play")}
            };
            transformer.SetStylesheetParameters(parameters);

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.OutputWriter = Console.Out;

            // Transform the source XML and serialize the result document
            transformer.ApplyTemplates(input, serializer);

        }

    }


    /// <summary>
    /// Run an XSLT transformation using the id() function, with DTD validation
    /// </summary>

    public class XsltUsingIdFunction : Example
    {

        public override string testName => "XsltUsingIdFunction";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance
            Processor processor = new Processor();

            // Load the source document. The Microsoft .NET parser does not report attributes of type ID. The only
            // way to use the function is therefore (a) to use a different parser, or (b) to use xml:id. We
            // choose the latter course.

            const string doc = "<!DOCTYPE table [" +
                               "<!ELEMENT table (row*)>" +
                               "<!ELEMENT row EMPTY>" +
                               "<!ATTLIST row xml:id ID #REQUIRED>" +
                               "<!ATTLIST row value CDATA #REQUIRED>]>" +
                               "<table><row xml:id='A123' value='green'/><row xml:id='Z789' value='blue'/></table>";

            DocumentBuilder builder = processor.NewDocumentBuilder();
            builder.DtdValidation = true;
            builder.BaseUri = samplesDir;
            XdmNode input = builder.Build(new StringReader(doc));

            // Define a stylesheet that uses the id() function
            const string stylesheet = "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>\n" +
                                      "<xsl:template match='/'>\n" +
                                      "  <xsl:copy-of select=\"id('Z789')\"/>\n" +
                                      "</xsl:template>\n" +
                                      "</xsl:stylesheet>";

            XsltCompiler compiler = processor.NewXsltCompiler();
            compiler.BaseUri = new Uri("http://localhost/stylesheet");
            XsltExecutable exec = compiler.Compile(new StringReader(stylesheet));

            // Create a transformer for the stylesheet
            Xslt30Transformer transformer = exec.Load30();

            // Set the destination
            XdmDestination results = new();

            // Transform the XML
            transformer.ApplyTemplates(input, results);

            // Show the result
            Console.WriteLine(results.XdmNode.ToString());

        }

    }

    /// <summary>
    /// Show a transformation using a user-written result document handler. This example
    /// captures each of the result documents in a DOM, and creates a Hashtable that indexes
    /// the DOM trees according to their absolute URI. On completion, it writes all the DOMs
    /// to the standard output.
    /// </summary>

    public class XsltUsingResultHandler : Example
    {

        public override string testName => "XsltUsingResultHandler";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/othello.xml"));

            // Define a stylesheet that splits the document up
            const string stylesheet = "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>\n" +
                                      "<xsl:template match='/'>\n" +
                                      "  <xsl:for-each select='//ACT'>\n" +
                                      "    <xsl:result-document href='{position()}.xml'>\n" +
                                      "      <xsl:copy-of select='TITLE'/>\n" +
                                      "    </xsl:result-document>\n" +
                                      "  </xsl:for-each>\n" +
                                      "</xsl:template>\n" +
                                      "</xsl:stylesheet>";

            XsltCompiler compiler = processor.NewXsltCompiler();
            compiler.BaseUri = new Uri("http://localhost/stylesheet");
            XsltExecutable exec = compiler.Compile(new StringReader(stylesheet));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = exec.Load30();

            // Establish the result document handler
            Dictionary<string, DomDestination> results = new();
            transformer.ResultDocumentHandler = (href, baseUri) => {
                return results[href] = new DomDestination();
            };

            // Transform the source XML to a NullDestination (because we only want the secondary result files).
            NullDestination destination = new() {BaseUri = samplesDir};
            transformer.ApplyTemplates(input, destination);

            // Process the captured DOM results
            foreach (var entry in results) {
                string uri = entry.Key;
                Console.WriteLine("\nResult File " + uri);
                DomDestination dom = results[uri];
                Console.Write(dom.XmlDocument.OuterXml);
            }

        }

    }

    /// <summary>
    /// Show a transformation using a user-supplied collection finder
    /// </summary>

    public class XsltUsingCollectionFinder : Example
    {

        public override string testName => "XsltUsingCollectionFinder";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();
            DocumentBuilder builder = processor.NewDocumentBuilder();

            // Define a stylesheet that uses the collection() function
            const string stylesheet = "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>\n" +
                                      "<xsl:template name='main'>\n" +
                                      " <out>\n" +
                                      "  <xsl:for-each select=\"collection('http://www.example.org/my-collection')\">\n" +
                                      "    <document uri='{document-uri(.)}' nodes='{count(//*)}'/>\n" +
                                      "  </xsl:for-each><zzz/>\n" +
                                      "  <xsl:for-each select=\"collection('http://www.example.org/my-collection')\">\n" +
                                      "    <document uri='{document-uri(.)}' nodes='{count(//*)}'/>\n" +
                                      "  </xsl:for-each>\n" +
                                      " </out>\n" +
                                      "</xsl:template>\n" +
                                      "</xsl:stylesheet>";

            // Register the collection finder
            processor.CollectionFinder = uri => {
                if (uri.ToString() == "http://www.example.org/my-collection") {
                    List<IResource> resources = new() {
                        new XmlFileResource(builder, samplesDir + "data/books.xml"),
                        new XmlFileResource(builder, samplesDir + "data/othello.xml")
                    };
                    return resources;
                }
                return processor.StandardCollectionFinder(uri);
            };

            XsltCompiler compiler = processor.NewXsltCompiler();
            compiler.BaseUri = new Uri("http://localhost/stylesheet");
            XsltExecutable exec = compiler.Compile(new StringReader(stylesheet));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = exec.Load30();

            // Set the destination
            XdmDestination results = new();

            // Transform the XML, calling a named initial template
            transformer.CallTemplate(new QName("", "main"), results);

            // Show the result
            Console.WriteLine(results.XdmNode.ToString());

        }

        private class XmlFileResource : IResource {
            private readonly string fileName;
            private readonly DocumentBuilder builder;

            public XmlFileResource(DocumentBuilder builder, string fileName) {
                this.builder = builder;
                this.fileName = fileName;
            }
            public Uri ResourceUri => new Uri(fileName);
            
            public XdmItem GetXdmItem() {
                return builder.Build(ResourceUri);
            }

            public string ContentType => "application/xml";
        }
    }

    /// <summary>
    /// Show a transformation using a collection that maps to a directory
    /// </summary>

    public class XsltUsingDirectoryCollection : Example
    {

        public override string testName => "XsltUsingDirectoryCollection";

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new();

            // Define a stylesheet that uses the collection() function
            string stylesheet =
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>\n" +
                "<xsl:template name='main'>\n" +
                " <out>\n" +
                "  <xsl:for-each select=\"collection('" + samplesDir + "?recurse=yes;select=*.xml;on-error=warning')\">\n" +
                "    <document uri='{document-uri(.)}' nodes='{count(//*)}'/>\n" +
                "  </xsl:for-each><zzz/>\n" +
                " </out>\n" +
                "</xsl:template>\n" +
                "</xsl:stylesheet>";


            XsltCompiler compiler = processor.NewXsltCompiler();
            compiler.BaseUri = new Uri("http://localhost/stylesheet");
            XsltExecutable exec = compiler.Compile(new StringReader(stylesheet));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = exec.Load30();

            // Set the destination
            XdmDestination results = new();

            // Transform the XML, calling a named initial template
            transformer.CallTemplate(new QName("main"), results);

            // Show the result
            Console.WriteLine(results.XdmNode.ToString());

        }

    }
    

    /// <summary>
    /// Show a transformation using calls to integrated extension functions (full API)
    /// </summary>

    public class XsltIntegratedExtension : Example
    {

        public override string testName => "XsltIntegratedExtension";

        public override void run(Uri samplesDir)
        {

            // Create a Processor instance.
            Processor processor = new();

            // Identify the Processor version
            Console.WriteLine(processor.ProductVersion);

            // Create the stylesheet
            const string stylesheet = @"<xsl:transform version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                                      @"      xmlns:math='http://example.math.co.uk/demo' " +
                                      @"      xmlns:env='http://example.env.co.uk/demo' " +
                                      @"      exclude-result-prefixes='math env'> " +
                                      @" <xsl:template name='go'> " +
                                      @" <out sqrt2='{math:sqrt(2.0e0)}' " +
                                      @"      defaultNamespace='{env:defaultNamespace()}' " +
                                      @"      sqrtEmpty='{math:sqrt(())}'> " +
                                      @"   <defaultNS value='{env:defaultNamespace()}' xsl:xpath-default-namespace='http://default.namespace.com/' /> " +
                                      @" </out> " +
                                      @" </xsl:template> " +
                                      @" </xsl:transform>";
            

            // Register the integrated extension functions math:sqrt and env:defaultNamespace
            processor.RegisterExtensionFunction(new Sqrt());
            processor.RegisterExtensionFunction(new DefaultNamespace());

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = processor.NewXsltCompiler().Compile(new StringReader(stylesheet)).Load30();

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.OutputWriter = Console.Out;
            serializer.SetOutputProperty(Serializer.INDENT, "yes");

            // Transform the source XML, calling a named initial template, and serialize the result document
            transformer.CallTemplate(new QName("go"), serializer);
        }

    }

    /// <summary>
    /// Example extension function to compute a square root, using the full API
    /// </summary>

    public class Sqrt : ExtensionFunctionDefinition
    {
        public override QName FunctionName => new QName("http://example.math.co.uk/demo", "sqrt");

        public override int MinimumNumberOfArguments => 1;

        public override int MaximumNumberOfArguments => 1;

        public override XdmSequenceType[] ArgumentTypes =>
            new[]{
                new XdmSequenceType(XdmAtomicType.BuiltInAtomicType(QName.XS_DOUBLE), '?')
            };

        public override XdmSequenceType ResultType(XdmSequenceType[] ArgumentTypes)
        {
            return new(XdmAtomicType.BuiltInAtomicType(QName.XS_DOUBLE), '?');
        }

        public override bool TrustResultType => true;


        public override ExtensionFunctionCall MakeFunctionCall()
        {
            return new SqrtCall();
        }
    }

    internal class SqrtCall : ExtensionFunctionCall
    {
        public override XdmValue Call(XdmValue[] arguments, DynamicContext context) {
            if (arguments[0].Empty) {
                return XdmEmptySequence.Instance;
            }
            XdmAtomicValue arg = (XdmAtomicValue)arguments[0].ItemAt(0);
            double val = (double)arg.Value;
            double sqrt = System.Math.Sqrt(val);
            return new XdmAtomicValue(sqrt);
        }
    }

    /// <summary>
    /// Example extension function to return the default namespace from the static context
    /// </summary>

    public class DefaultNamespace : ExtensionFunctionDefinition
    {
        public override QName FunctionName => new QName("http://example.env.co.uk/demo", "defaultNamespace");

        public override int MinimumNumberOfArguments => 0;

        public override int MaximumNumberOfArguments => 0;

        public override XdmSequenceType[] ArgumentTypes => Array.Empty<XdmSequenceType>();

        public override bool DependsOnFocus => true;

        // actually it depends on the static context rather than the focus; but returning true is necessary
        // to avoid the call being extracted to a global variable.
        public override XdmSequenceType ResultType(XdmSequenceType[] ArgumentTypes) {
            return new(XdmAtomicType.BuiltInAtomicType(QName.XS_STRING), '?');
        }

        public override bool TrustResultType => true;

        public override ExtensionFunctionCall MakeFunctionCall() {
            return new DefaultNamespaceCall();
        }
    }

    internal class DefaultNamespaceCall : ExtensionFunctionCall
    {
        private string defaultNamespace;

        public override void SupplyStaticContext(StaticContext context) {
            defaultNamespace = context.GetNamespaceForPrefix("");
        }

        public override XdmValue Call(XdmValue[] arguments, DynamicContext context) {
            return defaultNamespace != null ? new XdmAtomicValue(defaultNamespace) : XdmEmptySequence.Instance;
        }
    }

    /// <summary>
    /// Show a transformation using calls to an extension function implemented using a lambda expression
    /// </summary>

    public class XsltSimpleExtension : Example
    {

        public override string testName => "XsltSimpleExtension";

        public override void run(Uri samplesDir) {

            // Create a Processor instance.
            Processor processor = new();

            // Identify the Processor version
            Console.WriteLine(processor.ProductVersion);
            
            // Create the stylesheet
            const string stylesheet = @"<xsl:transform version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                                      @"    xmlns:math='http://example.math.co.uk/demo'> " +
                                      @" <xsl:template name='go'> " +
                                      @"   <out sqrt2='{math:sqrtSimple(2.0e0)}' " +
                                      @"        sqrtEmpty='{math:sqrtSimple(())}'/> " +
                                      @" </xsl:template>" +
                                      @" </xsl:transform>";

            // Register the extension function math:sqrtSimple
            processor.RegisterExtensionFunction(
                new QName("http://example.math.co.uk/demo", "sqrtSimple"), 
                processor.ParseItemType("function(xs:double?) as xs:double?"), 
                arg => arg[0].Empty ? XdmEmptySequence.Instance : new XdmAtomicValue(Math.Sqrt(((XdmAtomicValue)arg[0]).AsDouble())));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = processor.NewXsltCompiler().Compile(new StringReader(stylesheet)).Load30();

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.OutputWriter = Console.Out;
            serializer.SetOutputProperty(Serializer.INDENT, "yes");

            // Transform the source XML, calling a named initial template, and serialize the result document
            transformer.CallTemplate(new QName("go"), serializer);
        }

    }
    
    /// <summary>
    /// Show a query producing a document as its result and serializing this to a FileStream
    /// </summary>

    public class XQueryToStream : Example
    {

        public override string testName => "XQueryToStream";

        public override void run(Uri samplesDir)
        {
            Processor processor = new(true);
            XQueryCompiler compiler = processor.NewXQueryCompiler();
            compiler.BaseUri = samplesDir;
            compiler.DeclareNamespace("saxon", "http://saxon.sf.net/");
            XQueryExecutable exp = compiler.Compile("<saxon:example>{static-base-uri()}</saxon:example>");
            XQueryEvaluator eval = exp.Load();
            Serializer qout = processor.NewSerializer();
            qout.SetOutputProperty(Serializer.METHOD, "xml");
            qout.SetOutputProperty(Serializer.INDENT, "yes");
            qout.SetOutputProperty(Serializer.SAXON_INDENT_SPACES, "1");
            qout.OutputStream = new FileStream("testoutput.xml", FileMode.Create, FileAccess.Write);
            Console.WriteLine("Output written to testoutput.xml");
            eval.Run(qout);
        }

    }

    /// <summary>
    /// Show a query producing a single atomic value as its result and returning the value
    /// to the C# application
    /// </summary>

    public class XQueryToAtomicValue : Example
    {

        public override string testName => "XQueryToAtomicValue";

        public override void run(Uri samplesDir) {
            Processor processor = new();
            XQueryCompiler compiler = processor.NewXQueryCompiler();
            XQueryExecutable exp = compiler.Compile("avg(for $i in 1 to 10 return $i * $i)");
            XQueryEvaluator eval = exp.Load();
            XdmAtomicValue result = (XdmAtomicValue)eval.EvaluateSingle();
            Console.WriteLine("Result type: " + result.Value.GetType());
            Console.WriteLine("Result value: " + (decimal)result.Value);
        }

	}

	/// <summary>
	/// Show a query producing a sequence as its result and returning the sequence
	/// to the C# application in the form of an iterator. For each item in the
	/// result, its string value is output.
	/// </summary>

	public class XQueryToSequence : Example
	{

		public override string testName => "XQueryToSequence";

        public override void run(Uri samplesDir)
		{
			Processor processor = new();
			XQueryCompiler compiler = processor.NewXQueryCompiler();
			XQueryExecutable exp = compiler.Compile("for $i in 1 to 10 return $i * $i");
			XQueryEvaluator eval = exp.Load();
			XdmValue value = eval.Evaluate();
			foreach(XdmItem item in value)
			{
				Console.WriteLine(item.ToString());
			}

		}

	}

    /// <summary>
    /// Show a query producing a DOM as its input and producing a DOM as its output
    /// </summary>

    public class XQueryToDom : Example
    {

        public override string testName => "XQueryToDom";

        public override void run(Uri samplesDir)
        {
            Processor processor = new();

            XmlDocument input = new();
            input.Load(new Uri(samplesDir, "data/books.xml").ToString());
            XdmNode indoc = processor.NewDocumentBuilder().Build(new XmlNodeReader(input));

            XQueryCompiler compiler = processor.NewXQueryCompiler();
            XQueryExecutable exp = compiler.Compile("<doc>{reverse(/*/*)}</doc>");
            XQueryEvaluator eval = exp.Load();
            eval.ContextItem = indoc;
            DomDestination qout = new();
            eval.Run(qout);
            XmlDocument outdoc = qout.XmlDocument;
            Console.WriteLine(outdoc.OuterXml);
        }

    }

    /// <summary>
    /// Show a query producing a Saxon tree as its input and producing a Saxon tree as its output
    /// </summary>

    public class XQueryToXdm : Example
    {

        public override string testName => "XQueryToXdm";

        public override void run(Uri samplesDir)
        {
            Processor processor = new();

            DocumentBuilder loader = processor.NewDocumentBuilder();
            loader.BaseUri = new Uri(samplesDir, "data/books.xml");
            XdmNode indoc = loader.Build(loader.BaseUri);

            XQueryCompiler compiler = processor.NewXQueryCompiler();
            XQueryExecutable exp = compiler.Compile("<doc>{reverse(/*/*)}</doc>");
            XQueryEvaluator eval = exp.Load();
            eval.ContextItem = indoc;
            XdmDestination qout = new();
            eval.Run(qout);
            XdmNode outdoc = qout.XdmNode;
            Console.WriteLine(outdoc.OuterXml);
        }

    }

    /// <summary>
    /// Show a query making a direct call to a user-defined function defined in the query
    /// </summary>

    public class XQueryCallFunction : Example
    {

        public override string testName => "XQueryCallFunction";

        public override void run(Uri samplesDir)
        {
            Processor processor = new();

            XQueryCompiler qc = processor.NewXQueryCompiler();
            Uri uri = new(samplesDir, "data/books.xml");
            XQueryExecutable exp1 = qc.Compile("declare namespace f='f.ns';" +
                   "declare variable $z := 1 + xs:integer(doc-available('" + uri.ToString() + "'));" +
                   "declare variable $p as xs:integer external;" +
                   "declare function f:t1($v1 as xs:integer) { " +
                   "   $v1 div $z + $p" +
                   "};" +
                   "10");
            XQueryEvaluator ev = exp1.Load();
            ev.SetExternalVariable(new QName("", "p"), new XdmAtomicValue(39));
            XdmValue v1 = new XdmAtomicValue(10);
            XdmValue result = ev.CallFunction(new QName("f.ns", "f:t1"), new XdmValue[] { v1 });
            Console.WriteLine("First result (expected 44): " + result.ToString());
            v1 = new XdmAtomicValue(20);
            result = ev.CallFunction(new QName("f.ns", "f:t1"), new XdmValue[] { v1 });
            Console.WriteLine("Second result (expected 49): " + result.ToString());
        }

    }

    /// <summary>
    /// Show a query reading an input document using an XmlReader (the .NET XML parser)
    /// </summary>

    public class XQueryFromXmlReader : Example
    {

        public override string testName => "XQueryFromXmlReader";

        public override void run(Uri samplesDir)
        {
            Processor processor = new();

            string inputFileName = new Uri(samplesDir, "data/books.xml").ToString();

            // Add a validating reader - needed in case there are entity references
            XmlReaderSettings settings = new() {
                ValidationType = ValidationType.DTD, 
                DtdProcessing = DtdProcessing.Parse,
                XmlResolver = new XmlUrlResolver()
            };
            XmlReader validator = XmlReader.Create(inputFileName, settings);

            XdmNode doc = processor.NewDocumentBuilder().Build(validator);

            XQueryCompiler compiler = processor.NewXQueryCompiler();
            XQueryExecutable exp = compiler.Compile("/");
            XQueryEvaluator eval = exp.Load();
            eval.ContextItem = doc;
            Serializer qout = processor.NewSerializer();
            qout.SetOutputProperty(Serializer.METHOD, "xml");
            qout.SetOutputProperty(Serializer.INDENT, "yes");
            qout.OutputStream = new FileStream("testoutput2.xml", FileMode.Create, FileAccess.Write);
            Console.WriteLine("Output written to testoutput2.xml");
            eval.Run(qout);
        }

    }

    /// <summary>
    /// Show a query producing a sequence as its result and returning the sequence
    /// to the C# application in the form of an iterator. The sequence is then
    /// output by serializing each item individually, with each item on a new line.
    /// </summary>

    public class XQueryToSerializedSequence : Example
    {

        public override string testName => "XQueryToSerializedSequence";

        public override void run(Uri samplesDir)
        {
            Processor processor = new();
            string inputFileName = new Uri(samplesDir, "data/books.xml").ToString();
            XmlTextReader reader = new(inputFileName,
                UriConnection.getReadableUriStream(new Uri(samplesDir, "data/books.xml"))) {
                Normalization = true
            };

            // Add a validating reader - needed in case there are entity references
            XmlReaderSettings settings = new() {
                ValidationType = ValidationType.DTD, 
                DtdProcessing = DtdProcessing.Parse
            };
            XmlReader validator = XmlReader.Create(reader, settings);  // TODO: NOT USED!!

            XdmNode doc = processor.NewDocumentBuilder().Build(reader);

            XQueryCompiler compiler = processor.NewXQueryCompiler();
            XQueryExecutable exp = compiler.Compile("//ISBN");
            XQueryEvaluator eval = exp.Load();
            eval.ContextItem = doc;

            foreach (XdmNode node in eval)
            {
                Console.WriteLine(node.OuterXml);
            }
        }

    }

    /// <summary>
    /// Show a query that takes a parameter (external variable) as input.
    /// The query produces a single atomic value as its result and returns the value
    /// to the C# application. 
    /// </summary>

    public class XQueryUsingParameter : Example
    {

        public override string testName => "XQueryUsingParameter";

        public override void run(Uri samplesDir)
        {
            Processor processor = new();
            XQueryCompiler compiler = processor.NewXQueryCompiler();
            compiler.DeclareNamespace("p", "http://saxon.sf.net/ns/p");
            XQueryExecutable exp = compiler.Compile(
                    "declare variable $p:in as xs:integer external; $p:in * $p:in");
            XQueryEvaluator eval = exp.Load();
            eval.SetExternalVariable(new QName("http://saxon.sf.net/ns/p", "p:in"), new XdmAtomicValue(12));
            XdmAtomicValue result = (XdmAtomicValue)eval.EvaluateSingle();
            Console.WriteLine("Result type: " + result.Value.GetType());
            Console.WriteLine("Result value: " + (long)result.Value);
        }

    }

    /// <summary>
    /// Show a query consisting of two modules, using a QueryResolver to resolve
    /// the "import module" declaration
    /// </summary>

    public class XQueryMultiModule : Example {

        public override string testName => "XQueryMultiModule";

        public override void run(Uri samplesDir) {

            const string mod1 = "import module namespace m2 = 'http://www.example.com/module2';" +
                                "m2:square(3)";

            const string mod2 = "module namespace m2 = 'http://www.example.com/module2';" +
                                "declare function m2:square($p) { $p * $p };";

            Processor processor = new();
            XQueryCompiler compiler = processor.NewXQueryCompiler();
            compiler.XQueryResolver = (module, queryBase, hints) => {
                if (module == "http://www.example.com/module2") {
                    return new[] {
                        new TextResource(mod2, new Uri(module))
                    };
                }
                return null;
            };
            XQueryExecutable exp = compiler.Compile(mod1);
            XQueryEvaluator eval = exp.Load();

            XdmAtomicValue result = (XdmAtomicValue) eval.EvaluateSingle();
            Console.WriteLine("Result type: " + result.Value.GetType());
            Console.WriteLine("Result value: " + (long) result.Value);
        }
    }

    /// <summary>
    /// Demonstrate using a try-catch expression in the query, a feature of XQuery 3.0
    /// </summary>

    public class XQueryTryCatch : Example
    {

        public override string testName => "XQueryTryCatch";

        public override void run(Uri samplesDir)
        {

            const string query = "xquery version '3.1'; try {doc('book.xml')}catch * {\"XQuery 3.0 catch clause - file not found.\"}";
            Processor processor = new();

            XQueryCompiler compiler = processor.NewXQueryCompiler();
            XQueryExecutable exp = compiler.Compile(query);
            XQueryEvaluator eval = exp.Load();
            Serializer qout = processor.NewSerializer(Console.Out);
            eval.Run(qout);
        }

    }

    /// <summary>
    /// Demonstrate XQuery extensibility using lambda-expression extensions
    /// </summary>

    public class XQueryExtensibility : Example
    {

        public override string testName => "XQueryExtensibility";

        public override void run(Uri samplesDir)
        {
            const string query = "declare namespace ext = \"urn:sampleExtensions\";" +
                                 "<out>" +
                                 "  <addition>{ext:add(2,2)}</addition>" +
                                 "  <average>{ext:average((1,2,3,4,5,6))}</average>" +
                                 "  <language>{ext:hostLanguage()}</language>" +
                                 "</out>";

            Processor processor = new();
            processor.RegisterExtensionFunction(
                new QName("urn:sampleExtensions", "add"), 
                processor.ParseItemType("function(xs:integer, xs:integer) as xs:integer"),
                (args) => new XdmAtomicValue(((XdmAtomicValue)args[0][0]).AsLong() + ((XdmAtomicValue)args[1][0]).AsLong()));
            processor.RegisterExtensionFunction(
                new QName("urn:sampleExtensions", "average"), 
                processor.ParseItemType("function(xs:integer*) as xs:decimal"),
                (args) => {
                    decimal total = 0;
                    foreach (var item in args[0]) {
                        total += ((XdmAtomicValue)item).AsLong();
                    }
                    return new XdmAtomicValue((decimal)total / args[0].Count)
                        ;
                });

            processor.RegisterExtensionFunction(new HostLanguage());
            XQueryCompiler compiler = processor.NewXQueryCompiler();
            XQueryExecutable exp = compiler.Compile(query);
            XQueryEvaluator eval = exp.Load();
            Serializer qout = processor.NewSerializer(Console.Out);
            eval.Run(qout);
        }

        private class HostLanguage : ExtensionFunctionDefinition {
            // This extension function needs to use the full interface because it accesses the context
            public override QName FunctionName => new ("urn:sampleExtensions", "hostLanguage");
            public override int MinimumNumberOfArguments => 0;
            public override int MaximumNumberOfArguments => 0;
            public override XdmSequenceType[] ArgumentTypes => Array.Empty<XdmSequenceType>();
            public override XdmSequenceType ResultType(XdmSequenceType[] ArgumentTypes) {
                return new XdmSequenceType(XdmAtomicType.String, ' ');
            }

            public override ExtensionFunctionCall MakeFunctionCall() {
                return new HostLanguageCall();
            }

            private class HostLanguageCall : ExtensionFunctionCall {
                public override XdmValue Call(XdmValue[] arguments, DynamicContext context) {
                    return new XdmAtomicValue(context.Implementation.getController().getExecutable().getHostLanguage()
                        .ToString());
                }
            }
        }

    }

    /// <summary>
    /// Demonstrate XQuery Update
    /// </summary>

    public class XQueryUpdate : Example
    {

        public override string testName => "XQueryUpdate";

        public override void run(Uri samplesDir)
        {
            Processor processor = new(true);

            DocumentBuilder loader = processor.NewDocumentBuilder();
            loader.BaseUri = new Uri(samplesDir, "data/books.xml");
            loader.TreeModel = TreeModel.LinkedTree;
            XdmNode indoc = loader.Build(new Uri(samplesDir, "data/books.xml"));

            Console.Out.WriteLine("=========== BEFORE UPDATE ===========");

            Serializer serializer0 = processor.NewSerializer();
            serializer0.SetOutputProperty(Serializer.METHOD, "xml");
            serializer0.SetOutputProperty(Serializer.INDENT, "yes");
            serializer0.OutputWriter = Console.Out;
            processor.WriteXdmValue(indoc, serializer0);

            const string query = "for $i in //PRICE return \n" +
                                 "replace value of node $i with $i - 0.05";

            XQueryCompiler compiler = processor.NewXQueryCompiler();
            compiler.UpdatingEnabled = true;
            XQueryExecutable exp = compiler.Compile(query);
            XQueryEvaluator eval = exp.Load();
            eval.ContextItem = indoc;
            XdmNode[] updatedNodes = eval.RunUpdate();

            // Serialize the updated result to Console.out
            foreach (XdmNode root in updatedNodes)
            {
                Uri documentUri = root.DocumentUri;
                if (documentUri != null && documentUri.Scheme == "file")
                {
                    Serializer serializer = processor.NewSerializer();
                    serializer.SetOutputProperty(Serializer.METHOD, "xml");
                    serializer.SetOutputProperty(Serializer.INDENT, "yes");
                    Console.Out.WriteLine("=========== AFTER UPDATE ===========");
                    serializer.OutputWriter = Console.Out;
                    processor.WriteXdmValue(root, serializer);
                }
            }

			// To serialize to the original source file, replace the above code with the following

			//foreach (XdmNode root in updatedNodes)
			//{
			//	Uri documentUri = root.DocumentUri;
			//	if (documentUri != null && documentUri.Scheme == "file")
			//	{
			//		Stream stream = UriConnection.getWritableUriStream(documentUri);
			//		Serializer serializer = processor.NewSerializer();
			//		serializer.SetOutputProperty(Serializer.METHOD, "xml");
			//		serializer.SetOutputProperty(Serializer.INDENT, "yes");
			//		serializer.SetOutputStream(stream);
			//		processor.WriteXdmValue(root, serializer);
			//	}
			//}
			//
			//Console.Out.WriteLine("=========== AFTER UPDATE ===========");
			//
			//processor.WriteXdmValue(indoc, serializer0);
        }
	}

    /// <summary>
    /// Demonstrate schema aware XQuery
    /// </summary>

    public class XQuerySchemaAware : Example
    {

        public override string testName => "XQuerySchemaAware";

        public override void run(Uri samplesDir)
        {
            Processor processor = new(true);

            String inputFileName = new Uri (samplesDir, "data/books.xml").ToString();
            String inputSchemaName = new Uri (samplesDir, "data/books.xsd").ToString();
            String query = "import schema default element namespace \"\" at \"" + inputSchemaName + "\";\n" +
                "for $integer in (validate { doc(\"" + inputFileName + "\") })//schema-element(ITEM)\n" +
                "return <OUTPUT>{$integer}</OUTPUT>";

            XQueryCompiler compiler = processor.NewXQueryCompiler();
            compiler.XQueryLanguageVersion = "3.1";
            XQueryExecutable exp = compiler.Compile(query);
            XQueryEvaluator eval = exp.Load();
            Serializer qout = processor.NewSerializer(Console.Out);
            qout.SetOutputProperty(Serializer.METHOD, "xml");
            qout.SetOutputProperty(Serializer.INDENT, "yes");
            eval.Run(qout);
        }

    }

    /// <summary>
    /// Demonstrate schema aware XPath
    /// </summary>

    public class XPathSchemaAware : Example
    {

        public override string testName => "XPathSchemaAware";

        public override void run(Uri samplesDir)
        {
            Processor processor = new(true);
            processor.SchemaManager.Compile(new Uri(samplesDir, "data/books.xsd"));

            // Add a reader
            XmlReaderSettings settings = new() {
                DtdProcessing = DtdProcessing.Ignore
            };
            XmlReader xmlReader = XmlReader.Create(UriConnection.getReadableUriStream(new Uri(samplesDir, "data/books.xml")), settings);

            DocumentBuilder builder = processor.NewDocumentBuilder();

            builder.SchemaValidationMode = SchemaValidationMode.Strict;
            XdmNode doc = builder.Build(xmlReader);

            XPathCompiler compiler = processor.NewXPathCompiler();
            compiler.ImportSchemaNamespace("");
            XPathExecutable exp = compiler.Compile("if (//ITEM[@CAT='MMP']/QUANTITY instance of element(*,xs:integer)*) then 'true' else 'false'");
            XPathSelector eval = exp.Load();
            eval.ContextItem = doc;
            XdmAtomicValue result = (XdmAtomicValue)eval.EvaluateSingle();
            Console.WriteLine("Result type: " + result.ToString());
        }

    }

    /// <summary>
    /// Show XSLT streaming of document
    /// </summary>

    public class XsltStreamDoc : Example
    {

        public override string testName => "XsltStreamDoc";

        public override void run(Uri samplesDir)
        {
            Processor processor = new(true);

            // Create the stylesheet
            string stylesheet = "<xsl:transform version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>\n" +
				" <xsl:template name='main'>\n" +
				"  <xsl:source-document streamable='yes' href='" + new Uri(samplesDir, "data/othello.xml").ToString() + "'>\n" +
				"   <xsl:value-of select=\"count(copy-of(//LINE)[count(tokenize(.)) &gt; 0] )\" />\n" +
				"  </xsl:source-document>\n"+
				" </xsl:template>\n" +
				"</xsl:transform>";

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = processor.NewXsltCompiler().Compile(new StringReader(stylesheet)).Load30();

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.OutputWriter = Console.Out;
            //  serializer.SetOutputProperty(Serializer.INDENT, "yes");

            // Transform the source XML, calling a named initial template, and serialize the result document.
            transformer.CallTemplate(new QName("main"), serializer);
        }

    }

    /// <summary>
    /// Show validation of an instance document against a schema, 
    /// if the document is valid then run a schema aware query
    /// </summary>

    public class Validate : Example
    {

        public override string testName => "Validate";

        public override void run(Uri samplesDir)
        {
            // Load a schema

            Processor processor = new(true);
            processor.SetProperty("http://saxon.sf.net/feature/timing", "true");
            processor.SetProperty("http://saxon.sf.net/feature/validation-warnings", "false"); //Set to true to suppress the exception
            SchemaManager manager = processor.SchemaManager;
            manager.XsdVersion = "1.1";
            List<Error> errorList = new();
            manager.ErrorReporter = err => errorList.Add(err);
            Uri schemaUri = new Uri(samplesDir, "data/books.xsd");

            try
            {
                manager.Compile(schemaUri);
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                Console.WriteLine("Schema compilation failed with " + errorList.Count + " errors");
                foreach (Error error in errorList)
                {
                    Console.WriteLine("At line " + error.Location.LineNumber + ": " + error.Message);
                }
                return;
            }


            // Use this to validate an instance document

            SchemaValidator validator = manager.NewSchemaValidator();

            XmlReaderSettings settings = new() {
                DtdProcessing = DtdProcessing.Ignore
            };
            string inputFileName = new Uri(samplesDir, "data/books-invalid.xml").ToString();
            XmlReader xmlReader = XmlReader.Create(inputFileName, settings);
            Console.WriteLine("Validating input file " + inputFileName);
            List<ValidationFailure> errors = new();
            validator.InvalidityListener = failure => errors.Add(failure);
            XdmDestination psvi = new();
            validator.SetDestination(psvi);
            try
            {
                validator.Validate(xmlReader);
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                Console.WriteLine("Instance validation failed with " + errors.Count + " errors");
                foreach (ValidationFailure error in errors)
                {
                    Console.WriteLine("At line " + error.LineNumber + ": " + error.Message);
                }
                return;
            }

            Console.WriteLine("Input file is valid");

            // Run a query on the result to check that it has type annotations

            XQueryCompiler xq = processor.NewXQueryCompiler();
            xq.SchemaAware = true;
            XQueryEvaluator xv = xq.Compile("data((//PRICE)[1]) instance of xs:decimal").Load();
            xv.ContextItem = psvi.XdmNode;
            Console.WriteLine("Price is decimal? " + xv.EvaluateSingle().ToString());
        }
    }


    public class UriConnection
    {

        // Get a stream for reading from a file:// URI

        public static Stream getReadableUriStream(Uri uri)
        {
            WebRequest request = (WebRequest)WebRequest.Create(uri);
            return request.GetResponse().GetResponseStream();
        }

        // Get a stream for writing to a file:// URI

        public static Stream getWritableUriStream(Uri uri)
        {
            FileWebRequest request = (FileWebRequest)WebRequest.CreateDefault(uri);
            request.Method = "POST";
            return request.GetRequestStream();
        }
    }

    ///
    /// A factory class to generate a resource resolver. In the case of a URI ending with ".txt",
    /// the resource resolver returns the URI itself, wrapped as an XML document. 
    /// In the case of the URI "empty.xslt", it returns an empty
    /// stylesheet. In all other cases, it returns null, which has the effect of delegating
    /// processing to the standard XmlResolver.
    ///

    public class UserXmlResolver 
    {

        public string Message = null;

        public ResourceResolver GetResourceResolver() {
            return (request) => {
                Uri uri = request.Uri;
                if (Message != null) {
                    Console.WriteLine(Message + uri + " (nature=" + request.Nature + ")");
                }

                if (uri.ToString().EndsWith(".txt")) {
                    return new TextResource("<uri>" + uri + "</uri>", uri);
                }

                if (uri.ToString().EndsWith("empty.xslt")) {
                    return new TextResource("<transform xmlns='http://www.w3.org/1999/XSL/Transform' version='2.0'/>",
                        uri);
                }
                else {
                    return null;
                }
            };
        }


    }

}


