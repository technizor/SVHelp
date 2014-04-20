package xjetstorm.svh;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.MaskFormatter;

import org.jetstorm.stormFrame.StormConstraints;
import org.jetstorm.stormFrame.StormFrame;

public class Window extends StormFrame implements PropertyChangeListener
{
	private static final String hexDigits = "0123456789ABCDEF";
	private static final long serialVersionUID = 543294312060413346L;
	private JPanel contentPane;
	private JPanel trainer;
	private JPanel trainerLabel;
	private JPanel trainerBox;
	private JPanel egg;
	private JPanel eggInfo;
	private JPanel eggLabel;
	private JPanel eggBox;
	private JPanel eggButtons;
	private JFormattedTextField tid;
	private JFormattedTextField sid;
	private JFormattedTextField pid;
	private JTextField tsv;
	private JTextField esv;
	private JButton saveTrainer;
	private JButton openPidList;
	private JButton openEsvList;
	private String tidS;
	private String sidS;
	private String tsvS;
	private String pidS;
	private String esvS;
	private Integer tidN;
	private Integer sidN;
	private Integer tsvN;
	private Integer pidN;
	private Integer esvN;

	public static void main(final String[] args)
	{
		new Window();
	}

	public Window()
	{
		super("SV Tool", false, false);

		setVisible(true);

	}

	private void updateEggValues()
	{
		pidS = String.format("%08X", pidN);
		esvS = String.format("%04d", esvN);
		pid.setValue(pidS);
		esv.setText(esvS);
	}

	private void updateTrainerValues()
	{
		tidS = String.format("%05d", tidN);
		sidS = String.format("%05d", sidN);
		tsvS = String.format(" %04d", tsvN);
		tid.setValue(tidS);
		sid.setValue(sidS);
		if (tidN >= 65536 || sidN >= 65536)
			tsv.setText("Error");
		else
			tsv.setText(tsvS);
	}

