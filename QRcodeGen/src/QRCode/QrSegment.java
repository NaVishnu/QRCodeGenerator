package QRCode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

//A segment of character/binary/control data in a QR Code symbol. Instances of this class are immutable.
public final class QrSegment {
	//Returns a segment representing the specified binary data encoded in byte mode.
	//All input byte arrays are acceptable.
	public static QrSegment makeBytes(byte[] data) {
		Objects.requireNonNull(data);
		BitBuffer bb = new BitBuffer();
		for (byte b : data)
			bb.appendBits(b & 0xFF, 8);
		return new QrSegment(Mode.BYTE, data.length, bb);
	}
	
	//Returns a segment representing the specified string of decimal digits encoded in numeric code.
	public static QrSegment makeNumeric(CharSequence digits) {
		Objects.requireNonNull(digits);
		if (!isNumeric(digits))
			throw new IllegalArgumentException("String contains non-numeric characters");
		
		BitBuffer bb = new BitBuffer();
		for (int i = 0; i < digits.length(); ) {  //Consume up to 3 digits per iteration
			int n = Math.min(digits.length() - i, 3);
			bb.appendBits(Integer.parseInt(digits.subSequence(i, i + n).toString()), n * 3 + 1);
			i += n;
		}
		return new QrSegment(Mode.NUMERIC, digits.length(), bb);
	}
	
	//Returns a segment representing the specified text string encoded in alphanumeric code.
	public static QrSegment makeAlphanumeric(CharSequence text) {
		Objects.requireNonNull(text);
		if (!isAlphanumeric(text))
			throw new IllegalArgumentException("String contains unencodable characters in alphanumeric mode");
		
		BitBuffer bb = new BitBuffer();
		int i;
		for (i = 0; i <= text.length() - 2; i += 2) {  //Process groups of 2
			int temp = ALPHANUMERIC_CHARSET.indexOf(text.charAt(i)) * 45;
			temp += ALPHANUMERIC_CHARSET.indexOf(text.charAt(i + 1));
			bb.appendBits(temp, 11);
		}
		if (i < text.length())  //1 character remaining
			bb.appendBits(ALPHANUMERIC_CHARSET.indexOf(text.charAt(i)), 6);
		return new QrSegment(Mode.ALPHANUMERIC, text.length(), bb);
	}
	
	//Returns a list of zero or more segments to represent the specified Unicode text string.
	//The result may use various segment modes and switch modes to optimize the length of the bit stream.
	public static List<QrSegment> makeSegments(CharSequence text) {
		Objects.requireNonNull(text);
		
		//Select the most efficient segment encoding automatically
		List<QrSegment> result = new ArrayList<>();
		if (text.equals(""));  //Leave result empty
		else if (isNumeric(text))
			result.add(makeNumeric(text));
		else if (isAlphanumeric(text))
			result.add(makeAlphanumeric(text));
		else
			result.add(makeBytes(text.toString().getBytes(StandardCharsets.UTF_8)));
		return result;
	}
	
	//Returns a segment representing an Extended Channel Interpretation (ECI)
	//designator with the specified assignment value.
	public static QrSegment makeEci(int assignVal) {
		BitBuffer bb = new BitBuffer();
		if (assignVal < 0)
			throw new IllegalArgumentException("ECI assignment value out of range");
		else if (assignVal < (1 << 7))
			bb.appendBits(assignVal, 8);
		else if (assignVal < (1 << 14)) {
			bb.appendBits(0b10, 2);
			bb.appendBits(assignVal, 14);
		} else if (assignVal < 1_000_000) {
			bb.appendBits(0b110, 3);
			bb.appendBits(assignVal, 21);
		} else
			throw new IllegalArgumentException("ECI assignment value out of range");
		return new QrSegment(Mode.ECI, 0, bb);
	}
	
