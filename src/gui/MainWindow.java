package gui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.imgscalr.Scalr;

/** @author Hubert */
public class MainWindow {
	
	private static enum Kolor {
		CZERWONY, ZIELONY, NIEBIESKI
	}

	private final static String tBLAD = "B³¹d";
	private final static String tWEJSCIE = "Scie¿ka do obrazka";
	private final static String tWYJSCIE = "Gdzie zapisaæ obrazek z wtopion¹ b¹dŸ wyzerowan¹ wiadomoœci¹";
	private final static String tWIADOMOSC = "Scie¿ka do pliku z wiadomoœci¹ lub gdzie zapisaæ ekstrahowan¹ wiadomoœæ";
	private final static String bSZUKAJ = "Szukaj...";
	private final static String eBRAKPLIKU = "Brak pliku w: ";
	private final static String eMALY = "Za ma³y obrazek.";
	private final static String SUKCES = "Operacja zakoñczona pomyœlnie.";

	private boolean tryb = true;
	private JFrame frame;
	private static JPanel panel;
	private static JTextField sciezkaWejscie;
	private static JTextField sciezkaWyjscie;
	private static JTextField sciezkaWiadomosc;
	private static JLabel obrazekWejscie;
	private static JLabel obrazekWyjscie;
	private static JLabel opisObrazekWejscie;
	private static JLabel opisObrazekWyjscie;
	private static BufferedImage imgWej;

