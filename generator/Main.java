import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;


public class Main {
	
	public final static String HELP = "\nUsage: sip -i <input_file> -o <output_file> [options]\n\n"
			+ "Options:\n"
			+ "    -i <file>       : Input file (Required)\n"
			+ "    -o <file>       : Output file (Required)\n"
			+ "    -E              : Embed everything, overrides other embed options [false]\n"
			+ "    --embed-images  : Embed images (Not recommended) [false]\n"
			+ "    --embed-scripts : Embed JavaScript scripts [false]\n"
			+ "    --embed-css     : Embed CSS stylesheets [false]\n"
			+ "    -C              : Don't compress HTML [false]\n"
			+ "    -t <title>      : Custom window title [Get from <title>]\n"
			+ "    -L <site_root>  : Get resources with relatives paths from internet [false]\n"
			+ "    -s              : Skip all (Useful for automated scripts)\n";
	public final static String FINGERPRINT = "D87E580A0F85F020E716C55418AADC82C4C37446";
	
	static File inputFile;
	static File outputFile;
	static boolean embedAll = false, embedImages = false, embedScripts = false,
			embedCSS = false, compress = true, relativesFromStorage = true, skipAll = false;
	static String customTitle = "";
	static String siteRoot = "";
	static long startTime;
	
	public static void main(String[] args) {
		parseArgs(args);
		startTime = System.currentTimeMillis();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("\n[+] Finished in " + ((System.currentTimeMillis() - startTime)/1000) + " seconds");
			}
		}));
		System.out.println("[+] Reading input..");
		String input = "";
		try {
			input = IOUtils.toString(new FileInputStream(inputFile));
		} catch (IOException e) {
			System.out.println("[!] Unable to read input!");
			System.exit(255);
		}
		String outputData = formatInput(input);
		int imageSize = calcDimensions(outputData);
		outputData = padString(outputData, imageSize * imageSize * 3);
		BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB);
		image = writeToImage(pack(outputData), image);
		try {
			System.out.println("[+] Writing image to disk..");
			ImageIO.write(image, "png", outputFile);
		} catch (IOException e) {
			System.out.println("[!] Unable to write the image: " + e.getMessage());
			System.exit(255);
		}
	}
	
	public static String formatInput(String input) {
		if (compress) {
			System.out.println("[+] Compressing input..");
			input = (new HtmlCompressor()).compress(input);
		}
		Document document = Jsoup.parse(input);
		if (embedAll) {
			document = embedImages(document);
			document = embedStyles(document);
			document = embedScripts(document);
		} else {
			if (embedImages)
				document = embedImages(document);
			if (embedCSS)
				document = embedStyles(document);
			if (embedScripts)
				document = embedScripts(document);
		}
		System.out.println("[+] Creating JSON data..");
		JSONObject json = new JSONObject();
		try {
			json.put("html", document.toString());
			json.put("title", customTitle.isEmpty() ? document.title() : customTitle);
		} catch (JSONException e) {
			System.out.println("[!] Error while creating JSON data");
			System.exit(2);
		}
		return FINGERPRINT + json.toString();
	}
	
	public static Document embedImages(Document doc) {
		System.out.println("[+] Embedding images..");
		Elements imgs = doc.select("img");
		for (Element img : imgs) {
			String oldData = img.attr("src");
			byte[] newData = getResource(oldData);
			if (!(new String(newData).equals(oldData)))
				img.attr("src", "data:image/png;base64," + Base64.getEncoder().encodeToString(newData));
		}
		return doc;
	}
	
	public static Document embedStyles(Document doc) {
		System.out.println("[+] Embedding styles..");
		Elements styles = doc.select("link[rel=stylesheet]");
		for (Element style : styles) {
			String oldData = style.attr("href");
			String newData = new String(getResource(style.attr("href")));
			if (!newData.equals(oldData))
				style.replaceWith(Jsoup.parse("<style rel=\"stylesheet\" type=\"text/css\">" + newData + "</style>", "", Parser.xmlParser()));
		}
		return doc;
	}
	
	public static Document embedScripts(Document doc) {
		System.out.println("[+] Embedding scripts..");
		Elements scripts = doc.select("script");
		for (Element script : scripts) {
			String oldData = script.attr("src");
			String newData = new String(getResource(oldData));
			if (!newData.equals(oldData))
				script.replaceWith(Jsoup.parse("<script type=\"text/javascript\">" + newData + "</script>", "", Parser.xmlParser()));
		}
		return doc;
	}
	
	public static byte[] getResource(String source) {
		do {
			if (source.isEmpty() || source.startsWith("data:"))
				return source.getBytes();
			System.out.println("[+] Getting resource: " + source + " ...");
			try {
				if (source.startsWith("http://") || source.startsWith("https://")) {
					return IOUtils.toByteArray(new URL(source));
				} else {
					if (source.matches(".+://.+"))
						throw new IOException("Unknown protocol: " + source.split("://")[0] + "://");
					if (Paths.get(source).isAbsolute())
						return source.getBytes();
					if (relativesFromStorage) {
						return IOUtils.toByteArray(new FileInputStream(source));
					} else {
						return IOUtils.toByteArray(new URL((siteRoot.endsWith("/") ? siteRoot : siteRoot + "/")
								+ (source.startsWith("/") ? source.substring(1) : source)));
					}
				}
			} catch (IOException e) {
				System.out.println("[!] Unable to read resource: " + e.getMessage());
				if (skipAll)
					return source.getBytes();
				System.out.println("[?] What do you want to do? [1] Retry  [2] New source  [3] Skip  [4] Skip all [5] Quit");
				Scanner scanner = new Scanner(System.in);
				String choice = "";
				do {
					System.out.print("[>] ");
					choice = scanner.nextLine();
				} while (choice == null || choice.isEmpty());
				switch (choice) {
				case "1":
					continue;
				case "2":
					System.out.print("[?] New source: ");
					String newSource = scanner.nextLine();
					source = newSource;
					continue;
				case "3":
					return source.getBytes();
				case "4":
					skipAll = true;
					return source.getBytes();
				case "5":
					System.exit(255);
				default:
					return source.getBytes();
				}
			}
		} while (true);
	}
	
	public static BufferedImage writeToImage(int[] data, BufferedImage image) {
		System.out.println("[+] Writing data to image..");
		int counter = 0;
		for (int i=0; i < image.getWidth(); i++) {
			for (int j=0; j < image.getHeight(); j++) {
				image.setRGB(j, i, data[counter]);
				++counter;
			}
		}
		return image;
	}
	
	public static int[] pack(String string) {
		System.out.println("[+] Packing data..");
		int[] data = new int[string.length()/3];
		for (int i=0; i < string.length();) {
			data[i/3] = string.charAt(i) << 16;
			data[i/3] += string.charAt(i+1) << 8;
			data[i/3] += string.charAt(i+2);
			i += 3;
		}
		return data;
	}
	
	public static void parseArgs(String[] args) {
		if (args.length < 2) {
			System.out.println(HELP);
			System.exit(0);
		}
		for (int i=0; i < args.length; i++) {
			switch (args[i]) {
			case "-i":
				inputFile = new File(args[++i]);
				break;
			case "-o":
				outputFile = new File(args[++i]);
				break;
			case "-E":
				embedAll = true;
				break;
			case "--embed-images":
				embedImages = true;
				break;
			case "--embed-scripts":
				embedScripts = true;
				break;
			case "--embed-css":
				embedCSS = true;
				break;
			case "-C":
				compress = false;
				break;
			case "-t":
				customTitle = args[++i];
				break;
			case "-L":
				relativesFromStorage = false;
				siteRoot = args[++i];
				break;
			case "-s":
				skipAll = true;
				break;
			case "-h":
			case "--help":
			case "-?":
				System.out.println(HELP);
				System.exit(0);
				break;
			default:
				System.out.println("Unexpected: " + args[i]);
				System.exit(1);
			}
		}
		if (inputFile == null) {
			System.out.println("No input file specified");
			System.exit(0);
		} else if (outputFile == null) {
			System.out.println("No output file specified");
			System.exit(0);
		}
	}
	
	public static int calcDimensions(String data) {
		return (int) Math.ceil(Math.sqrt(Math.ceil(((double)data.length())/3)));
	}
	
	public static String padString(String str, int leng) {
        for (int i = str.length(); i <= leng - 1; i++)
            str += " ";
        return str;
    }
}
