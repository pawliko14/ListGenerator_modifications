package WB;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.itextpdf.text.DocumentException;

import PDF.PDF_AnalizaCNC;
import PDF.PDF_AnalizaStruktur;
import PDF.PDF_Braki_w_uruchomieniu;
import PDF.PDF_Construction;
import PDF.PDF_Lista_produkcyjna;
import PDF.PDF_Godziny;
import PDF.PDF_Marketing;
import PDF.PDF_Lista_materialow_zlecen;
import PDF.PDF_Harmonogram_montazu;
import PDF.PDF_Programisci;
import PDF.PDF_Harmonogram_projektow;
import PDF.PDF_SACA;
import PDF.PDF_Serwis;
import PDF.PDF_Lista_niewydanych;
import PDF.PDF_Lista_niewydanych_102;
import PDF.PDF_Lista_niewydanych_103;
import PDF.PDF_Zakupy;

public class MainProgramWindowStart extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static JCheckBox chk02 = new JCheckBox("");
	public static JCheckBox chk01 = new JCheckBox("");
	public static JCheckBox chk03 = new JCheckBox("");
	public static JCheckBox chk04 = new JCheckBox("");
	public static JCheckBox chk05 = new JCheckBox("");
	public static JCheckBox chk00 = new JCheckBox("");
	public static JCheckBox chk06 = new JCheckBox("");
	public static JCheckBox chk07 = new JCheckBox("");
	public static JCheckBox chk08 = new JCheckBox("");
	public static JCheckBox chk09 = new JCheckBox("");
	public static JCheckBox chk10 = new JCheckBox("");
	public static JCheckBox chk11 = new JCheckBox("");
	public static JCheckBox chk12 = new JCheckBox("");
	public static JCheckBox chk13 = new JCheckBox("");
	private JPanel contentPane;

	/**
	 * Launch the application.
	 * @throws ParseException 
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws DocumentException 
	 */
	public static void main(String[] args) {

		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					MainProgramWindowStart frame = new MainProgramWindowStart();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		SimpleDateFormat godz = new SimpleDateFormat("HH:mm");
		SimpleDateFormat doNazwy2 = new SimpleDateFormat("yyyy.MM.dd");
		Calendar date = Calendar.getInstance();
		String nazwa;
		
		//LOG		
		if(godz.format(date.getTime()).startsWith("06"))
			nazwa = "listy_log_output.txt";
		else
			nazwa = "extra_log_output.txt";
		PrintStream out;
		try {
			out = new PrintStream(new FileOutputStream("\\\\192.168.90.203\\common\\Listy_testowe_"+nazwa));
			System.setOut(out);
			System.setErr(out);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println(godz.format(date.getTime()));
		
		//utworzenie folderu na listy
		File theDir = new File(PDF.Parameters.getPathToSave()+"/"+doNazwy2.format(date.getTime()));
		// if the directory does not exist, create it
		if (!theDir.exists()) {
		    try{
		        theDir.mkdir();
		    } 
		    catch(SecurityException se){
		        //handle it
		    	System.out.println("Blad w tworzeniu folderu z listami");
		    }  
		}
		
		//Harmonogramy - maszyn i wszystkich projektów
		try {
			new PDF_Harmonogram_projektow();
		} catch (SQLException e) {
			System.out.println("Blad SQL w tworzeniu harmonogramu");
			e.printStackTrace();
		}
			//PDF_AnalizaStruktur.run();
		chk00.setSelected(true);
		
		//PDF harmonogramy maszyn
		try {
			PDF_Harmonogram_projektow.createPDFMachines();
			PDF_Harmonogram_projektow.createPDFAll();
		} catch (SQLException e) {
			System.out.println("Blad SQL w tworzeniu pdfów harmonogramu");
			e.printStackTrace();
		}
		chk01.setSelected(true);
		
		//Lista dla programistów
		try {
			PDF_AnalizaCNC.run();
		} catch (SQLException e) {
		e.printStackTrace();
		}
		chk02.setSelected(true);
		
		//Lista produkcyjna 'Czesiowa' oraz zlozenia spawane
		PDF_Lista_produkcyjna.create();
		chk03.setSelected(true);
		
		//Lista dla zakupów
		PDF_Zakupy.create();
		chk04.setSelected(true);
		
		//Lista materialow (krajalnia)
		PDF_Lista_materialow_zlecen.createPDFs();
		chk05.setSelected(true);
		
		//Listy niewydanych artykulow wedlug taktow - tylko magazyny 102, 103 i wszystko razem
		PDF_Lista_niewydanych_102.create();
		PDF_Lista_niewydanych_103.create();
		PDF_Lista_niewydanych.create();
		chk06.setSelected(true);
		
		//Lista montazowa - co jest wykonane
		try {
			PDF_Harmonogram_montazu.create(true);
		} catch (SQLException e) {
			System.out.println("Blad SQL w tworzeniu listy montazowej");
			e.printStackTrace();
		}
		chk07.setSelected(true);
	
		//Lista otwartych artykulow zamowionych w SACA do maszyn
		try {
			PDF_SACA.createDB();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		PDF_SACA.createDoc();
		chk08.setSelected(true);
		
		//Harmonogram dla marketingu (z podzialem na same magazynowki)
		PDF_Marketing.create();
		PDF_Marketing.createOnlyMagazine();
		try {
			PDF_Marketing.checkInvoices();
		} catch (SQLException e) {
			System.out.println("Blad SQL w tworzeniu listy zaliczek");
			e.printStackTrace();
		}
		chk09.setSelected(true);
		
		//Lista zadan dla konstrukcji
		PDF_Construction.create();
		chk10.setSelected(true);
				
		//Lista dla programistów
		try {
			PDF_Programisci.create();
		} catch (SQLException e) {
			System.out.println("Blad SQL w tworzeniu listy montazowej");
			e.printStackTrace();
		}
		chk11.setSelected(true);
		
		//Lista dla serwisu
		try {
			PDF_Serwis.create();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		chk12.setSelected(true);
		
		//co poniedzialek raport godzin (ile pracownicy siê rejestruj¹)
//		if(date.get(Calendar.DAY_OF_WEEK)==Calendar.MONDAY)
//			try {
//				PDF_Godziny.createWeekRaport();
//			} catch (SQLException e) {
//				e.printStackTrace();
//			}
//	
//		try {
//			PDF_Braki_w_uruchomieniu.create();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
		chk12.setSelected(true);
		
	//	Maintenance();
		System.exit(0);
		
	} 
	
//koniec maina
	
	

	/**
	 * Create the frame.
	 */
	public MainProgramWindowStart() {
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 280, 598);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblCvsToSqlite = new JLabel("Program progress:");
		lblCvsToSqlite.setHorizontalAlignment(SwingConstants.CENTER);
		lblCvsToSqlite.setFont(new Font("Century", Font.BOLD, 22));
		lblCvsToSqlite.setBounds(0, 0, 278, 50);
		contentPane.add(lblCvsToSqlite);
		chk02.setEnabled(false);
		
		chk02.setBounds(169, 170, 22, 23);
		contentPane.add(chk02);
		chk01.setEnabled(false);
		
		chk01.setBounds(169, 144, 22, 23);
		contentPane.add(chk01);
		chk03.setEnabled(false);
		
		chk03.setBounds(169, 196, 22, 23);
		contentPane.add(chk03);
		chk04.setEnabled(false);
		
		chk04.setBounds(169, 222, 22, 23);
		contentPane.add(chk04);
		chk05.setEnabled(false);

		chk05.setBounds(169, 248, 22, 23);
		contentPane.add(chk05);
		
		Image img = new ImageIcon(this.getClass().getResource("/BackgroundImage.jpg")).getImage();
		
		JLabel lblLists = new JLabel("Lists:");
		lblLists.setHorizontalAlignment(SwingConstants.CENTER);
		lblLists.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblLists.setBounds(0, 61, 278, 36);
		contentPane.add(lblLists);
		
		JLabel lblProjectSchedule = new JLabel("- Harmonogram projekt\u00F3w");
		lblProjectSchedule.setBounds(36, 144, 127, 23);
		contentPane.add(lblProjectSchedule);
		
		JLabel lblCzesiowaLista = new JLabel("- Lista CNC");
		lblCzesiowaLista.setBounds(36, 170, 127, 23);
		contentPane.add(lblCzesiowaLista);
		
		JLabel lblTaktowaLista = new JLabel("- Zakupowa");
		lblTaktowaLista.setBounds(36, 222, 115, 23);
		contentPane.add(lblTaktowaLista);
		
		JLabel lblMaterialLists = new JLabel("- Lista monta\u017Cowa");
		lblMaterialLists.setBounds(36, 300, 127, 23);
		contentPane.add(lblMaterialLists);
		
		
		JLabel lblMaterialsLists = new JLabel("- dla SACA");
		lblMaterialsLists.setBounds(36, 326, 127, 23);
		contentPane.add(lblMaterialsLists);
		
		JLabel lblAnalizaStruktur = new JLabel("- Analiza struktur");
		lblAnalizaStruktur.setBounds(35, 118, 128, 23);
		contentPane.add(lblAnalizaStruktur);
		chk00.setEnabled(false);
		
		chk00.setBounds(169, 118, 22, 23);
		contentPane.add(chk00);
		
		chk07.setEnabled(false);
		chk07.setBounds(169, 300, 22, 23);
		contentPane.add(chk07);
		
		chk06.setEnabled(false);
		chk06.setBounds(169, 274, 22, 23);
		contentPane.add(chk06);
		
		chk08.setEnabled(false);
		chk08.setBounds(169, 326, 22, 23);
		contentPane.add(chk08);
		
		chk09.setEnabled(false);
		chk09.setBounds(169, 352, 22, 23);
		contentPane.add(chk09);
		
		chk10.setEnabled(false);
		chk10.setBounds(169, 378, 22, 23);
		contentPane.add(chk10);
		
		chk11.setEnabled(false);
		chk11.setBounds(169, 404, 22, 23);
		contentPane.add(chk11);
		
		JLabel lblTaktList = new JLabel("- Listy materia\u0142owe");
		lblTaktList.setBounds(36, 248, 115, 23);
		contentPane.add(lblTaktList);
		
		JLabel lblTaktList_1 = new JLabel("- Listy taktowe");
		lblTaktList_1.setBounds(36, 274, 127, 23);
		contentPane.add(lblTaktList_1);
		
		JLabel lblWeldingLists = new JLabel("- Produkcyjna i spawane");
		lblWeldingLists.setBounds(36, 196, 127, 23);
		contentPane.add(lblWeldingLists);
		
		JCheckBox chk12 = new JCheckBox("");
		chk12.setEnabled(false);
		chk12.setBounds(169, 430, 22, 23);
		contentPane.add(chk12);
		
		JLabel lblMarketingLists = new JLabel("- Lista konstrukcji");
		lblMarketingLists.setBounds(36, 378, 127, 23);
		contentPane.add(lblMarketingLists);
		
		JLabel lblCalendars = new JLabel("- dla programist\u00F3w CNC");
		lblCalendars.setBounds(36, 404, 127, 23);
		contentPane.add(lblCalendars);
		
		JLabel lblSaca = new JLabel("- dla Marketingu");
		lblSaca.setBounds(36, 352, 127, 23);
		contentPane.add(lblSaca);
		
		JLabel lblConstruction = new JLabel("- Serwisowa");
		lblConstruction.setBounds(36, 430, 127, 23);
		contentPane.add(lblConstruction);
		
		JCheckBox chk11 = new JCheckBox("");
		chk11.setEnabled(false);
		chk11.setBounds(169, 404, 22, 23);
		contentPane.add(chk11);
		
		JCheckBox chk13 = new JCheckBox("");
		chk13.setEnabled(false);
		chk13.setBounds(169, 456, 22, 23);
		contentPane.add(chk13);
		
		JLabel lblListaZlece = new JLabel("- Braki");
		lblListaZlece.setBounds(36, 456, 127, 23);
		contentPane.add(lblListaZlece);

		
		JLabel lblNewLabel_11 = new JLabel("");
		lblNewLabel_11.setIcon(new ImageIcon(img));
		lblNewLabel_11.setBounds(-70, -61, 566, 631);
		contentPane.add(lblNewLabel_11);
	}
	
	public static void Maintenance(){
		
		//usuwanie starych folderow z listami (z przed 4 miesiecy (120 dni) )
		Calendar fourMonthsDate = Calendar.getInstance();
		fourMonthsDate.add(Calendar.DAY_OF_YEAR, -120);
		//System.out.println(doNazwy.format(date.getTime()));
		File directory = new File(PDF.Parameters.getPathToSave());
		File[] files = directory.listFiles();
        if(files!=null){
            for(int i=0; i<files.length; i++) {
            	Date lastmodified = new Date(files[i].lastModified());
            	// DO ZMIANYs
            	//System.out.println(doNazwy.format(lastmodified)+"  "+files[i].getName()+"  "+doNazwy.format(fourMonthsDate.getTime()));
            	//System.out.println(lastmodified.compareTo(fourMonthsDate.getTime()));
            	if(lastmodified.compareTo(fourMonthsDate.getTime())<0){
            		String nazwa = files[i].getName();
            		if(nazwa.length()==10)
            			deleteDirectory(files[i]);
            	}
            }
        }
		//deleteDirectory(directory);	
		
	}
	
	public static boolean deleteDirectory(File directory) {
	    if(directory.exists()){
	        File[] files = directory.listFiles();
	        if(files!=null){
	            for(int i=0; i<files.length; i++) {
	                if(files[i].isDirectory()) {
	                    deleteDirectory(files[i]);
	                }
	                else {
	                    files[i].delete();
	                }
	            }
	        }
	    }
	    return(directory.delete());
	}
}
