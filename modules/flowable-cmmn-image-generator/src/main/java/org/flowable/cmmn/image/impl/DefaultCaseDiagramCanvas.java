/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.cmmn.image.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.flowable.cmmn.image.exception.FlowableImageException;
import org.flowable.cmmn.image.util.ReflectUtil;
import org.flowable.cmmn.model.GraphicInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a canvas on which CMMN 1.1 constructs can be drawn.
 *
 * Some of the icons used are licensed under a Creative Commons Attribution 2.5 License, see http://www.famfamfam.com/lab/icons/silk/
 *
 * @see DefaultCaseDiagramGenerator
 * @author Tijs Rademakers
 */
public class DefaultCaseDiagramCanvas {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultCaseDiagramCanvas.class);

    public enum SHAPE_TYPE {
        Rectangle, Rhombus, Ellipse
    }

    // Predefined sized
    protected static final int ARROW_WIDTH = 5;
    protected static final int CONDITIONAL_INDICATOR_WIDTH = 16;
    protected static final int DEFAULT_INDICATOR_WIDTH = 10;
    protected static final int MARKER_WIDTH = 12;
    protected static final int FONT_SIZE = 11;
    protected static final int FONT_SPACING = 2;
    protected static final int TEXT_PADDING = 3;
    protected static final int ANNOTATION_TEXT_PADDING = 7;
    protected static final int LINE_HEIGHT = FONT_SIZE + FONT_SPACING;

    // Colors
    protected static Color TASK_BOX_COLOR = new Color(249, 249, 249);
    protected static Color SUBPROCESS_BOX_COLOR = new Color(255, 255, 255);
    protected static Color EVENT_COLOR = new Color(255, 255, 255);
    protected static Color CONNECTION_COLOR = new Color(88, 88, 88);
    protected static Color CONDITIONAL_INDICATOR_COLOR = new Color(255, 255, 255);
    protected static Color HIGHLIGHT_COLOR = Color.RED;
    protected static Color LABEL_COLOR = new Color(112, 146, 190);
    protected static Color TASK_BORDER_COLOR = new Color(187, 187, 187);
    protected static Color STAGE_BORDER_COLOR = new Color(0, 0, 0);
    protected static Color EVENT_BORDER_COLOR = new Color(88, 88, 88);

    // Fonts
    protected static Font LABEL_FONT;
    protected static Font ANNOTATION_FONT;

    // Strokes
    protected static Stroke THICK_TASK_BORDER_STROKE = new BasicStroke(3.0f);
    protected static Stroke GATEWAY_TYPE_STROKE = new BasicStroke(3.0f);
    protected static Stroke ASSOCIATION_STROKE = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] { 2.0f, 2.0f }, 0.0f);

    // icons
    protected static int ICON_PADDING = 5;
    protected static BufferedImage TIMER_IMAGE;
    protected static BufferedImage USERLISTENER_IMAGE;
    protected static BufferedImage USERTASK_IMAGE;
    protected static BufferedImage SERVICETASK_IMAGE;
    protected static BufferedImage CASETASK_IMAGE;
    protected static BufferedImage PROCESSTASK_IMAGE;
    protected static BufferedImage DECISIONTASK_IMAGE;

    protected int canvasWidth = -1;
    protected int canvasHeight = -1;
    protected int minX = -1;
    protected int minY = -1;
    protected BufferedImage caseDiagram;
    protected Graphics2D g;
    protected FontMetrics fontMetrics;
    protected boolean closed;
    protected ClassLoader customClassLoader;
    protected String activityFontName = "Arial";
    protected String labelFontName = "Arial";
    protected String annotationFontName = "Arial";

    /**
     * Creates an empty canvas with given width and height.
     *
     * Allows to specify minimal boundaries on the left and upper side of the canvas. This is useful for diagrams that have white space there. Everything beneath these minimum values will be cropped.
     * It's also possible to pass a specific font name and a class loader for the icon images.
     *
     */
    public DefaultCaseDiagramCanvas(int width, int height, int minX, int minY, String imageType,
            String activityFontName, String labelFontName, String annotationFontName, ClassLoader customClassLoader) {

        this.canvasWidth = width;
        this.canvasHeight = height;
        this.minX = minX;
        this.minY = minY;
        if (activityFontName != null) {
            this.activityFontName = activityFontName;
        }
        if (labelFontName != null) {
            this.labelFontName = labelFontName;
        }
        if (annotationFontName != null) {
            this.annotationFontName = annotationFontName;
        }
        this.customClassLoader = customClassLoader;

        initialize(imageType);
    }

    /**
     * Creates an empty canvas with given width and height.
     *
     * Allows to specify minimal boundaries on the left and upper side of the canvas. This is useful for diagrams that have white space there (eg Signavio). Everything beneath these minimum values
     * will be cropped.
     *
     * @param minX
     *            Hint that will be used when generating the image. Parts that fall below minX on the horizontal scale will be cropped.
     * @param minY
     *            Hint that will be used when generating the image. Parts that fall below minX on the horizontal scale will be cropped.
     */
    public DefaultCaseDiagramCanvas(int width, int height, int minX, int minY, String imageType) {
        this.canvasWidth = width;
        this.canvasHeight = height;
        this.minX = minX;
        this.minY = minY;

        initialize(imageType);
    }

    public void initialize(String imageType) {
        if ("png".equalsIgnoreCase(imageType)) {
            this.caseDiagram = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        } else {
            this.caseDiagram = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        }

        this.g = caseDiagram.createGraphics();
        if (!"png".equalsIgnoreCase(imageType)) {
            this.g.setBackground(new Color(255, 255, 255, 0));
            this.g.clearRect(0, 0, canvasWidth, canvasHeight);
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(Color.black);

        Font font = new Font(activityFontName, Font.BOLD, FONT_SIZE);
        g.setFont(font);
        this.fontMetrics = g.getFontMetrics();

        LABEL_FONT = new Font(labelFontName, Font.ITALIC, 10);
        ANNOTATION_FONT = new Font(annotationFontName, Font.PLAIN, FONT_SIZE);

        try {
            TIMER_IMAGE = ImageIO.read(ReflectUtil.getResource("org/flowable/icons/timer.png", customClassLoader));
            USERLISTENER_IMAGE = ImageIO.read(ReflectUtil.getResource("org/flowable/icons/user.png", customClassLoader));
            USERTASK_IMAGE = ImageIO.read(ReflectUtil.getResource("org/flowable/icons/userTask.png", customClassLoader));
            SERVICETASK_IMAGE = ImageIO.read(ReflectUtil.getResource("org/flowable/icons/serviceTask.png", customClassLoader));
            CASETASK_IMAGE = ImageIO.read(ReflectUtil.getResource("org/flowable/icons/caseTask.png", customClassLoader));
            PROCESSTASK_IMAGE = ImageIO.read(ReflectUtil.getResource("org/flowable/icons/processTask.png", customClassLoader));
            DECISIONTASK_IMAGE = ImageIO.read(ReflectUtil.getResource("org/flowable/icons/decisionTask.png", customClassLoader));

        } catch (IOException e) {
            LOGGER.warn("Could not load image for case diagram creation: {}", e.getMessage());
        }
    }

    /**
     * Generates an image of what currently is drawn on the canvas.
     *
     * Throws an {@link FlowableImageException} when {@link #close()} is already called.
     */
    public InputStream generateImage(String imageType) {
        if (closed) {
            throw new FlowableImageException("CaseDiagramGenerator already closed");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(caseDiagram, imageType, out);

        } catch (IOException e) {
            throw new FlowableImageException("Error while generating case image", e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
                // Exception is silently ignored
            }
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    /**
     * Generates an image of what currently is drawn on the canvas.
     *
     * Throws an {@link FlowableImageException} when {@link #close()} is already called.
     */
    public BufferedImage generateBufferedImage(String imageType) {
        if (closed) {
            throw new FlowableImageException("CaseDiagramGenerator already closed");
        }

        // Try to remove white space
        minX = (minX <= 5) ? 5 : minX;
        minY = (minY <= 5) ? 5 : minY;
        BufferedImage imageToSerialize = caseDiagram;
        if (minX >= 0 && minY >= 0) {
            imageToSerialize = caseDiagram.getSubimage(minX - 5, minY - 5, canvasWidth - minX + 5, canvasHeight - minY + 5);
        }
        return imageToSerialize;
    }

    /**
     * Closes the canvas which disallows further drawing and releases graphical resources.
     */
    public void close() {
        g.dispose();
        closed = true;
    }

    public void drawAssociation(int[] xPoints, int[] yPoints, double scaleFactor) {
        drawConnection(xPoints, yPoints, "association", scaleFactor);
    }

    public void drawConnection(int[] xPoints, int[] yPoints, String connectionType, double scaleFactor) {

        Paint originalPaint = g.getPaint();
        Stroke originalStroke = g.getStroke();

        g.setPaint(CONNECTION_COLOR);
        if (connectionType.equals("association")) {
            g.setStroke(ASSOCIATION_STROKE);
        }

        for (int i = 1; i < xPoints.length; i++) {
            Integer sourceX = xPoints[i - 1];
            Integer sourceY = yPoints[i - 1];
            Integer targetX = xPoints[i];
            Integer targetY = yPoints[i];
            Line2D.Double line = new Line2D.Double(sourceX, sourceY, targetX, targetY);
            g.draw(line);
        }

        Line2D.Double line = new Line2D.Double(xPoints[xPoints.length - 2], yPoints[xPoints.length - 2], xPoints[xPoints.length - 1], yPoints[xPoints.length - 1]);
        drawArrowHead(line, scaleFactor);

        g.setPaint(originalPaint);
        g.setStroke(originalStroke);
    }

    public void drawArrowHead(Line2D.Double line, double scaleFactor) {
        int doubleArrowWidth = (int) (2 * ARROW_WIDTH / scaleFactor);
        if (doubleArrowWidth == 0) {
            doubleArrowWidth = 2;
        }
        Polygon arrowHead = new Polygon();
        arrowHead.addPoint(0, 0);
        int arrowHeadPoint = (int) (-ARROW_WIDTH / scaleFactor);
        if (arrowHeadPoint == 0) {
            arrowHeadPoint = -1;
        }
        arrowHead.addPoint(arrowHeadPoint, -doubleArrowWidth);
        arrowHeadPoint = (int) (ARROW_WIDTH / scaleFactor);
        if (arrowHeadPoint == 0) {
            arrowHeadPoint = 1;
        }
        arrowHead.addPoint(arrowHeadPoint, -doubleArrowWidth);

        AffineTransform transformation = new AffineTransform();
        transformation.setToIdentity();
        double angle = Math.atan2(line.y2 - line.y1, line.x2 - line.x1);
        transformation.translate(line.x2, line.y2);
        transformation.rotate((angle - Math.PI / 2d));

        AffineTransform originalTransformation = g.getTransform();
        g.setTransform(transformation);
        g.fill(arrowHead);
        g.setTransform(originalTransformation);
    }
    
    public void drawGenericEventListener(GraphicInfo graphicInfo, double scaleFactor) {
        drawEventListener(graphicInfo, null, scaleFactor);
    }

    public void drawTimerEventListener(GraphicInfo graphicInfo, double scaleFactor) {
        drawEventListener(graphicInfo, TIMER_IMAGE, scaleFactor);
    }

    public void drawUserEventListener(GraphicInfo graphicInfo, double scaleFactor) {
        drawEventListener(graphicInfo, USERLISTENER_IMAGE, scaleFactor);
    }
    
    public void drawEventListener(GraphicInfo graphicInfo, BufferedImage image, double scaleFactor) {
        Paint originalPaint = g.getPaint();
        g.setPaint(EVENT_COLOR);
        Ellipse2D circle = new Ellipse2D.Double(graphicInfo.getX(), graphicInfo.getY(),
                graphicInfo.getWidth(), graphicInfo.getHeight());
        g.fill(circle);
        g.setPaint(EVENT_BORDER_COLOR);
        g.draw(circle);
        g.setPaint(originalPaint);
        if (image != null) {
            // calculate coordinates to center image
            int imageX = (int) Math.round(graphicInfo.getX() + (graphicInfo.getWidth() / 2) - (image.getWidth() / (2 * scaleFactor)));
            int imageY = (int) Math.round(graphicInfo.getY() + (graphicInfo.getHeight() / 2) - (image.getHeight() / (2 * scaleFactor)));
            g.drawImage(image, imageX, imageY,
                    (int) (image.getWidth() / scaleFactor), (int) (image.getHeight() / scaleFactor), null);
        }

    }

    public void drawTask(BufferedImage icon, String name, GraphicInfo graphicInfo, double scaleFactor) {
        drawTask(name, graphicInfo, scaleFactor);
        g.drawImage(icon, (int) (graphicInfo.getX() + ICON_PADDING / scaleFactor),
                (int) (graphicInfo.getY() + ICON_PADDING / scaleFactor),
                (int) (icon.getWidth() / scaleFactor), (int) (icon.getHeight() / scaleFactor), null);
    }

    public void drawTask(String name, GraphicInfo graphicInfo, double scaleFactor) {
        drawTask(name, graphicInfo, false, scaleFactor);
    }

    public void drawStage(String name, GraphicInfo graphicInfo, double scaleFactor) {
        int x = (int) graphicInfo.getX();
        int y = (int) graphicInfo.getY();
        int width = (int) graphicInfo.getWidth();
        int height = (int) graphicInfo.getHeight();
        g.drawRoundRect(x, y, width, height, 6, 6);

        // Add the name as text, vertical
        if (scaleFactor == 1.0 && name != null && name.length() > 0) {
            // Include some padding
            int availableTextSpace = height - 6;

            // Create rotation for derived font
            AffineTransform transformation = new AffineTransform();
            transformation.setToIdentity();
            transformation.rotate(270 * Math.PI / 180);

            Font currentFont = g.getFont();
            Font theDerivedFont = currentFont.deriveFont(transformation);
            g.setFont(theDerivedFont);

            String truncated = fitTextToWidth(name, availableTextSpace);
            int realWidth = fontMetrics.stringWidth(truncated);

            g.drawString(truncated, x + 2 + fontMetrics.getHeight(), 3 + y + availableTextSpace - (availableTextSpace - realWidth) / 2);
            g.setFont(currentFont);
        }
    }

    protected void drawTask(String name, GraphicInfo graphicInfo, boolean thickBorder, double scaleFactor) {
        Paint originalPaint = g.getPaint();
        int x = (int) graphicInfo.getX();
        int y = (int) graphicInfo.getY();
        int width = (int) graphicInfo.getWidth();
        int height = (int) graphicInfo.getHeight();

        // Create a new gradient paint for every task box, gradient depends on x and y and is not relative
        g.setPaint(TASK_BOX_COLOR);

        int arcR = 6;
        if (thickBorder) {
            arcR = 3;
        }

        // shape
        RoundRectangle2D rect = new RoundRectangle2D.Double(x, y, width, height, arcR, arcR);
        g.fill(rect);
        g.setPaint(TASK_BORDER_COLOR);

        if (thickBorder) {
            Stroke originalStroke = g.getStroke();
            g.setStroke(THICK_TASK_BORDER_STROKE);
            g.draw(rect);
            g.setStroke(originalStroke);
        } else {
            g.draw(rect);
        }

        g.setPaint(originalPaint);
        // text
        if (scaleFactor == 1.0 && name != null && name.length() > 0) {
            int boxWidth = width - (2 * TEXT_PADDING);
            int boxHeight = height - 16 - ICON_PADDING - ICON_PADDING - MARKER_WIDTH - 2 - 2;
            int boxX = x + width / 2 - boxWidth / 2;
            int boxY = y + height / 2 - boxHeight / 2 + ICON_PADDING + ICON_PADDING - 2 - 2;

            drawMultilineCentredText(name, boxX, boxY, boxWidth, boxHeight);
        }
    }

    protected void drawMilestone(String name, GraphicInfo graphicInfo, double scaleFactor) {
        Paint originalPaint = g.getPaint();
        int x = (int) graphicInfo.getX();
        int y = (int) graphicInfo.getY();
        int width = (int) graphicInfo.getWidth();
        int height = (int) graphicInfo.getHeight();

        // Create a new gradient paint for every task box, gradient depends on x and y and is not relative
        g.setPaint(TASK_BOX_COLOR);

        int arcR = 24;

        // shape
        RoundRectangle2D rect = new RoundRectangle2D.Double(x, y, width, height, arcR, arcR);
        g.fill(rect);
        g.setPaint(TASK_BORDER_COLOR);

        g.draw(rect);

        g.setPaint(originalPaint);
        // text
        if (scaleFactor == 1.0 && name != null && name.length() > 0) {
            int boxWidth = width - (2 * TEXT_PADDING);
            int boxHeight = height - 16 - ICON_PADDING - ICON_PADDING - MARKER_WIDTH - 2 - 2;
            int boxX = x + width / 2 - boxWidth / 2;
            int boxY = y + height / 2 - boxHeight / 2 + ICON_PADDING + ICON_PADDING - 2 - 2;

            drawMultilineCentredText(name, boxX, boxY, boxWidth, boxHeight);
        }
    }

    protected void drawMultilineCentredText(String text, int x, int y, int boxWidth, int boxHeight) {
        drawMultilineText(text, x, y, boxWidth, boxHeight, true);
    }

    protected void drawMultilineAnnotationText(String text, int x, int y, int boxWidth, int boxHeight) {
        drawMultilineText(text, x, y, boxWidth, boxHeight, false);
    }

    protected void drawMultilineText(String text, int x, int y, int boxWidth, int boxHeight, boolean centered) {
        // Create an attributed string based in input text
        AttributedString attributedString = new AttributedString(text);
        attributedString.addAttribute(TextAttribute.FONT, g.getFont());
        attributedString.addAttribute(TextAttribute.FOREGROUND, Color.black);

        AttributedCharacterIterator characterIterator = attributedString.getIterator();

        int currentHeight = 0;
        // Prepare a list of lines of text we'll be drawing
        List<TextLayout> layouts = new ArrayList<>();
        String lastLine = null;

        LineBreakMeasurer measurer = new LineBreakMeasurer(characterIterator, g.getFontRenderContext());

        TextLayout layout = null;
        while (measurer.getPosition() < characterIterator.getEndIndex() && currentHeight <= boxHeight) {

            int previousPosition = measurer.getPosition();

            // Request next layout
            layout = measurer.nextLayout(boxWidth);

            int height = ((Float) (layout.getDescent() + layout.getAscent() + layout.getLeading())).intValue();

            if (currentHeight + height > boxHeight) {
                // The line we're about to add should NOT be added anymore, append three dots to previous one instead
                // to indicate more text is truncated
                if (!layouts.isEmpty()) {
                    layouts.remove(layouts.size() - 1);

                    if (lastLine.length() >= 4) {
                        lastLine = lastLine.substring(0, lastLine.length() - 4) + "...";
                    }
                    layouts.add(new TextLayout(lastLine, g.getFont(), g.getFontRenderContext()));
                }
                break;
            } else {
                layouts.add(layout);
                lastLine = text.substring(previousPosition, measurer.getPosition());
                currentHeight += height;
            }
        }

        int currentY = y + (centered ? ((boxHeight - currentHeight) / 2) : 0);
        int currentX = 0;

        // Actually draw the lines
        for (TextLayout textLayout : layouts) {

            currentY += textLayout.getAscent();
            currentX = x + (centered ? ((boxWidth - ((Double) textLayout.getBounds().getWidth()).intValue()) / 2) : 0);

            textLayout.draw(g, currentX, currentY);
            currentY += textLayout.getDescent() + textLayout.getLeading();
        }

    }

    protected String fitTextToWidth(String original, int width) {
        String text = original;

        // remove length for "..."
        int maxWidth = width - 10;

        while (fontMetrics.stringWidth(text + "...") > maxWidth && text.length() > 0) {
            text = text.substring(0, text.length() - 1);
        }

        if (!text.equals(original)) {
            text = text + "...";
        }

        return text;
    }

    public void drawUserTask(String name, GraphicInfo graphicInfo, double scaleFactor) {
        drawTask(USERTASK_IMAGE, name, graphicInfo, scaleFactor);
    }

    public void drawServiceTask(String name, GraphicInfo graphicInfo, double scaleFactor) {
        drawTask(SERVICETASK_IMAGE, name, graphicInfo, scaleFactor);
    }

    public void drawCaseTask(String name, GraphicInfo graphicInfo, double scaleFactor) {
        drawTask(CASETASK_IMAGE, name, graphicInfo, scaleFactor);
    }

    public void drawProcessTask(String name, GraphicInfo graphicInfo, double scaleFactor) {
        drawTask(PROCESSTASK_IMAGE, name, graphicInfo, scaleFactor);
    }

    public void drawDecisionTask(String name, GraphicInfo graphicInfo, double scaleFactor) {
        drawTask(DECISIONTASK_IMAGE, name, graphicInfo, scaleFactor);
    }

    public void drawCriterion(GraphicInfo graphicInfo, boolean fillShape) {
        Polygon rhombus = new Polygon();
        int x = (int) graphicInfo.getX();
        int y = (int) graphicInfo.getY();
        int width = (int) graphicInfo.getWidth();
        int height = (int) graphicInfo.getHeight();

        rhombus.addPoint(x, y + (height / 2));
        rhombus.addPoint(x + (width / 2), y + height);
        rhombus.addPoint(x + width, y + (height / 2));
        rhombus.addPoint(x + (width / 2), y);
        g.draw(rhombus);

        if (fillShape) {
            g.fill(rhombus);
        }
    }

    public void drawEntryCriterion(GraphicInfo graphicInfo) {
        drawCriterion(graphicInfo, false);
    }

    public void drawExitCriterion(GraphicInfo graphicInfo) {
        Paint originalPaint = g.getPaint();
        // Create a new gradient paint for every task box, gradient depends on x and y and is not relative
        g.setPaint(Color.BLACK);

        drawCriterion(graphicInfo, true);

        g.setPaint(originalPaint);
    }

    public void drawHighLight(int x, int y, int width, int height) {
        Paint originalPaint = g.getPaint();
        Stroke originalStroke = g.getStroke();

        g.setPaint(HIGHLIGHT_COLOR);
        g.setStroke(THICK_TASK_BORDER_STROKE);

        RoundRectangle2D rect = new RoundRectangle2D.Double(x, y, width, height, 20, 20);
        g.draw(rect);

        g.setPaint(originalPaint);
        g.setStroke(originalStroke);
    }

    /**
     * This method makes coordinates of connection flow better.
     *
     * @param sourceShapeType
     * @param targetShapeType
     * @param sourceGraphicInfo
     * @param targetGraphicInfo
     * @param graphicInfoList
     *
     */
    public List<GraphicInfo> connectionPerfectionizer(SHAPE_TYPE sourceShapeType, SHAPE_TYPE targetShapeType, GraphicInfo sourceGraphicInfo, GraphicInfo targetGraphicInfo, List<GraphicInfo> graphicInfoList) {
        Shape shapeFirst = createShape(sourceShapeType, sourceGraphicInfo);
        Shape shapeLast = createShape(targetShapeType, targetGraphicInfo);

        if (graphicInfoList != null && graphicInfoList.size() > 0) {
            GraphicInfo graphicInfoFirst = graphicInfoList.get(0);
            GraphicInfo graphicInfoLast = graphicInfoList.get(graphicInfoList.size() - 1);
            if (shapeFirst != null) {
                graphicInfoFirst.setX(shapeFirst.getBounds2D().getCenterX());
                graphicInfoFirst.setY(shapeFirst.getBounds2D().getCenterY());
            }
            if (shapeLast != null) {
                graphicInfoLast.setX(shapeLast.getBounds2D().getCenterX());
                graphicInfoLast.setY(shapeLast.getBounds2D().getCenterY());
            }

            Point p = null;

            if (shapeFirst != null) {
                Line2D.Double lineFirst = new Line2D.Double(graphicInfoFirst.getX(), graphicInfoFirst.getY(), graphicInfoList.get(1).getX(), graphicInfoList.get(1).getY());
                p = getIntersection(shapeFirst, lineFirst);
                if (p != null) {
                    graphicInfoFirst.setX(p.getX());
                    graphicInfoFirst.setY(p.getY());
                }
            }

            if (shapeLast != null) {
                Line2D.Double lineLast = new Line2D.Double(graphicInfoLast.getX(), graphicInfoLast.getY(), graphicInfoList.get(graphicInfoList.size() - 2).getX(), graphicInfoList.get(graphicInfoList.size() - 2).getY());
                p = getIntersection(shapeLast, lineLast);
                if (p != null) {
                    graphicInfoLast.setX(p.getX());
                    graphicInfoLast.setY(p.getY());
                }
            }
        }

        return graphicInfoList;
    }

    /**
     * This method creates shape by type and coordinates.
     *
     * @param shapeType
     * @param graphicInfo
     * @return Shape
     */
    private static Shape createShape(SHAPE_TYPE shapeType, GraphicInfo graphicInfo) {
        if (SHAPE_TYPE.Rectangle == shapeType) {
            // source is rectangle
            return new Rectangle2D.Double(graphicInfo.getX(), graphicInfo.getY(), graphicInfo.getWidth(), graphicInfo.getHeight());
        } else if (SHAPE_TYPE.Rhombus == shapeType) {
            // source is rhombus
            Path2D.Double rhombus = new Path2D.Double();
            rhombus.moveTo(graphicInfo.getX(), graphicInfo.getY() + graphicInfo.getHeight() / 2);
            rhombus.lineTo(graphicInfo.getX() + graphicInfo.getWidth() / 2, graphicInfo.getY() + graphicInfo.getHeight());
            rhombus.lineTo(graphicInfo.getX() + graphicInfo.getWidth(), graphicInfo.getY() + graphicInfo.getHeight() / 2);
            rhombus.lineTo(graphicInfo.getX() + graphicInfo.getWidth() / 2, graphicInfo.getY());
            rhombus.lineTo(graphicInfo.getX(), graphicInfo.getY() + graphicInfo.getHeight() / 2);
            rhombus.closePath();
            return rhombus;
        } else if (SHAPE_TYPE.Ellipse == shapeType) {
            // source is ellipse
            return new Ellipse2D.Double(graphicInfo.getX(), graphicInfo.getY(), graphicInfo.getWidth(), graphicInfo.getHeight());
        } else {
            // unknown source element, just do not correct coordinates
        }
        return null;
    }

    /**
     * This method returns intersection point of shape border and line.
     *
     * @param shape
     * @param line
     * @return Point
     */
    private static Point getIntersection(Shape shape, Line2D.Double line) {
        if (shape instanceof Ellipse2D) {
            return getEllipseIntersection(shape, line);
        } else if (shape instanceof Rectangle2D || shape instanceof Path2D) {
            return getShapeIntersection(shape, line);
        } else {
            // something strange
            return null;
        }
    }

    /**
     * This method calculates ellipse intersection with line
     *
     * @param shape
     *            Bounds of this shape used to calculate parameters of inscribed into this bounds ellipse.
     * @param line
     * @return Intersection point
     */
    private static Point getEllipseIntersection(Shape shape, Line2D.Double line) {
        double angle = Math.atan2(line.y2 - line.y1, line.x2 - line.x1);
        double x = shape.getBounds2D().getWidth() / 2 * Math.cos(angle) + shape.getBounds2D().getCenterX();
        double y = shape.getBounds2D().getHeight() / 2 * Math.sin(angle) + shape.getBounds2D().getCenterY();
        Point p = new Point();
        p.setLocation(x, y);
        return p;
    }

    /**
     * This method calculates shape intersection with line.
     *
     * @param shape
     * @param line
     * @return Intersection point
     */
    private static Point getShapeIntersection(Shape shape, Line2D.Double line) {
        PathIterator it = shape.getPathIterator(null);
        double[] coords = new double[6];
        double[] pos = new double[2];
        Line2D.Double l = new Line2D.Double();
        while (!it.isDone()) {
            int type = it.currentSegment(coords);
            switch (type) {
            case PathIterator.SEG_MOVETO:
                pos[0] = coords[0];
                pos[1] = coords[1];
                break;
            case PathIterator.SEG_LINETO:
                l = new Line2D.Double(pos[0], pos[1], coords[0], coords[1]);
                if (line.intersectsLine(l)) {
                    return getLinesIntersection(line, l);
                }
                pos[0] = coords[0];
                pos[1] = coords[1];
                break;
            case PathIterator.SEG_CLOSE:
                break;
            default:
                // whatever
            }
            it.next();
        }
        return null;
    }

    /**
     * This method calculates intersections of two lines.
     *
     * @param a
     *            Line 1
     * @param b
     *            Line 2
     * @return Intersection point
     */
    private static Point getLinesIntersection(Line2D a, Line2D b) {
        double d = (a.getX1() - a.getX2()) * (b.getY2() - b.getY1()) - (a.getY1() - a.getY2()) * (b.getX2() - b.getX1());
        double da = (a.getX1() - b.getX1()) * (b.getY2() - b.getY1()) - (a.getY1() - b.getY1()) * (b.getX2() - b.getX1());
        double ta = da / d;

        Point p = new Point();
        p.setLocation(a.getX1() + ta * (a.getX2() - a.getX1()), a.getY1() + ta * (a.getY2() - a.getY1()));
        return p;
    }
}
