package PDF;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import WB.DBConnection;

public class PDF_AnalizaCNC {
	
	private static Font ffont = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 10); 
	
	/*public static void main(String[] args) throws SQLException {
		//run();
		PDF_CzesiusList.create();
	}*/
	
	
	//metoda wykonuj¹ca analizê CNC
	public static void run() throws SQLException {
		System.out.println("Start analizy CNC");
		Connection connection = DBConnection.dbConnector();
		//pobierz wszystkie zlecenia 500 potrzebne do produkcji
		String sql01 = "Select ItemNo, matsource from Partsoverview where MatSource like '500/%' group by ItemNo order by ItemNo, matsource";
		String sql04 = "Insert into cnc values (?, ?, ?, ?, ?, ?)";
		String sql05 = "Delete from cnc";
		Statement st05 = connection.createStatement();
		st05.executeUpdate(sql05);
		st05.close();
		PreparedStatement st04 = connection.prepareStatement(sql04);
		Statement st = connection.createStatement();
		ResultSet rs = st.executeQuery(sql01);
		while(rs.next())
		{
			String kodArt = rs.getString("ItemNo");
			String nrSerii = rs.getString("MatSource");
			String nrBonu ="";
			String stanowisko = "";
			String inne = "";
			String seq = "";
			//we¿ wszystkie operacje na stanowiskach numerycznych
			String sql02 = "Select werkpost, seq, werkbonnummer from werkbon "
					+ " where artikelcode = '"+kodArt+"' and project = '"+nrSerii+"' and status = 10 and "
							+ "( werkpost like 'TN%' or "
							+ " werkpost like 'FN%' or "
							+ " werkpost like 'BN%' or "
							+ " werkpost like 'CO%' or "
							+ " werkpost like 'WN%' or "
							+ " werkpost like 'CW81' )";
			Statement st02 = connection.createStatement();
			ResultSet rs02 = st02.executeQuery(sql02);
			while(rs02.next()) {
				nrBonu = rs02.getString("werkbonnummer");
				stanowisko = rs02.getString("werkpost");
				seq = rs02.getString("seq");
				boolean found = false;
				inne="";
				//pobierane s¹ dane o tej operacji dla ka¿dego tego artyku³u
				String sql03 = "Select werkuren.werkpost from werkbon "
						+ " join werkuren on werkbon.werkbonnummer = werkuren.werkbonnummer "
						+ " where werkbon.artikelcode = '"+kodArt+"' and werkbon.seq = "+rs02.getString("seq")+" and project <> '"+rs.getString("MatSource")+"' "
								+ "and werkbon.werkpost = '"+stanowisko+"' group by werkuren.werkpost";
				Statement st03 = connection.createStatement();
				ResultSet rs03 = st03.executeQuery(sql03);
				while(rs03.next()) {
					if(rs03.getString("werkpost").equals(stanowisko)) {
						//jeœli ta operacja by³a wykonywana wczeœniej
						found = true;
					}
					else {
						//jeœli ta operacja by³a wykonywana na innych stanowiskach
						if(inne.length()>1) inne+= ", ";
						inne+= rs03.getString("werkpost");
					}
				}
				rs03.close(); st03.close();
				if(inne.equals("")) inne = null;
				//dodaj do tabeli CNC tylko wtedy, kiedy ta operacja nigdy nie by³a wykonana na tym stanowisku numerycznym
				if(!found) {
					st04.setString(1,  kodArt);
					st04.setString(2,  nrSerii);
					st04.setString(3,  nrBonu);
					st04.setString(4,  stanowisko);
					st04.setString(5,  inne);
					st04.setString(6,  seq);
					st04.executeUpdate();
					
					Statement st06 = connection.createStatement();
					ResultSet rs06 = st06.executeQuery("Select MatSource, werkbon.werkbonnummer from partsoverview "
							+ "join werkbon on werkbon.project = partsoverview.MatSource "
							+ "where ItemNo = '"+kodArt+"' and MatSource <> '"+nrSerii+"' and MatSource <> 'Na magazynie' and MatSource <> 'Nie uruchomione' and werkbon.seq = "+seq+" and werkbon.werkpost = '"+stanowisko+"'");
					while(rs06.next()) {
						st04.setString(2,  rs06.getString("MatSource"));
						st04.setString(3,  rs06.getString("werkbonnummer"));
						st04.executeQuery();
					}
					rs06.close(); st06.close();
				}
			}
			rs02.close(); st02.close();
		}
		rs.close();st.close(); st04.close();
		connection.close();
		System.out.println("Koniec analizy CNC, start pdf");
		pdf();
		System.out.println("Koniec pdf CNC");
	}

	public static void pdf() throws SQLException  {
		// TODO Auto-generated method stub
		
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		Document cnc = new Document(PageSize.A4, 10, 10, 25, 25);
		String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
		String name = "Programy CNC.pdf";
		File f = new File(path+name);
		if(f.exists() && !f.isDirectory())
			name = godz.format(date.getTime())+" "+name;
		PdfWriter writer;
		try {
			writer = PdfWriter.getInstance(cnc, new FileOutputStream(path+name));
			writer.setPageEvent(new PDF_MyFooter());
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (DocumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		cnc.open();
		
		PdfPTable table = new PdfPTable(6);
		float widths[] = new float[] { 8, 10, 14, 5, 8, 8};
		try {
			table.setWidths(widths);
		} catch (DocumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		table.setWidthPercentage(100);
		table.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.setHorizontalAlignment(Element.ALIGN_CENTER);
		createAHeader(table);
		
		
		Connection connection = DBConnection.dbConnector();
		//dla kazdego projektu
		String sql01 = "Select nrMaszyny, opis, klient, dataKoniecMontazu from calendar where Zakonczone = 0 and Wyslano <> 1 order by dataKoniecMontazu";
		Statement st01 = connection.createStatement();
		ResultSet rs01 = st01.executeQuery(sql01);
		while (rs01.next()) {
			boolean header = false;
			//pobierz wszystkie dane o operacjach cnc
			String sql02 = "Select NumerSerii, kodartykulu, seq, stanowisko, inne, partsoverview.itemdesc from cnc "
					+ "inner join partsoverview on cnc.numerserii = partsoverview.matsource "
					+ "where partsoverview.orderno = '"+rs01.getString("nrMaszyny")+"' "
							+ "group by NumerSerii, kodartykulu, seq, stanowisko, inne, partsoverview.itemdesc";
			Statement st02 = connection.createStatement();
			ResultSet rs02 = st02.executeQuery(sql02);
			while(rs02.next()) {
				if(!header) {
					createProjectHeader(table, rs01.getString("nrMaszyny"), rs01.getString("opis"), rs01.getString("klient"), rs01.getString("dataKoniecMontazu"));
					header = true;
				}
				addRow(rs02.getString("NumerSerii"), rs02.getString("kodArtykulu"), rs02.getString("ItemDesc"), rs02.getString("seq"), rs02.getString("stanowisko"), rs02.getString("inne"), table);
			}
			rs02.close();st02.close();
		}
		rs01.close(); st01.close();
		table.setHeaderRows(1);
		
		try {
			cnc.add(table);
			cnc.close();
		}
		catch (Exception e) {
			cnc = new Document(PageSize.A4, 10, 10, 25, 25);
			PdfWriter writer2;
			try {
				writer2 = PdfWriter.getInstance(cnc, new FileOutputStream(path+name));
				writer2.setPageEvent(new PDF_MyFooter());
				cnc.open();
				Paragraph a = new Paragraph ("Error: Blad w imporcie danych (GTT)");
				cnc.add(a);
				cnc.close();
			} catch (FileNotFoundException | DocumentException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		connection.close();
	}

	private static void addRow(String s1, String s2, String s3, String s4, String s5,
			String s6, PdfPTable t) {
		// TODO Auto-generated method stub
		String [] row = new String[] {s1, s2, s3, s4, s5, s6};
		for(int i = 0; i<row.length; i++) {
			PdfPCell cell1 = new PdfPCell(new Phrase(row[i], ffont));
			cell1.setMinimumHeight(30);
			cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell1);
		}
	}

	private static void createProjectHeader(PdfPTable table, String nrMaszyny, String Opis, String Klient,
			String data) {
		// TODO Auto-generated method stub
		String [] project = new String[] {nrMaszyny, Opis, Klient, data};
		
		for(int i = 0; i<project.length; i++) {
			PdfPCell cell1 = new PdfPCell(new Phrase(project[i], ffont));
			cell1.setMinimumHeight(30);
			if(i==1 || i==2) cell1.setColspan(2);
			cell1.setBackgroundColor(BaseColor.ORANGE);
			cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell1);
		}
	}

	private static void createAHeader(PdfPTable table) {
		// TODO Auto-generated method stub
		String [] head = new String[] {"Seria", "Kod", "Nazwa", "Nr op.", "Stanowisko", "Gdzie wykonano"};
		
		for(int i = 0; i<head.length; i++) {
			PdfPCell cell1 = new PdfPCell(new Phrase(head[i], ffont));
			cell1.setMinimumHeight(30);
			cell1.setBackgroundColor(BaseColor.ORANGE);
			cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell1);
		}
		
	}
	

}
