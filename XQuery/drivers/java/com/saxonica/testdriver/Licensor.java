////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2022 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package com.saxonica.testdriver;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;

/**
 * Abstraction of a class that activates a processor by supplying a license key. Default implementation does
 * nothing, relying on the standard mechanism to search for a license file when required.
 */
public class Licensor {

    public void activate(Processor processor) {
        // do nothing
    }

    public void activate(Configuration config) {
        // do nothing
    }
}

// Copyright (c) 2018-2022 Saxonica Limited