	/** Main */
	public static void main(String[] args) {
		try {
			UIManager
					.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Throwable e) {
			try {
				UIManager.setLookAndFeel(UIManager
						.getSystemLookAndFeelClassName());
			} catch (Throwable e1) {
				e1.printStackTrace();
			}
		}
		UIManager.put("FileChooser.lookInLabelText", "Szukaj w:");
		UIManager.put("FileChooser.fileNameLabelText", "Nazwa pliku:");
		UIManager.put("FileChooser.filesOfTypeLabelText", "Typy plików:");
		UIManager.put("FileChooser.acceptAllFileFilterText", "Wszystkie pliki");
		UIManager.put("FileChooser.cancelButtonText", "Anuluj");
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/** Konstruktor */
	public MainWindow() {
		initGUI();
	}
	
	private static void wtapianie(Kolor kolor) {
		try {
			wczytajWejscie();
		} catch (IOException e) {
			return;
		}
		/* Wczytanie wiadomoœci */
		byte[] wiadomosc = null;
		try {
			wiadomosc = Files
					.readAllBytes(Paths.get(sciezkaWiadomosc.getText()));
		} catch (NoSuchFileException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(panel,
					eBRAKPLIKU + e.getLocalizedMessage(), tBLAD,
					JOptionPane.ERROR_MESSAGE);
			sciezkaWiadomosc.setText(eBRAKPLIKU + sciezkaWiadomosc.getText());
			return;
		} catch (IOException | OutOfMemoryError e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(panel, e.getLocalizedMessage(),
					tBLAD, JOptionPane.ERROR_MESSAGE);
			sciezkaWiadomosc.setText(e.getLocalizedMessage());
			return;
		}
		final int width = imgWej.getWidth();
		final int height = imgWej.getHeight();
		final int length = wiadomosc.length;
		if (width * height >= Integer.SIZE + length * 8) {
			/* Zapis d³ugoœci wiadomoœci */
			byte[] wiadomoscBit = new byte[Integer.SIZE + length * 8];
			int b = 0, maska = 0x80000000;
			for (int i = Integer.SIZE - 1; i >= 0; i--) {
				wiadomoscBit[b++] = (byte) ((length & maska) >> i);
				maska >>>= 1;
			}
			/* Bajty do bitów */
			for (int i = 0; i < length; i++) {
				maska = 0x80;
				for (int j = 7; j >= 0; j--) {
					wiadomoscBit[b++] = (byte) ((wiadomosc[i] & maska) >> j);
					maska >>>= 1;
				}
			}
			/* Wybór maski koloru */
			final int shift;
			switch (kolor) {
			case CZERWONY:
				maska = 0x00010000;
				shift = 16;
				break;
			case ZIELONY:
				maska = 0x00000100;
				shift = 8;
				break;
			case NIEBIESKI:
				maska = 0x00000001;
				shift = 0;
				break;
			default:
				throw new IllegalArgumentException("Niepoprawny kolor: "
						+ kolor);
			}
			/* Wtapianie */
			b = 0;
			int rgb;
			BufferedImage imgWyj = new BufferedImage(width, height,
					BufferedImage.TYPE_INT_RGB);
			for (int x = 0; x < width; x++)
				for (int y = 0; y < height; y++) {
					rgb = imgWej.getRGB(x, y);
					if (b < wiadomoscBit.length)
						if (((rgb & maska) >> shift) != wiadomoscBit[b++])
							rgb ^= maska;
					imgWyj.setRGB(x, y, rgb);
				}
			imgWej.flush();
			try {
				zapiszWyjscie(imgWyj);
			} catch (IOException e) {
				return;
			}
			JOptionPane.showMessageDialog(panel, SUKCES + "\nZapisano "
					+ length + " bajtów wiadomoœci.", "Sukces",
					JOptionPane.INFORMATION_MESSAGE);
		} else
			JOptionPane.showMessageDialog(panel, eMALY + "\nWymiary obrazka: "
					+ width + " x " + height + " = " + (width * height)
					+ " pikseli - " + Integer.SIZE + " na d³ugoœæ = "
					+ (width * height / 8 - Integer.SIZE / 8)
					+ " bajtów mo¿liwych do zapisania.\nD³ugoœæ wiadomoœci: "
					+ length + " bajtów.", tBLAD, JOptionPane.ERROR_MESSAGE);
	}
	
	private static void wczytajWejscie() throws IOException {
		try {
			imgWej = ImageIO.read(new File(sciezkaWejscie.getText()));
		} catch (IOException e) {
			e.printStackTrace();
			sciezkaWejscie.setText(e.getLocalizedMessage());
			opisObrazekWejscie.setText(e.getLocalizedMessage());
			obrazekWejscie
					.setIcon(new ImageIcon(
							MainWindow.class
									.getResource("/javax/swing/plaf/basic/icons/image-failed.png")));
			throw e;
		}
		obrazekWejscie.setIcon(new ImageIcon(Scalr.resize(imgWej,
				obrazekWejscie.getWidth(), obrazekWejscie.getHeight(),
				Scalr.OP_ANTIALIAS)));
		opisObrazekWejscie.setText(Paths.get(sciezkaWejscie.getText())
				.getFileName().toString()
				+ " ("
				+ imgWej.getWidth()
				+ " x "
				+ imgWej.getHeight()
				+ " pikseli)");
	}

	private static void zapiszWyjscie(BufferedImage imgWyj) throws IOException {
		File plikWyj = new File(sciezkaWyjscie.getText());
		try {
			plikWyj.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(panel, e.getLocalizedMessage(),
					tBLAD, JOptionPane.ERROR_MESSAGE);
			throw e;
		}
		try {
			if (!ImageIO.write(imgWyj, "bmp", plikWyj)) {
				final String eWriter = "Nie znaleziono ImageWritera";
				JOptionPane.showMessageDialog(panel, eWriter, tBLAD,
						JOptionPane.ERROR_MESSAGE);
				obrazekWyjscie
						.setIcon(new ImageIcon(
								MainWindow.class
										.getResource("/javax/swing/plaf/basic/icons/image-failed.png")));
				throw new IOException(eWriter);
			}
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(panel, e.getLocalizedMessage(),
					tBLAD, JOptionPane.ERROR_MESSAGE);
			obrazekWyjscie
					.setIcon(new ImageIcon(
							MainWindow.class
									.getResource("/javax/swing/plaf/basic/icons/image-failed.png")));
			throw e;
		}
		obrazekWyjscie.setIcon(new ImageIcon(Scalr.resize(imgWyj,
				obrazekWyjscie.getWidth(), obrazekWyjscie.getHeight(),
				Scalr.OP_ANTIALIAS)));
		imgWyj.flush();
		opisObrazekWyjscie.setText(Paths.get(sciezkaWyjscie.getText())
				.getFileName().toString()
				+ " ("
				+ imgWyj.getWidth()
				+ " x "
				+ imgWyj.getHeight()
				+ " pikseli)");
	}

	/** Tworzy GUI */
	private void initGUI() {
		frame = new JFrame();
		frame.setResizable(false);
		frame.setTitle("Steganografia");
		frame.setBounds(100, 100, 800, 550);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		panel = new JPanel();
		frame.getContentPane().add(panel);
		panel.setLayout(null);

		obrazekWejscie = new JLabel();
		obrazekWejscie
				.setIcon(new ImageIcon(
						MainWindow.class
								.getResource("/javax/swing/plaf/basic/icons/image-delayed.png")));
		obrazekWejscie.setHorizontalAlignment(SwingConstants.CENTER);
		obrazekWejscie.setBounds(24, 178, 360, 313);
		panel.add(obrazekWejscie);

		obrazekWyjscie = new JLabel();
		obrazekWyjscie
				.setIcon(new ImageIcon(
						MainWindow.class
								.getResource("/javax/swing/plaf/basic/icons/image-delayed.png")));
		obrazekWyjscie.setHorizontalAlignment(SwingConstants.CENTER);
		obrazekWyjscie.setBounds(408, 178, 360, 313);
		panel.add(obrazekWyjscie);

		sciezkaWejscie = new JTextField();
		sciezkaWejscie.setText(tWEJSCIE);
		sciezkaWejscie.setToolTipText(tWEJSCIE);
		sciezkaWejscie.setBounds(105, 8, 565, 26);
		panel.add(sciezkaWejscie);

		sciezkaWyjscie = new JTextField();
		sciezkaWyjscie.setText(tWYJSCIE);
		sciezkaWyjscie.setToolTipText(tWYJSCIE);
		sciezkaWyjscie.setBounds(105, 35, 565, 26);
		panel.add(sciezkaWyjscie);

		sciezkaWiadomosc = new JTextField();
		sciezkaWiadomosc.setText(tWIADOMOSC);
		sciezkaWiadomosc.setToolTipText(tWIADOMOSC);
		sciezkaWiadomosc.setBounds(105, 64, 565, 26);
		panel.add(sciezkaWiadomosc);

		final JButton SzukajWejscie = new JButton(bSZUKAJ);
		SzukajWejscie.setBounds(680, 8, 94, 26);
		panel.add(SzukajWejscie);

		final JButton SzukajWyjscie = new JButton(bSZUKAJ);
		SzukajWyjscie.setBounds(680, 35, 94, 26);
		panel.add(SzukajWyjscie);

		final JButton SzukajWiadomosc = new JButton(bSZUKAJ);
		SzukajWiadomosc.setBounds(680, 64, 94, 26);
		panel.add(SzukajWiadomosc);

		final JLabel lblWejscie = new JLabel("Wejœcie:");
		lblWejscie.setBounds(24, 14, 56, 14);
		panel.add(lblWejscie);

		final JLabel lblWyjscie = new JLabel("Wyjœcie:");
		lblWyjscie.setBounds(24, 41, 56, 14);
		panel.add(lblWyjscie);

		final JLabel lblWiadomosc = new JLabel("Wiadomoœæ:");
		lblWiadomosc.setBounds(24, 70, 87, 14);
		panel.add(lblWiadomosc);

		final JToggleButton TrybS = new JToggleButton("Wtapiaj");
		final JToggleButton TrybD = new JToggleButton("Ekstrahuj");
		final ActionListener przelacznik = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tryb = !tryb;
				if (e.getSource() == TrybS)
					TrybD.setSelected(!TrybD.isSelected());
				else
					TrybS.setSelected(!TrybS.isSelected());
			}
		};
		final String prz = "Prze³¹cznik trybu pracy";
		TrybS.setSelected(tryb);
		TrybS.setToolTipText(prz);
		TrybS.setBounds(485, 96, 94, 26);
		TrybS.addActionListener(przelacznik);
		panel.add(TrybS);
		TrybD.setToolTipText(prz);
		TrybD.setBounds(576, 96, 94, 26);
		TrybD.addActionListener(przelacznik);
		panel.add(TrybD);

