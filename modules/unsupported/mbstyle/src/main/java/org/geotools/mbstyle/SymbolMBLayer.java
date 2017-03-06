package org.geotools.mbstyle;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.mbstyle.CircleMBLayer.CirclePitchScale;
import org.geotools.mbstyle.CircleMBLayer.CircleTranslateAnchor;
import org.geotools.mbstyle.FillMBLayer.FillTranslateAnchor;
import org.geotools.mbstyle.parse.MBFormatException;
import org.geotools.mbstyle.parse.MBObjectParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opengis.filter.expression.Expression;

public class SymbolMBLayer extends MBLayer {

    private JSONObject layout;

    private JSONObject paint;

    private static String TYPE = "symbol";

    public static enum SymbolPlacement {
        /**
         * The label is placed at the point where the geometry is located.
         */
        POINT,

        /**
         * The label is placed along the line of the geometry. Can only be used on LineString and Polygon geometries.
         */
        LINE
    }

    /**
     * Interpreted differently when applied to different fields.
     */
    public static enum Alignment {
        MAP, VIEWPORT, AUTO
    }

    public static enum IconTextFit {
        /**
         * The icon is displayed at its intrinsic aspect ratio.
         */
        NONE,

        /**
         * The icon is scaled in the x-dimension to fit the width of the text.
         */
        WIDTH,

        /**
         * The icon is scaled in the y-dimension to fit the height of the text.
         */
        HEIGHT,

        /**
         * The icon is scaled in both x- and y-dimensions.
         */
        BOTH
    }

    public static enum Justification {
        /**
         * The text is aligned to the left.
         */
        LEFT,

        /**
         * The text is centered.
         */
        CENTER,

        /**
         * The text is aligned to the right.
         */
        RIGHT
    }

    public static enum TextAnchor {
        /**
         * The center of the text is placed closest to the anchor.
         */
        CENTER,

        /**
         * The left side of the text is placed closest to the anchor.
         */
        LEFT,

        /**
         * The right side of the text is placed closest to the anchor.
         */
        RIGHT,

        /**
         * The top of the text is placed closest to the anchor.
         */
        TOP,

        /**
         * The bottom of the text is placed closest to the anchor.
         */
        BOTTOM,

        /**
         * The top left corner of the text is placed closest to the anchor.
         */
        TOP_LEFT,

        /**
         * The top right corner of the text is placed closest to the anchor.
         */
        TOP_RIGHT,

        /**
         * The bottom left corner of the text is placed closest to the anchor.
         */
        BOTTOM_LEFT,

        /**
         * The bottom right corner of the text is placed closest to the anchor.
         */
        BOTTOM_RIGHT,

    }

    public static enum TextTransform {
        /**
         * The text is not altered.
         */
        NONE,

        /**
         * Forces all letters to be displayed in uppercase.
         */
        UPPERCASE,

        /**
         * Forces all letters to be displayed in lowercase.
         */
        LOWERCASE,

    }

    public static enum TranslateAnchor {
        /**
         * Translation relative to the map.
         */
        MAP,

        /**
         * Translation relative to the viewport.
         */
        VIEWPORT
    }

    /**
     * 
     * @param json
     */
    public SymbolMBLayer(JSONObject json) {
        super(json,new MBObjectParser(SymbolMBLayer.class));
        paint = super.getPaint();
        layout = super.getLayout();
    }

    /**
	 * (Optional) One of point, line. Defaults to point.
	 * 
	 * Label placement relative to its geometry.
	 * 
	 * @return SymbolPlacement
	 * @throws MBFormatException
	 */
    public SymbolPlacement getSymbolPlacement() {
    	Object value = paint.get("symbol-placement");
		if (value != null && "line".equalsIgnoreCase((String) value)) {
			return SymbolPlacement.LINE;
		} else {
			return SymbolPlacement.POINT;
		}
    }

    /**
     *  (Optional) Units in pixels. Defaults to 250. Requires SymbolPlacement.LINE
     *  
     *  Distance between two symbol anchors.
     * 
     * @return Number representing distance between two symbol anchors
     * @throws MBFormatException
     */
    public Number getSymbolSpacing() throws MBFormatException {
    	return parse.optional(Integer.class, layout, "symbol-spacing", 250);
    }

