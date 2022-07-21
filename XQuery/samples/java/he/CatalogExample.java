package he;

import net.sf.saxon.s9api.*;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;

public class CatalogExample {
    public static void main(String[] argv) throws Exception {
        Processor processor = new Processor(false);

        String cwd = System.getProperty("user.dir");
        String datapath = cwd + "/src/test/testdata/resolvers";

        String catalog = datapath + "/catalog.xml";
        processor.setCatalogFiles(catalog);

        InputSource docsrc = new InputSource(datapath + "/input.xml");
        InputSource xslsrc = new InputSource(datapath + "/stylesheet.xsl");

        XsltCompiler compiler = processor.newXsltCompiler();
        XsltExecutable exec = compiler.compile(new SAXSource(xslsrc));
        Xslt30Transformer transformer = exec.load30();

        XdmDestination destination = new XdmDestination();

        transformer.transform(new SAXSource(docsrc), destination);

        System.err.println(destination.getXdmNode());
    }
}