		opisObrazekWejscie = new JLabel("Wejœcie");
		opisObrazekWejscie.setHorizontalAlignment(SwingConstants.CENTER);
		opisObrazekWejscie.setBounds(24, 150, 360, 16);
		panel.add(opisObrazekWejscie);

		opisObrazekWyjscie = new JLabel("Wyjœcie");
		opisObrazekWyjscie.setHorizontalAlignment(SwingConstants.CENTER);
		opisObrazekWyjscie.setBounds(408, 150, 360, 16);
		panel.add(opisObrazekWyjscie);

		final JButton Odswiez = new JButton("Odœwie¿");
		Odswiez.setToolTipText("Odœwie¿ podgl¹d obrazka wejœciowego");
		Odswiez.setBounds(157, 121, 94, 26);
		panel.add(Odswiez);

		final String tip = "Wybór czêœci piksela";
		final JRadioButton Czerwony = new JRadioButton("Czerwony");
		Czerwony.setToolTipText(tip);
		Czerwony.setSelected(true);
		Czerwony.setBounds(256, 100, 75, 18);
		panel.add(Czerwony);
		final JRadioButton Zielony = new JRadioButton("Zielony");
		Zielony.setToolTipText(tip);
		Zielony.setBounds(334, 100, 74, 18);
		panel.add(Zielony);
		final JRadioButton Niebieski = new JRadioButton("Niebieski");
		Niebieski.setToolTipText(tip);
		Niebieski.setBounds(399, 100, 74, 18);
		panel.add(Niebieski);
		final ButtonGroup Radio = new ButtonGroup();
		Radio.add(Czerwony);
		Radio.add(Zielony);
		Radio.add(Niebieski);

		final JButton Uruchom = new JButton("Uruchom");
		Uruchom.setBounds(680, 96, 94, 26);
		panel.add(Uruchom);
	}
}