    /**
	 * Access symbol-spacing.
	 * 
	 * @return Access symbol-spacing as literal or function expression,
	 *         defaults to 250.
	 * @throws MBFormatException
	 */
    public Expression symbolSpacing() throws MBFormatException {
        return parse.percentage(layout, "symbol-spacing", 250);
    }

    /**
     * (Optional) Defaults to false.
     * 
     * If true, the symbols will not cross tile edges to avoid mutual collisions. Recommended in layers that don't have enough padding in the vector tile to prevent collisions, or if it is a point symbol layer placed after a line symbol layer.
     * @return  Boolean
     * @throws MBFormatException
     */
    public Boolean getSymbolAvoidEdges() throws MBFormatException {
        return parse.getBoolean(layout, "symbol-avoid-edges", false);
    }

    /**
     * (Optional) Defaults to false. Requires icon-image.
     * 
     * If true, the icon will be visible even if it collides with other previously drawn symbols.
     * @return Boolean
     * @throws MBFormatException
     */
    public Boolean getIconAllowOverlap() throws MBFormatException {
        return parse.getBoolean(layout, "ico-allow-overlap", false);
    }

    /**
     * (Optional) Defaults to false. Requires icon-image.
     * 
     * If true, other symbols can be visible even if they collide with the icon.
     * @return Boolean
     * @throws MBFormatException
     */
    public Boolean getIconIgnorePlacement() throws MBFormatException {
        return parse.getBoolean(layout, "icon-ignore-placement", false);
    }

    /**
     * (Optional) Defaults to false. Requires icon-image. Requires text-field.
     * 
     * If true, text will display without their corresponding icons when the icon collides with other symbols and the text does not.
     * @return Boolean
     * @throws MBFormatException
     */
    public Boolean getIconOptional() throws MBFormatException {
        return parse.getBoolean(layout, "icon-optional", false);
    }

    /**
     * Optional enum. One of map, viewport, auto. Defaults to auto. Requires icon-image. In combination with symbol-placement, determines the rotation
     * behavior of icons.
     * 
     * Possible values:
     * 
     * {@link Alignment#MAP} When symbol-placement is set to point, aligns icons east-west. When symbol-placement is set to line, aligns icon x-axes
     * with the line.
     * 
     * {@link Alignment#VIEWPORT} Produces icons whose x-axes are aligned with the x-axis of the viewport, regardless of the value of
     * symbol-placement.
     * 
     * {@link Alignment#AUTO} When symbol-placement is set to point, this is equivalent to viewport. When symbol-placement is set to line, this is
     * equivalent to map.
     * 
     * @return
     */
    public Alignment getIconRotationAlignment() {
    	Object value = layout.get("icon-rotation-alignment");
		if (value != null && "map".equalsIgnoreCase((String) value)) {
			return Alignment.MAP;
		} else if (value != null && "viewport".equalsIgnoreCase((String) value)){
			return Alignment.VIEWPORT;
		} else {
			return Alignment.AUTO;
		}
		
    }

    /**
     * (Optional) Defaults to 1. Requires icon-image.
     * 
     * Scale factor for icon. 1 is original size, 3 triples the size.
     * @return Number
     * @throws MBFormatException
     */
    public Number getIconSize() throws MBFormatException{
        return parse.optional(Number.class, layout, "icon-size", 1.0);
    }

    /**
     * Access icon-size.
	 * 
	 * @return Access icon-size as literal or function expression, defaults to 1.
	 * @throws MBFormatException 
     * @return
     */
    public Expression iconSize() {
        return parse.percentage(layout, "icon-size", 1.0);
    }

    /**
     *  (Optional) One of none, width, height, both. Defaults to none. Requires icon-image. Requires text-field.
     *  Scales the icon to fit around the associated text.
     *  
     * @return IconTextFit
     * 
     */
    public IconTextFit getIconTextFit() {
        Object value = layout.get("icon-text-fit");
        if (value != null && "width".equalsIgnoreCase((String) value)){
        	return IconTextFit.WIDTH;
        } else if (value != null && "height".equalsIgnoreCase((String) value)){
        	return IconTextFit.HEIGHT;
        } else if (value != null && "both".equalsIgnoreCase((String) value)){
        	return IconTextFit.BOTH;
        } else {
        	return IconTextFit.NONE;
        }

    }

