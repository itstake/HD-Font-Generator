package mnm.hdfontgen;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.*;

public class AsciiPackUtils {

    private static char[][] ascii;
    private static String packJson;
    private static Font fallback;

    /**
     * Renders a ascii {@link BufferedImage} of the given font.
     *
     * @param font The HD font
     * @return The rendered font
     * @throws IOException
     */
    public static BufferedImage render(HDFont font) throws IOException {
        loadAsciiTxt();

        return render(font, ascii);

    }

    public static void addGlyphSize(HDFont font, int tableInt) throws IOException {
        int a = tableInt << 8;
        String codePageIntString = Integer.toString(tableInt, 16);
        if (tableInt < 0x10) {
            codePageIntString = 0 + codePageIntString;
        }
        File customCodePage = new File("custom_unicode_page_" + codePageIntString + ".txt");
        char[][] table = new char[16][16];
        if (customCodePage.exists()) {
            System.out.println("Custom Unicode Page " + codePageIntString + " detected");
            try {
                String codePageContent = "";
                String codePageCustomFont = "";
                BufferedReader codePageReader = new BufferedReader(new InputStreamReader(new FileInputStream(customCodePage), "UTF-8"));
                String temp;
                boolean isFirstline = true;
                while ((temp = codePageReader.readLine()) != null) {
                    codePageContent += temp + "\n";
                }
                String[] split = codePageContent.split("\n");
                table = new char[16][16];
                for (int i = 0; i < split.length; i++) {
                    if (i == 0) {
                        codePageCustomFont = split[i];
                        System.out.println("Font name: " + split[i]);
                    } else {
                        table[i - 1] = split[i].toCharArray();
                    }
                }
                Font newFont = findFont(codePageCustomFont);
                if(newFont != null)
                    font = new HDFont(newFont, font.getSize(), true);
            } catch (Exception e) {
                throw new IOException("Custom Unicode Table file is invalid.", e);
            }
        } else {
            for (int y = 0; y < 16; y++) {
                int b = y << 4;
                for (int x = 0; x < 16; x++) {
                    table[y][x] = (char) (a + b + x);
                }
            }
        }
        addGlyphSize(font, table);
    }

    public static BufferedImage render(HDFont font, int tableInt) throws IOException {
        int a = tableInt << 8;
        String codePageIntString = Integer.toString(tableInt, 16);
        if (tableInt < 0x10) {
            codePageIntString = 0 + codePageIntString;
        }
        File customCodePage = new File("custom_unicode_page_" + codePageIntString + ".txt");
        char[][] table = new char[16][16];
        if (customCodePage.exists()) {
            System.out.println("Custom Unicode Page " + codePageIntString + " detected");
            try {
                String codePageContent = "";
                String codePageCustomFont = "";
                BufferedReader codePageReader = new BufferedReader(new InputStreamReader(new FileInputStream(customCodePage), "UTF-8"));
                String temp;
                boolean isFirstline = true;
                while ((temp = codePageReader.readLine()) != null) {
                        codePageContent += temp + "\n";
                }
                String[] split = codePageContent.split("\n");
                table = new char[16][16];
                for (int i = 0; i < split.length; i++) {
                    if (i == 0) {
                        codePageCustomFont = split[i];
                        System.out.println("Font name: " + split[i]);
                    } else {
                        table[i - 1] = split[i].toCharArray();
                    }
                }
                Font newFont = findFont(codePageCustomFont);
                if(newFont != null)
                    font = new HDFont(newFont, font.getSize(), true);
            } catch (Exception e) {
                throw new IOException("Custom Unicode Table file is invalid.", e);
            }
            return render(font, table);
        }

        for (int y = 0; y < 16; y++) {
            int b = y << 4;
            for (int x = 0; x < 16; x++) {
                table[y][x] = (char) (a + b + x);
            }
        }
        return render(font, table);
    }

    private static Font findFont(String codePageCustomFont) {
        for(Font f:GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
            if(f.getFontName().equals(codePageCustomFont)) {
                System.out.println("Detected Font: " + f.getFontName());
                return f;
            }
        }
        return null;
    }

