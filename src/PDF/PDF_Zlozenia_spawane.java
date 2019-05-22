package PDF;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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


public class PDF_Zlozenia_spawane {
	
	//klasa wewnetrzna okreslajace pare skladajaca sie z:
	// nr projektu
	// nr zlozenia spawanego, ktore trzeba wykonac do zrealizowania projektu
	private class para{
		private String zlozenie;
		private String numerProjektu;
		public para(String z, String nr){
			zlozenie = z;
			numerProjektu = nr;
		}
		public String getZlozenie(){
			return zlozenie;
		}
		
		public String getProject(){
			return numerProjektu;
		}
	}
	
	
	private List<para> zlozenie = new ArrayList<para>();
	public List<String> listaSACA = new ArrayList<String>();
	private static Font ffont = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 8);
	private static Font ffont2 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 6);
	private static Font ffont2a = FontFactory.getFont(BaseFont.TIMES_BOLD, BaseFont.CP1250, BaseFont.EMBEDDED, 6 );
	public void dodaj(String z, String numerProjektu){
		zlozenie.add(new para(z, numerProjektu));
	}
	
	public boolean czyIstnieje(String z, String numerProjektu){
		return zlozenie.contains(new para(z, numerProjektu));
	}
	
	
	
	public void create() throws SQLException{
		Connection myConn = DBConnection.dbConnector();
		Document list = new Document(PageSize.A4.rotate(), 10, 10, 25, 25);
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat SACA = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		PdfWriter writer;
		try{
			
			// ANALIZA ELEMENTÓW SPAWANYCH DLA SACA	- co saca musi nam wykonac do naszych spawanych?
			/*
			 * Tworzona jest tabela Spawane 
			 * znajduja sie w niej pó³fabrykaty na z³o¿enia spawane, które nie s¹ 500/ - najczêœciej z saca (do okreœlenia typu artyku³u)
			 */
			Statement createNewTable= myConn.createStatement();
			createNewTable.executeUpdate("Delete from Spawane");
			createNewTable.close();
			String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
			String name = "Zlozenia spawane.pdf";
			File f = new File(path+name);
			if(f.exists() && !f.isDirectory())
				name = godz.format(date.getTime())+" "+name;
			writer = PdfWriter.getInstance(list, new FileOutputStream(path+name));
			list.open();
			writer.setPageEvent(new PDF_MyFooter());
			PdfPTable table = new PdfPTable(23);
			float widths[] = new float[] {15, 35, 40, 18, 9, 9, 9, 18, 18, 20, 18, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9};
			table.setWidths(widths);
			table.setWidthPercentage(100);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			addHeader(table);
			String project = "";
			//dla kazdego zlozenia spawanego:
			for(para p : zlozenie){
				//dodawanie headera projektu
				if(!project.equals(p.getProject())){
					project=p.getProject();
					String sql = "Select opis, dataKoniecMontazu, dataprodukcji, klient from calendar where nrMaszyny = '"+p.getProject()+"'";
					Statement stm1 = myConn.createStatement();
					ResultSet rs1 = stm1.executeQuery(sql);
					String opis = "", dataprodukcji="", klient = "";
					while(rs1.next()){
						opis = rs1.getString("opis");
						dataprodukcji = rs1.getString("dataprodukcji");
						klient = rs1.getString("klient");
					}
					addProjectHeader(new String[] {p.getProject(), opis, dataprodukcji, klient}, table);
				}
				//dodawanie wiersza ze zlozeniem spawanym
				//pobieranie danych o zlozeniu:
				Statement takeInfo = myConn.createStatement();
				ResultSet rs = takeInfo.executeQuery("SELECT MatSource, ItemNo, ItemDesc, ConsumerOrder, Quantity, ProjectMaterials.MaterialAssigned FROM PartsOverview "
						+ "left join ProjectMaterials on Order500 = MatSource "
						+ "where OrderNo = '"+p.getProject()+"' and MatSource ='"+p.getZlozenie()+"'");
				while(rs.next()){
					String batchNumber = rs.getString("MatSource");
					addCell(table, batchNumber);
					addCell(table, rs.getString("ItemNo"));
					addCell(table, rs.getString("ItemDesc"));
					addCell(table, rs.getString("ConsumerOrder"));
					addCell(table, rs.getString("Quantity"));
					addCells(2, table);
					//material
					String Material = rs.getString("MaterialAssigned");
					if(Material==null)
						Material = "";
					addCell(table, Material);
					addCell(table, "");
					addCell(table, batchNumber);
					addCell(table, "");
					//technologia
					int ileTechn = 0;
					Statement TakeTechn = myConn.createStatement();
					ResultSet technology = TakeTechn.executeQuery("SELECT werkpost, status from werkbon " +
							"where Afdeling = '500' and Afdelingseq='"+batchNumber.substring(4, batchNumber.length())+"' order by seq asc limit 12");
					//System.out.println("pobrano technologie");
					while(technology.next()){
						addTechnCell(table, technology.getString("werkpost"), technology.getString("status"), false);
						ileTechn++;
						if(ileTechn == 12) break;
					}
					TakeTechn.close();
					addCells(12-ileTechn, table);
					
					
					//wyciaganie elementow podzlozenia
					Statement takeSubParts = myConn.createStatement();
					ResultSet rs2 = takeSubParts.executeQuery("Select artikelcode, artikelomschrijving, besteld, geleverd, besteleenheid, leverancier, ordernummer from storenotesdetail "
								+ "where projectnummer = '"+batchNumber+"' order by leverancier, ordernummer");
					while(rs2.next()){
						String piecset = "";
						Statement st100 = myConn.createStatement();
						ResultSet rs100 = st100.executeQuery("select matsource from partsoverview where OrderNo = '"+p.getProject()+"' and ItemNo ='"+rs2.getString("artikelcode")+"'");
						while(rs100.next())  piecset = rs100.getString("MatSource");
						rs100.close();
						st100.close();
						if(piecset==null) piecset="";
						boolean wazne = false;
						Font czcionka = ffont2;
						if(piecset.startsWith("119003")) { 
							piecset = "SACA mech";
						//sprawdzenie czy pilne
							Statement checkPilne = myConn.createStatement();
							String sqlcheck = "select wazne from saca where datadodania = '"+SACA.format(date.getTime())+"' and projekt = '"+p.getProject()+"' and kodartykulu = '"+rs2.getString("artikelcode")+"'";
							ResultSet checkRS = checkPilne.executeQuery(sqlcheck);
							while(checkRS.next()) if(checkRS.getString(1).equals("1")) wazne = true;
							checkRS.close();
							checkPilne.close();
						}
						if(wazne) czcionka = ffont2a;
						addCell(true, table, batchNumber, czcionka);
						addCell(true, table, rs2.getString("artikelcode"), czcionka);
						addCell(true, table, rs2.getString("artikelomschrijving"), czcionka);
						addCell(true, table, " ", czcionka);
						addCell(true, table, rs2.getString("besteld"), czcionka);
						addCell(true, table, rs2.getString("geleverd"), czcionka);
						addCell(true, table, rs2.getString("besteleenheid"), czcionka);
						
						//sprawdzanie materialu
						String MaterialSub = "";
						if(piecset!="") {
							Statement st1 = myConn.createStatement();
							ResultSet rs1 = st1.executeQuery("select materialassigned from projectmaterials where order500 = '"+piecset+"'");
							while(rs1.next())  MaterialSub = rs1.getString("MaterialAssigned");
							rs1.close();
							st1.close();
						}
						if(MaterialSub==null) MaterialSub = "";

						addCell(true, table, MaterialSub, czcionka);
						addCell(true, table, rs2.getString("leverancier")+"/"+rs2.getString("ordernummer"), czcionka);
						
						//lokalizacja
						String lokalizacjaArt = "";
						Statement lokalizacja = myConn.createStatement();
						ResultSet lok = lokalizacja.executeQuery("Select Lokalizacja from Stock where kodArtykulu = '"+rs2.getString("artikelcode")+"'");
						while(lok.next()){
							lokalizacjaArt=lok.getString("Lokalizacja");
							if(lokalizacjaArt==null)
								lokalizacjaArt = "";
						}
						lok.close();
					
						//sprawdzenie czy detal wchodzacy w sklad zlozenia spawanego jest wykonany 
						//UWAGA - detal moze byc nieprzyjety na magazyn (ciagle piecsetka), ale moze miec zrobione wszystkie operacje technologiczne
						boolean done = true;
						if(piecset.startsWith("500")){
								Statement TakeTechn01 = myConn.createStatement();
								ResultSet technology01 = TakeTechn01.executeQuery("SELECT werkpost, status from werkbon " +
										"where Afdeling = '500' and Afdelingseq='"+piecset.substring(4, piecset.length())+"' and status <> 90 order by seq asc limit 12");
								//System.out.println("pobrano technologie");
								while(technology01.next())
								{
									done = false;
								}
								TakeTechn01.close();
						}
						else if (piecset.equals("")||piecset.equals("Na magazynie")) done = true;
						else done = false;
						
						
						if(done) {
							addCellGreen(table, piecset);
						}
						else addCell(true, table, piecset, czcionka);
						addCell(true, table, lokalizacjaArt, czcionka);
						
						//technologia
						int ileTechn2 = 0;
						if(!piecset.equals("")){
							if(piecset.startsWith("500")){
								Statement TakeTechn2 = myConn.createStatement();
								ResultSet technology2 = TakeTechn2.executeQuery("SELECT werkpost, status from werkbon " +
										"where Afdeling = '500' and Afdelingseq='"+piecset.substring(4, piecset.length())+"' order by seq asc limit 12");
								//System.out.println("pobrano technologie");
								while(technology2.next())
								{
									addTechnCell(table, technology2.getString("werkpost"), technology2.getString("status"), false);
									ileTechn2++;
									if(ileTechn2 == 12) break;
								}
								TakeTechn2.close();
							}
							else{
								Statement insert = myConn.createStatement();
								// Projekt, kod, nazwa, ile zam, ile dost, nr zamowienia
								insert.executeUpdate("INSERT INTO Spawane values "+
										"('"+p.getProject()+"', "+
										"'"+rs2.getString("artikelcode")+"', "+
										"'"+rs2.getDouble("besteld")+"', "+
										"'"+rs2.getDouble("geleverd")+"', " +
										"'"+piecset+"', " +
										"'"+batchNumber+"', " +
										"'"+rs.getString("ItemNo")+"')");
								insert.close();
							}
						}
						addCells(true, 12-ileTechn2, table);
					}
					takeSubParts.close();
				}
				takeInfo.close();
				
			}
			if(table.size()==0 ){
				Paragraph a = new Paragraph("Document is empty", ffont2);
				list.add(a);
			}
			else{
				table.setHeaderRows(1);
				list.add(table);
			}
		list.close();
		myConn.close();
		} catch (FileNotFoundException | DocumentException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//dodanie naglowka do dokumentu
	private static void addHeader(PdfPTable t){
		String [] header = new String [] {"Seria z³o¿enia", "Kod artyku³u ", "Nazwa artyku³u", " ", "Zamówiono", "Dostarczono", "Jednostka", "Materia³", "Nr bonu", "Nr 500", "Lokalizacja", "Postêp produkcji"};
			
			//adding header to our file
			for(int i = 0; i<header.length; i++) {
				PdfPCell cell1 = new PdfPCell(new Phrase(header[i], ffont));
				cell1.setMinimumHeight(30);
				if(i==11) cell1.setColspan(12);
				cell1.setBackgroundColor(BaseColor.ORANGE);
				cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
				t.addCell(cell1);
			}
		}
		
	private static void addProjectHeader(String [] dane, PdfPTable t){
		String [] header = new String[] {"Numer projektu", "Nazwa projektu", "Data produkcji czêœci", "Klient"};
		for(int j = 0; j<2; j++) {
			for(int i = 0; i<header.length; i++) {
				
				PdfPCell cell1;
				if(j ==0)
					cell1 = new PdfPCell(new Phrase(header[i], ffont));
				else 
					cell1 = new PdfPCell(new Phrase(dane[i], ffont));
				cell1.setFixedHeight(15f);
				if(i==0)
					cell1.setColspan(2);
				if(i==2||i==3)
					cell1.setColspan(3);
				cell1.setBackgroundColor(BaseColor.ORANGE);
				cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
				t.addCell(cell1);
			}
			addCells(14,t);
		}
	}
	
	//dodanie pistych komorek (z okresleniem czy szara)
	private static void addCells(boolean ifgrey, int a, PdfPTable t){
		for(int i = 1; i<=a; i++){
			PdfPCell cell = new PdfPCell(new Phrase(" ",ffont2));
			if(ifgrey)
				cell.setBackgroundColor(new BaseColor(200, 200, 200));
			cell.setNoWrap(true);
			t.addCell(cell);
		}
	}
	
	private static void addCells(int a, PdfPTable t){
		addCells(false, a, t);
	}
	
	public static void addCell(PdfPTable t, String z){
		addCell(false, t, z);
	}
	
	//dodanie komorki
	public static void addCell(boolean grey, PdfPTable t, String z){
		PdfPCell cell = new PdfPCell(new Phrase(z, ffont2));
		if(grey)
			cell.setBackgroundColor(new BaseColor(200, 200, 200));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setFixedHeight(15f);
		t.addCell(cell);
	}
	
	//dodanie komorki (z opcja sprawdzenia czy ma byc szare tlo) + jaka czcionka
	public static void addCell(boolean grey,PdfPTable t, String z, Font font){
		PdfPCell cell = new PdfPCell(new Phrase(z, font));
		if(grey)
			cell.setBackgroundColor(new BaseColor(200, 200, 200));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setFixedHeight(15f);
		t.addCell(cell);
	}
	
	//dodanie zielonej komorki
	public static void addCellGreen(PdfPTable t, String z){
		PdfPCell cell = new PdfPCell(new Phrase(z, ffont2));
		cell.setBackgroundColor(new BaseColor(18, 162, 25));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setFixedHeight(15f);
		t.addCell(cell);
	}
	
	//metoda dodajaca komorke z technologia (formatowanie zalezne od statusu)
	private static void addTechnCell(PdfPTable t, String stan, String status, boolean czyPogrubic){
		PdfPCell cell = new PdfPCell(new Phrase(stan, ffont2));
		if(status.equals("10"))	cell.setBackgroundColor(BaseColor.WHITE);
		if(status.equals("20")) cell.setBackgroundColor(new BaseColor(255, 255, 0));
		if(status.equals("90")) cell.setBackgroundColor(new BaseColor(18, 162, 25));
		if(czyPogrubic){
			cell.setBorderWidth(1.2f);
		}
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setFixedHeight(10f);
		t.addCell(cell);
	}
}