    /**
     * (Optional) Units in pixels. Defaults to 0,0,0,0. Requires icon-image. Requires text-field. Requires icon-text-fit = one of both, width, height.
     * 
     * Size of the additional area added to dimensions determined by icon-text-fit, in clockwise order: top, right, bottom, left.
     * @return
     */
    public List<Number> getIconTextFitPadding() {
    	// TODO
        // json.get("icon-text-fit-padding")
        return null;
    }

    public Expression iconTextFitPadding() {
    	// TODO
        // json.get("icon-text-fit-padding")
        return null;
    }

    /**
     * (Optional) A string with {tokens} replaced, referencing the data property to pull from.
     * @return String
     * @throws MBFormatException
     */
    public String getIconImage() throws MBFormatException {
        return parse.optional(String.class, layout, "icon-image", null);
    }

    /**
     * Access icon-image as literal or function expression
     * @throws MBFormatException
     * 
     */
    public Expression iconImage() {
        return parse.string(layout, "icon-image", "");
    }

    /**
     * (Optional) Units in degrees. Defaults to 0. Requires icon-image.
     * 
     * Rotates the icon clockwise.
     * @return Number
     * @throws MBFormatException
     */
    public Number getIconRotate() throws MBFormatException {
        return parse.optional(Number.class, layout, "icon-rotate", 0.0);
    }

    /**
     * Access icon-rotate as literal or function expression
     * @throws MBFormatException
     */
    public Expression iconRotate() throws MBFormatException {
        return parse.percentage(layout, "icon-rotate", 0);
    }

    /**
     * (Optional) Units in pixels. Defaults to 2. Requires icon-image.
     * 
     * Size of the additional area around the icon bounding box used for detecting symbol collisions.
     * @return Number
     * @throws MBFormatException
     */
    public Number getIconPadding() throws MBFormatException {
        return parse.optional(Number.class, layout, "icon-padding", 2.0);
    }

    /**
     * Access icon-padding as literal or function expression
     * @throws MBFormatException
     */
    public Expression iconPadding() throws MBFormatException {
        return parse.percentage(layout, "icon-padding", 2.0);
    }

    /**
     * (Optional) Defaults to false. Requires icon-image. Requires icon-rotation-alignment = map. Requires symbol-placement = line.
     * 
     * If true, the icon may be flipped to prevent it from being rendered upside-down.
     * @return Boolean
     * @throws MBFormatException
     */
    public Boolean getIconKeepUpright() throws MBFormatException {
        return parse.getBoolean(layout, "icon-keep-upright", false);
    }

    /**
     * (Optional) Defaults to 0,0. Requires icon-image.
     * 
     * Offset distance of icon from its anchor. Positive values indicate right and down, while negative values indicate left and up. When combined with icon-rotate the offset will be as if the rotated direction was up.
     * @return double []
     * @throws MBFormatException
     */
    public double[] getIconOffset() throws MBFormatException {
    	return parse.array(layout, "icon-offset", new double[] { 0.0, 0.0 });
    }

    /**
	 * Access icon-offset
	 * 
	 * @return Point
	 * @throws MBFormatException
	 */
    public Point iconOffset() throws MBFormatException {
    	if (layout.get("icon-offset") != null) {
			JSONArray array = (JSONArray) layout.get("icon-offset");
			Number x = (Number) array.get(0);
			Number y = (Number) array.get(1);
			return new Point(x.intValue(), y.intValue());
		} else {
			return new Point(0, 0);
		}
    }

    /**
     * 
     * Optional enum. One of map, viewport, auto. Defaults to auto. Requires text-field. Orientation of text when map is pitched.
     * 
     * Possible values:
     * 
     * {@link Alignment#MAP} The text is aligned to the plane of the map.
     * 
     * {@link Alignment#VIEWPORT} The text is aligned to the plane of the viewport.
     * 
     * {@link Alignment#AUTO} Automatically matches the value of text-rotation-alignment.
     * 
     * @return Alignment
     */
    public Alignment getTextPitchAlignment() {
    	Object value = layout.get("text-pitch-alignment");
		if (value != null && "map".equalsIgnoreCase((String) value)) {
			return Alignment.MAP;
		} else if (value != null && "viewport".equalsIgnoreCase((String) value)){
			return Alignment.VIEWPORT;
		} else {
			return Alignment.AUTO;
		}
    }

