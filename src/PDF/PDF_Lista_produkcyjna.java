package PDF;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
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
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import WB.DBConnection;
import WB.MainProgramWindowStart;

public class PDF_Lista_produkcyjna {

	private static Font ffont;
	private static Font ffont2;
	private static Font ffont2a = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 6, Font.BOLDITALIC); 
	private static Font ffont2abc = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 16, Font.BOLDITALIC); 
	private static Font ffont2b = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 6, Font.ITALIC); 
	
	
	public static void create(){
		
		System.out.println("Start listy produkcyjnej");
		Connection myConn = DBConnection.dbConnector();
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		Document LProd = new Document(PageSize.A3.rotate(), 10, 10, 25, 25);
		try
		{
			String fName = "//192.168.90.203/Logistyka/Tosia/Projekty JAVA";
			FontFactory.register(fName);
			ffont = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 10);
			ffont2 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 6); 
			String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
			String name = "Lista produkcyjna.pdf";
			File f = new File(path+name);
			if(f.exists() && !f.isDirectory())
				name = godz.format(date.getTime())+" "+name;
			PdfWriter writer = PdfWriter.getInstance(LProd, new FileOutputStream(path+name));
			LProd.open();
			writer.setPageEvent(new PDF_MyFooter());
			PdfPTable table = new PdfPTable(31);
			float widths[] = new float[] { 18, 9, 28, 45, 16, 8, 15, 18, 15, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 30};
			table.setWidths(widths);
			table.setWidthPercentage(100);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			addHeader(table);
			addRow(1,table);
			
			//creating a new list for welding base
			PDF_Zlozenia_spawane ListaSpawanaStara = new PDF_Zlozenia_spawane();
			
			Statement pobierzProjectSchedule = myConn.createStatement();
			String sql = "Select nrMaszyny, opis, klient, dataprodukcji, dataKoniecMontazu, komentarz from calendar where zakonczone = 0 order by dataprodukcji, nrmaszyny";
			ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql);
			
			while(ProjectSchedule.next()){
				
				String calyNumer = ProjectSchedule.getString("nrMaszyny");
				String [] p = calyNumer.split("/");
				String ProjectGroup = p[0];
				String ProjectNumber = p[1];
				//if(ProjectNumber.startsWith("160500")) ProjectNumber = "16050010";
				String MontageFinishDate = ProjectSchedule.getString("dataKoniecMontazu");
				String ProductionDate = ProjectSchedule.getString("dataProdukcji");
				String ProjectName = ProjectSchedule.getString("Opis");
				String klient = ProjectSchedule.getString("klient");
				Statement ListContent = myConn.createStatement();
				ResultSet ProjectContent;
				//taking list of articles for project:
				if(ProjectGroup.equals("2") || ProjectGroup.equals("6")){
					ProjectContent = ListContent.executeQuery("SELECT MatSource, ItemNo, ItemDesc, ConsumerOrder, Quantity, Slack, Storenote FROM partsoverview " +
							"where `OrderNo` = '"+calyNumer+"' AND (MatSource like '500/%' or MatSource like 'Nie u%') order by MatSource");
				}
				else
				{
					ProjectContent = ListContent.executeQuery("SELECT MatSource, `ItemNo`, ItemDesc, `ConsumerOrder`, Quantity, Slack, Storenote FROM partsoverview " +
						     "where `OrderNo` = '"+calyNumer+"' order by MatSource");
				}
				
				boolean naglowek = false;
				
				while(ProjectContent.next()) 
				{
					if(!naglowek){
						addProjectHeader(new String [] {ProjectGroup+"/"+ProjectNumber, ProjectName, ProductionDate, MontageFinishDate, klient}, table);
						naglowek=true;
					}
					
					//uzupelnienie tabeli o podstawowe dane
					addCell(table, ProjectContent.getString("MatSource"));
					addCell(table, ProjectContent.getString("Slack"));
					addCell(table, ProjectContent.getString("ItemNo"));
					addCell(table, ProjectContent.getString("ItemDesc"));
					addCell(table, ProjectContent.getString("ConsumerOrder"));
					addCell(table, ProjectContent.getString("Quantity"));
					//pobiera material, z ktorego jest wykonane zamowienie
					Statement takeMaterial = myConn.createStatement();
					ResultSet Mat = takeMaterial.executeQuery("Select MaterialAssigned from ProjectMaterials where Order500='"+ProjectContent.getString("MatSource")+"'");
					String Material = "";
					while(Mat.next()){
							Material=Mat.getString("MaterialAssigned");
							if(Material==null)
								Material = "";
					}
					takeMaterial.close();
					addCell(table, Material);
					String batchNumber = ProjectContent.getString("MatSource");
					int ileTechn = 0;
					String comment = " ";
					boolean pilne = false;
					//String storenote = ProjectContent.getString("Storenote");
					//String [] bon = storenote.split("/");
					Statement takeTakt = myConn.createStatement();
					ResultSet T;
					//pobranie przeznaczenia czêœci - bierze opis taktu, do ktorego idzie bon magazynowy i odcina 'FAT - ' 
					T = takeTakt.executeQuery("select montageomschrijving from storenotesdetail where projectnummer = '"+ProjectContent.getString("ConsumerOrder")+"' and artikelcode = '"+ProjectContent.getString("ItemNo")+"'");
					String Takt = "";
					while(T.next()){
						Takt = T.getString("MONTAGEOMSCHRIJVING");
					}
					if(Takt==null) Takt = "";
					if(Takt.startsWith("FAT -")){
						Takt = Takt.substring(5, Takt.length());
					}
					takeTakt.close();
					PdfPCell cell2 = new PdfPCell(new Phrase(Takt, ffont2));
					cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
					cell2.setVerticalAlignment(Element.ALIGN_MIDDLE);
					cell2.setFixedHeight(10f);
					cell2.setNoWrap(false);
					table.addCell(cell2);
					if(batchNumber.startsWith("500/"))
					{	
						String number = batchNumber.substring(4, batchNumber.length());
						//System.out.println("pobierze takt");
						
						//sprawdza czy piêæsetka jest pilna / ma komentarz
						Statement a =  myConn.createStatement();
						ResultSet important500 = a.executeQuery("Select Zamowienie, Komentarz, Pilne from PilneDoCzesiowej where Zamowienie = '"+batchNumber+"'");
						if(important500.next()){
							comment = important500.getString("Komentarz");
							PdfPCell cell = new PdfPCell(new Phrase(batchNumber, ffont2));
							cell.setHorizontalAlignment(Element.ALIGN_CENTER);
							cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
							cell.setFixedHeight(10f);
							if(important500.getString("Pilne").equals("1")){
								cell.setBackgroundColor(new BaseColor(240, 123, 126));
								pilne = true;
							}
							table.addCell(cell);
						
						}
						else
							addCell(table, batchNumber);
						a.close();
						//sprawdzenie operacji czy jest numeryczna i czy trzeba zaznaczyc, ze pierwsze uruchomienie / byla robiona na innych stanowiskach
						Statement b = myConn.createStatement();
						ResultSet otherTechn = b.executeQuery("Select inne from cnc where NumerSerii = '"+batchNumber+"' and inne is not null order by seq");
						while(otherTechn.next()) comment+= otherTechn.getString("inne")+"; ";
						otherTechn.close(); b.close();
						Statement c = myConn.createStatement();
						ResultSet createCNC = c.executeQuery("Select inne from cnc where NumerSerii = '"+batchNumber+"' and inne is null order by seq");
						while(createCNC.next()) comment+= "Pierwsze uruchomienie";
						createCNC.close(); c.close();
						
						//pobranie technologii
						ResultSet technology;
						Statement TakeTechn = myConn.createStatement();
						technology = TakeTechn.executeQuery("SELECT werkpost, status, werkbonnummer from werkbon " +
								"where Afdeling = '500' and Afdelingseq='"+number+"' order by seq asc limit 21");
						while(technology.next())
						{
							//je¿eli operacja spawania to przekaz informacje do listy spawanej
							if(technology.getString("werkpost").equals("ZS04")&&technology.getString("status").equals("10")&&!ListaSpawanaStara.czyIstnieje(batchNumber, calyNumer)) 
								ListaSpawanaStara.dodaj(batchNumber, calyNumer);
							//dodaj komórkê z operacj¹ technologiczna
							addTechnCell(table, technology.getString("werkpost"), technology.getString("status"), technology.getString("werkbonnummer"), myConn);
							ileTechn++;
							if(ileTechn == 21) break;
						}
						TakeTechn.close();
						//System.out.println("skonczono technologie");
						//dodanie pustych komorek do konca linijki (z pozostawieniem jednej na komentarz)
						addEmptyCells(21-ileTechn, table);
						//komentarz, jesli istnieje
						PdfPCell cell = new PdfPCell(new Phrase(comment, ffont2));
						cell.setHorizontalAlignment(Element.ALIGN_CENTER);
						cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
						cell.setFixedHeight(10f);
						if(pilne){
							cell.setBackgroundColor(new BaseColor(240, 123, 126));
						}
						table.addCell(cell);
					}
					else
						addEmptyCells(23, table);					
					comment = " ";
					pilne = false;
				}
				if(naglowek)
					addRow(2, table);
				ListContent.close();
			}
			
			pobierzProjectSchedule.close();
			System.out.println("Koniec listy produkcyjnej");
			System.out.println(table.size());
			//sprawdzenie czy lista nie jest pusta
			if(table.size()<=2 ){
				Paragraph a = new Paragraph("No data, check import from GTT", ffont2abc);
				LProd.add(a);
			}
			else{
				table.setHeaderRows(1);
				LProd.add(table);
			}
			LProd.close();
			myConn.close();
			//nowa
			checkComments();
			//zaznacz w oknie ze zrobione
			MainProgramWindowStart.chk02.setSelected(true);
			
			System.out.println("Rozpoczeto liste spawana");
			if(table.size()>2 ){
				ListaSpawanaStara.create();
			}
			System.out.println("Koniec listy spawanej");
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//metoda dodawania naglowka
	private static void addHeader(PdfPTable t){
		String [] naglowek = new String [] {"Numer serii", "OpóŸnienie", "Numer artyku³u", "Nazwa artyku³u", "Projekt", "Ilosc", "Materia³", "Takt", "Numer serii", "Komentarz"};
		
		for(int i = 0; i<naglowek.length; i++) {
			PdfPCell cell1 = new PdfPCell(new Phrase(naglowek[i], ffont));
			cell1.setMinimumHeight(30);
			cell1.setBackgroundColor(BaseColor.ORANGE);
			if(i==9) cell1.setBackgroundColor(new BaseColor(0,176,240));
			cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell1);
			
			if(i==8) {
				for(float j = 1; j<=21; j++){
					PdfPCell cell8 = new PdfPCell(new Phrase(String.format("%s", Math.round(j*10)), ffont));
					BaseColor color = new BaseColor(255, Math.round(255*(j/21)), 0);
					cell8.setMinimumHeight(30);
					cell8.setBackgroundColor(color);
					cell8.setHorizontalAlignment(Element.ALIGN_CENTER);
					cell8.setVerticalAlignment(Element.ALIGN_MIDDLE);
					t.addCell(cell8);
				}
			}
		}
	}
	//metoda dodawania jednego rzêdu (a razy)
	private static void addRow(int a, PdfPTable t){
		for (int i = 1; i<=a; i++){
			PdfPCell cell = new PdfPCell(new Phrase(" "));
			cell.setColspan(t.getNumberOfColumns());
			cell.setFixedHeight(10f);
			t.addCell(cell);
		}
	}
	
	//dodawanie a-razy pustych komorek
	private static void addEmptyCells(int a, PdfPTable t){
		for(int i = 1; i<=a; i++){
			PdfPCell cell = new PdfPCell(new Phrase(" ",ffont2));
			cell.setNoWrap(true);
			t.addCell(cell);
		}
	}
	
	//dodaj komorke
	private static void addCell(PdfPTable t, String z){
		PdfPCell cell = new PdfPCell(new Phrase(z, ffont2));

		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setFixedHeight(10f);
		if(z.startsWith("Nie u")) cell.setBackgroundColor(new BaseColor(255,127,39));
		if(z.startsWith("Na m")) cell.setBackgroundColor(new BaseColor(218, 211, 220));
		t.addCell(cell);
	}
	
	//dodaj naglowek projektu
	private static void addProjectHeader(String [] dane, PdfPTable t){
		String [] naglowki = new String[] {"Numer projektu", "Nazwa projektu", "Data produkcji", "Data koñca monta¿u", "Klient"};
		
		for(int j = 0; j<2; j++) {
			for(int i = 0; i<naglowki.length; i++) {
				PdfPCell cell1;
				if(j==0)
					cell1 = new PdfPCell(new Phrase(naglowki[i], ffont));
				else
					cell1 = new PdfPCell(new Phrase(dane[i], ffont));
				cell1.setFixedHeight(15f);
				if(i==0||i==4) cell1.setColspan(3);
				if(i==2||i==3) cell1.setColspan(2);
				cell1.setBackgroundColor(BaseColor.ORANGE);
				cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
				t.addCell(cell1);
			}
			t.completeRow();
		}
	}
	
	//dodaj komorke z technologia - formatowanie zalezne od statusu operacji
	private static void addTechnCell(PdfPTable t, String stan, String status, String nrBonu, Connection conn) throws SQLException{
		Phrase p = null; 
		if(status.equals("10")) {
			Statement st1 = conn.createStatement();
			ResultSet rs1 = st1.executeQuery("Select kodArtykulu, inne from cnc where numerBonu = '"+nrBonu+"'");
			p = new Phrase(stan, ffont2);
			while(rs1.next()) {
				if(rs1.getString("inne") == null) {
					p = new Phrase(stan, ffont2a);
				}
				else
					p = new Phrase(stan, ffont2b);
			}
			rs1.close(); st1.close();
		}
		else p = new Phrase(stan, ffont2);
		
		PdfPCell cell = new PdfPCell(p);
		if(status.equals("10"))	cell.setBackgroundColor(BaseColor.WHITE);
		if(status.equals("20")) cell.setBackgroundColor(new BaseColor(255, 255, 0));
		if(status.equals("90")) cell.setBackgroundColor(new BaseColor(18, 162, 25));
		
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_TOP);
		cell.setFixedHeight(10f);
		t.addCell(cell);
	}
	
	//metoda sprawdzajaca, czy w tabeli komentarzy znajduja sie adnotacje do zamknietych serii 500 - jesli tak to usuwa
	private static void checkComments() throws SQLException{
		Connection myConn = DBConnection.dbConnector();
		String sql1 = "Select PilneDoCzesiowej.Zamowienie,  PilneDoCzesiowej.Komentarz from PilneDoCzesiowej left outer join BESTELLING on BESTELLING.LEVERANCIERORDERNUMMER = PilneDoCzesiowej.Zamowienie"
				+ " where bestelling.statuscode <>'O'";
		Statement st1 = myConn.createStatement();
		ResultSet rs1 = st1.executeQuery(sql1);
		while(rs1.next()){
			String Numer = rs1.getString("Zamowienie");
			String sql2 = "Delete from PilneDoCzesiowej where Zamowienie = '"+Numer+"'";
			Statement st2 = myConn.createStatement();
			st2.executeUpdate(sql2);
			st2.close();			
		}
		st1.close();
		myConn.close();
	}
	
}
