package PDF;

import java.io.File;
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
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import WB.DBConnection;

public class PDF_Construction {
	private static Font font;
	private static Font ffont;
	private static Font ffont2;
	
	public static void create(){
		/*
		 * Lista pokazuj¹ca wszystkie karty pracy konstrukcji - taski do zrobienia
		 */
		System.out.println("start listy konstrukcyjnej");
		Connection myConn = DBConnection.dbConnector();
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		Document Lista = new Document(PageSize.A4);
		try
		{
			FontFactory.register(PDF_Construction.class.getClassLoader().getResource("times.ttf").toString(), "times");
			font = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 12);
			ffont = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 10);
			ffont2 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 6); 
			String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
			String name = "Construction.pdf";
			File f = new File(path+name);
			if(f.exists() && !f.isDirectory())
				name = godz.format(date.getTime())+" "+name;
			PdfWriter writer = PdfWriter.getInstance(Lista, new FileOutputStream(path+name));
			Lista.open();
			writer.setPageEvent(new PDF_MyFooter());
			PdfPTable table = new PdfPTable(8);
			float widths[] = new float[] { 16, 28, 45, 20, 16, 16, 30, 15};
			table.setWidths(widths);
			table.setWidthPercentage(100);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			//metoda znajdujaca wszystkie zadania konstrukcyjne
			analyze();
			
			//dla kazdego projektu
			Statement pobierzProjectSchedule = myConn.createStatement();
			String sql = "select * from construction join calendar on construction.projekt=calendar.nrmaszyny where Status500 = 'O' order by calendar.dataKoniecMontazu, nrMaszyny";
			ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql);
			String poprzedni = "";
			boolean naglowek = false;
			while(ProjectSchedule.next()){
				
				String calyNumer = ProjectSchedule.getString("nrMaszyny");
				String [] p = calyNumer.split("/");
				String ProjectGroup = p[0];
				String ProjectNumber = p[1];
				if(!poprzedni.equals(calyNumer)){
					naglowek = false;
					poprzedni = calyNumer;
				}
				String MontazFinishDate = ProjectSchedule.getString("dataKoniecMontazu");
				String ProductionDate = ProjectSchedule.getString("dataProdukcji");
				String ProjectName = ProjectSchedule.getString("Opis");
				String klient = ProjectSchedule.getString("klient");
				
				if(!naglowek){
					if(table.size()!=0){
						Lista.add(table);
						Lista.add(new Paragraph("\n \n \n", font));
						table = new PdfPTable(8);
						table.setWidths(widths);
						table.setWidthPercentage(100);
						table.setHorizontalAlignment(Element.ALIGN_CENTER);
						table.setHorizontalAlignment(Element.ALIGN_CENTER);
					}
					addProjectHeader(new String[] {ProjectGroup+"/"+ProjectNumber, ProjectName, ProductionDate, klient, MontazFinishDate}, table);
					naglowek=true;
				}
				
				String nr500 = ProjectSchedule.getString("Projekt500");
				String numerBonu = ProjectSchedule.getString("KartaPracy");
				String status = ProjectSchedule.getString("StatusWorkbon");
				addCell(table, numerBonu);
				addCell(table, ProjectSchedule.getString("KodArtykulu"));
				addCell(table, ProjectSchedule.getString("Nazwa"));
				if(status.equals("10")){
					addCell(table, "Not started");
				}
				else if(status.equals("20")){
					PdfPCell cell = new PdfPCell(new Phrase("Started", ffont2));
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
					cell.setFixedHeight(20f);
					cell.setBackgroundColor(new BaseColor(255, 255, 0));
					table.addCell(cell);
				}
				else if(status.equals("90")){
					PdfPCell cell = new PdfPCell(new Phrase("Ended", ffont2));
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
					cell.setFixedHeight(20f);
					cell.setBackgroundColor(new BaseColor(18, 162, 25));
					table.addCell(cell);
				}
				else
				{
					PdfPCell cell = new PdfPCell(new Phrase());
					table.addCell(cell);
				}
				
				//sprawdzenie czasu teoretycznego
				String sql1 = "select hoeveelheid, instelminuten, werkminuten, status from werkbon where werkbonnummer = '"+numerBonu+"'";
				Statement st1 = myConn.createStatement();
				ResultSet rs1 = st1.executeQuery(sql1);
				int MinTeoret = 0;
				while(rs1.next()){
					MinTeoret = rs1.getInt(2) + rs1.getInt(3);
				}
				rs1.close();
				st1.close();
				int HTeoret = MinTeoret/60;
				MinTeoret = MinTeoret%60;
				String CzasTeoret = "";
				if(HTeoret<10)
					CzasTeoret = "0";
				CzasTeoret += Integer.toString(HTeoret)+":";
				if(MinTeoret<10)
					CzasTeoret+="0";
				CzasTeoret+= Integer.toString(MinTeoret);
				addCell(table, CzasTeoret);
				
				//sprawdzenie ile godzin jest zarejestrowane na karcie pracy
				String konstruktorzy = "";
				String sql2 = "select sum(tijd), status from Rejestracja where werkbon = '"+numerBonu+"'";
				Statement st2 = myConn.createStatement();
				ResultSet rs2 = st2.executeQuery(sql2);
				int minutRejestr = 0;
				while(rs2.next()){
					minutRejestr = rs2.getInt(1);
				}
				rs2.close();
				st2.close();
				int hRejestr = minutRejestr / 60;
				minutRejestr = minutRejestr % 60;
				String CzasRejestr = "";
				if(hRejestr<10)
					CzasRejestr = "0";
				CzasRejestr += Integer.toString(hRejestr)+":";
				if(minutRejestr<10)
					CzasRejestr+="0";
				CzasRejestr+= Integer.toString(minutRejestr);
				addCell(table, CzasRejestr);
				//sprawdzenie kto sie zarejestrowa³
				String sql3 = "select cfnaam from Rejestracja where werkbon = '"+numerBonu+"' group by CFNAAM";
				Statement st3 = myConn.createStatement();
				ResultSet rs3 = st3.executeQuery(sql3);
				while(rs3.next()){
					konstruktorzy+=rs3.getString(1)+", ";
				}
				rs3.close();
				st3.close();
				if(konstruktorzy.length()>3)
					konstruktorzy = konstruktorzy.substring(0, konstruktorzy.length()-2);
				addCell(table, konstruktorzy);
				
				//deadline date
				String sql4 = "select leveringsdatumvoorzien from bestellingdetail where leverancier = '500' and ordernummer = '"+nr500+"'";
				
				Statement st4= myConn.createStatement();
				ResultSet rs4 = st4.executeQuery(sql4);
				String deadline = "";
				while(rs4.next())
					deadline = rs4.getString(1);
				addCell(table, deadline);
			}
			pobierzProjectSchedule.close();
			System.out.println("konczymy liste konstrukcyjna");
			Lista.add(table);
			Lista.close();
			myConn.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
		
	private static void addProjectHeader(String [] dane, PdfPTable t1){
		dane[0] = "Project number: "+dane[0];
		dane[2] = "Planned production date:  " +dane[2];
		dane[3] = "   Client:   "+dane[3];
		dane[4] = "Required assembly finish:  " +dane[4];
		PdfPTable t = new PdfPTable (8);
		
		for(int i = 0; i<dane.length; i++) {
			PdfPCell cell1;
			if(i==0) cell1 = new PdfPCell(new Phrase(dane[i], font));
			//inna czcionka
			else cell1 = new PdfPCell(new Phrase(dane[i], ffont));
			cell1.setFixedHeight(20f);
			if(i==0)
				cell1.setColspan(2);
			else if (i==1||i==2||i==4)
				cell1.setColspan(3);
			else if(i==3)
				cell1.setColspan(5);
			//cell1.setBackgroundColor(BaseColor.ORANGE);
			cell1.setBorder(Rectangle.NO_BORDER);
			cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell1);
		}
		PdfPCell a = new PdfPCell(t);
		a.setBorder(Rectangle.BOTTOM);
		a.setBorderWidthBottom(1.1f);
		a.setColspan(8);
		t1.addCell(a);
		
		addHeader(t1);
	}
	
	private static void addCell(PdfPTable t, String z){
		PdfPCell cell = new PdfPCell(new Phrase(z, ffont2));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setFixedHeight(20f);
		t.addCell(cell);
	}
	
	private static void addHeader(PdfPTable t){
		int n = 0;
		String [] header = new String [] {"Work card number", "Task code", "Description", "status", "Theor. time", "Registered time", "Constructors registr.", "Deadline"};
		for(int i = 0; i<header.length; i++) {
			PdfPCell cell1 = new PdfPCell(new Phrase(header[i], ffont2));
			cell1.setFixedHeight(30);
			if(i==0)
				cell1.setMinimumHeight(30);
			cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell1);
			n++;
		}
		
		if(n<8){
			for(int j = n; j<=8; j++){
				PdfPCell cell = new PdfPCell();
				t.addCell(cell);
			}
		}
	}
	
	
	public static void analyze() throws SQLException{
		Connection myConn = DBConnection.dbConnector();
		
		Statement createTable;
		createTable = myConn.createStatement();
		createTable.execute("Delete from Construction");
		createTable.close();
		
		//dla wszystkich otwartych zamówieñ na prace konstrukcyjne - sprawdz status
		Statement st = myConn.createStatement();
		String sql = "SELECT * FROM werkbon join bestelling on werkbon.project = bestelling.leverancierordernummer where werkpost = 'KM01' and bestelling.statuscode = 'O' and artikelcode like 'KM%'";
		ResultSet rs = st.executeQuery(sql);
		while(rs.next()){
			String nr500 = rs.getString("afdelingseq");
			String kodArt = rs.getString("artikelcode");
			String nazwa = rs.getString("omschrijving");
			String kartaPracy = rs.getString("werkbonnummer");
			String status500 = rs.getString("statuscode");
			String statusWorkbon = rs.getString("status");
			String serialNumber = kodArt.substring(3, 9);
			
			String sql2 = "select nrMaszyny from Calendar where nrMaszyny = '2/"+serialNumber+"' or nrMaszyny = '6/"+serialNumber+"' order by nrMaszyny";
			String sql3 = "select nrMaszyny from Calendar where nrMaszyny like ('2/"+serialNumber+"%' or nrMaszyny like '6/"+serialNumber+"%') and zakonczone = 0 order by nrMaszyny";
			String sql4 = "select nrMaszyny from Calendar where nrMaszyny = '0/"+serialNumber+"'";
			Statement st2 = myConn.createStatement();
			ResultSet rs2 = st2.executeQuery(sql2);
			boolean found = false;
			String nrMaszyny = "";
			//sprawdzenie wsrod maszyn uruchomionych produkcyjnie
			while(rs2.next()){
				if(!found){
					nrMaszyny = rs2.getString("nrMaszyny");
					found=true;
				}
			}
			rs2.close();
			st2.close();
		
			//sprawdzenie wsrod podprojektow (bez uruchomienia glownych projektow)
			if(!found){
				Statement st3 = myConn.createStatement();
				ResultSet rs3 = st3.executeQuery(sql3);
				while(rs3.next()){
					if(!found){
						nrMaszyny = rs3.getString("nrMaszyny");
						found=true;
					}
				}
				rs3.close();
				st3.close();
			}
			
			//sprawdz tylko wsrod zlecen produkcyjnych w sprzedazy:
			if(!found){
				Statement st4 = myConn.createStatement();
				ResultSet rs4 = st4.executeQuery(sql4);
				while(rs4.next()){
					if(!found){
						nrMaszyny = rs4.getString("nrMaszyny");
						found=true;
					}
				}
				rs4.close();
				st4.close();
			}
			
			//wpisz do bazy pracê konstrukcji
			Statement czyJest = myConn.createStatement();
			String sprawdzanie = "SELECT * FROM Construction WHERE KodArtykulu= '"+kodArt+"'";
			ResultSet wynikSprawdzenia = czyJest.executeQuery(sprawdzanie);
			boolean jest = false;
			while (wynikSprawdzenia.next()){
				jest=true;
			}
			czyJest.close();
			wynikSprawdzenia.close();
			
			if(jest){
				String UpdateQuery = "Update Construction set Projekt = ?, StatusWorkbon = ?, Status500 = ?  where KodArtykulu = '"+kodArt+"'";
				PreparedStatement UpdateProject = myConn.prepareStatement(UpdateQuery);
				UpdateProject.setString(1, nrMaszyny);
				UpdateProject.setString(2, statusWorkbon);
				UpdateProject.setString(3, status500);
				
				UpdateProject.executeUpdate();
				UpdateProject.close();
			}
			else{
				String InsertQuery = "INSERT INTO Construction VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
				PreparedStatement insertProject = myConn.prepareStatement(InsertQuery);
				insertProject.setString(1, kodArt);
				insertProject.setString(2, nazwa);
				insertProject.setString(3, nr500);
				insertProject.setString(4, kartaPracy);
				insertProject.setString(5, serialNumber);
				insertProject.setString(6, nrMaszyny);
				insertProject.setString(7, statusWorkbon);
				insertProject.setString(8, status500);
				
				insertProject.executeUpdate();
				insertProject.close();
			}
			
		}
		rs.close();
		st.close();
		
		myConn.close();
		
	}
}