    /**
     * 
     * Optional enum. One of map, viewport, auto. Defaults to auto. Requires text-field. In combination with symbol-placement, determines the rotation
     * behavior of the individual glyphs forming the text.
     * 
     * Possible values:
     * 
     * {@link Alignment#MAP} When symbol-placement is set to point, aligns text east-west. When symbol-placement is set to line, aligns text x-axes
     * with the line.
     * 
     * {@link Alignment#VIEWPORT} Produces glyphs whose x-axes are aligned with the x-axis of the viewport, regardless of the value of
     * symbol-placement.
     * 
     * {@link Alignment#AUTO} When symbol-placement is set to point, this is equivalent to viewport. When symbol-placement is set to line, this is
     * equivalent to map.
     * 
     * @return
     */
    public Alignment getTextRotationAlignment() {
    	Object value = layout.get("text-rotation-alignment");
		if (value != null && "map".equalsIgnoreCase((String) value)) {
			return Alignment.MAP;
		} else if (value != null && "viewport".equalsIgnoreCase((String) value)){
			return Alignment.VIEWPORT;
		} else {
			return Alignment.AUTO;
		}
    }

    /**
     * (Optional) Value to use for a text label. Feature properties are specified using tokens like {field_name}.
     * 
     * @return String
     * @throws MBFormatException
     */
    public String getTextField() throws MBFormatException {
        return parse.optional(String.class, layout, "text-field", "");
    }

    /**
     * Access text-field as literal or function expression
     * @return String
     * @throws MBFormatException
     */
    public Expression textField() throws MBFormatException {
    	return parse.string(layout, "text-field", "");
    }

    /**
     * (Optional) Font stack to use for displaying text. 
     * 
     * Defaults to <code>["Open Sans Regular","Arial Unicode MS Regular"]</code>. Requires text-field. 
     * 
     */
    public List<String> getTextFont() {
        String[] fonts = parse.array(String.class, layout, "text-font",
                new String[] { "Open Sans Regular", "Arial Unicode MS Regular" });
        return Arrays.asList(fonts); 
    }

    /**
     * Access text-font as a literal or function expression.
     */
    public List<Expression> textFont() {
        List<Expression> fontExpressions = new ArrayList<>();
        String[] fonts = parse.array(String.class, layout, "text-font", new String[] {"Open Sans Regular","Arial Unicode MS Regular"});
    	for (int i = 0; i < fonts.length; i++) {
    	    fontExpressions.add(ff.literal(fonts[i]));
    	}
    	return fontExpressions;
    }

    /**
     * (Optional) Units in pixels. Defaults to 16. Requires text-field.
     * 
     * Font size.
     * @return Number
     * @throws MBFormatException
     */
    public Number getTextSize() throws MBFormatException {
        return parse.optional(Number.class, layout, "text-size", 16.0);
    }

    /**
     * Access text-size as literal or function expression
     * @throws MBFormatException
     */
    public Expression textSize() throws MBFormatException {
        return parse.percentage(layout, "text-size", 16.0);
    }

    /**
     * (Optional) Units in ems. Defaults to 10. Requires text-field.
     * 
     * The maximum line width for text wrapping.
     * @return Number
     * @throws MBFormatException
     */
    public Number getTextMaxWidth() throws MBFormatException {
        return parse.optional(Number.class, layout, "text-max-width", 10.0);
    }

    /**
     * Access text-max-width as literal or function expression
     * @throws MBFormatException
     */
    public Expression textMaxWidth() throws MBFormatException {
        return parse.percentage(layout, "text-max-width", 10.0);
    }

    /**
     * (Optional) Units in ems. Defaults to 1.2. Requires text-field.
     * 
     * Text leading value for multi-line text.
     * @return Number
     * @throws MBFormatException
     */
    public Number getTextLineHeight() throws MBFormatException {
    	return parse.optional(Number.class, layout, "text-line-height", 1.2);
    }

    /**
     * Access text-line-height as literal or function expression
     * @throws MBFormatException
     */
    public Expression textLineHeight() throws MBFormatException {
    	return parse.percentage(layout, "text-line-height", 1.2);
    }