	//Tests whether the specified string can be encoded as a segment in numeric mode.
	//A string is encodable iff each character is in the range 0 to 9.
	public static boolean isNumeric(CharSequence text) {
		return NUMERIC_REGEX.matcher(text).matches();
	}
	
	//Tests whether the specified string can be encoded as a segment in alphanumeric mode.
	//A string is encodable iff each character is in the following set: 0 to 9, A to Z
	//(uppercase only), space, dollar, percent, asterisk, plus, hyphen, period, slash, colon.
	public static boolean isAlphanumeric(CharSequence text) {
		return ALPHANUMERIC_REGEX.matcher(text).matches();
	}
	
	
	//The mode indicator of this segment
	public final Mode mode;
	
	//The length of this segment's unencoded data.
	//Measured in characters for numeric/alphanumeric/kanji mode, bytes for byte mode, and 0 for ECI mode.
	public final int numChars;
	
	//The data bits of this segment. Not null. Accessed through getData().
	final BitBuffer data;
	
	//Constructs a QR Code segment with the specified attributes and data.
	//The character count (numCh) must agree with the mode and the bit buffer length,
	//but the constraint isn't checked. The specified bit buffer is cloned and stored.
	public QrSegment(Mode md, int numCh, BitBuffer data) {
		mode = Objects.requireNonNull(md);
		Objects.requireNonNull(data);
		if (numCh < 0)
			throw new IllegalArgumentException("Invalid value");
		numChars = numCh;
		this.data = data.clone();  //Make defensive copy
	}
	
	
	//Returns the data bits of the segment.
	public BitBuffer getData() {
		return data.clone();  // Make defensive copy
	}
	
	
	//Calculates the number of bits needed to encode the given segments at the given version.
	//Returns a non-negative number if successful. Otherwise returns -1 if a segment has too
	//many characters to fit its length field, or the total bits exceeds Integer.MAX_VALUE.
	static int getTotalBits(List<QrSegment> segs, int version) {
		Objects.requireNonNull(segs);
		long result = 0;
		for (QrSegment seg : segs) {
			Objects.requireNonNull(seg);
			int ccbits = seg.mode.numCharCountBits(version);
			if (seg.numChars >= (1 << ccbits))
				return -1;  //The segment's length doesn't fit the field's bit width
			result += 4L + ccbits + seg.data.bitLength();
			if (result > Integer.MAX_VALUE)
				return -1;  //The sum will overflow an int type
		}
		return (int)result;
	}
	
	//Describes precisely all strings that are encodable in numeric mode.
	private static final Pattern NUMERIC_REGEX = Pattern.compile("[0-9]*");
	
	//Describes precisely all strings that are encodable in alphanumeric mode.
	private static final Pattern ALPHANUMERIC_REGEX = Pattern.compile("[A-Z0-9 $%*+./:-]*");
	
	//The set of all legal characters in alphanumeric mode, where
	//each character value maps to the index in the string.
	static final String ALPHANUMERIC_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";
	
	//Describes how a segment's data bits are interpreted.
	public enum Mode {
		NUMERIC     (0x1, 10, 12, 14),
		ALPHANUMERIC(0x2,  9, 11, 13),
		BYTE        (0x4,  8, 16, 16),
		KANJI       (0x8,  8, 10, 12),
		ECI         (0x7,  0,  0,  0);

		//The mode indicator bits, which is a uint4 value (range 0 to 15).
		final int modeBits;
		
		//Number of character count bits for three different version ranges.
		private final int[] numBitsCharCount;
		
		//Constructor
		private Mode(int mode, int... ccbits) {
			modeBits = mode;
			numBitsCharCount = ccbits;
		}
		
		//Returns the bit width of the character count field for a segment in this mode
		//in a QR Code at the given version number. The result is in the range [0, 16].
		int numCharCountBits(int ver) {
			assert QrCode.MIN_VERSION <= ver && ver <= QrCode.MAX_VERSION;
			return numBitsCharCount[(ver + 7) / 17];
		}
		
	}
	
}