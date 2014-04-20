package xjetstorm.svh;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.MaskFormatter;

public class MainWindow extends JFrame
{
	private class EggBox extends JPanel
	{
		private static final String HEX_DIGITS = "0123456789ABCDEF";
		private static final long serialVersionUID = 7002020669466770864L;
		
		private Integer esv;
		private String esvDisplay;
		private JTextField esvField;
		private JPanel fileInput;
		private JPanel inputBox;
		private JPanel inputField;
		private JPanel inputLabel;
		private JLabel[] labels;
		private JButton openEsvList;
		private JButton openPidList;
		private Integer pid;
		private String pidDisplay;
		private JFormattedTextField pidField;
		private MaskFormatter pidFormat;
		
		EggBox()
		{
			labels = new JLabel[2];
			labels[0] = new JLabel("PID:");
			labels[1] = new JLabel("ESV:");
			labels[0].setHorizontalAlignment(SwingConstants.RIGHT);
			labels[1].setHorizontalAlignment(SwingConstants.RIGHT);

			pidFormat = createFormatter("HHHHHHHH");
			pidFormat.setPlaceholderCharacter('0');
			pidFormat.setOverwriteMode(true);
			pidFormat.setValueContainsLiteralCharacters(false);

			pidField = new JFormattedTextField(pidFormat);
			esvField = new JTextField();
			pidField.setHorizontalAlignment(SwingConstants.RIGHT);

			esvField.setHorizontalAlignment(SwingConstants.RIGHT);
			esvField.setEditable(false);
			esvField.setColumns(6);

			pid = 0;
			esv = 0;
			updateEggValues();

			openPidList = new JButton("Open PID File");
			openPidList
					.setToolTipText("Select a file to read PIDs from. A valid PID is hexadecimal, up to 8 digits in length (excluding optional \"0x\" prefix).");
			openEsvList = new JButton("Open ESV List");
			openEsvList
					.setToolTipText("Opens the list of processed PID/ESV with the default text editor.");

			inputLabel = new JPanel(new GridLayout(2, 1));
			inputLabel.add(labels[0]);
			inputLabel.add(labels[1]);

			inputField = new JPanel(new GridLayout(2, 1));
			inputField.add(pidField);
			inputField.add(esvField);

			fileInput = new JPanel(new GridLayout(2, 1));
			fileInput.add(openPidList);
			fileInput.add(openEsvList);

			inputBox = new JPanel(new BorderLayout());
			inputBox.add(inputLabel, BorderLayout.WEST);
			inputBox.add(inputField, BorderLayout.EAST);

			setLayout(new BorderLayout());
			add(inputBox, BorderLayout.NORTH);
			add(fileInput, BorderLayout.SOUTH);

			ActionListener aListener = new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent evt)
				{
					Object source = evt.getSource();
					if (source.equals(openPidList)) {
						openPidList();
					} else if (source.equals(openEsvList)) {
						openEsvList();
					}
				}
			};
			openPidList.addActionListener(aListener);
			openEsvList.addActionListener(aListener);
			pidField.addPropertyChangeListener("value",
					new PropertyChangeListener()
					{
						@Override
						public void propertyChange(PropertyChangeEvent evt)
						{
							Object source = evt.getSource();
							if (source.equals(pidField)) {
								pid = convertHex((String) pidField.getValue());
								esv = calculateEsv(pid);
								updateEggValues();
							}
						}
					});
		}
		
		private Integer calculateEsv(Integer pid)
		{
			return ((pid / 65536) ^ (pid % 65536)) >> 4;
		}

		private Integer convertHex(String value)
		{
			char[] digits = value.toCharArray();
			int num = 0;
			for (int i = digits.length - 1, p = 1; i >= 0; i--, p *= 16) {
				num += HEX_DIGITS.indexOf(digits[i]) * p;
			}
			return num;
		}

		private MaskFormatter createFormatter(String mask)
		{
			MaskFormatter format = null;
			try {
				format = new MaskFormatter(mask);
			} catch (java.text.ParseException pe) {
				System.out.println("Incorrect format: " + mask);
			}
			return format;
		}

		private int hexToInt(String str)
		{
			int n = 0;
			char[] digits = str.toCharArray();
			for (int i = digits.length - 1, p = 1; i >= 0; i--, p *= 16)
				n += HEX_DIGITS.indexOf(digits[i]) * p;
			return n;
		}

		private boolean isValidHex(String str)
		{
			char[] digits = str.substring(0, 8).toCharArray();
			for (char ch : digits)
				if (HEX_DIGITS.indexOf(ch) == -1)
					return false;
			return true;
		}

		private void openEsvList()
		{
			try {
				java.awt.Desktop.getDesktop().edit(new File("esvList.txt"));
			} catch (IllegalArgumentException iae) {
				System.out.println("Cannot open file!");
			} catch (IOException e) {
				System.out.println("File does not exist!");
			}
		}

		private void openPidList()
		{
			FileDialog chooser = new FileDialog((java.awt.Frame) null);
			chooser.setVisible(true);
			File[] fileQueue = chooser.getFiles();
			for (File f : fileQueue) {
				try {
					processFile(f);
				} catch (IOException ioe) {
					System.out.println(String.format(
							"Could not process file: %s", f.getAbsolutePath()));
				}
			}
			openEsvList();
		}

		private void processFile(File input) throws IOException
		{
			File output = new File("esvList.txt");
			BufferedReader reader = new BufferedReader(new FileReader(input));
			PrintWriter writer = new PrintWriter(new FileWriter(output,
					output.exists()));
			String header = String.format("### %s ### Date:%s ###",
					input.getAbsolutePath(), new Date().toString());
			writer.println(header);
			String line = reader.readLine();
			while (line != null) {
				if (line.indexOf("0x") == 0) {
					Integer pidNum = hexToInt(line.substring(2));
					Integer esvNum = calculateEsv(pidNum);
					String outputStr = String.format("%08X %04d", pidNum,
							esvNum);
					writer.println(outputStr);
				}
				if (isValidHex(line)) {
					Integer pidNum = hexToInt(line);
					Integer esvNum = calculateEsv(pidNum);
					String outputStr = String.format("%08X %04d", pidNum,
							esvNum);
					writer.println(outputStr);
				}
				line = reader.readLine();
			}
			reader.close();
			writer.close();
		}

		private void updateEggValues()
		{
			pidDisplay = String.format("%08X", pid);
			esvDisplay = String.format("%04d", esv);
			pidField.setValue(pidDisplay);
			esvField.setText(esvDisplay);
		}
	}

	private class HelpBox extends JPanel
	{
		private static final long serialVersionUID = 3835260649015147435L;

		HelpBox()
		{
			
		}
	}
	
	private class TrainerBox extends JPanel
	{
		private static final String HEX_DIGITS = "0123456789ABCDEF";
		private static final long serialVersionUID = -1794462238045181035L;
		
		private JPanel tidPane;
		private JPanel sidPane;
		private JPanel tsvPane;
		private JLabel[] labels;
		private JButton saveTrainerButton;
		private Integer sid;
		private String sidDisplay;
		private JFormattedTextField sidField;
		private Integer tid;
		private String tidDisplay;
		private JFormattedTextField tidField;
		private MaskFormatter tidFormat;
		private Integer tsv;
		private String tsvDisplay;
		private JTextField tsvField;
		
		TrainerBox()
		{
			labels = new JLabel[3];
			labels[0] = new JLabel("TID:");
			labels[1] = new JLabel("SID:");
			labels[2] = new JLabel("TSV:");
			labels[0].setHorizontalAlignment(SwingConstants.RIGHT);
			labels[1].setHorizontalAlignment(SwingConstants.RIGHT);
			labels[2].setHorizontalAlignment(SwingConstants.RIGHT);

			tidFormat = createFormatter("#####");
			tidFormat.setPlaceholderCharacter('0');
			tidFormat.setOverwriteMode(true);
			tidFormat.setValueContainsLiteralCharacters(false);

			tidField = new JFormattedTextField(tidFormat);
			tidField.setHorizontalAlignment(SwingConstants.RIGHT);
			tidField.setMaximumSize(new Dimension(78,28));
			sidField = new JFormattedTextField(tidFormat);
			sidField.setHorizontalAlignment(SwingConstants.RIGHT);
			sidField.setMaximumSize(new Dimension(78,28));

			tsvField = new JTextField();
			tsvField.setHorizontalAlignment(SwingConstants.RIGHT);
			tsvField.setEditable(false);
			tsvField.setColumns(6);
			tsvField.setMaximumSize(new Dimension(78,28));

			loadTrainer();
			updateTrainerValues();

			saveTrainerButton = new JButton("Save Trainer ID");
			saveTrainerButton
					.setToolTipText("Saves your TID/SID so you don't need to remember it.");

			tidPane = new JPanel();
			tidPane.setLayout(new BoxLayout(tidPane, BoxLayout.LINE_AXIS));
			tidPane.add(labels[0]);
			tidPane.add(tidField);
			
			sidPane = new JPanel();
			sidPane.setLayout(new BoxLayout(sidPane, BoxLayout.LINE_AXIS));
			sidPane.add(labels[1]);
			sidPane.add(sidField);
			
			tsvPane = new JPanel();
			tsvPane.setLayout(new BoxLayout(tsvPane, BoxLayout.LINE_AXIS));
			tsvPane.add(labels[2]);
			tsvPane.add(tsvField);

			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			add(tidPane);
			add(sidPane);
			add(tsvPane);
			add(saveTrainerButton);

			saveTrainerButton.addActionListener(new ActionListener()
			{

				@Override
				public void actionPerformed(ActionEvent evt)
				{
					saveTrainer();
				}

			});
			PropertyChangeListener pListener = new PropertyChangeListener()
			{

				@Override
				public void propertyChange(PropertyChangeEvent evt)
				{
					Object source = evt.getSource();
					if (source.equals(tidField)) {
						tid = Integer.parseInt((String) tidField.getValue());
						tsv = (tid ^ sid) >> 4;
						updateTrainerValues();
					} else if (source.equals(sidField)) {
						sid = Integer.parseInt((String) sidField.getValue());
						tsv = calculateTsv(tid, sid);
						updateTrainerValues();
					}
				}

			};
			tidField.addPropertyChangeListener("value", pListener);
			sidField.addPropertyChangeListener("value", pListener);
		}
		
		private Integer calculateTsv(Integer tid, Integer sid)
		{
			return (tid ^ sid) >> 4;
		}

		private MaskFormatter createFormatter(String mask)
		{
			MaskFormatter format = null;
			try {
				format = new MaskFormatter(mask);
			} catch (java.text.ParseException pe) {
				System.out.println("Incorrect format: " + mask);
			}
			return format;
		}

		private int hexToInt(String str)
		{
			int n = 0;
			char[] digits = str.toCharArray();
			for (int i = digits.length - 1, p = 1; i >= 0; i--, p *= 16)
				n += HEX_DIGITS.indexOf(digits[i]) * p;
			return n;
		}

		private String intToHex(int num)
		{
			StringBuilder str = new StringBuilder(4);
			for (int i = 0; i < 4; i++) {
				int digit = num % 16;
				char ch = HEX_DIGITS.charAt(digit);
				str.insert(0, ch);
				num /= 16;
			}
			return str.toString();
		}

		private void loadTrainer()
		{
			try {
				BufferedReader reader = new BufferedReader(new FileReader(
						new File("config.ini")));
				String line = reader.readLine();
				String tidS = line.substring(0, 4);
				String sidS = line.substring(4, 8);
				tid = hexToInt(tidS);
				sid = hexToInt(sidS);
				tsv = (tid ^ sid) >> 4;
				reader.close();
			} catch (IOException ioe) {
				tid = 0;
				sid = 0;
				System.out.println("Failed to load config.");
			}
			tsv = (tid ^ sid) >> 4;
		}

		private void saveTrainer()
		{
			try {
				PrintWriter writer = new PrintWriter(new FileWriter(new File(
						"config.ini")));
				String str = intToHex(tid) + intToHex(sid);
				writer.print(str);
				writer.close();
			} catch (IOException ioe) {
				System.out.println("Failed to save config.");
			}
		}

		private void updateTrainerValues()
		{
			tidDisplay = String.format("%05d", tid);
			sidDisplay = String.format("%05d", sid);
			tsvDisplay = String.format("%04d", tsv);
			tidField.setValue(tidDisplay);
			sidField.setValue(sidDisplay);
			if (tid >= 65536 || sid >= 65536)
				tsvField.setText("Error");
			else
				tsvField.setText(tsvDisplay);
		}
	}

	private static final byte[] EGGICON = { -119, 80, 78, 71, 13, 10, 26, 10,
			0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 64, 0, 0, 0, 64, 8, 6, 0, 0,
			0, -86, 105, 113, -34, 0, 0, 1, -94, 73, 68, 65, 84, 120, -38, -19,
			-37, -63, 113, -61, 32, 16, 5, 80, -107, -95, 118, -46, -126, -113,
			41, 33, 109, -88, -115, -108, -95, 107, 74, 80, 11, 58, -70, -124,
			92, -15, 68, -29, -11, -116, -119, 96, 97, 89, 96, 109, -66, 102,
			-2, -47, -96, 125, 6, 100, -55, 104, 114, -50, 77, 35, 103, 2, 0,
			0, -38, 119, -6, 121, -7, 112, -79, 0, -32, -35, 0, 78, 10, -116,
			30, 45, 65, 0, 96, -95, -32, -98, 32, 0, -88, 81, -8, -10, -77, 28,
			-47, 62, -88, 93, 77, 8, 0, 104, 52, 50, -49, -77, -5, -117, -5,
			-35, -97, -93, 125, 120, -19, 63, -6, 5, 64, 39, -128, 96, -31,
			-38, 16, 76, -5, 37, 16, 0, -88, 90, -72, 20, 68, -40, -82, 4, 2,
			0, 69, -105, 57, 41, 64, -91, 72, 46, -109, 0, 104, 58, -12, 27,
			37, 103, 42, 0, 32, -89, -16, -17, 109, 121, -54, 118, 93, -113,
			88, 3, -72, -18, -21, -111, -108, -87, 0, -128, 18, 0, 63, -81, 56,
			21, 0, -112, 114, -39, -29, 10, -73, 10, -111, 50, 21, 0, -96, 49,
			-12, -75, 32, 106, 45, -78, -79, -87, 0, 0, 11, 0, -75, -89, 20, 0,
			114, 1, -92, -117, -97, 116, -24, 74, 1, -88, 31, -82, -33, -40,
			98, 8, 0, 11, -117, -97, 95, 8, -41, 78, -18, 23, 64, 0, 103, 83,
			1, 0, 49, 0, -6, 96, -24, 4, 91, -33, 20, 113, -25, 17, 2, 4, 64,
			41, -128, -107, -97, -74, -46, -59, 24, 0, 82, 0, 43, 15, 63, 115,
			-121, -2, 63, -120, 123, 29, 0, 72, 5, -80, -10, 16, -76, 20, -96,
			-8, 94, 96, 120, -128, -34, -117, -95, 20, 96, 89, -65, -114, 0,
			-96, 20, -128, 22, 17, -126, -24, 5, -110, 90, 48, 37, -74, -8, 1,
			64, -14, 88, -100, 26, 12, -91, 54, 16, -75, -21, 23, 26, -118, -6,
			-1, 2, 0, 8, 20, 30, 58, 1, 14, -120, 11, 7, -50, -11, 11, 0, 45,
			-128, 16, 68, -22, 80, -28, 10, -47, 78, -11, -3, 1, -61, 2, -8,
			16, -87, 0, -83, 32, -102, -17, 18, 27, 22, 64, 10, 97, -87, 112,
			0, -44, -40, 44, -51, -127, 104, 23, 108, 110, -73, -8, -80, 0, 28,
			-120, -91, -126, 1, -48, -29, -75, 57, -65, -128, -36, -68, -4,
			123, -125, -61, 3, 88, -50, 13, -67, 16, 38, -81, 66, 99, -4, -21,
			0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126 };

	private static final long serialVersionUID = 543294312060413346L;

	public static void main(final String[] args)
	{
		new MainWindow();
	}

	private JTabbedPane contentPane;

	private Point initialClick;

	public MainWindow()
	{
		setTitle("SV Toolbox by xJetStorm");
		setResizable(false);

		InputStream in = new ByteArrayInputStream(EGGICON);
		try {
			BufferedImage image = ImageIO.read(in);
			setIconImage(image);
		} catch (IOException e) {
			e.printStackTrace();
		}

		contentPane = new JTabbedPane();
		contentPane.addTab("Trainer", null, new TrainerBox(), "Check Trainer Information");
		contentPane.setMnemonicAt(0, 'T');
		contentPane.addTab("Egg", null, new EggBox(), "Check Egg Information");
		contentPane.setMnemonicAt(1, 'E');
		contentPane.addTab("Help", null, new HelpBox(), "Program Help");
		contentPane.setMnemonicAt(2, 'H');

		MouseMotionAdapter mouseMotionAdapter = new MouseMotionAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent e)
			{
				// get location of Window
				final int thisX = getLocation().x;
				final int thisY = getLocation().y;
				// Determine how much the mouse moved since the initial click
				final int xMoved = e.getX() - initialClick.x;
				final int yMoved = e.getY() - initialClick.y;
				final int[] positions = getPosition();
				if (!isResizable()) {
					// Move window to this position
					final int X = thisX + xMoved;
					final int Y = thisY + yMoved;
					setLocation(X, Y);
				} else {
					if (thisX <= positions[0] || thisY <= positions[1]) {
						// Attempt to resize if the initial point is 2 pixels
						// from one of the borders.
						// Move window to this position
						final int X = thisX + xMoved;
						final int Y = thisY + yMoved;
						setBounds(X, Y, (positions[2] - positions[0])
								- (positions[0] + xMoved),
								(positions[3] - positions[1])
										- (positions[1] + yMoved));
					} else if (thisX >= positions[2] || thisY >= positions[3]) {
						// Attempt to resize if the initial point is 2 pixels
						// from one of the borders.
						// Move window to this position
						setBounds(positions[0], positions[1],
								(positions[2] - positions[0]) + xMoved,
								(positions[3] - positions[1]) + yMoved);
					}
				}

			}
		};
		MouseAdapter mouseAdapter = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				initialClick = e.getPoint();
				getComponentAt(initialClick);
			}
		};
		contentPane.addMouseMotionListener(mouseMotionAdapter);
		contentPane.addMouseListener(mouseAdapter);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setContentPane(contentPane);
		refresh();
		setLocationRelativeTo(null);
		setVisible(true);

	}

	private int[] getPosition()
	{
		int[] positions = { getX(), getY(), getX() + getWidth(),
				getY() + getHeight() };
		return positions;
	}

	public void refresh()
	{
		try {
			UIManager
					.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
		}
		SwingUtilities.updateComponentTreeUI(this);
		pack();
	}
}
