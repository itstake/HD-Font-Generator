package mnm.hdfontgen;

import javax.imageio.ImageIO;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FontGenerator implements Runnable {

    private static boolean quiet;

    @Override
    public void run() {
        try {
            GeneratorWindow window = GeneratorWindow.instance = new GeneratorWindow();
            window.frmHdFontGenerator.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generate(HDFont font) throws IOException {
        List<FontTexture> list = new ArrayList<>();
        GlyphSizeMaker.clearByte();
        String desc = font.getFriendlyName();
        if (!quiet)
            System.out.println("Rendering ascii");

        BufferedImage ascii = AsciiPackUtils.render(font);
        if(!new File(desc).isDirectory())
            new File(desc).mkdir();
        if(!new File(desc + "/assets").isDirectory())
            new File(desc + "/assets").mkdir();
        if(!new File(desc + "/assets/minecraft").isDirectory())
            new File(desc + "/assets/minecraft").mkdir();
        if(!new File(desc + "/assets/minecraft/font").isDirectory())
            new File(desc + "/assets/minecraft/font").mkdir();
        if(!new File(desc + "/assets/minecraft/textures").isDirectory())
            new File(desc + "/assets/minecraft/textures").mkdir();
        if(!new File(desc + "/assets/minecraft/textures/font").isDirectory())
            new File(desc + "/assets/minecraft/textures/font").mkdir();
        File asciiFile = new File(desc + "/assets/minecraft/textures/font/ascii.png");
        ImageIO.write(ascii, "png", asciiFile);
        ascii.flush();
        if (font.isUnicode()) {
            for (int i = 0x00; i <= 0xff; i++) {
                String name = Integer.toString(i, 16);
                if (i < 0x10) {
                    name = 0 + name;
                }

                if (!quiet)
                    System.out.println("Rendering unicode page " + name);
                HDFont glyphFont = new HDFont(font.getFont(), TextureSize.x32, true);
                AsciiPackUtils.addGlyphSize(glyphFont, i);
                BufferedImage page = AsciiPackUtils.render(font, i);
                ImageIO.write(page, "png", new File(desc + "/assets/minecraft/textures/font/unicode_page_" + name + ".png"));
                page.flush();
            }
        }
        GlyphSizeMaker.write(new File(desc + "/assets/minecraft/font/glyph_sizes.bin"));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(desc + "/pack.mcmeta"), "UTF-8"));
        writer.write("{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": 2,\n" +
                "    \"description\": \"" + desc + "\"\n" +
                "  }" +
                "}");
        writer.flush();
        writer.close();
        printSuccess(font);
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            // open the gui
            EventQueue.invokeLater(new FontGenerator());
        } else if (args.length == 2 || args.length == 3) {
            // headless
            String name = args[0].replace('_', ' ');
            String size = args[1];
            boolean unicode = false;
            if (args.length == 3) {
                // flags
                unicode = args[2].contains("u");
                quiet = args[2].contains("q");
            }

            TextureSize texSize = TextureSize.forSize(Integer.parseInt(size));
            if (texSize == null) {
                printSizes(size);
                System.exit(1);
            }

            FontGenerator.generate(new HDFont(Font.decode(name), texSize, unicode));
        } else {
            // wrong usage
            printUsage();
            System.exit(1);
        }
    }

    private static void printSuccess(HDFont hdfont) {
        if (!quiet)
            System.out.println("Generated font: " + hdfont.getFriendlyName() + " folder");
    }

    private static void printSizes(String size) {
        System.out.println(size + " is not a supported texture size.\n" + " Supported values are: "
                + TextureSize.getSizes());
    }

    private static void printUsage() {
        System.out.println("Command Line Usage: <font name> <texture size> [flags]");
        System.out.println("Flags:");
        System.out.println("    u    Export unicode");
        System.out.println("    q    Quiet");
    }
}