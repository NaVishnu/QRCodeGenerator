package Main;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
import QRCode.QrCode;
import QRCode.QrSegment;
import QRCode.QrSegmentAdvanced;

public class Demo {
	public static void main(String[] args) throws IOException {
		doBasicDemo();
		doVarietyDemo();
		doSegmentDemo();
		doMaskDemo();
	}
	
	
	//Creating a single basic QR code and writing it to png and svg files.
	private static void doBasicDemo() throws IOException {
		String text = "a";          //User-supplied Unicode text
		QrCode.Ecc errCorLvl = QrCode.Ecc.LOW;  //Error correction level
		
		QrCode qr = QrCode.encodeText(text, errCorLvl);  //Make the QR Code symbol
		
		BufferedImage img = toImage(qr, 10, 4);          //Convert to bitmap image
		File imgFile = new File("demoQR.png");   //File path for output
		ImageIO.write(img, "png", imgFile);              //Write image to file
		
		String svg = toSvgString(qr, 4, "#FFFFFF", "#000000");  //Convert to SVG XML code
		File svgFile = new File("demo.svg");          //File path for output
		Files.write(svgFile.toPath(),                           //Write image to file
			svg.getBytes(StandardCharsets.UTF_8));
	}
	
	private static BufferedImage toImage(QrCode qr, int scale, int border, int lightColor, int darkColor) {
		Objects.requireNonNull(qr);
		if (scale <= 0 || border < 0)
			throw new IllegalArgumentException("Value out of range");
		if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale)
			throw new IllegalArgumentException("Scale or border too large");
		
		BufferedImage result = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < result.getHeight(); y++) {
			for (int x = 0; x < result.getWidth(); x++) {
				boolean color = qr.getModule(x / scale - border, y / scale - border);
				result.setRGB(x, y, color ? darkColor : lightColor);
			}
		}
		return result;
	}
	
	private static void doVarietyDemo() throws IOException {
		QrCode qr;
		
		//Numeric mode encoding (3.33 bits per digit)
		qr = QrCode.encodeText("01123581321345589144233377610987159725844181", QrCode.Ecc.MEDIUM);
		writePng(toImage(qr, 13, 1), "fiboQR.png");
		
		//Alphanumeric mode encoding (5.5 bits per character)
		qr = QrCode.encodeText("ALPHANUMERIC IN QRCODE 1234567890 $+%*-./:", QrCode.Ecc.HIGH);
		writePng(toImage(qr, 10, 2), "alphanumericQR.png");
		
		//Unicode text as UTF-8
		qr = QrCode.encodeText("????????????wa???????????? ????????", QrCode.Ecc.QUARTILE);
		writePng(toImage(qr, 10, 3), "unicodeQR.png");
		
		//Moderately large QR Code using longer text
		qr = QrCode.encodeText(
			"Gandalf did not move. And in that very moment, away behind in same courtyard of the City, a cock crowed."
			+ "Shrill and clear he crowed, recking nothing of wizardry or war, welcoming only th morning that in the "
			+ "sky far above the shadows of death was coming with the dawn."
			+ "And as if in answer there came from far away another note."
			+ "Horns, horns, horns."
			+ "In dark Mindoullin's sides they dimly echoed. Great horns of the North wildly blowing."
			+ "Rohan had come at last.", QrCode.Ecc.HIGH);
		writePng(toImage(qr, 6, 10), "lotrQR.png");
	}
	
	private static void doSegmentDemo() throws IOException {
		QrCode qr;
		List<QrSegment> segs;
		
		//Illustration "silver"
		String silver0 = "THE SQUARE ROOT OF 2 IS 1.";
		String silver1 = "41421356237309504880168872420969807856967187537694807317667973799";
		qr = QrCode.encodeText(silver0 + silver1, QrCode.Ecc.LOW);
		writePng(toImage(qr, 10, 3), "sqrt2-monolithic-QR.png");
		
		segs = Arrays.asList(QrSegment.makeAlphanumeric(silver0), QrSegment.makeNumeric(silver1));
		qr = QrCode.encodeSegments(segs, QrCode.Ecc.LOW);
		writePng(toImage(qr, 10, 3), "sqrt2-segmented-QR.png");
		
		//Illustration "golden"
		String golden0 = "Golden ratio ?? = 1.";
		String golden1 = "6180339887498948482045868343656381177203091798057628621354486227052604628189024497072072041893911374";
		String golden2 = "......";
		qr = QrCode.encodeText(golden0 + golden1 + golden2, QrCode.Ecc.LOW);
		writePng(toImage(qr, 8, 5), "phi-monolithic-QR.png");
		
		segs = Arrays.asList(
			QrSegment.makeBytes(golden0.getBytes(StandardCharsets.UTF_8)),
			QrSegment.makeNumeric(golden1),
			QrSegment.makeAlphanumeric(golden2));
		qr = QrCode.encodeSegments(segs, QrCode.Ecc.LOW);
		writePng(toImage(qr, 8, 5), "phi-segmented-QR.png");
	}
	
	private static void doMaskDemo() throws IOException {
		QrCode qr;
		List<QrSegment> segs;
		
		//Project Navin's github profile  URL
		segs = QrSegment.makeSegments("https://github.com/NavinAananthan/");
		qr = QrCode.encodeSegments(segs, QrCode.Ecc.HIGH, QrCode.MIN_VERSION, QrCode.MAX_VERSION, -1, true);  // Automatic mask
		writePng(toImage(qr, 8, 6, 0xE0FFE0, 0x206020), "project-navin-automask-QR.png");
		qr = QrCode.encodeSegments(segs, QrCode.Ecc.HIGH, QrCode.MIN_VERSION, QrCode.MAX_VERSION, 3, true);  // Force mask 3
		writePng(toImage(qr, 8, 6, 0xFFE0E0, 0x602020), "project-navin-mask3-QR.png");
	}
	
	private static BufferedImage toImage(QrCode qr, int scale, int border) {
		return toImage(qr, scale, border, 0xFFFFFF, 0x000000);
	}
	
	private static void writePng(BufferedImage img, String filepath) throws IOException {
		ImageIO.write(img, "png", new File(filepath));
	}
	
	private static String toSvgString(QrCode qr, int border, String lightColor, String darkColor) {
		Objects.requireNonNull(qr);
		Objects.requireNonNull(lightColor);
		Objects.requireNonNull(darkColor);
		if (border < 0)
			throw new IllegalArgumentException("Border must be non-negative");
		long brd = border;
		StringBuilder sb = new StringBuilder()
			.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n")
			.append(String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" viewBox=\"0 0 %1$d %1$d\" stroke=\"none\">\n",
				qr.size + brd * 2))
			.append("\t<rect width=\"100%\" height=\"100%\" fill=\"" + lightColor + "\"/>\n")
			.append("\t<path d=\"");
		for (int y = 0; y < qr.size; y++) {
			for (int x = 0; x < qr.size; x++) {
				if (qr.getModule(x, y)) {
					if (x != 0 || y != 0)
						sb.append(" ");
					sb.append(String.format("M%d,%dh1v1h-1z", x + brd, y + brd));
				}
			}
		}
		return sb
			.append("\" fill=\"" + darkColor + "\"/>\n")
			.append("</svg>\n")
			.toString();
	}
}