    /**
     *  (Optional) Units in ems. Defaults to 0. Requires text-field.
     *  
     *  Text tracking amount.
     * @return Number
     * @throws MBFormatException
     */
    public Number getTextLetterSpacing() throws MBFormatException {
    	return parse.optional(Number.class, layout, "text-letter-spacing", 0.0);
    }

    /**
     * Access text-line-height as literal or function expression
     * @throws MBFormatException
     */
    public Expression textLetterSpacing() throws MBFormatException {
    	return parse.percentage(layout, "text-letter-spacing", 0.0);
    }

    /**
     * 
     * Optional enum. One of left, center, right. Defaults to center. Requires text-field.
     * 
     * Text justification options:
     * 
     * {@link Justification#LEFT} The text is aligned to the left.
     * 
     * {@link Justification#CENTER} The text is centered.
     * 
     * {@link Justification#RIGHT} The text is aligned to the right.
     * 
     * @return Justification
     */
    public Justification getTextJustify() {
    	Object value = layout.get("text-justify");
		if (value != null && "left".equalsIgnoreCase((String) value)) {
			return Justification.LEFT;
		} else if (value != null && "right".equalsIgnoreCase((String) value)){
			return Justification.RIGHT;
		} else {
			return Justification.CENTER;
		}
    }

    /**
     * 
     * Optional enum. One of center, left, right, top, bottom, top-left, top-right, bottom-left, bottom-right. Defaults to center. Requires text-field.  Part of the text placed closest to the anchor.
     * 
     * {@link TextAnchor#CENTER} The center of the text is placed closest to the anchor.
     * 
     * {@link TextAnchor#LEFT} The left side of the text is placed closest to the anchor.
     * 
     * {@link TextAnchor#RIGHT} The right side of the text is placed closest to the anchor.
     * 
     * {@link TextAnchor#TOP} The top of the text is placed closest to the anchor.
     * 
     * {@link TextAnchor#BOTTOM} The bottom of the text is placed closest to the anchor.
     * 
     * {@link TextAnchor#TOP_LEFT} The top left corner of the text is placed closest to the anchor.
     * 
     * {@link TextAnchor#TOP_RIGHT} The top right corner of the text is placed closest to the anchor.
     * 
     * {@link TextAnchor#BOTTOM_LEFT} The bottom left corner of the text is placed closest to the anchor.
     * 
     * {@link TextAnchor#BOTTOM_RIGHT} The bottom right corner of the text is placed closest to the anchor.
     * 
     * @return TextAnchor
     */
    public TextAnchor getTextAnchor() {
    	Object value = layout.get("text-anchor");
		if (value != null && "left".equalsIgnoreCase((String) value)) {
			return TextAnchor.LEFT;
		} else if (value != null && "right".equalsIgnoreCase((String) value)){
			return TextAnchor.RIGHT;
		} else if (value != null && "top".equalsIgnoreCase((String) value)){
			return TextAnchor.TOP;
		} else if (value != null && "bottom".equalsIgnoreCase((String) value)){
			return TextAnchor.BOTTOM;
		} else if (value != null && "top-left".equalsIgnoreCase((String) value)){
			return TextAnchor.TOP_LEFT;
		} else if (value != null && "top-right".equalsIgnoreCase((String) value)){
			return TextAnchor.TOP_RIGHT;
		} else if (value != null && "bottom-left".equalsIgnoreCase((String) value)){
			return TextAnchor.BOTTOM_LEFT;
		} else if (value != null && "bottom-right".equalsIgnoreCase((String) value)){
			return TextAnchor.BOTTOM_RIGHT;
		} else {
			return TextAnchor.CENTER;
		}
    }

    /**
     * (Optional) Units in degrees. Defaults to 45. Requires text-field. Requires symbol-placement = line.
     * 
     * Maximum angle change between adjacent characters.
     * 
     * @return Number
     * @throws MBFormatException
     */
    public Number getTextMaxAngle() throws MBFormatException {
    	return parse.optional(Number.class, layout, "text-max-angle", 45.0);
    }

    /**
     * Access text-max-angle as literal or function expression
     * @throws MBFormatException
     */
    public Expression textMaxAngle() {
    	return parse.percentage(layout, "text-max-angle", 45.0);
    }