    public static void addGlyphSize(HDFont font, char[][] chars) throws IOException {
        if (fallback == null) {
            try {
                fallback = Font.createFont(Font.TRUETYPE_FONT,
                        ClassLoader.getSystemResourceAsStream("unifont-7.0.06.ttf"));
            } catch (FontFormatException | IOException e) {
                throw new IOException("Unable to read Unifont fallback font.", e);
            }
        }
        final int size = font.getSize().getTextureSize() / 2;

        for (int y = 0; y < 16; y++) { // rows
            for (int x = 0; x < 16; x++) { // columns
                char ch = chars[y][x];
                BufferedImage c = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D gc = c.createGraphics();

                // select the font if the character is supported.
                Font f = fallback;
                if (font.getFont().canDisplay((int) ch)) {
                    f = font.getFont();
                }
                f = f.deriveFont(0, size);

                // decrease font size for large fonts.
                int s = size;
                while (f.getStringBounds(new char[]{ch}, 0, 1, new FontRenderContext(f.getTransform(), false, false))
                        .getHeight() > size) {
                    f = f.deriveFont(0, s--);
                }

                // pre-render the character
                gc.setFont(f);
                if (gc.getFontMetrics(f).charWidth(ch) >= 16) {
                    GlyphSizeMaker.addByte(Byte.parseByte(String.valueOf(15), 10));
                } else {
                    GlyphSizeMaker.addByte(Byte.parseByte(String.valueOf(gc.getFontMetrics(f).charWidth(ch) - 1), 10));
                }
                gc.dispose();
            }
        }
    }

    /**
     * Renders the character array of the given font onto a
     * {@link BufferedImage}
     *
     * @param font
     * @param ascii
     * @return
     * @throws IOException
     * @throws FontFormatException
     */
    public static BufferedImage render(HDFont font, char[][] ascii) throws IOException {
        if (fallback == null) {
            try {
                fallback = Font.createFont(Font.TRUETYPE_FONT,
                        ClassLoader.getSystemResourceAsStream("unifont-7.0.06.ttf"));
            } catch (FontFormatException | IOException e) {
                throw new IOException("Unable to read Unifont fallback font.", e);
            }
        }
        final int size = font.getSize().getTextureSize() / 2;
        BufferedImage image = new BufferedImage(size * 16, size * 16, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2d = image.createGraphics();

        for (int y = 0; y < 16; y++) { // rows
            for (int x = 0; x < 16; x++) { // columns
                char ch = ascii[y][x];
                int yy = size - size / 4; // move each character up a bit

                // Limit the render of each character so they don't spill.
                BufferedImage c = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D gc = c.createGraphics();

                // select the font if the character is supported.
                Font f = fallback;
                if (font.getFont().canDisplay((int) ch)) {
                    f = font.getFont();
                }
                f = f.deriveFont(0, size);

                // decrease font size for large fonts.
                int s = size;
                while (f.getStringBounds(new char[]{ch}, 0, 1, new FontRenderContext(f.getTransform(), false, false))
                        .getHeight() > size) {
                    f = f.deriveFont(0, s--);
                }

                // pre-render the character
                gc.setFont(f);
                gc.drawChars(new char[]{ch}, 0, 1, 0, yy);
                gc.dispose();

                // draw the pre-rendered character
                g2d.drawImage(c, x * size, y * size, null);
            }
        }
        g2d.dispose();
        return image;
    }

    private static void loadAsciiTxt() throws IOException {
        if (ascii == null) {
            try {
                String[] split = readInputStream("ascii.txt").split("\n");
                ascii = new char[16][16];
                for (int i = 0; i < split.length; i++) {
                    ascii[i] = split[i].toCharArray();
                }
            } catch (Exception e) {
                throw new IOException("Ascii file is invalid.", e);
            }
        }
    }

    private static void loadPackJson() throws IOException {
        if (packJson == null) {
            packJson = readInputStream("pack.mcmeta.json");
        }
    }

    private static String readInputStream(String name) throws IOException {
        InputStream in = ClassLoader.getSystemResourceAsStream(name);
        if (in == null) {
            throw new IOException(name + " does not exist.");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            while (in.available() > 0) {
                baos.write(in.read());
            }
            return baos.toString("utf-8");
        } finally {
            closeQuietly(in);
            closeQuietly(baos);
        }
    }

    private static void closeQuietly(Closeable close) {
        if (close != null) {
            try {
                close.close();
            } catch (IOException e) {
                // be quiet
            }
        }
    }
}
