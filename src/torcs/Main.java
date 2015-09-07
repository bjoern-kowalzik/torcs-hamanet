package torcs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import torcs.scr.Client;

public class Main {

	// path to torcs
	static final String TORCS_FOLDER = "Z:\\torcs";

	// array containing all drivers to compete in the race
	static final String[] DRIVERS = new String[] { "torcs.simple.SimpleDriver",
			"driver.old.LuisaDriver", "driver.old.DahlemBoyzDriver",
			"driver.old.MERKVRIVSDriver", "driver.old.PiltoversFinestDriver",
			"driver.old.HotPotatoDriver" };

	// track name
	static final String TRACK_NAME = "forza";
	// static final String TRACK_NAME = "g-track-3";

	// track category
	static final String TRACK_CATEGORY = "road";

	// verbose flag
	// True: all data send to and received from the server will be logged to
	// std.out
	static final boolean VERBOSE = false;

	// damage flag.
	// True: damage counts and cars can be demolished. Default is false
	static final boolean DAMAGE = false;

	// the stage. Either QUALIFYING or RACE
	static final Client.Stage STAGE = Client.Stage.RACE;

	public static void main(String[] args) throws IOException {

		// start command shell and create a writer to interact with processes
		ProcessBuilder builder = new ProcessBuilder("cmd.exe");
		Process p = builder.start();
		Writer writer = new OutputStreamWriter(p.getOutputStream());

		// kill old processes
		writer.write("taskkill /f /im wtorcs.exe\n");
		writer.write("taskkill /f /im java.exe\n");
		writer.flush();

		// write quick race file
		writeQuickRace();

		// write car textures
		replaceTextures(writer);

		// start executables
		startTorcs(writer);
		startDrivers(writer);

		// read from the command shell
		InputStream is = p.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}
	}

	private static void replaceTextures(Writer writer) {

		// iterate on the drivers
		for (int i = 0; i < DRIVERS.length; ++i) {

			// find source texture file
			String source = DRIVERS[i]
					.substring(0, DRIVERS[i].lastIndexOf('.'));
			source = source.replace('.', '/');
			source = source + "/car1-trb1.rgb";
			InputStream in = Main.class.getClassLoader().getResourceAsStream(
					source);

			// target texture file
			Path target = (new File(TORCS_FOLDER + "\\drivers\\scr_server\\"
					+ i + "\\car1-trb1.rgb")).toPath();

			// replace file
			try {
				Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
				in.close();
			} catch (IOException e) {
				System.out.println("Warning: Can not replace texture file: "
						+ target);
				// e.printStackTrace();
			} catch (NullPointerException e) {
				System.out
						.println("Warning: Texture File not found: " + source);
				// e.printStackTrace();
			}
		}
	}

	private static void writeQuickRace() {

		// delete old quick race config
		File quickRaceConfig = new File(TORCS_FOLDER
				+ "\\config\\raceman\\quickrace.xml");
		quickRaceConfig.delete();

		// create reader and writer
		BufferedReader br = null;
		BufferedWriter bw = null;
		try {
			br = new BufferedReader(new InputStreamReader(Main.class
					.getClassLoader()
					.getResourceAsStream("torcs/quickrace.xml")));
			bw = new BufferedWriter(new FileWriter(quickRaceConfig));
			String line;
			int lineCounter = 0;
			while ((line = br.readLine()) != null) {
				if (lineCounter == 15) {
					bw.write("      <attstr name=\"name\" val=\"" + TRACK_NAME
							+ "\"/>\n");
					bw.write("      <attstr name=\"category\" val=\""
							+ TRACK_CATEGORY + "\"/>\n");
				} else if (lineCounter == 47) {
					for (int i = 0; i < DRIVERS.length; ++i) {
						bw.write("      <section name=\"" + (i + 1) + "\">\n");
						bw.write("        <attnum name=\"idx\" val=\"" + i
								+ "\"/>\n");
						bw.write("        <attstr name=\"module\" val=\"scr_server\"/>\n");
						bw.write("      </section>\n");
					}
				}
				bw.write(line + "\n");
				lineCounter++;
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (bw != null)
					bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void startTorcs(Writer writer) throws IOException {

		// check, if path is absolute
		String path = TORCS_FOLDER;
		if ((new File(path)).isAbsolute()) {
			writer.write(path.substring(0, 2) + "\n");
			path = (path.substring(2, path.length()));
		}

		writer.write("cd " + path + "\n");
		writer.write("start \"torcs\" \"wtorcs.exe\" -nofuel");
		if (!DAMAGE) {
			writer.write(" -nodamage");
		}
		writer.write("\n");
		writer.flush();
	}

	private static void startDrivers(Writer writer) throws IOException {

		// initial values for port and id
		int port = 3001;
		int id = 1;

		// iterate on the drivers
		for (String driver : DRIVERS) {
			// start each driver in its own process
			writer.write("start /MIN \"" + driver + "\" \"java\" ");
			writer.write("-cp " + System.getProperty("java.class.path")
					+ " -Xmx256m ");
			writer.write("torcs.scr.Client ");
			writer.write(driver + " host:127.0.0.1 port:" + (port++)
					+ " id:SCR" + (id++) + " damage:" + (DAMAGE ? "on" : "off")
					+ " verbose:" + (VERBOSE ? "on" : "off") + " track:"
					+ TRACK_NAME + " stage:" + STAGE);
			writer.write("\n");
			writer.flush();
		} // end of for
	}
}
