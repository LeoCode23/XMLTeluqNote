package com.saxonica.testdriver.ee;

import net.sf.saxon.lib.ModuleURIResolver;
import net.sf.saxon.lib.StandardModuleURIResolver;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.Type;

import javax.xml.transform.stream.StreamSource;
import java.util.ArrayList;
import java.util.List;

/**
 * This ModuleURIResolver forms part of the test driver for the XQTS test suite. It locates library
 * modules for a given namespace based on information in the XQTS catalog
 */
public class XQTSModuleURIResolver implements ModuleURIResolver {

    public NodeInfo testCase;
    public NodeInfo catalogTop;

    public XQTSModuleURIResolver(NodeInfo testCase) {
        this.testCase = testCase;
        catalogTop = testCase;
        while (true) {
            NodeInfo p = catalogTop.getParent();
            if (p.getNodeKind() == Type.ELEMENT) {
                catalogTop = p;
            } else {
                break;
            }
        }
    }

    /**
     * Resolve a identifying a query module, given the identifying URI and
     * a set of associated location hints.
     *
     * @param moduleURI the module URI of the module to be imported; or null when
     *                  loading a non-library module.
     * @param baseURI   The base URI of the module containing the "import module" declaration;
     *                  null if no base URI is known
     * @param locations The set of URIs specified in the "at" clause of "import module",
     *                  which serve as location hints for the module
     * @return an array of StreamSource objects each identifying the contents of a query module to be
     *         imported. Each StreamSource must contain a
     *         non-null absolute System ID which will be used as the base URI of the imported module,
     *         and either an InputSource or a Reader representing the text of the module. The method
     *         may also return null, in which case the system attempts to resolve the URI using the
     *         standard module URI resolver.
     * @throws net.sf.saxon.trans.XPathException
     *          if the module cannot be located, and if delegation to the default
     *          module resolver is not required.
     */

    @Override
    public StreamSource[] resolve(String moduleURI, String baseURI, String[] locations) throws XPathException {
        NamePool pool = testCase.getConfiguration().getNamePool();
        int moduleNC = pool.allocateFingerprint("http://www.w3.org/2005/02/query-test-XQTSCatalog", "module");
        AxisIterator iter = testCase.iterateAxis(AxisInfo.CHILD, new NameTest(Type.ELEMENT, moduleNC, pool));
        List<String> catalogLocations = new ArrayList<>(5);
        while (true) {
            NodeInfo m = iter.next();
            if (m == null) break;
            if (moduleURI.equals(m.getAttributeValue("", "namespace"))) {
                String moduleRef = m.getStringValue();
                // take a short cut here: hard code information from the catalog
                if (moduleRef.equals("module-defs")) {
                    moduleRef = "moduleDefs-lib";
                }
                moduleRef = "TestSources/" + moduleRef + ".xq";
                catalogLocations.add(moduleRef);
            }
        }
        if (catalogLocations.isEmpty()) {
            // in XQTS-extra we don't catalog modules, we just reference them directly
            StandardModuleURIResolver r = new StandardModuleURIResolver(testCase.getConfiguration());
            return r.resolve(moduleURI, baseURI, locations);
        } else {
            String[] locs = new String[catalogLocations.size()];
            for (int i = 0; i < locs.length; i++) {
                locs[i] = (String) catalogLocations.get(i);
            }
            StandardModuleURIResolver r = new StandardModuleURIResolver(testCase.getConfiguration());
            return r.resolve(moduleURI, catalogTop.getBaseURI(), locs);
        }
    }
}

// Copyright (c) 2018-2022 Saxonica Limited
