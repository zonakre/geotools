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
package org.geotools.mbstyle.sprite;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.geotools.mbstyle.parse.MBFormatException;
import org.geotools.renderer.style.ExternalGraphicFactory;
import org.geotools.renderer.style.ImageGraphicFactory;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.logging.Logging;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opengis.feature.Feature;
import org.opengis.filter.expression.Expression;

/**
 * 
 * <p>
 * Implementation of an {@link ExternalGraphicFactory} that takes the base address of a Mapbox sprite resource and an icon name, and retrieves the
 * icon from the sprite sheet.
 * </p>
 * 
 * <p>
 * Note that this factory expects a slightly modified URL of the following form:
 * </p>
 * 
 * <code>{baseUrl}?icon={iconName}</code>
 * 
 * <p>
 * Only the baseUrl is used to retrieve the sprite sheet (at {baseUrl}.png) and sprite index (at {baseUrl}.json). The iconName is then used by this
 * factory to select the correct icon from the spritesheet.
 * </p>
 * 
 * For example, for the following Mapbox style:
 * 
 * <pre>
 * {
 *  "version": 8,
 *  "name": "Mapbox Streets",
 *  "sprite": "mapbox://sprites/mapbox/streets-v8",
 *  "glyphs": "mapbox://fonts/mapbox/{fontstack}/{range}.pbf",
 *  "sources": {...},
 *  "layers": [...]
 * }
 * </pre>
 *
 * <p>
 * If a layer in this style references an icon in the spritesheet, e.g. iconName, then the URL for the external graphic should be
 * <code>mapbox://sprites/mapbox/streets-v?icon=iconName</code>
 * </p>
 * 
 * @see <a href="https://www.mapbox.com/mapbox-gl-js/style-spec/#sprite">https://www.mapbox.com/mapbox-gl-js/style-spec/#sprite</a>
 * 
 */
public class MapboxGraphicFactory implements ExternalGraphicFactory {

    /**
     * {@link ExternalGraphic} instances with this format will be handled by the {@link MapboxGraphicFactory}.
     */
    public static final String FORMAT = "mbsprite";

    JSONParser jsonParser = new JSONParser();

    protected static Map<URL, BufferedImage> imageCache = Collections
            .synchronizedMap(new SoftValueHashMap<>());

    protected static Map<URL, JSONObject> indexCache = Collections
            .synchronizedMap(new SoftValueHashMap<>());

    private static final Logger LOGGER = Logging.getLogger(MapboxGraphicFactory.class);

    @Override
    public Icon getIcon(Feature feature, Expression url, String format, int size) throws Exception {

        // Only handle the correct format
        if (!FORMAT.equalsIgnoreCase(format.trim())) {
            return null;
        }

        URL loc = url.evaluate(feature, URL.class);
        URL baseUrl = parseBaseUrl(loc);
        String iconName = parseIconName(loc);

        // Retrieve and parse the sprite index file.
        JSONObject spriteIndexJson = getSpriteIndex(baseUrl);

        IconInfo iconInfo = parseIconInfoFromIndex(spriteIndexJson, iconName);

        // Retrieve the sprite sheet and get the icon as a sub image
        BufferedImage spriteImg = getSpriteSheet(baseUrl);
        BufferedImage iconSubImg = spriteImg.getSubimage(iconInfo.getX(), iconInfo.getY(),
                iconInfo.getWidth(), iconInfo.getHeight());

        // Use "size" to scale the image, if > 0
        if (size > 0 && iconSubImg.getHeight() != size) {
            double dsize = (double) size;

            double scaleY = dsize / iconSubImg.getHeight(); // >1 if you're magnifying
            double scaleX = scaleY; // keep aspect ratio!

            AffineTransform scaleTx = AffineTransform.getScaleInstance(scaleX, scaleY);
            AffineTransformOp ato = new AffineTransformOp(scaleTx, AffineTransformOp.TYPE_BILINEAR);
            iconSubImg = ato.filter(iconSubImg, null);
        }

        return new ImageIcon(iconSubImg);

    }

    /**
     * Parse the "icon" query parameter from the provided {@link URL}.
     * 
     * @param url The url from which to parse the icon query parameter.
     * @return The first value of the "icon" query parameter.
     * @throws IllegalArgumentException If the URL does not have "icon" as a query parameter.
     */
    protected String parseIconName(URL url) {
        String queryString = url.getQuery();
        if (queryString == null) {
            throw new IllegalArgumentException(
                    "Mapbox-style sprite urls must have the icon name as a query parameter, but no query string was provided. Url: "
                            + url.toExternalForm());
        }

        String[] paramPairs = queryString.split("&");
        for (String s : paramPairs) {
            String[] kv = s.split("=", 2);
            if (kv.length == 2 && "icon".equalsIgnoreCase(kv[0])) {
                return kv[1];
            }
        }

        throw new IllegalArgumentException(
                "Mapbox-style sprite urls must have icon={iconName} as a query parameter. URL was: "
                        + queryString);
    }

