/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2017, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.mbstyle.parse;

import static org.geotools.mbstyle.MapboxTestUtils.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.geotools.filter.text.ecql.ECQL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.Not;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.expression.Literal;

public class MBFilterTest {

    @Test
    public void existential() throws ParseException {
        JSONParser parser = new JSONParser();
        JSONArray json = (JSONArray) parser.parse("[\"has\", \"key\"]");
        
        MBFilter mbfilter = new MBFilter(json);
        Filter filter = mbfilter.filter();
        assertEquals("key IS NULL", ECQL.toCQL(filter) );
        
        json = (JSONArray) parser.parse("[\"!has\", \"key\", \"value\"]");
        mbfilter = new MBFilter(json);
        filter = mbfilter.filter();
        assertEquals("NOT (key IS NULL)", ECQL.toCQL(filter) );
    }
    
    @Test
    public void comparisonFilters() throws ParseException {
        JSONParser parser = new JSONParser();
        JSONArray json = (JSONArray) parser.parse("[\"==\", \"key\", \"value\"]");
        
        MBFilter mbfilter = new MBFilter(json);
        
        // being really quick here, no need to check null / instanceof if we just cast
        PropertyIsEqualTo equal = (PropertyIsEqualTo) mbfilter.filter();
        assertEquals( "key", ((PropertyName)equal.getExpression1()).getPropertyName() );
        assertEquals( "value", ((Literal)equal.getExpression2()).getValue() );
        
        // okay that takes too long just check ECQL
        assertEquals("key = 'value'", ECQL.toCQL(equal));
        
        json = (JSONArray) parser.parse("[\"!=\", \"key\", \"value\"]");
        mbfilter = new MBFilter(json);
        Filter filter = mbfilter.filter();
        assertEquals("key <> 'value'", ECQL.toCQL(filter));

        json = (JSONArray) parser.parse("[\">\", \"key\", \"value\"]");
        mbfilter = new MBFilter(json);
        filter = mbfilter.filter();
        assertEquals("key > 'value'", ECQL.toCQL(filter));
        
        json = (JSONArray) parser.parse("[\"<\", \"key\", \"value\"]");
        mbfilter = new MBFilter(json);
        filter = mbfilter.filter();
        assertEquals("key < 'value'", ECQL.toCQL(filter));

        json = (JSONArray) parser.parse("[\">=\", \"key\", \"value\"]");
        mbfilter = new MBFilter(json);
        filter = mbfilter.filter();
        assertEquals("key >= 'value'", ECQL.toCQL(filter));
        
        json = (JSONArray) parser.parse("[\"<=\", \"key\", \"value\"]");
        mbfilter = new MBFilter(json);
        filter = mbfilter.filter();
        assertEquals("key <= 'value'", ECQL.toCQL(filter));
    }
    
    @Test
    public void membership() throws ParseException {
        JSONParser parser = new JSONParser();
        JSONArray json = (JSONArray) parser.parse("[\"in\", \"a\", 1, 2, 3]");
        
        MBFilter mbfilter = new MBFilter(json);
        Filter filter = mbfilter.filter();
        assertEquals("EQUALS(in(a,1,2,3), 'true')", ECQL.toCQL(filter) );
        
        json = (JSONArray) parser.parse("[\"!in\", \"a\", 1, 2, 3]");
        mbfilter = new MBFilter(json);
        filter = mbfilter.filter();
        assertEquals("EQUALS(in(a,1,2,3), 'false')", ECQL.toCQL(filter) );
    }
    
    @Test
    public void combining() throws ParseException {
        JSONParser parser = new JSONParser();
        JSONArray json = (JSONArray) parser
                .parse("[\"all\", [\"==\",\"a\",1],[\"==\",\"b\",2]]");
        
        MBFilter mbfilter = new MBFilter(json);
        Filter filter = mbfilter.filter();
        assertEquals("a = 1 AND b = 2", ECQL.toCQL(filter) );
        
        
        json = (JSONArray) parser
                .parse("[\"any\", [\"==\",\"a\",1],[\"==\",\"b\",2]]");
        
        mbfilter = new MBFilter(json);
        filter = mbfilter.filter();
        assertEquals("a = 1 OR b = 2", ECQL.toCQL(filter) );
        
        json = (JSONArray) parser
                .parse("[\"none\", [\"==\",\"a\",1],[\"==\",\"b\",2]]");
        
        mbfilter = new MBFilter(json);
        filter = mbfilter.filter();
        assertEquals("NOT (a = 1) AND NOT (b = 2)", ECQL.toCQL(filter) );
    }

}