    /**
     * (Optional) Units in degrees. Defaults to 0. Requires text-field.
     * 
     * Rotates the text clockwise.
     * @return Number
     * @throws MBFormatException
     */
    public Number getTextRotate() throws MBFormatException {
    	return parse.optional(Number.class, layout, "text-rotate", 0.0);
    }

    /**
     * Access text-rotate as literal or function expression
     * @throws MBFormatException
     */
    public Expression textRotate() throws MBFormatException {
    	return parse.percentage(layout, "text-rotate", 0.0);
    }

    /**
     * (Optional) Units in pixels. Defaults to 2. Requires text-field.
     * 
     * Size of the additional area around the text bounding box used for detecting symbol collisions.
     * @return Number
     * @throws MBFormatException
     */
    public Number getTextPadding() throws MBFormatException {
    	return parse.optional(Number.class, layout, "text-padding", 2.0);
    }

    /**
     * Access text-padding as literal or function expression
     * @throws MBFormatException
     */
    public Expression textPadding() throws MBFormatException {
    	return parse.percentage(layout, "text-padding", 2.0);
    }

    /**
     * (Optional) Defaults to true. Requires text-field. Requires text-rotation-alignment = map. Requires symbol-placement = line.
     * 
     * If true, the text may be flipped vertically to prevent it from being rendered upside-down.
     * @return Boolean
     * @throws MBFormatException
     */
    public Boolean getTextKeepUpright() throws MBFormatException {
    	return parse.getBoolean(layout, "text-keep-upright", true);
    }

    /**
     * 
     * One of none, uppercase, lowercase. Defaults to none. Requires text-field.
     * 
     * Specifies how to capitalize text, similar to the CSS text-transform property.
     * 
     * {@link TextTransform#NONE} The text is not altered.
     * 
     * {@link TextTransform#UPPERCASE} Forces all letters to be displayed in uppercase.
     * 
     * {@link TextTransform#LOWERCASE} Forces all letters to be displayed in lowercase.
     * 
     * @return TextTransform
     */
    public TextTransform getTextTransform() {
    	Object value = layout.get("text-transform");
		if (value != null && "uppercase".equalsIgnoreCase((String) value)) {
			return TextTransform.UPPERCASE;
		} else if (value != null && "lowercase".equalsIgnoreCase((String) value)){
			return TextTransform.LOWERCASE;
		} else {
			return TextTransform.NONE;
		}
    }

    /**
     * (Optional) Units in ems. Defaults to 0,0. Requires text-field.
     * 
     * Offset distance of text from its anchor. Positive values indicate right and down, while negative values indicate left and up.
     * @return double[]
     * @throws MBFormatException
     */
    public double[] getTextOffset() throws MBFormatException {
    	return parse.array(layout, "text-offset", new double[] { 0.0, 0.0 });
    }

    /**
	 * Access text-offset
	 * 
	 * @return Point
	 * @throws MBFormatException
	 */
    public Point textOffset() throws MBFormatException {
    	if (layout.get("text-offset") != null) {
			JSONArray array = (JSONArray) layout.get("text-offset");
			Number x = (Number) array.get(0);
			Number y = (Number) array.get(1);
			return new Point(x.intValue(), y.intValue());
		} else {
			return new Point(0, 0);
		}
    }

    /**
     * (Optional) Defaults to false. Requires text-field.
     * 
     * If true, the text will be visible even if it collides with other previously drawn symbols.
     * @return Boolean
     * @throws MBFormatException
     */
    public Boolean getTextAllowOverlap() throws MBFormatException {
    	return parse.getBoolean(layout, "text-allow-overlap", false);
    }

    /**
     * Defaults to false. Requires text-field.
     * 
     * If true, other symbols can be visible even if they collide with the text.
     * @return Boolean
     * @throws MBFormatException
     */
    public Boolean getTextIgnorePlacement() throws MBFormatException {
    	return parse.getBoolean(layout, "text-ignore-placement", false);
    }

    /**
     * Defaults to false. Requires text-field. Requires icon-image.
     * 
     * If true, icons will display without their corresponding text when the text collides with other symbols and the icon does not.
     * 
     * @return Boolean
     * @throws MBFormatException
     */
    public Boolean getTextOptional() throws MBFormatException {
    	return parse.getBoolean(layout, "text-optional", false);
    }

