package com.saxonica.testdriver.ee;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.CollectionFinder;
import net.sf.saxon.lib.Resource;
import net.sf.saxon.lib.ResourceCollection;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.TreeInfo;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.resource.AbstractResourceCollection;
import net.sf.saxon.resource.XmlResource;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.Type;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This CollectionFinder forms part of the test driver for the XQTS test suite.
 * It locates collections for a given namespace based on information in the XQTS catalog
 */
public class XQTSCollectionFinder implements CollectionFinder {

    private final TreeInfo catalog;
    private final NodeInfo collectionElement;
    private final boolean isDefault;


    public XQTSCollectionFinder(TreeInfo catalog, NodeInfo collectionElement, boolean isDefault) {
        this.catalog = catalog;
        this.collectionElement = collectionElement;
        this.isDefault = isDefault;
    }

    /**
     * Locate the collection of resources corresponding to a collection URI.
     *
     * @param context       The XPath dynamic evaluation context
     * @param href The collection URI: an absolute URI, formed by resolving the argument
     *                      supplied to the fn:collection or fn:uri-collection against the static
     *                      base URI
     * @return a ResourceCollection object representing the resources in the collection identified
     * by this collection URI. Result should not be null.
     * @throws XPathException if the collection was not found
     */
    @Override
    public ResourceCollection findCollection(XPathContext context, String href) throws XPathException {
        NamePool pool = catalog.getConfiguration().getNamePool();
        int inputDocumentNC = pool.allocateFingerprint("http://www.w3.org/2005/02/query-test-XQTSCatalog", "input-document");

        if (href == null) {
            href = "";
        }

        if (!(href.equals(collectionElement.getAttributeValue("", "ID")) || (href.equals("") && isDefault))) {
            throw new XPathException("Unknown collection name " + href);
        }

        AxisIterator iter = collectionElement.iterateAxis(
                AxisInfo.CHILD, new NameTest(Type.ELEMENT, inputDocumentNC, pool));
        List<String> names = new ArrayList<>(5);
        List<XmlResource> nodes = new ArrayList<>(5);
        while (true) {
            NodeInfo m = iter.next();
            if (m == null) {
                break;
            }
            nodes.add(new XmlResource(m));
            String shortName = m.getStringValue();
            String longName = "TestSources/" + shortName + ".xml";
            URI uri;
            try {
                uri = new URI(collectionElement.getBaseURI()).resolve(longName);
            } catch (URISyntaxException e) {
                throw new XPathException(e);
            }
            names.add(uri.toString());
        }
        return new AbstractResourceCollection(context.getConfiguration()) {
            @Override
            public Iterator<String> getResourceURIs(XPathContext context) {
                return names.iterator();
            }

            @Override
            public Iterator<? extends Resource> getResources(XPathContext context) {
                return nodes.iterator();
            }
        };
    }

}

// Copyright (c) 2018-2022 Saxonica Limited
