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

public class PDF_Zakupy {
	private static Font ffont, ffont2;
	private static Connection connection;

	public static void create() {
		System.out.println("Start listy zakupowej");
		ffont = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 10);
		ffont2 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 12);
		connection = DBConnection.dbConnector();
		
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		try
		{
			String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
			String name = "Zakupy.pdf";
			
			PdfPTable table = new PdfPTable(8);
			float widths[] = new float[] {10, 6, 10, 10, 6, 10, 6, 6 };
			table.setWidths(widths);
			table.setWidthPercentage(100);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			addHeader(table);
			
			File f = new File(path+name);
			if(f.exists() && !f.isDirectory())
				name = godz.format(date.getTime())+" "+name;
			
			//dla kazdego projektu
			Statement pobierzProjectSchedule = connection.createStatement();
			String sql = "Select nrMaszyny, opis, klient, dataprodukcji, dataKoniecMontazu, komentarz from calendar where Zakonczone = 0 and komentarz not like 'Zmiana%' and klient <> 'MAGAZYN' order by dataKoniecMontazu";
			ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql);
			
			while(ProjectSchedule.next()){
				
				String calyNumer = ProjectSchedule.getString("nrMaszyny");
				String [] p = calyNumer.split("/");
				String nazwa = ProjectSchedule.getString("opis");
				String klient = ProjectSchedule.getString("klient");
				String FinishDate = ProjectSchedule.getString("dataKoniecMontazu");
				
				boolean naglowek = false;
				
				//pobiera wszystkie zamowienia, ktore potrzebne sa do zrealizowania piecsetek = zamowien wewnetrznych.
				//pobiera je z partsoverview gdzie zamowieniem nadrzednym jest 500/nr
				//wszystkie zamowienia, ktore nie pochodza z saca
				//serie 500/......
				String sql02 = "Select MatSource, ItemNo, ItemDesc, ConsumerOrder, Storenote from partsoverview "
						+ " where OrderNo = '"+calyNumer+"'"
						+ " and MatSource not like '500%'"
						+ " and MatSource not like '119003%'"
						+ " and MatSource <> 'Na magazynie'"
						+ " and MatSource <> 'Nie uruchomione'"
						+ " and MatSource not like '"+p[0]+"/%'"
						+ " and ConsumerOrder like '500/%'";
				Statement st1 = connection.createStatement();
				ResultSet rs1 = st1.executeQuery(sql02);
				while(rs1.next()) {
					if(!naglowek){
						addProjectHeader(table, calyNumer, nazwa, klient, FinishDate);
						naglowek=true;
					}
					String [] tab = new String [8];
					String zamowienie = rs1.getString("MatSource");
					String kod = rs1.getString("ItemNo");
					String opisArt = rs1.getString("ItemDesc");
					String zlecenieNadrz = rs1.getString("ConsumerOrder");
					String storenote = rs1.getString("Storenote");
					tab = searchData(zamowienie, kod, storenote, opisArt, zlecenieNadrz, p[1]);
					addRow(tab, table);
					
				}
				rs1.close();
				st1.close();				
				
				//pobiera wszystkie zamowienia POZOSTALE - czyli takie, ktorych consumer order jest inny niz 500
				//najczesciej sa to zamowienia na czesci idace prosto na bony magazynowe maszyny!
				//bony
				String sql03 = "Select MatSource, ItemNo, ItemDesc, ConsumerOrder, Storenote from partsoverview where OrderNo = '"+calyNumer+"'"
						+ " and MatSource not like '500%'"
						+ " and MatSource not like '119003%'"
						+ " and MatSource <> 'Na magazynie'"
						+ " and MatSource <> 'Nie uruchomione'"
						+ " and MatSource not like '"+p[0]+"/%'"
						+ " and ConsumerOrder not like '500/%'";
				
				Statement st3 = connection.createStatement();
				ResultSet rs3 = st3.executeQuery(sql03);
				while(rs3.next()) {
					if(!naglowek){
						addProjectHeader(table, calyNumer, nazwa, klient, FinishDate);
						naglowek=true;
					}
					String zamowienie = rs3.getString("MatSource");
					String kod = rs3.getString("ItemNo");
					String opisArt = rs3.getString("ItemDesc");
					String zlecenieNadrz = rs3.getString("ConsumerOrder");
					String storenote = rs3.getString("Storenote");
					addRow(searchData(zamowienie, kod, storenote, opisArt, zlecenieNadrz, p[1]), table);
					
				}
				rs3.close();
				st3.close();
				
				//pobieranie zamowien prosto na projekt, gdzie artykulem jest M
				//z pola tekstowego zamowieia pobiera nazwe zamowionego artykulu
				//zamówienia prosto na projekt
				String sql01 = "SELECT LEVERANCIER.NAAM, BESTELLING.LEVERANCIERORDERNUMMER, BESTELLINGDETAIL.ARTIKELCODE, BESTELLINGDETAIL.ARTIKELOMSCHRIJVING, BESTELLINGDETAIL.BESTELD, BESTELLINGDETAIL.BESTELEENHEID, BESTELLINGDETAIL.GELEVERD, bestelling.referentie, bestellingdetail.tekst, bestellingdetail.leveringsdatumbevestigd FROM BESTELLINGDETAIL  "
						+ " JOIN BESTELLING ON BESTELLING.LEVERANCIER = BESTELLINGDETAIL.LEVERANCIER AND BESTELLING.ORDERNUMMER = BESTELLINGDETAIL.ORDERNUMMER"
						+ " JOIN LEVERANCIER ON BESTELLINGDETAIL.LEVERANCIER = LEVERANCIER.LEVERANCIERNR "
						+ " WHERE BESTELLING.AFDELING = '"+p[0]+"' AND BESTELLING.AFDELINGSEQ like '"+p[1]+"%' AND BESTELLING.LEVERANCIER <> 2 AND (BESTELLING.LEVERANCIER NOT LIKE '119003%' and bestelling.leverancier <> '118034' and bestelling.leverancier <> '6' and bestelling.leverancier <> '2') AND BESTELLINGDETAIL.BOSTDEH <> 0";
				Statement st8 = connection.createStatement();
				ResultSet rs8 = st8.executeQuery(sql01);
				String [] tab = new String [8];
				while(rs8.next()) {
					if(!naglowek){
						addProjectHeader(table, calyNumer, nazwa, klient, FinishDate);
						naglowek=true;
					}
					tab[0] = rs8.getString("naam");
					tab[1] = rs8.getString("leverancierordernummer");
					tab[2] = rs8.getString("artikelcode");
					if(tab[2].equals("M")) tab[3] = rs8.getString("tekst");
					else tab[3] = rs8.getString("artikelomschrijving");
					double zapotrzebowanie = rs8.getDouble("besteld")-rs8.getDouble("geleverd");
					if(zapotrzebowanie%1 == 0 ) tab[4] = String.format("%.0f", zapotrzebowanie);
					tab[4] += " "+rs8.getString("besteleenheid");
					tab[5] = rs8.getString("Referentie");
					tab[6] = rs8.getString("leveringsdatumbevestigd");
					if(!tab[2].equals("M")) {
						Statement st001 = connection.createStatement();
						ResultSet rs001 = st001.executeQuery("Select ilosc, jednostka from stock where kodArtykulu = '"+tab[2]+"'");
						while(rs001.next()) {
							tab[7] = String.format("%d",rs001.getInt("ilosc"))+" "+rs001.getString("jednostka");
						}
						rs001.close();
						st001.close();
					}
					else { tab[7] = "";}
					addRow(tab,table);					
				}
				rs8.close(); st8.close();
				
				//kontrola czesci nieuruchomionych / brakow
				//NIE URUCHOMIONE
				String sql0202 = "Select MatSource, ItemNo, ItemDesc, Storenote, ConsumerOrder, artikel_aankoop.leveranciernr from partsoverview "
						+ " left join artikel_algemeen on  partsoverview.itemno =  artikel_algemeen.artikelcode "
						+ " left join artikel_aankoop on partsoverview.itemno = artikel_aankoop.artikelcode "
						+ " where OrderNo = '"+calyNumer+"' "
						+ " and MatSource = 'Nie uruchomione' "
						+ " and ConsumerOrder like '500/%' and artikel_algemeen.verschaffingscode <> 'P' and artikel_aankoop.levmanplanning = 1";
				
				//sprawdziæ czy to nie saca (sprawdzenie w dziale artykul standardowego dostawce)
				Statement st6 = connection.createStatement();
				ResultSet rs6 = st6.executeQuery(sql0202);
				while(rs6.next()) {
					boolean P3 = true;
					if(rs6.getString("leveranciernr").startsWith("119003")) P3 = false;
					if(P3) { if(!naglowek){
							addProjectHeader(table, calyNumer, nazwa, klient, FinishDate);
							naglowek=true;
						}
						addRow(searchData(rs6.getString("MatSource"), rs6.getString("ItemNo"), rs6.getString("Storenote"), rs6.getString("ItemDesc"), rs6.getString("ConsumerOrder"), p[1]), table);
					}
				}
				rs6.close(); st6.close();
			}
			ProjectSchedule.close();
			pobierzProjectSchedule.close();
			Document Zakupy = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
			PdfWriter writer = PdfWriter.getInstance(Zakupy, new FileOutputStream(path+name));
			Zakupy.open();
			writer.setPageEvent(new PDF_MyFooter());
						
			if(table.size()==0 ){
				Paragraph a = new Paragraph("Document is empty", ffont);
				Zakupy.add(a);
			}
			else{
				table.completeRow();
				table.setHeaderRows(1);
				Zakupy.add(table);
			}
			Zakupy.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		System.out.println("Koniec listy zakupowej");
	}
	
	private static void addRow(String[] tab, PdfPTable t) {
		// TODO Auto-generated method stub
		for(int i = 0; i<tab.length; i++) {
			PdfPCell cell = new PdfPCell(new Phrase(tab[i], ffont));
			cell.setFixedHeight(30);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell);
		}
	}

	private static void addHeader(PdfPTable t){
		
		String naglowek[] = new String[] { "Dostawca", "Nr zamówienia", "Kod art", "Nazwa art", "Zapotrzebowanie", "Referencje", "Potwierdzona data dost", "Stan magazynowy"};
				
		for(int i = 0; i<naglowek.length; i++) {
			PdfPCell cell = new PdfPCell(new Phrase(naglowek[i], ffont));
			cell.setMinimumHeight(30);
			cell.setBackgroundColor(new BaseColor(0,176,240));
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell);
			
		}
	}
	
	private static void addProjectHeader(PdfPTable t, String nrMaszyny, String nazwa, String klient, String data) {
		String zawartosc[] = new String [] {nrMaszyny, nazwa, klient, "Data koñca projektu:\n"+data};
		PdfPCell cellWolnaLinia = new PdfPCell();
		cellWolnaLinia.setColspan(t.getNumberOfColumns());
		cellWolnaLinia.setMinimumHeight(5);
		t.addCell(cellWolnaLinia);
		
		for(int i = 0; i < zawartosc.length; i++) {
			PdfPCell cell1 = new PdfPCell(new Phrase(zawartosc[i], ffont2));
			cell1.setColspan(2);
			cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell1.setMinimumHeight(30);
			cell1.setBackgroundColor(BaseColor.ORANGE);
			t.addCell(cell1);
		}
	}
	
	//metoda wyszukujaca wszystkich danych dotyczacych zamowienia i pozycji
	private static String [] searchData(String order, String code, String storenote, String desc, String ConsumerOrder, String projekt) throws SQLException {
		String [] tab = new String [8];
		tab[1] = order; tab[2] = code; tab[3] = desc;
		String dostawca = "", potwierdzonaData = "", referencje = "", stock = "", jednostka = "";
		double ileZamowiono = 0, dostarczono = 0;
		Statement  st00 = connection.createStatement();
		ResultSet rs00 = st00.executeQuery("select besteld, besteleenheid, geleverd from storenotesdetail where leverancier = '"+storenote.split("/")[0]+"' and ordernummer = '"+storenote.split("/")[1]+"'");
		while(rs00.next()) {
			ileZamowiono = rs00.getDouble("besteld");
			jednostka = rs00.getString("besteleenheid");
			dostarczono = rs00.getDouble("geleverd");
		}
		rs00.close();
		st00.close();
		double zapotrzebowanie = ileZamowiono-dostarczono;
		if(zapotrzebowanie%1 == 0 ) tab[4] = String.format("%d", (int)ileZamowiono);
		else tab[4] = String.format("%d", (int) zapotrzebowanie);
		tab[4] += " "+jednostka;
		
		if(!order.equals("Nie uruchomione")) {
			Statement st = connection.createStatement();
			//wyszukanie szczegolow zamowienia - jaki dostawca, jaka potwierdzona data dostawy, jakie referencje
			ResultSet rs = st.executeQuery("Select bestellingdetail.leveringsdatumbevestigd, bestelling.referentie, leverancier.naam from bestellingdetail "
					+ "join bestelling on bestelling.leverancier = bestellingdetail.leverancier and bestelling.ordernummer = bestellingdetail.ordernummer "
					+ "join leverancier on bestellingdetail.leverancier = leverancier.leveranciernr "
					+ " where concat(bestellingdetail.leverancier, \"/\", bestellingdetail.ordernummer) = '"+order+"'");
			while(rs.next()) {
				dostawca = rs.getString("naam");
				potwierdzonaData = rs.getString("leveringsdatumbevestigd");
				referencje = rs.getString("referentie");
			}
			rs.close();
			st.close();
			
			//sprawdzenie czy nie ma zamówienia na dany artyku³ z referencj¹
			/*if(!referencje.contains(projekt)) {
				Statement st1 = connection.createStatement();
				ResultSet rs1 = st1.executeQuery("Select bestellingdetail.leveringsdatumbevestigd, bestelling.referentie, leverancier.naam from bestellingdetail "
						+ "join bestelling on bestelling.leverancier = bestellingdetail.leverancier and bestelling.ordernummer = bestellingdetail.ordernummer "  
						+ "join leverancier on bestellingdetail.leverancier = leverancier.leveranciernr "
						+ "where bestelling.referentie like '%"+projekt+"%' and bestellingdetail.artikelcode = '"+code+"'");
				while(rs1.next()) {
					dostawca = rs1.getString("naam");
					potwierdzonaData = rs1.getString("leveringsdatumbevestigd");
					referencje = rs1.getString("referentie");
				}
				rs1.close(); st1.close();
			}*/
			
			tab[0] = dostawca; tab[5] = referencje; tab[6] = potwierdzonaData;
		}
		else {
			tab[0] = "";
			tab[1] = "Brak zamówienia";
			tab[5] = "";
			tab[6] = "";
			
		}
		//kontrola stanu magazynowego
		Statement st001 = connection.createStatement();
		ResultSet rs001 = st001.executeQuery("Select ilosc, jednostka from stock where kodArtykulu = '"+code+"'");
		while(rs001.next()) {
			stock = String.format("%d",rs001.getInt("ilosc"));
			stock+= " "+rs001.getString("jednostka");
		}
		rs001.close();
		st001.close();
		tab[7] = stock; 
		
		return tab;
	}
	
}