	private void loadTrainer()
	{
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(
					"config.ini")));
			String line = reader.readLine();
			String tidS = line.substring(0, 4);
			String sidS = line.substring(4, 8);
			tidN = hexToInt(tidS);
			sidN = hexToInt(sidS);
			tsvN = (tidN ^ sidN) >> 4;
			reader.close();
		} catch (IOException ioe) {
			tidN = 0;
			sidN = 0;
			System.out.println("Failed to load config.");
		}
		tsvN = (tidN ^ sidN) >> 4;
	}

	private void saveTrainer()
	{
		try {
			PrintWriter writer = new PrintWriter(new FileWriter(new File(
					"config.ini")));
			String str = intToHex(tidN) + intToHex(sidN);
			writer.print(str);
			writer.close();
		} catch (IOException ioe) {
			System.out.println("Failed to save config.");
		}
	}

	private int hexToInt(String str)
	{
		int n = 0;
		char[] digits = str.toCharArray();
		for (int i = digits.length - 1, p = 1; i >= 0; i--, p *= 16)
			n += hexDigits.indexOf(digits[i]) * p;
		return n;
	}

	private String intToHex(int num)
	{
		StringBuilder str = new StringBuilder(4);
		for (int i = 0; i < 4; i++) {
			int digit = num % 16;
			char ch = hexDigits.charAt(digit);
			str.insert(0, ch);
			num /= 16;
		}
		return str.toString();
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

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		Object source = evt.getSource();
		if (source.equals(tid)) {
			tidN = Integer.parseInt((String) tid.getValue());
			tsvN = (tidN ^ sidN) >> 4;
			updateTrainerValues();
		} else if (source.equals(sid)) {
			sidN = Integer.parseInt((String) sid.getValue());
			tsvN = calculateTsv(tidN, sidN);
			updateTrainerValues();
		} else if (source.equals(pid)) {
			pidN = convertHex((String) pid.getValue());
			esvN = calculateEsv(pidN);
			updateEggValues();
		}
	}

	private Integer convertHex(String value)
	{
		char[] digits = value.toCharArray();
		int num = 0;
		for (int i = digits.length - 1, p = 1; i >= 0; i--, p *= 16) {
			num += hexDigits.indexOf(digits[i]) * p;
		}
		return num;
	}

	private Integer calculateTsv(Integer tid, Integer sid)
	{
		return (tid ^ sid) >> 4;
	}

	private Integer calculateEsv(Integer pid)
	{
		return ((pid / 65536) ^ (pid % 65536)) >> 4;
	}

	@Override
	public void actionHandler(Object source)
	{
		if (source.equals(saveTrainer)) {
			saveTrainer();
		} else if (source.equals(openPidList)) {
			openPidList();
		} else if (source.equals(openEsvList)) {
			openEsvList();
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
				System.out.println(String.format("Could not process file: %s",
						f.getAbsolutePath()));
			}
		}
	}

	private void processFile(File input) throws IOException
	{
		File output = new File("esvList.txt");
		BufferedReader reader = new BufferedReader(new FileReader(input));
		PrintWriter writer = new PrintWriter(new FileWriter(output,
				output.exists()));
		String header = String.format("# File:%s Date:%s",
				input.getAbsolutePath(), new Date().toString());
		writer.println(header);
		String line = reader.readLine();
		while (line != null) {
			if (line.charAt(0) != '#' && line.length() <= 8) {
				Integer pidNum = hexToInt(line);
				Integer esvNum = calculateEsv(pidNum);
				String outputStr = String.format("%08X %04d", pidNum, esvNum);
				writer.println(outputStr);
			}
			line = reader.readLine();
		}
		reader.close();
		writer.close();
	}

	private void openEsvList()
	{
		try {
			java.awt.Desktop.getDesktop().edit(new File("esvList.txt"));
		} catch (IllegalArgumentException iae) {
			System.out.println("No File to Open!");
		} catch (IOException e) {
			System.out.println("No File to Open!");
		}
	}

	@Override
	public void addActionListeners()
	{
		saveTrainer.addActionListener(this);
		openPidList.addActionListener(this);
		openEsvList.addActionListener(this);
		tid.addPropertyChangeListener("value", this);
		sid.addPropertyChangeListener("value", this);
		pid.addPropertyChangeListener("value", this);
	}

	@Override
	public void buildDefaultElements()
	{
		buildElement(contentPane, new StormConstraints(1, 1, 0, 0, 0, 0, 0, 0));
	}

	@Override
	public void configureElements()
	{
		trainer = new JPanel();
		trainerLabel = new JPanel();
		trainerBox = new JPanel();
		loadTrainer();
		saveTrainer = new JButton("Save Trainer ID");

		MaskFormatter tidFormat = createFormatter("#####");
		tidFormat.setPlaceholderCharacter('0');
		tidFormat.setOverwriteMode(true);
		tidFormat.setValueContainsLiteralCharacters(false);
		tid = new JFormattedTextField(tidFormat);
		sid = new JFormattedTextField(tidFormat);
		tsv = new JTextField();
		tid.setHorizontalAlignment(JTextField.RIGHT);
		sid.setHorizontalAlignment(JTextField.RIGHT);
		tsv.setHorizontalAlignment(JTextField.RIGHT);
		tsv.setEditable(false);
		tid.setColumns(4);
		sid.setColumns(4);
		tsv.setColumns(4);
		updateTrainerValues();

		trainerLabel.setLayout(new GridLayout(3, 1));
		trainerBox.setLayout(new GridLayout(3, 1));

		trainerLabel.add(new JLabel("TID:"));
		trainerLabel.add(new JLabel("SID:"));
		trainerLabel.add(new JLabel("TSV:"));
		trainerBox.add(tid);
		trainerBox.add(sid);
		trainerBox.add(tsv);
		trainer.setLayout(new BorderLayout());
		trainer.add(trainerLabel, BorderLayout.WEST);
		trainer.add(trainerBox, BorderLayout.EAST);
		trainer.add(saveTrainer, BorderLayout.SOUTH);

		MaskFormatter pidFormat = createFormatter("HHHHHHHH");
		tidFormat.setPlaceholderCharacter('0');
		tidFormat.setOverwriteMode(true);
		tidFormat.setValueContainsLiteralCharacters(false);

		pidN = 0;
		esvN = 0;
		pid = new JFormattedTextField(pidFormat);
		esv = new JTextField();
		pid.setColumns(6);
		esv.setColumns(4);
		pid.setHorizontalAlignment(JTextField.RIGHT);
		esv.setHorizontalAlignment(JTextField.RIGHT);
		esv.setEditable(false);
		updateEggValues();

		openPidList = new JButton("Open PID File");
		openEsvList = new JButton("Open ESV List");

		egg = new JPanel();
		eggInfo = new JPanel();
		eggLabel = new JPanel();
		eggLabel.setLayout(new GridLayout(2, 1));
		eggLabel.add(new JLabel("PID:"));
		eggLabel.add(new JLabel("ESV:"));
		eggBox = new JPanel();
		eggBox.setLayout(new GridLayout(2, 1));
		eggBox.add(pid);
		eggBox.add(esv);
		eggButtons = new JPanel();
		eggButtons.setLayout(new GridLayout(2, 1));
		eggButtons.add(openPidList);
		eggButtons.add(openEsvList);
		eggInfo.setLayout(new BorderLayout());
		eggInfo.add(eggLabel, BorderLayout.WEST);
		eggInfo.add(eggBox, BorderLayout.EAST);
		egg.setLayout(new BorderLayout());
		egg.add(eggInfo, BorderLayout.NORTH);
		egg.add(eggButtons, BorderLayout.SOUTH);

		contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(trainer, BorderLayout.WEST);
		contentPane.add(egg, BorderLayout.EAST);
	}
}
