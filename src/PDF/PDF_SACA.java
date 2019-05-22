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

public class PDF_SACA {

	public static Font ffont = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 10); 
	public static Font ffont2 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 8); 
	public static Font ffont3 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 6); 

	public static void createDB() throws SQLException{
		System.out.println("przygotowanie danych dla SACA");
		Connection myConn = DBConnection.dbConnector();
		SimpleDateFormat data = new SimpleDateFormat("yyyy-MM-dd");
		Statement pobierzProjectSchedule = myConn.createStatement();
		String sql = "Select nrMaszyny, komentarz from calendar where Zakonczone = 0 order by dataProdukcji";
		ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql);
		Calendar date = Calendar.getInstance();
		String dzisiaj = data.format(date.getTime());
		Statement st1 = myConn.createStatement();
		st1.executeQuery("DELETE FROM SACA WHERE DATADODANIA = '"+dzisiaj+"'");
		st1.close();
		PreparedStatement insert = myConn.prepareStatement("INSERT INTO SACA VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		PreparedStatement update = myConn.prepareStatement("UPDATE SACA SET Nrzamowienia = ?, Datazlozeniazamowienia = ?, Datadostarczenia = ?, Nrzamowienia2 = ?, Pozostalonazamowieniu = ?, Pozostalodoprojektu = ? where datadodania = ? and projekt = ? and kodartykulu = ?");
		
		//dla kazdego projektu
		while(ProjectSchedule.next()){
			
			String calyNumer = ProjectSchedule.getString("nrMaszyny");
			Statement takeParts = myConn.createStatement();
			//pobranie wszystkich zamowien w saca dla projektu
			ResultSet parts = takeParts.executeQuery("SELECT ItemNo, ItemDesc, MatSource, ConsumerOrder, Quantity, storenote FROM partsoverview " +
					"where OrderNo = '"+calyNumer+"' and MatSource like '119003%' order by itemno"); 
			String nrDostawcy = "", nrZamDost = "";
			String PrevArticleNo = "";
			while(parts.next()){
				String ArticleNo = parts.getString("ItemNo");
				String ArticleName = parts.getString("ItemDesc");
				String nrZam = parts.getString("MatSource");
				//String zlozNadrzedne = parts.getString("ConsumerOrder");
				double ileDoProj = parts.getDouble("Quantity");
				String nrZamowienia="", jednostka="", dataDost = "", dataZam = "";
				double ileNaZam=0;
				String bon = parts.getString("storenote");
				nrDostawcy = nrZam.split("/")[0];
				nrZamDost = nrZam.split("/")[1];	
				
				//zlozNadGrupa = zlozNadrzedne.split("/")[0];
				//zlozNadNumer = zlozNadrzedne.split("/")[1];
				
				//pobranie szczegolow zamowienia do SACA
				Statement wybierzZamowienie = myConn.createStatement();
				ResultSet zamowienie = wybierzZamowienie.executeQuery("SELECT bostdeh, besteleenheid, bestellingdetail.besteldatum, bestelling.bestelbon, leverdatum from bestellingdetail "
						+ "join bestelling on bestelling.leverancier = bestellingdetail.leverancier and bestelling.ordernummer = bestellingdetail.ordernummer " 
						+ "where bestellingdetail.leverancier = '"+nrDostawcy+"' and bestellingdetail.ordernummer = '"+nrZamDost+"' and artikelcode = '"+ArticleNo+"'");
				while(zamowienie.next()){
						nrZamowienia = zamowienie.getString("bestelbon");
						dataDost = zamowienie.getString("leverdatum");
						ileNaZam = zamowienie.getDouble("bostdeh");
						jednostka = zamowienie.getString("besteleenheid");
						dataZam = zamowienie.getString("besteldatum");
				}
				wybierzZamowienie.close();
				zamowienie.close();
				
				//sprawdzenie typu artykulu (spawane, mechaniczne karoseria?)
				String typArtykuluSACA = "M";
				if(nrZam.startsWith("1190031")){
					typArtykuluSACA="K";
				}
				else{
					Statement sprawdzTyp = myConn.createStatement();
					String sql1 = "SELECT nrZamowienia from Spawane " +
							"where Projekt = '"+calyNumer+"' and kodArt = '"+ArticleNo.replace("'", "")+"' and nrZamowienia = 'SACA mech'";
					ResultSet wynik = sprawdzTyp.executeQuery(sql1);
					while(wynik.next()){
						typArtykuluSACA="S";
					}
					sprawdzTyp.close();
				}
				
				//zapamietanie artykulu w bazie
				if(ArticleNo.equals(PrevArticleNo)) {
					Statement check = myConn.createStatement();
					ResultSet rs = check.executeQuery("Select nrzamowienia, datazlozeniazamowienia, datadostarczenia, nrzamowienia2, pozostalonazamowieniu, pozostalodoprojektu from saca "
							+ "where datadodania = '"+dzisiaj+"' and projekt = '"+calyNumer+"' and kodartykulu = '"+ArticleNo+"'");
					while(rs.next()) {
						
						if(!rs.getString("datazlozeniazamowienia").contains(dataZam)) dataZam = rs.getString("datazlozeniazamowienia")+", "+dataZam;
						if(!rs.getString("datadostarczenia").contains(dataDost)) dataDost = rs.getString("datadostarczenia")+", "+dataDost;
						if(!rs.getString("nrzamowienia2").contains(nrZam)) nrZam = rs.getString("nrzamowienia2")+", "+nrZam;
						double doProj = rs.getDouble("pozostalodoprojektu"); double doZam = rs.getDouble("pozostalonazamowieniu");
						if(!rs.getString("nrZamowienia").contains(nrZamowienia)) { 
							nrZamowienia = rs.getString("nrZamowienia")+", "+nrZamowienia;
							ileNaZam += doZam;
						}
						ileDoProj += doProj;
					}
					check.close(); rs.close();
					update.setString(1, nrZamowienia);
					update.setString(2, dataZam);
					update.setString(3, dataDost);
					update.setString(4, nrZam);
					if(!jednostka.equals("SZT")) {
						update.setString(5, String.format("%.2f", ileNaZam));
						update.setString(6, String.format("%.2f", ileDoProj));
					}
					else {
						update.setString(5, String.format("%d", (int) ileNaZam));
						update.setString(6, String.format("%d", (int) ileDoProj));
					}
					update.setString(8, calyNumer);
					update.setString(9, ArticleNo);
					update.setString(7,  dzisiaj);
					update.executeUpdate();
				}
				else {
					String cel = "";
					if(typArtykuluSACA.equals("S")) cel = "SPAWALNIA";
					else {
						if(bon.startsWith("103")) cel = "MAGAZYN";
						else cel = "KRAJALNIA";
					}
					insert.setString(1, dzisiaj);
					insert.setString(2, nrZamowienia);
					insert.setString(3, dataZam);
					insert.setString(4, dataDost);
					insert.setString(5, nrZam);
					insert.setString(6, ArticleNo);
					insert.setString(7, ArticleName);
					if(!jednostka.equals("SZT")) {
						insert.setString(8, String.format("%.2f", ileNaZam));
						insert.setString(9, String.format("%.2f", ileDoProj));
					}
					else {
						insert.setString(8, String.format("%d", (int) ileNaZam));
						insert.setString(9, String.format("%d", (int) ileDoProj));
					}
					insert.setString(10, jednostka);
					insert.setString(11, typArtykuluSACA);
					insert.setString(12, calyNumer);
					insert.setString(13, cel);
					insert.setString(14, null);
					insert.setString(15, "0");
					insert.executeUpdate();
				}
				PrevArticleNo = ArticleNo;
			}
			takeParts.close();
			
			//pobranie dodatkowych zamowien SACA prosto na projekt (wtedy nie ma w tabeli partsoverview)
			
			String komentarz = ProjectSchedule.getString("Komentarz");
			String [] p = calyNumer.split("/");
			String ProjectGroup = p[0];
			String ProjectNumber = p[1];
			Statement takeParts2 = myConn.createStatement();
			String sql3 = "bestelling.afdeling = '"+ProjectGroup+"' "
					+ "and bestelling.afdelingseq = '"+ProjectNumber+"' ";
			if(komentarz.length()==8){
				String []p2 = komentarz.split("/");
				sql3="(("+sql3+") or (bestelling.afdeling = '"+p2[0]+"' and bestelling.afdelingseq = '"+p2[1]+"')) ";
			}
			String sql2 = "SELECT bestelling.*, bestellingdetail.artikelcode, bestellingdetail.artikelomschrijving, bestellingdetail.bostdeh, bestellingdetail.besteleenheid, bestellingdetail.geleverd FROM bestelling "
					+ "join bestellingdetail on bestelling.leverancier = bestellingdetail.leverancier and bestelling.ordernummer = bestellingdetail.ordernummer "
					+ "where "+sql3
					+ "and bestellingdetail.bostdeh <> 0 "
					+ "and bestelling.leverancier like '119003%' ";
			ResultSet parts2 = takeParts2.executeQuery(sql2); 
			while(parts2.next()){
				String typArtykuluSACA = "M";
				String nrZam = parts2.getString("leverancierordernummer");
				String dataDost = parts2.getString("leverdatum");
				String nrZamowienia = parts2.getString("bestelbon");
				String dataZam = parts2.getString("besteldatum");
				String ArticleNo = parts2.getString("artikelcode");
				String ArticleName = parts2.getString("artikelomschrijving");
				double ileNaZam = parts2.getDouble("bostdeh");
				double ileDoProj = parts2.getDouble("bostdeh");
				String jednostka = parts2.getString("besteleenheid");
				if(nrZam.startsWith("1190031")){
					typArtykuluSACA="K";
				}
				String cel = "MAGAZYN";
				
				Statement check = myConn.createStatement();
				String sql1 = "Select nrzamowienia, datazlozeniazamowienia, datadostarczenia, nrzamowienia2, pozostalonazamowieniu, pozostalodoprojektu from saca "
						+ "where datadodania = '"+dzisiaj+"' and projekt = '"+calyNumer+"' and kodartykulu = '"+ArticleNo+"'";
				ResultSet rs = check.executeQuery(sql1);
				boolean again = false;
				while(rs.next()) {
					again = true;
					
					if(!rs.getString("datazlozeniazamowienia").contains(dataZam)) dataZam = rs.getString("datazlozeniazamowienia")+", "+dataZam;
					if(!rs.getString("datadostarczenia").contains(dataDost)) dataDost = rs.getString("datadostarczenia")+", "+dataDost;
					if(!rs.getString("nrzamowienia2").contains(nrZam)) nrZam = rs.getString("nrzamowienia2")+", "+nrZam;
					double doProj = rs.getDouble("pozostalodoprojektu"); double doZam = rs.getDouble("pozostalonazamowieniu");
					ileDoProj += doProj;
					if(!rs.getString("nrZamowienia").contains(nrZamowienia)) { 
						nrZamowienia = rs.getString("nrZamowienia")+", "+nrZamowienia;
						ileNaZam += doZam;
					}
				}
				check.close(); rs.close();
				if(again) {
					update.setString(1, nrZamowienia);
					update.setString(2, dataZam);
					update.setString(3, dataDost);
					update.setString(4, nrZam);
					if(!jednostka.equals("SZT")) {
						update.setString(6, String.format("%.2f", ileDoProj));
						update.setString(5, String.format("%.2f", ileNaZam));
					}
					else {
						update.setString(6, String.format("%d", (int) ileDoProj));
						update.setString(5, String.format("%d", (int) ileNaZam));
					}
					update.setString(8, calyNumer);
					update.setString(9, ArticleNo);
					update.setString(7,  dzisiaj);
					update.executeUpdate();
				}
				else {
					
					insert.setString(1, dzisiaj);
					insert.setString(2, nrZamowienia);
					insert.setString(3, dataZam);
					insert.setString(4, dataDost);
					insert.setString(5, nrZam);
					insert.setString(6, ArticleNo);
					insert.setString(7, ArticleName);
					if(!jednostka.equals("SZT")) {
						insert.setString(9, String.format("%.2f", ileDoProj));
						insert.setString(8, String.format("%.2f", ileNaZam));
					}
					else {
						insert.setString(9, String.format("%d", (int) ileDoProj));
						insert.setString(8, String.format("%d", (int) ileNaZam));
					}
					insert.setString(10, jednostka);
					insert.setString(11, typArtykuluSACA);
					insert.setString(12, calyNumer);
					insert.setString(13, cel);
					insert.setString(14, null);
					insert.setString(15, "0");
					insert.executeUpdate();
				}
					
			}
			takeParts2.close(); parts2.close();
		}
		insert.close();
		update.close();
		Calendar weekAgo = Calendar.getInstance();
		weekAgo.add(Calendar.DAY_OF_YEAR, -6);
		Statement st2 = myConn.createStatement();
		st2.executeQuery("DELETE FROM SACA WHERE DATADODANIA < '"+data.format(weekAgo.getTime())+"' ");
		st2.close();
		
		//SPRAWDZENIE ZMIAN z poprzednimi dniami
		//System.out.println("sprawdzam czy zaszly zmiany");
		String sql10 = "Select * from saca where datadodania = '"+dzisiaj+"' and projekt not like '6/%' and typ <> 'K' order by projekt";
		Statement st10 = myConn.createStatement();
		ResultSet rs10 = st10.executeQuery(sql10);
		
		while(rs10.next()) {
			int p = 10;
			String projekt = rs10.getString("projekt");
			String kodArtykulu = rs10.getString("kodArtykulu");
			int iloscArtykulu1 = rs10.getInt("PozostaloDoProjektu");
			for(int i = 1; i<=7; i++) {
				Calendar nData = Calendar.getInstance();
				nData.add(Calendar.DAY_OF_YEAR, -i);
				if(nData.get(Calendar.DAY_OF_WEEK)!=Calendar.SATURDAY && nData.get(Calendar.DAY_OF_WEEK)!=Calendar.SUNDAY) {
					String sql11 = "Select projekt, kodArtykulu, pozostalodoprojektu from saca where datadodania = '"+data.format(nData.getTime())+"' and projekt = '"+projekt+"' and kodArtykulu='"+kodArtykulu+"'";
					Statement st11 = myConn.createStatement();
					ResultSet rs11 = st11.executeQuery(sql11);
					boolean exists = false;
					while(rs11.next()) {
						exists = true;
						if(rs11.getInt("PozostaloDoProjektu")<iloscArtykulu1) exists = false;
					}
					rs11.close(); st11.close();
					if(!exists) {
						String sql12 = "Select count(*) from saca where datadodania = '"+data.format(nData.getTime())+"' and projekt = '"+projekt+"'";
						Statement st12 = myConn.createStatement();
						ResultSet rs12 = st12.executeQuery(sql12);
						while(rs12.next()) {
							if(rs12.getInt(1)>0) p = i;
						}
						rs12.close(); st12.close();					
					}
					if(p!=10) break;
				}
			}
			if(p!=10) {
				String sql13 = "Update saca set zmiana = '"+p+"' where datadodania = '"+dzisiaj+"' and projekt = '"+projekt+"' and kodArtykulu='"+kodArtykulu+"'";
				Statement st13 = myConn.createStatement();
				st13.executeUpdate(sql13);
				st13.close();
			}
		}
		rs10.close();st10.close();
		
		//przeniesienie parametru wa¿ne
		
		//znajdz kiedy bylo ostatnie wazne:
		String find_datadodania = "select datadodania from saca where wazne = 1 order by datadodania desc limit 1";
		Statement find_data = myConn.createStatement();
		ResultSet found_data = find_data.executeQuery(find_datadodania);
		String dataDodania_last = "";
		while(found_data.next()) {
			dataDodania_last = found_data.getString(1);
		}
		found_data.close(); find_data.close();
		if(dataDodania_last == null) {		
			Calendar wczoraj = Calendar.getInstance();
			if(wczoraj.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY)
				wczoraj.add(Calendar.DAY_OF_YEAR, -3);
			else
				wczoraj.add(Calendar.DAY_OF_YEAR, -1);
			dataDodania_last = data.format(wczoraj.getTime());
		}
		
		String sql20 = "Select distinct projekt, kodartykulu from saca where datadodania='"+dataDodania_last+"' and wazne = 1";
		Statement st20 = myConn.createStatement();
		ResultSet rs20 = st20.executeQuery(sql20);
		while(rs20.next()) {
			String sql21 = "Update saca set wazne = 1 where datadodania = '"+dzisiaj+"' and projekt = '"+rs20.getString("projekt")+"' and kodArtykulu = '"+rs20.getString("kodartykulu")+"'";
			Statement st21 = myConn.createStatement();
			st21.executeUpdate(sql21);
			st21.close();
		}
		rs20.close();st20.close();
	}
	
	//stworz dokument
	public static void createDoc(){
		System.out.println("Start list SACA");
		Connection myConn = DBConnection.dbConnector();
		Document ListMech = new Document(PageSize.A4.rotate());
		Document ListK = new Document(PageSize.A4.rotate());
		Document List = new Document(PageSize.A4.rotate());
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat doNazwy2 = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		String dzis = doNazwy2.format(date.getTime());
		PdfWriter writerM;
		PdfWriter writerK;
		PdfWriter writer;
		try{
			String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
			//tworzenie 3 zestawien jednoczesnie
			String nameMech = "SACA mechaniczne.pdf";
			String nameK = "SACA karoseria.pdf";
			String name = "SACA wszystko.pdf";
			File fMech = new File(path+nameMech);
			File fK = new File(path+nameK);
			File f = new File(path+name);
			if(fMech.exists() && !fMech.isDirectory())
				nameMech = godz.format(date.getTime())+" "+nameMech;
			writerM = PdfWriter.getInstance(ListMech, new FileOutputStream(path+nameMech));
			if(fK.exists() && !fK.isDirectory())
				nameK = godz.format(date.getTime())+" "+nameK;
			writerK = PdfWriter.getInstance(ListK, new FileOutputStream(path+nameK));
			if(f.exists() && !f.isDirectory())
				name = godz.format(date.getTime())+" "+name;
			writer = PdfWriter.getInstance(List, new FileOutputStream(path+name));
			ListMech.open();
			ListK.open();
			List.open();
			writerM.setPageEvent(new PDF_MyFooter());
			writerK.setPageEvent(new PDF_MyFooter());
			writer.setPageEvent(new PDF_MyFooter());
			PdfPTable tableM = new PdfPTable(11);
			PdfPTable tableK = new PdfPTable(11);
			PdfPTable table = new PdfPTable(11);
			float widths[] = new float[] {20, 20, 20, 20, 40, 40, 10, 10, 10, 20, 4};
			Paragraph M = new Paragraph("Typ: M - mechaniczne \n\n", ffont2);
			Paragraph K = new Paragraph("Typ: K - karoseria \n\n", ffont2);
			Paragraph a = new Paragraph("Typ:  S - spawane, M - mechaniczne, K - karoseria \n\n", ffont2);
			ListMech.add(M);
			ListK.add(K);
			List.add(a);
			
			String sqlwazne = "Select count(*) from saca where dataDodania = '"+dzis+"' and wazne = 1";
			Statement stwazne = myConn.createStatement();
			ResultSet rsWazne = stwazne.executeQuery(sqlwazne);
			int p = 0;
			while(rsWazne.next()) {
				p = rsWazne.getInt(1);
			}
			rsWazne.close();stwazne.close();
			
			//jeœli s¹ jakieœ pilne detale z SACA
			if(p!=0) {
				//pierwsza strona = tabelka pilnych z saca (saca mechaniczne)
				Paragraph wazne = new Paragraph("Zapotrzebowanie na produkcjê FAT: \n \n",ffont);
				
				ListMech.add(wazne);
				PdfPTable wazneSaca = new PdfPTable(9);
				addHeaderW(wazneSaca);
				float widthsWazne[] = new float[] {10, 10, 12, 14, 8, 8, 8, 10, 5};
				String sqlwazne2 = "Select projekt, NrZamowienia2, kodartykulu, nazwaartykulu, sum(pozostalodoprojektu) as pozostalodoprojektu, pozostalonazamowieniu, jednostka, cel, typ from " + 
						" (Select projekt, NrZamowienia2, kodartykulu, nazwaartykulu, pozostalodoprojektu, pozostalonazamowieniu, jednostka, cel, saca.typ, calendar.dataprodukcji from saca "
						+ "join calendar on saca.projekt = calendar.nrMaszyny "
						+ "where datadodania = '"+dzis+"' and wazne = 1 "
								+ "group by NrZamowienia2, kodartykulu "
								+ "order by calendar.dataProdukcji asc, projekt asc, saca.typ desc, saca.kodartykulu) T group by KodArtykulu order by T.dataprodukcji";
				Statement Parts = myConn.createStatement();
				ResultSet rsParts = Parts.executeQuery(sqlwazne2);
				while(rsParts.next()) {
					addCell(wazneSaca, rsParts.getString("projekt"), ffont);
					addCell(wazneSaca, rsParts.getString("NrZamowienia2"), ffont);
					addCell(wazneSaca, rsParts.getString("kodartykulu"), ffont);
					addCell(wazneSaca, rsParts.getString("nazwaartykulu"), ffont);
					addCell(wazneSaca, rsParts.getString("pozostalodoprojektu"), ffont);
					addCell(wazneSaca, rsParts.getString("pozostalonazamowieniu"), ffont);
					addCell(wazneSaca, rsParts.getString("jednostka"), ffont);
					addCell(wazneSaca, rsParts.getString("cel"), ffont);
					addCell(wazneSaca, rsParts.getString("typ"), ffont);
				}
				rsParts.close();Parts.close();
				wazneSaca.setWidthPercentage(100);
				wazneSaca.setWidths(widthsWazne);
				wazneSaca.setHeaderRows(1);
				wazneSaca.setHorizontalAlignment(Element.ALIGN_CENTER);
				wazneSaca.setHorizontalAlignment(Element.ALIGN_CENTER);	
				
				ListMech.add(wazneSaca);
				ListMech.newPage();
			}
			
			//tworzenie listy wszystkich czesci robionych przez SACA na projekty:
			addHeader(tableM);
			addHeader(tableK);
			addHeader(table);
			Statement pobierzProjectSchedule = myConn.createStatement();
			String sql = "Select nrMaszyny, opis, klient, dataprodukcji, dataKoniecMontazu, komentarz from calendar where Zakonczone = 0 order by dataProdukcji";
			ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql);
			//dla kazdego projektu
			while(ProjectSchedule.next()){
				
				String calyNumer = ProjectSchedule.getString("nrMaszyny");
				String ProductionDate = ProjectSchedule.getString("dataProdukcji");
				String MontageFinishDate = ProjectSchedule.getString("dataKoniecMontazu");
				String ProjectName = ProjectSchedule.getString("Opis");
				String klient = ProjectSchedule.getString("klient");
				
				boolean headerM = false;
				boolean headerK = false;
				boolean header = false;
				Statement takeParts = myConn.createStatement();
				ResultSet parts = takeParts.executeQuery("SELECT * from saca where datadodania = '"+dzis+"' and projekt = '"+calyNumer+"' order by cel, kodartykulu"); 
				while(parts.next()){
					String ArticleNo = parts.getString("KodArtykulu");
					String ArticleName = parts.getString("NazwaArtykulu");
					String nrZam = parts.getString("NrZamowienia2");
					String ileDoProj = parts.getString("PozostaloDoProjektu");
					String nrZamowienia=parts.getString("nrZamowienia");
					String ileWZamowieniu = parts.getString("PozostaloNaZamowieniu");
					String jednostka=parts.getString("Jednostka");
					String dataDost = parts.getString("DataDostarczenia"); 
					String dataZam = parts.getString("DataZlozeniaZamowienia");
					String typArtykuluSACA = parts.getString("typ");
					String bon = parts.getString("cel");
					int b = parts.getInt("zmiana"); 
					//WRZUCANIE DANYCH W TABELE:
					if(typArtykuluSACA.equals("K")){
						if(!headerK){
							addProjectHeader(new String [] {calyNumer, ProjectName, ProductionDate, MontageFinishDate, klient}, tableK);
							headerK=true;
						}					
						if(!dataDost.equals(""))
						{
							addCell(tableK, nrZamowienia, b);
							if(dataZam.length()>10)
								addCell(tableK, dataZam.substring(0, 11), b);
							else
								addCell(tableK, dataZam, b);
							if(dataDost.length()>10)
								addCell(tableK, dataDost.substring(0, 11), b);
							else
								addCell(tableK, dataDost, b);
							addCell(tableK, nrZam, b);
							addCell(tableK, ArticleNo, b);
							addCell(tableK, ArticleName, b);
							addCell(tableK, ileDoProj, b);
							addCell(tableK, ileWZamowieniu, b);
							addCell(tableK, jednostka, b);
							addCell(tableK, bon, b);
							addCell(tableK,typArtykuluSACA, b);
						}
					}
					else if(typArtykuluSACA.equals("M")){
						if(!headerM){
							addProjectHeader(new String [] {calyNumer, ProjectName, ProductionDate, MontageFinishDate, klient}, tableM);
							headerM=true;
						}
						if(dataDost==null) dataDost = "";
						if(!dataDost.equals(""))
						{
							addCell(tableM, nrZamowienia, b);
							if(dataZam.length()>10)
								addCell(tableM, dataZam.substring(0, 11), b);
							else
								addCell(tableM, dataZam, b);
							if(dataDost.length()>10)
								addCell(tableM, dataDost.substring(0, 11), b);
							else
								addCell(tableM, dataDost, b);
							
							addCell(tableM, nrZam, b);
							addCell(tableM, ArticleNo, b);
							addCell(tableM, ArticleName, b);
							addCell(tableM, ileDoProj, b);
							addCell(tableM, ileWZamowieniu, b);
							addCell(tableM, jednostka, b);
							addCell(tableM, bon, b);
							addCell(tableM,typArtykuluSACA, b);
						}
					}
					
					if(!header){
						addProjectHeader(new String [] {calyNumer, ProjectName, ProductionDate, MontageFinishDate, klient}, table);
						header=true;
					}
					addCell(table, nrZamowienia, b);
					if(dataZam.length()>10)
						addCell(table, dataZam.substring(0, 11), b);
					else
						addCell(table, dataZam, b);
					if(dataDost.length()>10)
						addCell(table, dataDost.substring(0, 11), b);
					else
						addCell(table, dataDost, b);
					addCell(table, nrZam, b);
					addCell(table, ArticleNo, b);
					addCell(table, ArticleName, b);
					addCell(table, ileDoProj, b);
					addCell(table, ileWZamowieniu, b);
					addCell(table, jednostka, b);
					addCell(table, bon, b);
					addCell(table,typArtykuluSACA, b);
				}
				takeParts.close();
				if(headerM)
					addRow(tableM);
				if(headerK)
					addRow(tableK);
				if(header)
					addRow(table);
			}
			
			//formatowanie tabel:
			tableM.setWidthPercentage(100);
			tableM.setWidths(widths);
			tableM.setHeaderRows(1);
			tableM.setHorizontalAlignment(Element.ALIGN_CENTER);
			tableM.setHorizontalAlignment(Element.ALIGN_CENTER);	
			if(tableM.size()==0 ){
				Paragraph a1 = new Paragraph("Document is empty", ffont2);
				ListMech.add(a1);
			}
			else
				ListMech.add(tableM);
			ListMech.close();
			
			tableK.setWidthPercentage(100);
			tableK.setWidths(widths);
			tableK.setHeaderRows(1);
			tableK.setHorizontalAlignment(Element.ALIGN_CENTER);
			tableK.setHorizontalAlignment(Element.ALIGN_CENTER);	
			if(tableK.size()==0 ){
				Paragraph a1 = new Paragraph("Document is empty", ffont2);
				ListK.add(a1);
			}
			else
				ListK.add(tableK);
			ListK.close();
			
			table.setWidthPercentage(100);
			table.setWidths(widths);
			table.setHeaderRows(1);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);	
			if(table.size()==0 ){
				Paragraph a1 = new Paragraph("Document is empty", ffont2);
				List.add(a1);
			}
			else
				List.add(table);
			List.close();
			
			myConn.close();
		}
		catch (FileNotFoundException | DocumentException | SQLException e) {
				e.printStackTrace();
		}
		System.out.println("Koniec list SACA");
	}
	
	private static void addHeaderW(PdfPTable t){
		String [] nagl = new String[] {"Projekt", "Nr zamowienia", "Kod artykulu", "Nazwa artykulu", "Ile pilnych", "W zamowieniu", "Jednostka", "Dok¹d", "Typ"};
		for(int i = 0; i<nagl.length; i++) {
			PdfPCell cell1 = new PdfPCell(new Phrase(nagl[i], ffont));
			cell1.setMinimumHeight(30);
			cell1.setBackgroundColor(new BaseColor(255,121,0));
			cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell1);
		}
	}
	
	
	private static void addHeader(PdfPTable t){
		String [] header = new String [] {"Nr zamowienia", "Data zlozenia zamowienia", "Data dostarczenia zamowienia", "Nr zamowienia2", "Kod artykulu", "Nazwa artykulu", "Do projektu", "W zamówieniu", "Jednostka", "Dok¹d",  "Typ"};
		
		//adding header to our file
		for(int i = 0; i< header.length; i++)
		{
			PdfPCell cell1 = new PdfPCell(new Phrase(header[i], ffont));
			cell1.setMinimumHeight(30);
			cell1.setBackgroundColor(BaseColor.ORANGE);
			cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell1);
		}
	}
	
	private static void addProjectHeader(String[] dane, PdfPTable t){
		String [] header = new String [] {"Numer projektu", "Nazwa projektu", "Data produkcji czêœci", "Data koñca monta¿u", "Klient"};
		for(int j = 0; j<2; j++) {
			for(int i = 0; i<header.length; i++) {
				PdfPCell cell1;
				if(j==0) {
					cell1 = new PdfPCell(new Phrase(header[i], ffont));
				}
				else
					cell1 = new PdfPCell(new Phrase(dane[i], ffont));
				cell1.setFixedHeight(15f);
				cell1.setColspan(2);
				if(i==1)
					cell1.setColspan(3);
				cell1.setBackgroundColor(BaseColor.ORANGE);
				cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
				t.addCell(cell1);
			}
		}
	}

	private static void addRow(PdfPTable t){
		PdfPCell cell = new PdfPCell(new Phrase(" ",ffont2));
		cell.setNoWrap(true);
		cell.setColspan(11);
		t.addCell(cell);
		
	}
	
	public static void addCell(PdfPTable t, String z, int a){
		PdfPCell cell = new PdfPCell(new Phrase(z, ffont3));
		if(a!=0) {
			int green = Math.round((float)a*255/7); 
			cell.setBackgroundColor(new BaseColor(255, green, 0));
		}
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setMinimumHeight(15f);
		t.addCell(cell);
	}
	public static void addCell(PdfPTable t, String z, Font f){
		PdfPCell cell = new PdfPCell(new Phrase(z, f));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setMinimumHeight(15f);
		t.addCell(cell);
	}
	
}