    /**
     * (Optional) Defaults to 1. Requires icon-image.
     * 
     * The opacity at which the icon will be drawn.
     * @return Number
     * @throws MBFormatException
     */
    public Number getIconOpacity() throws MBFormatException {
    	return parse.optional(Number.class, paint, "icon-opacity", 1.0);
    }

    /**
     * Access icon-opacity as literal or function expression
     * @throws MBFormatException
     */
    public Expression iconOpacity() throws MBFormatException {
    	return parse.percentage(paint, "icon-opacity", 1.0);
    }
    
    /**
     * (Optional) Defaults to #000000. Requires icon-image.
     * 
     * The color of the icon. This can only be used with sdf icons.
     */
    public Color getIconColor() {
    	return parse.optional(Color.class, paint, "icon-color", Color.BLACK );
    }
    
    /** Access icon-color as literal or function expression, defaults to black. */
    public Expression iconColor() {      
        return parse.color(paint, "icon-color", Color.BLACK);
    }

    /**
     * (Optional) Defaults to rgba(0, 0, 0, 0). Requires icon-image.
     * 
     * The color of the icon's halo. Icon halos can only be used with SDF icons.
     */
    public Color getIconHaloColor() {
    	return parse.optional(Color.class, paint, "icon-halo-color", new Color(0,0,0,0));
    }

    /** Access icon-halo-color as literal or function expression, defaults to black. */
    public Expression iconHaloColor() {
    	return parse.color(paint, "icon-halo-color", Color.BLACK);
    }

    /**
     * (Optional) Units in pixels. Defaults to 0. Requires icon-image.
     * 
     * Distance of halo to the icon outline.
     * @return Number
     * @throws MBFormatException
     */
    public Number getIconHaloWidth() throws MBFormatException {
    	return parse.optional(Number.class, paint, "icon-halo-width", 0.0);
    }

    /**
     * Access icon-halo-width as literal or function expression
     * @throws MBFormatException
     */
    public Expression iconHaloWidth() {
    	return parse.percentage(paint, "icon-halo-width", 0.0);
    }

    /**
     * (Optional) Units in pixels. Defaults to 0. Requires icon-image.
     * 
     * Fade out the halo towards the outside.
     * @return Number
     * @throws MBFormatException
     */
    public Number getIconHaloBlur() throws MBFormatException {
    	return parse.optional(Number.class, paint, "icon-halo-blur", 0.0);
    }

    /**
     * Access icon-halo-blur as literal or function expression
     * @throws MBFormatException
     */
    public Expression iconHaloBlur() {
    	return parse.percentage(paint, "icon-halo-blur", 0.0);
    }

    /**
     * Units in pixels. Defaults to 0,0. Requires icon-image.
     * 
     * Distance that the icon's anchor is moved from its original placement. Positive values indicate right and down, while negative values indicate left and up.
     * @return double[]
     * @throws MBFormatException
     */
    public double[] getIconTranslate() throws MBFormatException {
    	return parse.array( paint, "icon-translate", new double[]{ 0.0, 0.0 } ); 
    }

    public Point iconTranslate() {
    	if (paint.get("icon-translate") != null) {
            JSONArray array = (JSONArray) paint.get("icon-translate");
            Number x = (Number) array.get(0);
            Number y = (Number) array.get(1);
            return new Point(x.intValue(), y.intValue());
        } else {
            return new Point(0, 0);
        }
    }

    /**
     * (Optional) One of map, viewport. Defaults to map. Requires icon-image. Requires icon-translate.
     * 
     * Controls the translation reference point.
     * 
     * {@link TranslateAnchor#MAP}: Icons are translated relative to the map.
     * 
     * {@link TranslateAnchor#VIEWPORT}: Icons are translated relative to the viewport.
     * 
     * Defaults to {@link TranslateAnchor#MAP}.
     * 
     */
    public TranslateAnchor getIconTranslateAnchor() {
    	Object value = paint.get("icon-translate-anchor");
        if (value != null && "viewport".equalsIgnoreCase((String) value)) {
            return TranslateAnchor.VIEWPORT;
        } else {
            return TranslateAnchor.MAP;
        }
    }

