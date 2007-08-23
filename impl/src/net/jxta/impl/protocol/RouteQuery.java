/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
 *  
 *  The Sun Project JXTA(TM) Software License
 *  
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *  
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *  
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *  
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must 
 *     not be used to endorse or promote products derived from this software 
 *     without prior written permission. For written permission, please contact 
 *     Project JXTA at http://www.jxta.org.
 *  
 *  5. Products derived from this software may not be called "JXTA", nor may 
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *  
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN 
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United 
 *  States and other countries.
 *  
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of 
 *  the license in source files.
 *  
 *  ====================================================================
 *  
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *  
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.impl.protocol;


import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Attributable;
import net.jxta.document.Attribute;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.XMLElement;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.protocol.RouteQueryMsg;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Enumeration;

/**
 * RouteQuery message used by the Endpoint Routing protocol to
 * query for route
 */
public class RouteQuery extends RouteQueryMsg {

    private static final String destPIDTag = "Dst";
    private static final String srcRouteTag = "Src";
    private static final String badHopTag = "Bad";

    /**
     * Default Constructor
     */
    public RouteQuery() {}

    /**
     * Constructs a RouteQuery
     *
     * @param dest     dest PeerID
     * @param srcRoute source source
     * @param badhops  lis of AccessPointAdvertisements
     */
    public RouteQuery(PeerID dest, RouteAdvertisement srcRoute, Collection<PeerID> badhops) {

        setDestPeerID(dest);
        setSrcRoute(srcRoute);
        setBadHops(badhops);
    }

    /**
     * Construct from a StructuredDocument
     *
     * @param root the element
     */
    public RouteQuery(Element root) {

        if (!XMLElement.class.isInstance(root)) {
            throw new IllegalArgumentException(getClass().getName() + " only supports XMLElement");
        }

        XMLElement doc = (XMLElement) root;

        String typedoctype = "";
        Attribute itsType = doc.getAttribute("type");

        if (null != itsType) {
            typedoctype = itsType.getValue();
        }

        String doctype = doc.getName();
        
        if (!doctype.equals(getAdvertisementType()) && !getAdvertisementType().equals(typedoctype)) {
            throw new IllegalArgumentException(
                    "Can not construct : " + getClass().getName() + " from doc containing a " + doc.getName());
        }

        readIt(doc);
    }

    private void readIt(XMLElement doc) {
        Enumeration elements = doc.getChildren();

        while (elements.hasMoreElements()) {
            XMLElement elem = (XMLElement) elements.nextElement();

            if (elem.getName().equals(destPIDTag)) {
                try {
                    URI pID = new URI(elem.getTextValue());
                    PeerID pid = (PeerID) IDFactory.fromURI(pID);

                    setDestPeerID(pid);
                } catch (URISyntaxException badID) {
                    throw new IllegalArgumentException("Bad PeerID ID in advertisement");
                } catch (ClassCastException badID) {
                    throw new IllegalArgumentException("Not a peer id");
                }
                continue;
            }

            if (elem.getName().equals(srcRouteTag)) {
                for (Enumeration eachXpt = elem.getChildren(); eachXpt.hasMoreElements();) {
                    XMLElement aXpt = (XMLElement) eachXpt.nextElement();

                    RouteAdvertisement route = (RouteAdvertisement) AdvertisementFactory.newAdvertisement(aXpt);

                    setSrcRoute(route);
                }
                continue;
            }

            if (elem.getName().equals(badHopTag)) {
                try {
                    URI pID = new URI(elem.getTextValue());
                    PeerID pid = (PeerID) IDFactory.fromURI(pID);

                    addBadHop(pid);
                } catch (URISyntaxException badID) {
                    throw new IllegalArgumentException("Bad PeerID ID in advertisement");
                } catch (ClassCastException badID) {
                    throw new IllegalArgumentException("Not a peer id");
                }
            }
        }
    }

    /**
     * return a Document represetation of this object
     */
    @Override
    public Document getDocument(MimeMediaType asMimeType) {

        StructuredDocument adv = StructuredDocumentFactory.newStructuredDocument(asMimeType, getAdvertisementType());

        if (adv instanceof XMLElement) {
            ((Attributable) adv).addAttribute("xmlns:jxta", "http://jxta.org");
        }

        Element e;

        PeerID dest = getDestPeerID();

        if (dest != null) {
            e = adv.createElement(destPIDTag, dest.toString());
            adv.appendChild(e);
        }

        RouteAdvertisement route = getSrcRoute();

        if (route != null) {
            e = adv.createElement(srcRouteTag);
            adv.appendChild(e);
            StructuredTextDocument xptDoc = (StructuredTextDocument) route.getDocument(asMimeType);

            StructuredDocumentUtils.copyElements(adv, e, xptDoc);
        }

        for (PeerID o : getBadHops()) {
            e = adv.createElement(badHopTag, o.toString());
            adv.appendChild(e);
        }

        return adv;
    }

    /**
     * return a string representation of this RouteQuery doc
     */
    @Override
    public String toString() {

        try {
            StructuredTextDocument doc = (StructuredTextDocument) getDocument(MimeMediaType.XMLUTF8);

            return doc.toString();
        } catch (Throwable e) {
            if (e instanceof Error) {
                throw (Error) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new UndeclaredThrowableException(e);
            }
        }
    }
}
