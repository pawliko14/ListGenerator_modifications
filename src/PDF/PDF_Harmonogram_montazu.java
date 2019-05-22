package PDF;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import WB.DBConnection;

import java.io.File;
import java.io.FileOutputStream;


public class PDF_Harmonogram_montazu {
	

	private static Font ffont = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 10); 
	private static Font ffont2 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 8); 
	private static Font ffont3 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 6); 
	
	public static final String IMG = "img/tick2.png";
		
	public static void create(Connection myConn){
		System.out.println("Rozpoczecie listy montazowej");
		
		//zdefiniowanie kolumn  przeznaczonych na artyku³y z '$' -> wa¿nych stategicznie. zdefiniowanie ich kolejnoœci
		String columns1[] = new String[] {"L$", "W$", "WR$", "K$", "P$", "S$", "SSZ$", "SSX$"};
		String columns2[] = new String[] {"%W$", "%K$", "%P$", "%S$"};
		
		//wykonaj analizê z³o¿eñ -> czy strategiczne elementy i z³o¿enia ze znakiem '$' s¹ zrobione / w trakcie pracy / niezaczête
		PDF_AnalizaZlozen.run(myConn);
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		Document ML = new Document(PageSize.A3.rotate(), 10, 10, 25, 25);
		ffont2.setColor(BaseColor.BLACK);
		try
		{
			String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
			String name = "Harmonogram montazu.pdf";
			File f = new File(path+name);
			if(f.exists() && !f.isDirectory())
				name = godz.format(date.getTime())+" "+name;
			PdfWriter writer = PdfWriter.getInstance(ML, new FileOutputStream(path+name));
			ML.open();
			writer.setPageEvent(new PDF_MyFooter());
			PdfPTable table = new PdfPTable(33);
			//ustawianie szerokosci
			float widths[] = new float[] { 10, 26, 49, 35, 30, 30, 10, 10, 10, 10, 10, 10, 10, 10, 20, 40, 20, 20, 20, 20, 10, 10, 10, 10, 20, 50, 30, 20, 20, 20, 20, 20, 30};
			addHeader(table);
			//addRow(1, table);
			
			//dla ka¿dego projektu
			Statement pobierzProjectSchedule = myConn.createStatement(); 
			// oryginalnie tutaj powinno byc or nrMaszyny like '5/%'
			String sql = "Select nrMaszyny, opis, klient, dataprodukcji, dataKoniecMontazu, dataKontrakt, Komentarz from calendar where (nrMaszyny like '2/%' or nrMaszyny like '6/%' or nrMaszyny like '0/%' or nrMaszyny like '5/%') and Zakonczone = 0 and wyslano <> 1 order by dataKoniecMontazu, nrMaszyny";
			ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql);
			
			//liczba porz¹dkowa na liœcie monta¿owej lp
			int lp = 0;
			
			while(ProjectSchedule.next()){
				
				//dane:
				String komentarz = ProjectSchedule.getString("Komentarz");
				String zmianaNumeru = "";
				boolean zmiana = false;
				// Zakomentowane dnia 14.03.2019, moze powodowac bledy
			//	if(komentarz.length()==8){
			//		zmianaNumeru = komentarz;
			//	    zmiana = true;
			//   }
				
				String calyNumer = ProjectSchedule.getString("nrMaszyny");
								
				String [] p = calyNumer.split("/");
				String ProjectGroup = p[0];
				String ProjectNumber = p[1];
				if(ProjectNumber.length()==6) {
				String zmianaGrupy = "";
				String zmianaNr = "";
				if(zmiana) {
				String [] p2 = zmianaNumeru.split("/");
				 zmianaGrupy = p2[0];
				 zmianaNr = p2[1];
				}
				String ShippmentDate = ProjectSchedule.getString("dataKontrakt");
				String MontageFinishDate = ProjectSchedule.getString("dataKoniecMontazu");
				String ProductionDate = ProjectSchedule.getString("dataProdukcji");
				String ProjectName = ProjectSchedule.getString("Opis");
				String klient = ProjectSchedule.getString("klient");
				int [] tab = new int[15];
				for(int i = 1; i<=14; i++) tab[i] = 0;
				
				//sprawdza ile bonów pracy (stanowisk monta¿owych) w technologii maszyny
				String sql2 = "SELECT count(*) from werkbon " +
						"where Afdeling = '"+ProjectGroup+"' and Afdelingseq='"+ProjectNumber+"'";
				if(zmiana)
					sql2 = "SELECT count(*) from werkbon " +
							"where Afdeling = '"+zmianaGrupy+"' and Afdelingseq='"+zmianaNr+"'";
				Statement TakeTechn123=myConn.createStatement();
				ResultSet technologia123 = TakeTechn123.executeQuery(sql2);
				int ileStanowisk = 0;
				while(technologia123.next()){
					ileStanowisk = technologia123.getInt("COUNT(*)");
				}
				technologia123.close();
				
				//je¿eli mamy do czynienia z projektem, który posiada technologiê LUB z projektem zleconym tylko w dziale sprzeda¿ (wtedy brak bonów bo brak uruchomienia)
				if(ileStanowisk!=0||ProjectGroup.equals("0")){
					lp++;
					PdfPCell lpCell = new PdfPCell(new Phrase(Integer.toString(lp)+".", ffont2));
					lpCell.setRowspan(2);
					lpCell.setFixedHeight(25f);
					lpCell.setHorizontalAlignment(Element.ALIGN_CENTER);
					lpCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
					table.addCell(lpCell);
					if(!ProjectGroup.equals("0"))
						addProjectHeader(table, new String [] {ProjectGroup+"/"+ProjectNumber, ProjectName, klient, MontageFinishDate, ProductionDate});
					else
						addProjectHeader(table, new String [] {ProjectNumber, ProjectName, klient, MontageFinishDate, ProductionDate});
					List<String> techn = new ArrayList<String>();
					List<String> statusy = new ArrayList<String>();
					List<String> colors = new ArrayList<String>();
					
					//projekty uruchomione w produkcji - 2 i 6
					if(ProjectGroup.equals("2")||ProjectGroup.equals("6")){
						//robienie listy z zlozeniami/artyku³ami z '$' -> wa¿ne strategicznie
						List<Zlozenie> Zloz = new ArrayList<Zlozenie>();
						Statement myStmt4 = myConn.createStatement();
						//bierzemy wszystkie zlozenia/artyku³y z '$' -> wa¿ne strategicznie
						String sql07 = "Select * from Zlozenia where projekt = '"+calyNumer+"'";
						ResultSet a = myStmt4.executeQuery(sql07);
						while(a.next()){
							String t = a.getString("typ");
							String zam = a.getString("nrZamowienia");
							//nowy obiekt zlozenia
							Zlozenie b = new Zlozenie(t, zam, a.getString("kodArtykulu"), a.getString("nazwa"), a.getInt("status"), a.getInt("doWydania102"), a.getInt("doWydania103"));
							Zloz.add(b);
						}
						myStmt4.close();
						
						
						colors.clear();
						
						//pobieranie informacji o zlozeniach (najpierw pierwsze 8) czyli columns1
						for(int i = 0; i<8; i++){
							//ze wzglêdu na mo¿liwoœæ obecnoœci wiêcej ni¿ jednego artyku³u danego typu -> np sanie suportu + przeróbka sañ suportu, które oznaczane s¹ tym samym znacznikiem np. SSX$ 
							//wprowadzony jest tymczasowy status s
							int status_tmp = -1;
							
							for(Zlozenie a1 : Zloz){
								//porównanie wymaganego typu w kolumnie z list¹ zanalizowanych z³o¿eñ dla tego projektu
								if(a1.getTyp().equals(columns1[i])){
									if(status_tmp==-1)
										status_tmp=a1.getStatus();
									else{
										if(a1.getStatus()==10){
											if(status_tmp==90)
												status_tmp=20;
										}
										else if(a1.getStatus()==20)
											status_tmp=20;
										else if(a1.getStatus()==90){
											if(status_tmp==10)
												status_tmp=20;
										}
									}
								}
							}
							if(status_tmp==0)
								status_tmp=90;
							//dodanie komórki oznaczaj¹cej dany artyku³
							PdfPCell komorka = new PdfPCell();
							komorka.setFixedHeight(24f);
							komorka.setRowspan(2);
							if(status_tmp==10)
								komorka.setBackgroundColor(BaseColor.WHITE);
							if(status_tmp==20)
								komorka.setBackgroundColor(new BaseColor(255, 255, 0));
							if(status_tmp==90) komorka.setBackgroundColor(new BaseColor(18, 162, 25));
							if(status_tmp==-1){
								komorka.setCellEvent(new PDF_Diagonal());
								komorka.setBackgroundColor(new BaseColor(225, 225, 225));
							}
							table.addCell(komorka);
						}
						
						//dla 14 grup technologicznych pobieramy stanowiska: (skrobanie, takt 1, 2 ,3 itd.)
						for (int i = 1; i<=14; i++){
							
							ResultSet WorkplaceCodes;
							Image img; Image img2; 
							try{
								img= Image.getInstance(PDF_Harmonogram_montazu.class.getClassLoader().getResource("tick2.png"));
								img2 = Image.getInstance(PDF_Harmonogram_montazu.class.getClassLoader().getResource("tick2.png"));
							}
							catch(NullPointerException e) {
								System.out.println("Nie znaleziono obrazu ticka");
								img = null;
								img2 = null;
							}
							//dodajemy info o zlozeniach (kolejnych czterech -wrzeciennik, konik, skrzynka, zamek - po 6. grupie stanowisk - po takcie 3)
							if(i==7){
								
								for(int j = 0; j<4; j++){
									int s = -1;
									//zmienna bon - mowi o tym, czy wykonane sa wszystkie czesci na dane zlozenie
									int bon102 = 0, bon103 = 0;
									int x = 0;
									for(Zlozenie a1 : Zloz){
										if(Zloz.contains("190502"))
										{
											 x = 1;
										}
										else
										{
											x = 2;
										}
										if(x == 1 || x == 2 || x == 0)
										{
											x = 10;
										}
										if(a1.getTyp().equals(columns2[j])){
											if(s==-1)
												s=a1.getStatus();
											else{
												if(a1.getStatus()==10){
													if(s==90)
														s=20;
												}
												else if(a1.getStatus()==20)
													s=20;
												else if(a1.getStatus()==90){
													if(s==10)
														s=20;
												}
											}
											//jesli bon = 0 -> jeszcze cos sie robi
											// jesli bon = 1 -> ca³e z³o¿enie mo¿e zostaæ wydane TICK 
											bon102 = a1.getBon102();
											bon103 = a1.getBon103();
										}
									}
									
									if(s==0)
										s=90;
									//dodaj komórkê z informacj¹ o z³o¿eniu
									PdfPCell komorka = new PdfPCell();
									komorka.setFixedHeight(24f);
									komorka.setRowspan(2);
									if(s==10){
										komorka.setBackgroundColor(BaseColor.WHITE);
									}
									if(s==20){
										komorka.setBackgroundColor(new BaseColor(255, 255, 0));
									}
									if(s==90){
										komorka.setBackgroundColor(new BaseColor(18, 162, 25));
									}
									if(s==-1){
										komorka.setCellEvent(new PDF_Diagonal());
										komorka.setBackgroundColor(new BaseColor(225, 225, 225));
									}
									if(s==10||s==20){
										if(bon102==1 && bon103==1){
											if(img!=null) {
												img.setAlignment(Element.ALIGN_TOP);
												komorka.addElement(img);
											}
											Paragraph p1 = new Paragraph(2, "\n");
											komorka.addElement(p1);
											if(img2!=null){
												img2.setAlignment(Element.ALIGN_BOTTOM);
											//komorka.setVerticalAlignment(Element.ALIGN_MIDDLE);
												komorka.addElement(img2);
											}
										}
										else if(bon102==1){
											if(img!=null) {
												komorka.addElement(img);
												komorka.setVerticalAlignment(Element.ALIGN_TOP);
											}
										}
										else if(bon103==1){
											if(img2!=null) {
												komorka.addElement(img2);
												komorka.setVerticalAlignment(Element.ALIGN_BOTTOM);
											}
										}
									}
									
									table.addCell(komorka);
								}
							}
							
							//pobieranie mozliwych workplace'ow w danej i-tej grupie taktu
							Statement myStmt = myConn.createStatement();
							WorkplaceCodes = myStmt.executeQuery("Select WorkplaceCode from montaz " +
									"where sequence = '"+i+"' group by workplacecode");
							
							while(WorkplaceCodes.next()){
								//patrzymy w technologii projektu czy dane stanowisko wystepuje
								
								String sql3 = "SELECT seq, status from werkbon " +
										"where Afdeling = '"+ProjectGroup+"' and Afdelingseq like '"+ProjectNumber+"%' and werkpost = '"+WorkplaceCodes.getString("WorkplaceCode")+"'";
								if(zmiana)
									sql3 = "SELECT seq, status from werkbon " +
											"where Afdeling = '"+zmianaGrupy+"' and Afdelingseq like '"+zmianaNr+"%' and werkpost = '"+WorkplaceCodes.getString("WorkplaceCode")+"'";

								//pobierz technologie dla projektu
								Statement TakeTechn = myConn.createStatement();
								ResultSet technologia = TakeTechn.executeQuery(sql3);
								
								//jesli wystepuje to je pokazujemy
								while(technologia.next()){
									//dla przypadku kiedy mowimy o malarnii:
									if(WorkplaceCodes.getString("WorkplaceCode").equals("MM01")){
										if(i == 2){
											if(technologia.getInt("seq")<100){
												techn.add(WorkplaceCodes.getString("WorkplaceCode"));
												statusy.add(technologia.getString("status"));
											}	
										}
										else
											if(technologia.getInt("seq")>=100){
												techn.add(WorkplaceCodes.getString("WorkplaceCode"));
												statusy.add(technologia.getString("status"));
											}
									}
									//dla kazdego innego stanowiska:
									else{
										statusy.add(technologia.getString("status"));
										techn.add(WorkplaceCodes.getString("WorkplaceCode"));
									}
								}
								TakeTechn.close();
							}
							myStmt.close();
							
							//ZAKOÑCZENIE GÓRNEJ LINII TECHNOLOGII DLA PROJEKTU
							//na tym poziomie mamy poukladana technologie do danego taktu: techn i statusy
							int ileKomorek = techn.size();
							if(ileKomorek>1){
								//jeœli za³adunek
								if(i==13){
									int status = 0;
									for(int a1=0; a1<techn.size(); a1++){
										if(status==0) status = Integer.parseInt(statusy.get(a1));
										if(status==10 && !statusy.get(a1).equals("10")) status=20;
										if(status==90 && !statusy.get(a1).equals("90")) status=20;
									}
									//dodanie jednej komórki oznaczaj¹cej koñcowe karty pracy dla za³adunku
									PdfPCell cell = new PdfPCell(new Phrase("MT16", ffont2));
									cell.setFixedHeight(14f);
									cell.setHorizontalAlignment(Element.ALIGN_CENTER);
									cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
									cell.setBorder(Rectangle.RIGHT);
									
									if(status==10)	cell.setBackgroundColor(BaseColor.WHITE);
									if(status==20){
										cell.setBackgroundColor(new BaseColor(255, 255, 0));
										tab[i] = 1;
									}
									if(status==90){
										cell.setBackgroundColor(new BaseColor(18, 162, 25));
										tab[i] = 1;
									}
									table.addCell(cell);
								}
								else{
									PdfPTable techn2 = new PdfPTable(ileKomorek);
									for(int a1=0; a1<techn.size(); a1++){
										//dodanie wszystkich komórek odpowiadaj¹cych kartom pracy
										PdfPCell cell4 = new PdfPCell(new Phrase(techn.get(a1), ffont2));
										cell4.setFixedHeight(14f);
										cell4.setHorizontalAlignment(Element.ALIGN_CENTER);
										cell4.setVerticalAlignment(Element.ALIGN_MIDDLE);
										cell4.setBorder(Rectangle.RIGHT);
										
										if(statusy.get(a1).equals("10"))	cell4.setBackgroundColor(BaseColor.WHITE);
										if(statusy.get(a1).equals("20")){
											tab[i] = 1;
											cell4.setBackgroundColor(new BaseColor(255, 255, 0));
										}
										if(statusy.get(a1).equals("90")){
											tab[i] = 1;
											cell4.setBackgroundColor(new BaseColor(18, 162, 25));
										}
							
										techn2.addCell(cell4);
									}
									//do glownej tabeli dodajemy tabelke ze stanowiskami (technologia) dla danej grupy
									table.addCell(techn2);
								}
							}
							//jesli tylko jeden bon pracy dla taktu
							else if(ileKomorek == 1)
							{
								PdfPCell cell4 = new PdfPCell(new Phrase(techn.get(0), ffont2));
								cell4.setFixedHeight(14f);
								cell4.setHorizontalAlignment(Element.ALIGN_CENTER);
								cell4.setVerticalAlignment(Element.ALIGN_MIDDLE);
								
								if(statusy.get(0).equals("10"))	cell4.setBackgroundColor(BaseColor.WHITE);
								if(statusy.get(0).equals("20")){
									tab[i] = 1;
									cell4.setBackgroundColor(new BaseColor(255, 255, 0));
								}
								if(statusy.get(0).equals("90")){
									tab[i] = 1;
									cell4.setBackgroundColor(new BaseColor(18, 162, 25));
								}
								
								table.addCell(cell4);
							}
							//jesli zadnego bonu
							else if(techn.isEmpty()){
								PdfPCell cell4 = new PdfPCell(new Phrase(" ", ffont2));
								cell4.setFixedHeight(14f);
								cell4.setHorizontalAlignment(Element.ALIGN_CENTER);
								cell4.setVerticalAlignment(Element.ALIGN_MIDDLE);
								table.addCell(cell4);
							}
							techn.clear();
							statusy.clear();
							
							//sprawdzamy bony magazynowe dla danej grupy technologii - czy wydano wszystko?
							Statement statement3 = myConn.createStatement();
							ResultSet AssemblyNumbers = statement3.executeQuery("Select AssemblyNo from montaz "+
									"where sequence= '"+i+"' group by AssemblyNo");
							boolean not102 = false;
							boolean not103 = false;
							while(AssemblyNumbers.next()) {
								
								String sql4 = "Select StorenoteType from NotFinishedStorenotes where " +
										"ProjectGroup = '"+Integer.parseInt(ProjectGroup)+"' AND " +
										"ProjectNumber like '"+Integer.parseInt(ProjectNumber)+ "%' AND MontageNumber = '"+AssemblyNumbers.getInt("AssemblyNo")+"'";
								if(zmiana)
									sql4 = "Select StorenoteType from NotFinishedStorenotes where " +
											"((ProjectGroup = '"+Integer.parseInt(zmianaGrupy)+"' AND " +
											"ProjectNumber like '"+Integer.parseInt(zmianaNr)+ "%') or " +
											"(ProjectGroup = '"+Integer.parseInt(ProjectGroup)+"' AND " +
											"ProjectNumber like '"+Integer.parseInt(ProjectNumber)+ "%')) " +
											"AND MontageNumber = '"+AssemblyNumbers.getInt("AssemblyNo")+"'";
								Statement myStmt2 = myConn.createStatement();
								ResultSet notFinished = myStmt2.executeQuery(sql4);
								while(notFinished.next()){
									if(notFinished.getInt("StorenoteType")==102){
										not102=true;
									}
									else
										not103=true;
								}
								myStmt2.close();
							}
							statement3.close();
							//dodajemy dwa elementy do listy 'colors' -> W jesli ma byc nieskonczony (bialy) bon, G jesli niebieskawy
							if(not102) colors.add("W");
							else colors.add("G");
							if(not103) colors.add("W");
							else colors.add("G");
						}
						
						PdfPCell komorka = new PdfPCell(new Phrase (ShippmentDate, ffont2));
						komorka.setFixedHeight(24f);
						komorka.setRowspan(2);
						komorka.setHorizontalAlignment(Element.ALIGN_CENTER);
						komorka.setVerticalAlignment(Element.ALIGN_MIDDLE);
						table.addCell(komorka);
						//dodanie rzedu z komorkami odpowiadajacymi bonom magazynowym
						for (int j = 1; j<=14; j++){
							//dla kazdej grupy taktowej dodajemy tabele dwukomorkowa
							PdfPTable colors2 = new PdfPTable(2);
							PdfPCell kom102;
							if(j==9||j==2){
								kom102 = new PdfPCell(new Phrase(" "));
								table.addCell(kom102);
							}
							else{
								if(colors.get((j*2)-2).equals("W")){
									kom102 = new PdfPCell(new Phrase("MCG", ffont3));
									if(tab[j]==1)
										kom102.setBackgroundColor(BaseColor.RED);
								}
								else if(colors.get((j*2)-2).equals("G")){
									kom102 = new PdfPCell(new Phrase());
									if(j!=2) kom102.setBackgroundColor(new BaseColor(154, 208, 228));	
								}
								else{
									kom102 = new PdfPCell(new Phrase());
								}
								kom102.setFixedHeight(10f);
								kom102.setHorizontalAlignment(Element.ALIGN_CENTER);
								kom102.setVerticalAlignment(Element.ALIGN_MIDDLE);
								kom102.setBorder(Rectangle.RIGHT);
								colors2.addCell(kom102);
								
								PdfPCell kom103;
								
								if(colors.get((j*2)-1).equals("W")){
									kom103 = new PdfPCell(new Phrase("K", ffont3));
									if(tab[j]==1)
										kom103.setBackgroundColor(BaseColor.RED);
								}
								else if(colors.get((j*2)-1).equals("G")) {
									kom103 = new PdfPCell(new Phrase());
									if(j!=2) kom103.setBackgroundColor(new BaseColor(154, 208, 228));
								}
								else{
									kom103 = new PdfPCell(new Phrase());
								}
								kom103.setFixedHeight(10f);
								kom103.setHorizontalAlignment(Element.ALIGN_CENTER);
								kom103.setVerticalAlignment(Element.ALIGN_MIDDLE);
								kom103.setBorder(Rectangle.NO_BORDER);
								colors2.addCell(kom103);
								//mamy stworzona tabele z dwoma komorkami odpowiadajacymi 102 i 103
								//dodajemy do glownej tabeli
								table.addCell(colors2);
							}
						}
					}
					//if project isn't 2/... -> jeœli projekt jest nieuruchomiony na produkcji
					else
					{
						for(int i = 0; i<26; i++){
							PdfPCell a = new PdfPCell(new Phrase(""));
							a.setRowspan(2);
							table.addCell(a);
						}
						PdfPCell komorka = new PdfPCell(new Phrase (ShippmentDate, ffont2));
						komorka.setFixedHeight(24f);
						komorka.setRowspan(2);
						komorka.setHorizontalAlignment(Element.ALIGN_CENTER);
						komorka.setVerticalAlignment(Element.ALIGN_MIDDLE);
						table.addCell(komorka);
					}
				} }
			}
			pobierzProjectSchedule.close();
			table.setWidthPercentage(100);
			table.setWidths(widths);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setHeaderRows(1);
			table.completeRow();
			table.completeRow();
			if(table.size()==0 ){
				Paragraph a = new Paragraph("Document is empty", ffont3);
				ML.add(a);
			}
			else
				ML.add(table);

			ML.close();
			System.out.println("koniec montazowej");
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}

	public static void create(boolean nowaNotFinished) throws SQLException{
		
		Connection myConn = DBConnection.dbConnector();
		if(nowaNotFinished){
			//pocz¹tkowo analiza bonów magazynowych - czy wszystko jest wydane?
			System.out.println("Montazowa: przygotowanie nieskonczonych bonów");
			Statement createTable;
			createTable = myConn.createStatement();
			createTable.execute("delete from NotFinishedStorenotes");
			createTable.close();
			//dla ka¿dego projektu
			Statement pobierzProjectSchedule = myConn.createStatement();
			String sql = "Select nrMaszyny, opis, klient, dataprodukcji, dataKoniecMontazu, Komentarz from calendar order by dataProdukcji";
			ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql);
			
			while(ProjectSchedule.next()){
				String calyNumer = ProjectSchedule.getString("nrMaszyny");
				String [] p = calyNumer.split("/");
				String ProjectGroup = p[0];
				String ProjectNumber = p[1];
				
				//pobiera wszystkie niewydane czêœci z bonów
				Statement takeTaktList = myConn.createStatement();
				ResultSet TaktsList;
				String Columns = "LEVERANCIER, ORDERNUMMER, ARTIKELCODE, ARTIKELOMSCHRIJVING, BESTELD, BESTELEENHEID, GELEVERD, BOSTDEH, CFSTOCK, MONTAGE, MONTAGEOMSCHRIJVING";
				TaktsList = takeTaktList.executeQuery("SELECT "+Columns+" from storenotesdetail " +
						"where projectnummer like '"+calyNumer+"%' and BOSTDEH <> 0 " +
						"order by MONTAGE asc, leverancier asc, ordernummer asc");
				
				while(TaktsList.next()){
					String StoreNoteType = TaktsList.getString("LEVERANCIER");
					Statement myStmt = myConn.createStatement();
					ResultSet czyDodano;
					String sql1 = "SELECT montagenumber FROM NotFinishedStorenotes WHERE ProjectGroup = '"+ProjectGroup+"' AND " +
							"ProjectNumber = '"+ProjectNumber+ "' AND MontageNumber = "+TaktsList.getInt("Montage")+ " AND StoreNoteType = "+ TaktsList.getInt("Leverancier");
					czyDodano = myStmt.executeQuery(sql1);
					if(!czyDodano.next()){
						//jeœli jakiœ bon ma niewydane czêœci to dodaje informacjê do tabeli "notfinishedstorenotes" 
								myStmt.executeUpdate("INSERT INTO NotFinishedStorenotes VALUES" +
								" ("+Integer.parseInt(ProjectGroup)+", " +
								Integer.parseInt(ProjectNumber)+", "+
								TaktsList.getInt("Montage")+", '"+ TaktsList.getString("Montageomschrijving")+"', "+
								StoreNoteType+")");
					}
					myStmt.close();
				}
				takeTaktList.close();
			}
		}
		
		//wywo³anie listy monta¿owej
		create(myConn);
		myConn.close();
		
	}
	
	//metoda dodaj¹ca nag³ówek
	private static void addHeader(PdfPTable t){
	
		String komorki[] = new String[] {" ", "Projekt", "Opis", "Klient", "Data koñca monta¿u", "Data produkcji czêœci", "£o¿e", "Korpus wrzec", "Wrzeciono", "Korpus konika", "Korpus skrz.pos.", "Korpus zamka", "Sanki", "Szuflada", "Skrêcanie", "Malowanie", "Skrobanie", "Takt 1: wspornik silnika", "Takt 2: oœ Z", "Takt 3: oœ X", "Wrzeciennik", "Konik", "Skrz. posuwów", "Zamek", "Takt 4: geometria, skrobanie konika, wrzec", "Takt 5: przyg do monta¿u blach, silnik g³ówny", "Malowanie os³on", "G³owica", "Hydr", "Takt 6", "Monta¿ koñcowy", "Za³adunek", "Data wysy³ki"};
		for(int i = 0; i<33; i++){
			PdfPCell cell = new PdfPCell(new Phrase(komorki[i], ffont));
			if(i>5 && i<32) cell.setRotation(90);
			cell.setFixedHeight(80f);
			cell.setBackgroundColor(BaseColor.ORANGE);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			if((i>5&&i<14)||(i>19&&i<25))
				cell.setVerticalAlignment(Element.ALIGN_TOP);
			else
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell);
		}
	}
	
	//metoda dodaj¹ca nag³ówek projektu
	private static void addProjectHeader(PdfPTable t, String [] dane){
		
		for(int i = 0; i< dane.length; i++) {
			PdfPCell cell = new PdfPCell(new Phrase(dane[i], ffont2));
			cell.setRowspan(2);
			cell.setFixedHeight(25f);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell);
		}
	}
	
	//wewnêtrzna klasa z³o¿enia
	private static class Zlozenie{
		
		private String typ;
		private String nrZamowienia;
		private String kodArtykulu;
		private String nazwa;
		private int bon102, bon103;
		private int status;
		
		public String getTyp() {
			return typ;
		}
		public void setTyp(String typ) {
			this.typ = typ;
		}
		public void setNrZamowienia(String nrZamowienia) {
			this.nrZamowienia = nrZamowienia;
		}
		public void setKodArtykulu(String kodArtykulu) {
			this.kodArtykulu = kodArtykulu;
		}
		public void setNazwa(String nazwa) {
			this.nazwa = nazwa;
		}
		public int getStatus() {
			return status;
		}
		public void setStatus(int status) {
			this.status = status;
		}
		public int getBon102() {
			return bon102;
		}
		public int getBon103() {
			return bon103;
		}
		public void setBon102 (int f){
			this.bon102 = f;
		}
		public void setBon103 (int g){
			this.bon103 = g;
		}
		public  Zlozenie(String a, String b, String c, String d, int e, int f, int g){
			setTyp(a);
			setNrZamowienia(b);
			setKodArtykulu(c);
			setNazwa(d);
			setStatus(e);
			setBon102(f);
			setBon103(g);
		}
		
	}
	
   
}