    /**
     * Return the base URL (without query parameters) from the provided URL.
     * 
     * @param loc The URL from which to remove query parameters.
     * @return The URL, without query parameters.
     * @throws MalformedURLException
     */
    protected URL parseBaseUrl(URL loc) throws MalformedURLException {
        String urlStr = loc.toExternalForm();
        int idx = urlStr.indexOf('?');
        if (idx == -1) {
            return new URL(urlStr);
        } else {
            return new URL(urlStr.substring(0, idx));
        }
    }

    /**
     * Parse the {@link IconInfo} for the provided iconName in the provided icon index.
     * 
     * @param iconIndex The Mapbox icon index file.
     * @param iconName The name of the icon in the index file.
     * @return An {@link IconInfo} for the icon.
     */
    private IconInfo parseIconInfoFromIndex(JSONObject iconIndex, String iconName) {
        if (!iconIndex.containsKey(iconName)) {
            throw new MapboxSpriteException(
                    "Sprite index file does not contain entry for icon with name: " + iconName);
        }

        Object o = iconIndex.get(iconName);
        if (!(o instanceof JSONObject)) {
            throw new MapboxSpriteException("Error parsing sprite index for \"" + iconName
                    + "\": Expected JSONObject, but is " + o.getClass().getSimpleName());
        }
        return new IconInfo((JSONObject) o);
    }

    /**
     * 
     * Retrieve the sprite sheet index for the provided sprite base url. The base url should have no extension.
     * 
     * @param baseUrl he base URL of the Mapbox sprite source (no extension).
     * @return The sprite sheet index
     * @throws IOException
     */
    private JSONObject getSpriteIndex(URL baseUrl) throws IOException {
        JSONObject spriteIndex = indexCache.get(baseUrl);
        if (spriteIndex == null) {
            String indexUrlStr = baseUrl.toExternalForm() + ".json";
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new URL(indexUrlStr).openStream()))) {
                spriteIndex = (JSONObject) jsonParser.parse(reader);
            } catch (ParseException e) {
                throw new MapboxSpriteException(
                        "Exception parsing sprite index file from: " + indexUrlStr, e);
            }
        }
        return spriteIndex;
    }

    /**
     * Retrieve the sprite sheet for the provided sprite base url. The base url should have no extension.
     * 
     * @param baseUrl The base URL of the Mapbox sprite source (no extension).
     * @return An optional for the sprite sheet; empty if it could not be retrieved.
     */
    private BufferedImage getSpriteSheet(URL baseUrl) {
        BufferedImage image = imageCache.get(baseUrl);
        if (image == null) {
            try {
                URL spriteSheetUrl = new URL(baseUrl.toExternalForm() + ".png");
                image = ImageIO.read(spriteSheetUrl);
            } catch (Exception e) {
                LOGGER.warning("Unable to retrieve sprite sheet from location: "
                        + baseUrl.toExternalForm() + " (" + e.getMessage() + ")");
                throw new MapboxSpriteException(
                        "Failed to retrieve sprite sheet for baseUrl: " + baseUrl.toExternalForm(),
                        e);
            }
            imageCache.put(baseUrl, image);
        }
        return image;
    }

    /**
     * Wrapper around a sprite index entry for a single icon. For example, here is an index entry for an icon named "poi":
     * 
     * <code>
     * "poi": {
     *  "width": 32,
     *  "height": 32,
     *  "x": 0,
     *  "y": 0,
     *  "pixelRatio": 1
     * }
     * </code>
     * 
     * 
     * @see <a href="https://www.mapbox.com/mapbox-gl-js/style-spec/#sprite">https://www.mapbox.com/mapbox-gl-js/style-spec/#sprite</a>
     *
     */
    private static class IconInfo {
        protected JSONObject json;

        public IconInfo(JSONObject json) {
            super();
            this.json = json;
        }

        public int getWidth() {
            return intOrException("width");
        }

        public int getHeight() {
            return intOrException("height");
        }

        public int getX() {
            return intOrException("x");
        }

        public int getY() {
            return intOrException("y");
        }

        public int getPixelRatio() {
            return intOrException("pixelRatio");
        }

        private int intOrException(String k) {
            if (!json.containsKey(k)) {
                throw new MBFormatException("");
            }
            Object o = json.get(k);
            if (o instanceof Number) {
                return ((Number) o).intValue();
            } else if (o instanceof String) {
                return Integer.valueOf((String) o);
            } else {
                throw new MBFormatException("");
            }
        }

    }

}