    /**
     * (Optional) Defaults to 1. Requires text-field.
     * 
     * The opacity at which the text will be drawn.
     * @return Number
     * @throws MBFormatException
     */
    public Number getTextOpacity() throws MBFormatException {
    	return parse.optional(Number.class, paint, "text-opacity", 1.0);
    }

    /**
     * Access text-opacity as literal or function expression
     * @throws MBFormatException
     */
    public Expression textOpacity() throws MBFormatException {
    	return parse.percentage(paint, "text-opacity", 1.0);
    }

    /**
     * Defaults to #000000. Requires text-field.
     * 
     * The color with which the text will be drawn.
     * @return Color
     * @throws MBFormatException
     */
    public Color getTextColor() throws MBFormatException {
        return parse.convertToColor(parse.optional(String.class, paint, "text-color", "#000000"));
    }
    
    /** Access text-color as literal or function expression, defaults to black. */
    public Expression textColor() {      
        return parse.color(paint, "text-color", Color.BLACK);
    }

    /**
     * Defaults to rgba(0, 0, 0, 0). Requires text-field.
     * 
     * The color of the text's halo, which helps it stand out from backgrounds.
     * @return Color
     * @throws MBFormatException
     */
    public Color getTextHaloColor() throws MBFormatException {
        if (!paint.containsKey("text-halo-color")) {
            return new Color(0,0,0,0);
        } else {
            return parse.convertToColor(parse.optional(String.class, paint, "text-halo-color", "#000000"));             
        }
    }

    /** Access text-halo-color as literal or function expression, defaults to black. */
    public Expression textHaloColor() {
    	return parse.color(paint, "text-halo-color", Color.BLACK);
    }

    /**
     * (Optional) Units in pixels. Defaults to 0. Requires text-field.
     * 
     * Distance of halo to the font outline. Max text halo width is 1/4 of the font-size.
     * @return Number
     * @throws MBFormatException
     */
    public Number getTextHaloWidth() throws MBFormatException {
    	return parse.optional(Number.class, paint, "text-halo-width", 0.0);
    }

    /**
     * Access text-halo-width as literal or function expression
     * @throws MBFormatException
     */
    public Expression textHaloWidth() throws MBFormatException {
    	return parse.percentage(paint, "text-halo-width", 0.0);
    }

    /**
     * (Optional) Units in pixels. Defaults to 0. Requires text-field.
     * 
     * The halo's fadeout distance towards the outside.
     * @return Number
     * @throws MBFormatException
     */
    public Number getTextHaloBlur() throws MBFormatException {
    	return parse.optional(Number.class, paint, "text-halo-blur", 0.0);
    }

    /**
     * Access text-halo-blur as literal or function expression
     * @throws MBFormatException
     */
    public Expression textHaloBlur() throws MBFormatException {
    	return parse.percentage(paint, "text-halo-blur", 0.0);
    }

    /**
     * (Optional) Units in pixels. Defaults to 0,0. Requires text-field.
     * 
     * Distance that the text's anchor is moved from its original placement. Positive values indicate right and down, while negative values indicate left and up.
     * @return double[]
     * @throws MBFormatException
     */
    public double[] getTextTranslate() {
    	return parse.array( paint, "text-translate", new double[]{ 0.0, 0.0 } ); 
    }

    public Point textTranslate() {
    	if (paint.get("text-translate") != null) {
            JSONArray array = (JSONArray) paint.get("text-translate");
            Number x = (Number) array.get(0);
            Number y = (Number) array.get(1);
            return new Point(x.intValue(), y.intValue());
        } else {
            return new Point(0, 0);
        }
    }

    /**
     * (Optional) One of map, viewport. Defaults to map. Requires text-field. Requires text-translate.
     * 
     * Controls the translation reference point.
     * 
     * {@link TranslateAnchor#MAP}: The text is translated relative to the map.
     * 
     * {@link TranslateAnchor#VIEWPORT}: The text is translated relative to the viewport.
     * 
     * Defaults to {@link TranslateAnchor#MAP}.
     * 
     */
    public TranslateAnchor getTextTranslateAnchor() {
    	Object value = paint.get("text-translate-anchor");
        if (value != null && "viewport".equalsIgnoreCase((String) value)) {
            return TranslateAnchor.VIEWPORT;
        } else {
            return TranslateAnchor.MAP;
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

}
