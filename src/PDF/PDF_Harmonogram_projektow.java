package PDF;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import WB.DBConnection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class PDF_Harmonogram_projektow {

	//czcionka; polaczenie;
	private static Font ffont =  FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 10);
	private static Connection myConn;
	
	public PDF_Harmonogram_projektow() throws SQLException {
		System.out.println("Tworzy harmonogram projektów");
		fromPartsoverview();
		fromWerkbons();
		checkProjectSchedule();
		System.out.println("Sprawdzilo project schedule");
		findChangeOfNumber();
		System.out.println("Sprawdzilo zmiane numeru");
		checkTypes();
		System.out.println("Sprawdzilo typy");
		fromVerkoop();
		checkSent();
		myConn.close();
		System.out.println("Zakoñczy³o tworzenie project schedule");
		
		dodanieCeny();
	}
	
	private void dodanieCeny() throws SQLException
	{
		myConn = DBConnection.dbConnector();
		
		//Take all projects from actual Calendar 2/ or 5/ or 6/ or 0/
				String sql = "select NrMaszyny from calendar where ( NrMaszyny like '2/%' or NrMaszyny like '5/%'\r\n" + 
						"or NrMaszyny like '6/%' or NrMaszyny like '0/%' )and cena = 0 ";
				Statement takeDate = myConn.createStatement();
				ResultSet r = takeDate.executeQuery(sql);
				
				// keeps project like 180525...
				ArrayList<String> Machine_list_without_prices = new ArrayList<String>();
				// keeps project like 2/180525...
				ArrayList<String> Machine_list_without_prices_plus_leverancier = new ArrayList<String>();

				while(r.next()) {
					String machine = r.getString("NrMaszyny");		
					Machine_list_without_prices_plus_leverancier.add(machine);
					
					String[] parts = machine.split("/");
					String part2 = parts[1]; 
					
					Machine_list_without_prices.add(part2);
					
				}			
		ArrayList<ArrayList<String>> Machine_list_with_prices = new ArrayList<ArrayList<String>>();

				
			for(int i = 0 ; i < Machine_list_without_prices.size(); i++)
			{
				//Jurgens sql
				String sql2 = "select (select sum(BEDRAG) from betaalconditie where klantnr = verkoop.KLANTNR and BESTELLINGNR = verkoop.BESTELLINGNR) as price,\r\n" + 
						"(select CFMUNT from betaalconditie where klantnr = verkoop.KLANTNR and BESTELLINGNR = verkoop.BESTELLINGNR and VOLGNUMMER = '1') as munt\r\n" + 
						"from verkoop where SERIENUMMER ='"+Machine_list_without_prices.get(i)+"'";
				Statement takeDate2 = myConn.createStatement();
				ResultSet result = takeDate2.executeQuery(sql2);
				
			System.out.println(" Machine_list_without_prices.get(" +i + "): " + Machine_list_without_prices.get(i));

				
				// co sie stanie jak bedzie empty?
				while(result.next())
				{
					ArrayList<String> row = new ArrayList<String>(3);

					String machine =Machine_list_without_prices_plus_leverancier.get(i);
					String price = result.getString("price");
					String munt = result.getString("munt");
					
					row.add(machine); row.add(price);row.add(munt);
					
					Machine_list_with_prices.add(row);
				}
			}
			
			int siz = Machine_list_with_prices.size();
			System.out.println("Size of list  of machines from Jurgens sql" + siz);
			
			for(int i = 0 ; i < Machine_list_with_prices.size();i++)
				System.out.println("Machine_list_with_prices ["+i+" ] " + Machine_list_with_prices.get(i));

			
			// usuniecie nulli z listy  - nie dziala przy liscie z listy
			//Machine_list_with_prices.removeAll(Collections.singletonList(null));
			// dalem 4 razy bo nie usunelo za pierwszym razem ( powinno sie zrobic zeby 
			// usuwalo kiedy sa jescze nulle, aaaalee.. )
			for(int x = 0; x < 4; x++)
			{
				for(int i = 0 ; i < Machine_list_with_prices.size();i++)
				{
					if(Machine_list_with_prices.get(i).contains(null))
					Machine_list_with_prices.remove(i);
				}
			}
			for(int i = 0 ; i < Machine_list_with_prices.size();i++)
				System.out.println("Machine_list_with_prices [ "+i+"] without nulli " + Machine_list_with_prices.get(i));
			
			// posiadajac juz kompletna liste machine_list_with_prices mozna
			// zrobic update Calendar - testowo
			
			System.out.println("Machine_list_with_prices 1 wartosc:  " + Machine_list_with_prices.get(5).get(0));
			System.out.println("Machine_list_with_prices 2 wartosc:  " + Machine_list_with_prices.get(5).get(1));
			System.out.println("Machine_list_with_prices 3 wartosc:  " + Machine_list_with_prices.get(5).get(2));

			
			for(int i = 0 ; i< Machine_list_with_prices.size() ; i++)
			{
				PreparedStatement UpdateProject = myConn.prepareStatement("UPDATE calendar set Cena = ?, Waluta = ? WHERE NrMaszyny ='"+Machine_list_with_prices.get(i).get(0)+"'");
				UpdateProject.setString(1, Machine_list_with_prices.get(i).get(1));
				UpdateProject.setString(2, Machine_list_with_prices.get(i).get(2));
				UpdateProject.executeUpdate();
				UpdateProject.close();
			}
			System.out.println("MZ updatowano!");

			myConn.close();
	}
	
	private void checkSent() throws SQLException {
		//metoda ustawia parametr Wyslano = 1 w kalendarzu
		//gdy projekt jest otwarty (w dziale Produkcja w HacoSofcie) AND
		//gdy zamówienie sprzeda¿owe zostaje zamkniête AND
		//gdy data kontraktowa (ta co pojawia siê na dokumencie WZ gdy maszyna wyje¿d¿a) jest starsza ni¿ dzieñ *dzisiaj*
		
		Calendar today = Calendar.getInstance();
		SimpleDateFormat data = new SimpleDateFormat("yyyy-MM-dd");
		
		myConn = DBConnection.dbConnector();
		Statement a = myConn.createStatement();
		a.executeUpdate("update calendar " + 
				"left join bestelling on bestelling.LEVERANCIERORDERNUMMER = NrMaszyny " + 
				"left join verkoop on verkoop.SERIENUMMER = bestelling.ORDERNUMMER " + 
				"set Wyslano = 1 " + 
				"where NrMaszyny LIKE '2/%' AND WYslano = 0 AND KLIENT IS NOT NULL AND DATAKONTRAKT <> '' AND ZAKONCZONE = 0 and verkoop.statuscode = 'H' and datakontrakt < '"+data.format(today.getTime())+"'");
		a.executeUpdate("update calendar " + 
				"left join bestelling on bestelling.LEVERANCIERORDERNUMMER = NrMaszyny " + 
				"left join verkoop on verkoop.SERIENUMMER = bestelling.ORDERNUMMER " + 
				"set Wyslano = 0 " + 
				"where NrMaszyny LIKE '2/%' AND WYslano = 1 AND KLIENT IS NOT NULL AND DATAKONTRAKT <> '' AND ZAKONCZONE = 0 and verkoop.statuscode = 'H' and datakontrakt >= '"+data.format(today.getTime())+"'");
		a.close();
		myConn.close();
		
	}

	private static void fromPartsoverview() throws SQLException {
		//stworzenie polaczenia z baza danych
		myConn = DBConnection.dbConnector();
		
		//SQL z partsoverview - bazujemy na danych z GTT
		String sql = "SELECT OrderNo, Description from partsoverview " +
				"where Description <>'subproject' " +
				"group by OrderNo ";
		Statement takeDate = myConn.createStatement();
		ResultSet r = takeDate.executeQuery(sql);
		while(r.next()){
			
			//bierzemy dane z GTT: projekt, opis
			String projekt = r.getString("OrderNo");
			String opis = r.getString("Description");
			double cena = 0;
			String waluta = "";
			String []p = null;
			p = projekt.split("/");
			String referencje = "", dataProdukcji="", zadanyTerminMark="", dataKoniecMontazu="", dataKontrakt="";
			
			//parametr other -> czy zamówienie inne ni¿ na maszynê?
			boolean other = true;
			Statement takeProjectSchedule = myConn.createStatement();
			
			//zderzamy informacje z partsoverview z informacjami z HacoSofta. Sprawdzamy tabele bestelling/bestellingdetail czyli dzial produkcji oraz
			//oraz sprawdzamy tabele verkoop czyli sprzedaz, poniewaz najpierw pojawia siê zlecenie sprzedazowe!!
			ResultSet rs = takeProjectSchedule.executeQuery("SELECT bestelling.REFERENTIE, bestelling.LEVERDATUM, bestellingdetail.leveringsdatumvoorzien as Produkcji, verkoop.leverdatum_gevraagd as zadany, verkoop.leverdatum_bevestigd as kontrakt, verkoop.verkoopprijs as cena, verkoop.munt as waluta FROM bestelling "
					+ " left outer join bestellingdetail on bestelling.leverancier =  bestellingdetail.leverancier and  bestelling.ordernummer =  bestellingdetail.ordernummer "
					+ " left outer join verkoop on bestelling.ordernummer = verkoop.serienummer " +
					" where bestelling.leverancier = '"+p[0]+"' and bestelling.ordernummer = '"+p[1]+"' AND VERKOOP.MACHINEBESTELLING = 1" );
					
			while(rs.next()){
				//jeœli znalaz³o dane w dziale sprzeda¿ jako zlecenie na maszynê to other == false
				other = false;
				//referencje z projektu produkcyjnego
				referencje = rs.getString("Referentie");
				//if(referencje.equals("null")) referencje = "";
				
				//data produkcji brana z dzia³u produkcja: leveringsdatumvoorzien
				dataProdukcji = rs.getString("Produkcji");
				
				//if(p[0].equals("2") && p[1].length()>6)
				//	projekt = projekt.substring(0, 8);
				
				//zadany termin -> to co marketing wprowadza na zleceniu produkcyjnym na kiedy ma byc zrobiony
				zadanyTerminMark = rs.getString("zadany");
				
				//koncowa data projektu produkcyjnego
				dataKoniecMontazu = rs.getString("LEVERDATUM");
				
				//potwierdzony termin klientowi (dzia³ sprzeda¿)
				dataKontrakt = rs.getString("kontrakt");
				
				//sumujemy cenê ca³ego zamówienia opatrzonego danym numerem seryjnym (podtrzymki, aneksy, dodatki, maszyna)
				cena += rs.getDouble("cena");
				
				//waluta
				waluta += rs.getString("waluta");

			}
			takeProjectSchedule.close();
			rs.close();
			//jeœli nie znalaz³o danych w dziale sprzeda¿ (machinebestelling == 1)
			if(other) {
				//sprawdz szczegoly zlecenia w dziale produkcja
				Statement checkOther = myConn.createStatement();
				ResultSet rsOther = checkOther.executeQuery("SELECT bestelling.REFERENTIE, bestelling.LEVERDATUM, bestellingdetail.leveringsdatumvoorzien as Produkcji FROM bestelling "
						+ " left outer join bestellingdetail on bestelling.leverancier =  bestellingdetail.leverancier and  bestelling.ordernummer =  bestellingdetail.ordernummer " +
						" where bestelling.leverancier = '"+p[0]+"' and bestelling.ordernummer = '"+p[1]+"'" );
				while(rsOther.next()){
					other = false;
					referencje = rsOther.getString("Referentie");
					//if(referencje.equals("null")) referencje = "";
					dataProdukcji = rsOther.getString("Produkcji");
					
					//if(p[0].equals("2") && p[1].length()>6)
					//	projekt = projekt.substring(0, 8);
					dataKoniecMontazu = rsOther.getString("LEVERDATUM");

				}
				rsOther.close(); checkOther.close();
			}
			
			//kontrola pustych pól z systemu:
			if(zadanyTerminMark==null) zadanyTerminMark = "";
			if(dataKontrakt==null||dataKontrakt.equals("empty")) dataKontrakt = "";
			//je¿eli nikt nie wpisa³ referencji do projektu produkcyjnego, to wez ze zlecenia nazwe klienta:
			if(referencje == null && !other) {
				//SPRAWDZENIE KLIENTA I MIEJSCA GDZIE DOSTAWA
				String sql1234 = "Select verkoop.klantnr, verkoop.levernaam, klant.naam, klant.alfacode from verkoop "
						+ "join klant on klant.klantnr = verkoop.klantnr "
						+ "where verkoop.serienummer = '"+p[1]+"'";
				Statement st1234 = myConn.createStatement();
				ResultSet rs1234 = st1234.executeQuery(sql1234);
				while(rs1234.next()) {
					//pobranie kodu alfa klienta bez bialych znaków
					referencje = rs1234.getString("alfacode").trim();
					
					String nazwaKlienta = rs1234.getString("naam");
					String nazwaOdbiorcy = rs1234.getString("levernaam");
					//jeœli klient nie jest odbiorc¹ to wyró¿nij obu:
					
					if(!nazwaKlienta.equals(nazwaOdbiorcy)) {
						referencje = referencje+" / "+nazwaOdbiorcy;
					}
					
				}
			}
			
			//czasem sie moze zdarzyc b³êdne formatowanie daty typu RRRR-MM-DD HH:MM, wiêc jakby co to odcinamy wszystko co jest za dziesi¹tym znakiem:
			if(zadanyTerminMark.length()>10) zadanyTerminMark = zadanyTerminMark.substring(0, 10);
			if(dataKontrakt.length()>10) dataKontrakt = dataKontrakt.substring(0, 10);
			
			//NA TYM POZIOMIE MAMY WSZYSTKIE INFORMACJE O PROJEKCIE WSKAZANYM W PARSTOVERVIEW
			//ALBO WPISUJEMY NOWY PROJEKT DO KALENDARZA ALBO UPDATE ISTNIEJACEGO
			
			Calendar today = Calendar.getInstance();
			SimpleDateFormat data = new SimpleDateFormat("yyyy-MM-dd");
			Statement czyJest = myConn.createStatement();
			
			//sprawdzamy czy dany projekt istnieje w kalendarzu
			String sprawdzanie = "SELECT * FROM calendar WHERE NrMaszyny= '"+projekt+"'";
			ResultSet wynikSprawdzenia = czyJest.executeQuery(sprawdzanie);
			
			//parametr jest oznaczajacy wystepowanie projektu w kalendarzu
			boolean jest = false;
			while (wynikSprawdzenia.next()){
				
				//jesli odnalazlo wpisy w kalendarzu to ustaw parametr jest na true
				jest=true;
			}
			czyJest.close();
			wynikSprawdzenia.close();
			
			//jeœli jest w kalendarzu (oraz jesli daty sa poprawnie sformatowane)
			//UPDATE
			if(jest && !(dataProdukcji == null || dataProdukcji=="" || dataKoniecMontazu == null || dataKoniecMontazu == "")){
				PreparedStatement UpdateProject = myConn.prepareStatement("UPDATE calendar set Klient = ?, DataProdukcji = ?, dataKoniecMontazu = ?, Zakonczone = ?, DataModyfikacji = ?, dataZadanaMarketing = ?, DataKontrakt = ?, Opis = ?, Cena = ?, Waluta = ? WHERE NrMaszyny ='"+projekt+"'");
				UpdateProject.setString(1, referencje);
				UpdateProject.setString(2, dataProdukcji);
				UpdateProject.setString(3, dataKoniecMontazu);
				UpdateProject.setString(4, "0");
				UpdateProject.setString(5, data.format(today.getTime()));
				UpdateProject.setString(6, zadanyTerminMark);
				UpdateProject.setString(7, dataKontrakt);
				UpdateProject.setString(8, opis);
				UpdateProject.setDouble(9, cena);
				UpdateProject.setString(10, waluta);
				UpdateProject.executeUpdate();
				UpdateProject.close();
			}
			
			//jeœli nie ma w kalendarzu (plus sprawdzenie poprawnosci formatowania dat)
			//insert
			if(!jest && !(dataProdukcji == null || dataProdukcji=="" || dataKoniecMontazu == null || dataKoniecMontazu == "") ){
				String InsertQuery = "INSERT INTO calendar VALUES (?, ?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?, ?, ?)";
				PreparedStatement insertProject = myConn.prepareStatement(InsertQuery);
				insertProject.setString(1, projekt);
				insertProject.setString(2, opis);
				insertProject.setString(3, referencje);
				insertProject.setString(4, dataProdukcji);
				insertProject.setString(5, dataKoniecMontazu);
				insertProject.setString(6, "");
				insertProject.setDouble(7, cena);
				insertProject.setString(8, waluta);
				insertProject.setString(9, "");
				insertProject.setString(10, "0");
				insertProject.setString(11, data.format(today.getTime()));
				insertProject.setString(12, zadanyTerminMark);
				insertProject.setString(13, dataKontrakt);
				insertProject.setString(14, "0");
				insertProject.executeUpdate();
				insertProject.close();
			}
		}
		takeDate.close();
		//koniec pobierania projektow z partsoverview
		myConn.close();
	}
	
	private static void fromWerkbons() throws SQLException {
		//stworzenie polaczenia z baza danych
		myConn = DBConnection.dbConnector();
		//sprawdz wszystkie projekty poprzez karty pracy (werkbon) z dzia³ów: ('2', '3', '4', '6', '14', '15')

		Statement takeProjectSchedule2 = myConn.createStatement();
		ResultSet rs2 = takeProjectSchedule2.executeQuery("SELECT werkbon.AFDELING,  werkbon.AFDELINGSEQ, werkbon.OMSCHRIJVING, bestelling.STATUSCODE FROM WERKBON join bestelling on WERKBON.AFDELING = BESTELLING.LEVERANCIER AND  werkbon.afdelingseq = bestelling.ordernummer " +
				"where werkbon.AFDELING in ('2', '3', '4', '6', '14', '15') and bestelling.statuscode ='O' " +
				"group by werkbon.afdelingseq " +
				"order by bestelling.leverdatum asc ");
		
		while(rs2.next()){
			
			String typProj = rs2.getString("Afdeling");
			String nrProj = rs2.getString("afdelingseq");
			Statement check = myConn.createStatement();
			
			//sprawdza, czy dany projekt jest w partsoverview. jesli jest to pobiera jego nazwe. 
			//sprawdzenie z partsoverview polega na sprawdzeniu, czy badany projekt nie jest podprojektem innego projektu
			//jesli subproject to w partsovervew: opis == 'subproject'
			ResultSet check2 = check.executeQuery("Select Description from Partsoverview where OrderNo = '"+typProj+"/"+nrProj+"'");
			String opis = "";
			while(check2.next()){
				opis = check2.getString("Description");
			}
			String ElMaszyny = opis;
			check.close();
			check2.close();
			if(opis==null)
				opis = "";
			
			//jeœli to nie jest subproject:
			if( !opis.equals("subproject") && ( !( (rs2.getString("AFDELING").equals("2") || rs2.getString("AFDELING").equals("6")) && (rs2.getString("AFDELINGSEQ").length()>6))
					|| rs2.getString("AFDELINGSEQ").equals("17052020") ) ){
				
				typProj = rs2.getString("Afdeling");
				nrProj = rs2.getString("afdelingseq");
				String projekt = typProj+"/"+nrProj;
				String TypMaszyny  = projekt;
				opis = rs2.getString("Omschrijving");
				String dataKoniecMontazu = "", referencje = "", zadanyTerminMark = "", dataKontrakt = "", dataProdukcji = "";
				
				//uzupelnienie informacji o projekcie z zamowienia produkcyjnego
				Statement b = myConn.createStatement();
				boolean other = true;
				ResultSet rs = b.executeQuery("SELECT bestelling.REFERENTIE, bestelling.LEVERDATUM, bestellingdetail.leveringsdatumvoorzien as Produkcji, verkoop.leverdatum_gevraagd as zadany, verkoop.leverdatum_bevestigd as kontrakt FROM bestelling "
						+ " left outer join bestellingdetail on bestelling.leverancier =  bestellingdetail.leverancier and  bestelling.ordernummer =  bestellingdetail.ordernummer "
						+ " left outer join verkoop on bestelling.ordernummer = verkoop.serienummer " +
						" where bestelling.leverancier = '"+typProj+"' and bestelling.ordernummer = '"+nrProj+"'" );
				while(rs.next()){
					other=false;
					referencje = rs.getString("Referentie");
					zadanyTerminMark = rs.getString("zadany");
					dataKoniecMontazu = rs.getString("LEVERDATUM");
					dataProdukcji = rs.getString("Produkcji");
					dataKontrakt = rs.getString("kontrakt");
				}
				b.close();
				//kontrola dat
				if(zadanyTerminMark==null) zadanyTerminMark = "";
				if(dataKontrakt==null||dataKontrakt.equals("empty")) dataKontrakt = "";
				if(zadanyTerminMark.length()>10) zadanyTerminMark = zadanyTerminMark.substring(0, 10);
				if(dataKontrakt.length()>10) dataKontrakt = dataKontrakt.substring(0, 10);
				
				//je¿eli nikt nie wpisa³ referencji do projektu produkcyjnego, to wez ze zlecenia nazwe klienta:
				if(referencje == null && !other) {
					//SPRAWDZENIE KLIENTA I MIEJSCA GDZIE DOSTAWA
					String sql1234 = "Select verkoop.klantnr, verkoop.levernaam, klant.naam, klant.alfacode from verkoop "
							+ "join klant on klant.klantnr = verkoop.klantnr "
							+ "where verkoop.serienummer = '"+nrProj+"'";
					Statement st1234 = myConn.createStatement();
					ResultSet rs1234 = st1234.executeQuery(sql1234);
					while(rs1234.next()) {
						//pobranie kodu alfa klienta bez bialych znaków
						referencje = rs1234.getString("alfacode").trim();
						
						String nazwaKlienta = rs1234.getString("naam");
						String nazwaOdbiorcy = rs1234.getString("levernaam");
						//jeœli klient nie jest odbiorc¹ to wyró¿nij obu:
						
						if(!nazwaKlienta.equals(nazwaOdbiorcy)) {
							referencje = referencje+" / "+nazwaOdbiorcy;
						}
					}
				}
							
				Statement czyJest = myConn.createStatement();
				String sprawdzanie = "SELECT * FROM calendar WHERE NrMaszyny= '"+projekt+"'";
				ResultSet wynikSprawdzenia = czyJest.executeQuery(sprawdzanie);
				
				Calendar today = Calendar.getInstance();
				SimpleDateFormat data = new SimpleDateFormat("yyyy-MM-dd");
				boolean czyZrobiloUpdate = false;
				
				while (wynikSprawdzenia.next() && !(dataProdukcji == null || dataProdukcji=="" || dataKoniecMontazu == null || dataKoniecMontazu == "")){
					PreparedStatement UpdateProject = myConn.prepareStatement("UPDATE calendar set Klient = ?, DataProdukcji = ?, dataKoniecMontazu = ?, Zakonczone = ?, DataModyfikacji = ?, dataZadanaMarketing = ?, DataKontrakt = ?, Opis = ? WHERE NrMaszyny ='"+projekt+"'");
					UpdateProject.setString(1, referencje);
					UpdateProject.setString(2, dataProdukcji);
					UpdateProject.setString(3, dataKoniecMontazu);
					UpdateProject.setString(4, "0");
					UpdateProject.setString(5, data.format(today.getTime()));
					UpdateProject.setString(6, zadanyTerminMark);
					UpdateProject.setString(7, dataKontrakt);
					UpdateProject.setString(8, opis);
					UpdateProject.executeUpdate();
					UpdateProject.close();
					czyZrobiloUpdate = true;
				}
				czyJest.close();
				
				if(!czyZrobiloUpdate && !(dataProdukcji == null || dataProdukcji=="" || dataKoniecMontazu == null || dataKoniecMontazu == "")){
					String InsertQuery = "INSERT INTO calendar VALUES (?,?,?, ?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?, ?, ?)";
					PreparedStatement insertProject = myConn.prepareStatement(InsertQuery);
					insertProject.setString(1, projekt);
					insertProject.setString(2, TypMaszyny);
					insertProject.setString(3, opis);
					insertProject.setString(4, ElMaszyny);
					insertProject.setString(5, referencje);
					insertProject.setString(6, dataProdukcji);
					insertProject.setString(7, dataKoniecMontazu);
					insertProject.setString(8, "");
					insertProject.setString(9, "0");
					insertProject.setString(10, "");
					insertProject.setString(11, "");
					insertProject.setString(12, "0");
					insertProject.setString(13, data.format(today.getTime()));
					insertProject.setString(14, zadanyTerminMark);
					insertProject.setString(15, dataKontrakt);
					insertProject.setString(16, "0");
					insertProject.executeUpdate();
					insertProject.close();
				}
			}
		}
		takeProjectSchedule2.close();	
		myConn.close();
	}
	
	private static void fromVerkoop() throws SQLException {
		
		//stworzenie polaczenia z baza danych
		myConn = DBConnection.dbConnector();
		
		//z bazy sprzedazy (szukaj nieuruchomionych nowych zlecen):
		String sql00 = "SELECT VERKOOP.SERIENUMMER,  VERKOOP.LEVERNAAM AS KLIENT, VERKOOP.BESTELDATUM AS DATAZAMOWIENIA, verkoop.verkoopprijs as cena, verkoop.munt as waluta, VERKOOP.LEVERDATUM_GEVRAAGD AS DATAZADANA, VERKOOPDETAIL.ARTIKELCODE, VERKOOPDETAIL.ARTIKELOMSCHRIJVING AS OPIS, VERKOOP.LEVERDATUM_BEVESTIGD AS KONTRAKT FROM VERKOOP"
				+ "	LEFT JOIN VERKOOPDETAIL ON VERKOOP.KLANTNR = VERKOOPDETAIL.KLANTNR AND VERKOOP.BESTELLINGNR = VERKOOPDETAIL.BESTELLINGNR "
				+ "WHERE STATUSCODE = 'O' AND "
				+ "MACHINEBESTELLING = 1 AND "
				+ "LEVERNAAM <> 'TOKARKI MAGAZYNOWE' AND "
				+ "VERKOOP.SERIENUMMER < 300000 AND "
				+ "VERKOOPDETAIL.SEQUENTIE IN (SELECT MIN(VERKOOPDETAIL.SEQUENTIE) FROM VERKOOPDETAIL WHERE VERKOOP.KLANTNR = VERKOOPDETAIL.KLANTNR AND VERKOOP.BESTELLINGNR = VERKOOPDETAIL.BESTELLINGNR)";
		Statement st00 = myConn.createStatement();
		ResultSet rs00 = st00.executeQuery(sql00);
		Calendar today = Calendar.getInstance();
		SimpleDateFormat data = new SimpleDateFormat("yyyy-MM-dd");
		String klient = "";
		while(rs00.next()){
			//NUMER SERYJNY
			String nrMasz = rs00.getString("VERKOOP.SERIENUMMER");
			String [] opisDzielony = rs00.getString("OPIS").split("Z");
			String opis = opisDzielony[0];		
			
			//SPRAWDZENIE KLIENTA I MIEJSCA GDZIE DOSTAWA
			String sql1234 = "Select verkoop.klantnr, verkoop.levernaam, klant.naam, klant.alfacode from verkoop "
					+ "join klant on klant.klantnr = verkoop.klantnr "
					+ "where verkoop.serienummer = '"+nrMasz+"'";
			Statement st1234 = myConn.createStatement();
			ResultSet rs1234 = st1234.executeQuery(sql1234);
			while(rs1234.next()) {
				//pobranie kodu alfa klienta bez bialych znaków
				klient = rs1234.getString("alfacode").trim();
				
				String nazwaKlienta = rs1234.getString("naam");
				String nazwaOdbiorcy = rs1234.getString("levernaam");
				//jeœli klient nie jest odbiorc¹ to wyró¿nij obu:
				
				if(!nazwaKlienta.equals(nazwaOdbiorcy)) {
					klient = klient+" / "+nazwaOdbiorcy;
				}
			}
			rs1234.close();
			st1234.close();
			
			//temat nowych projektow jest dosc skomplikowany: w kalendarzu mo¿e ju¿ wystêpowaæ projekt 0/ -> nieuruchomiony w dziale produkcja
			//ale mo¿e równie¿ wystêpowaæ podprojekt mimo, ¿e g³ówny projekt nigdy nie powstal (karty pracy etc)
			//nalezy sprawdzic wszystkie warunki -> kiedy projekt juz byl w kalendarzu nieuruchomiony, uruchomiony, podprojekt, etc
			String sql0001 = "Select nrMaszyny from calendar where (nrMaszyny like '0/"+nrMasz+"%') or (nrMaszyny like '%"+nrMasz+"%' and nrMaszyny not like '0/"+nrMasz+"%' and Zakonczone = 0) order by nrMaszyny desc";
			ResultSet rs0001 = myConn.createStatement().executeQuery(sql0001);
			//parametr jesli projekt istnieje w kalendarzu
			boolean k=false;
			double cena = 0;
			//pomocniczy parametr pomagajacy kiedy jest podprojekt 
			boolean pomocniczy1 = true;
			while(rs0001.next()) {
				
				//je¿eli w bazie jest tylko nieuruchomiony:
				if(rs0001.getString("nrMaszyny").equals("0/"+nrMasz)&&!k){
					cena = rs00.getDouble("cena");
					PreparedStatement UpdateProject = myConn.prepareStatement("UPDATE calendar set Klient = ?, DataProdukcji = ?, dataKoniecMontazu = ?, Zakonczone = ?, DataModyfikacji = ?, dataZadanaMarketing = ?, DataKontrakt = ?, Opis = ?, Komentarz = ?, Cena = ?, Waluta = ? WHERE NrMaszyny ='0/"+nrMasz+"'");
					UpdateProject.setString(1, klient);
					UpdateProject.setString(2, "");
					UpdateProject.setString(3, rs00.getString("DATAZADANA"));
					UpdateProject.setString(4, "0");
					UpdateProject.setString(5, data.format(today.getTime()));
					UpdateProject.setString(6, rs00.getString("DATAZADANA"));
					UpdateProject.setString(7, rs00.getString("KONTRAKT"));
					UpdateProject.setString(8, opis);
					UpdateProject.setString(9, "Niezarezerwowany");
					UpdateProject.setDouble(10, cena);
					UpdateProject.setString(11, rs00.getString("Waluta"));
					UpdateProject.executeUpdate();
					UpdateProject.close();
				}
				//je¿eli w kalendarzu jest wylacznie podprojekt (2/nrmasz01) bez projektu glownego:
				else if(rs0001.getString("nrMaszyny").equals("2/"+nrMasz+"01")&&!k) {
					pomocniczy1=false;
					
				}
				//je¿eli jest jakikolwiek inny projekt, np 6/nrmasz lub 2/nrmasz
				else if (!k){
					Statement UpdateProject = myConn.createStatement();
					//ustaw projekt 0/nrmasz jako zakonczony, poniewaz jest juz jakies uruchomienie produkcyjne
					UpdateProject.executeUpdate("UPDATE calendar set Zakonczone = 1 WHERE NrMaszyny ='0/"+nrMasz+"'");
					UpdateProject.close();
				}
				else if(k) {
					//je¿eli jest kolejny projekt/podprojekt tej maszyny
					pomocniczy1 = true;
				}
				k=true;
			}
			
			//jeœli jedynym projektem poza 0/nrmasz jest projekt 2/nrmasz01 -> karty pracy to zachowaj 0/
			if(!pomocniczy1) {
				Statement st = myConn.createStatement();
				st.executeUpdate("Update calendar set Zakonczone = 1, DataModyfikacji = '"+data.format(today.getTime())+"' where nrMaszyny = '2/"+nrMasz+"01'");
				st.executeUpdate("Update calendar set Zakonczone = 0, DataModyfikacji = '"+data.format(today.getTime())+"' where nrMaszyny = '/"+nrMasz+"'");
				st.close();
			}
			
			
			//je¿eli w bazie nie ma ¿adnego projektu
			
			if(!k){
				String InsertQuery = "INSERT INTO calendar VALUES (?, ?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?, ?, ?)";
				PreparedStatement insertProject = myConn.prepareStatement(InsertQuery);
				insertProject.setString(1, "0/"+nrMasz);
				insertProject.setString(2, opis);
				insertProject.setString(3, rs00.getString("KLIENT"));
				insertProject.setString(4, "");
				insertProject.setString(5, rs00.getString("DATAZADANA"));
				insertProject.setString(6, "");
				insertProject.setDouble(7, cena);
				insertProject.setString(8, rs00.getString("waluta"));
				insertProject.setString(9, "Niezarezerwowany");
				insertProject.setString(10, "0");
				insertProject.setString(11, data.format(today.getTime()));
				insertProject.setString(12, rs00.getString("DATAZADANA"));
				insertProject.setString(13, rs00.getString("KONTRAKT"));
				insertProject.setString(14, "0");
				insertProject.executeUpdate();
				insertProject.close();
			}
		}
		rs00.close();
		st00.close();
		
	}
	
	//metoda checkTypes sprawdza jakiego typu jest maszyna - po opisie
	//typy:
	/*
	 * 560-710 - TUR MN
	 * CONTUR
	 * 800 - TUR MN 800 I 930
	 * KONW - MASZYNY KONWENCJONALNE
	 * FCT - FCT + FTM = OGOLNIE SKOSNE
	 * 1350 - MASZYNY WYSOKIE OD 1100 DO 1550 
	 */
	public static void checkTypes() throws SQLException {
		// TODO Auto-generated method stub
		Connection connection = DBConnection.dbConnector();
		PreparedStatement UpdateProject = connection.prepareStatement("UPDATE calendar set Typ = ? WHERE NrMaszyny = ? ");
		String sql1 = "SELECT * FROM calendar where Typ = '' and (NrMaszyny like '0/%' or NrMaszyny like '2/%' or NrMaszyny like '6/%') and Zakonczone = 0";
		Statement st1 = connection.createStatement();
		ResultSet rs1 = st1.executeQuery(sql1);
		while(rs1.next()){
			String numer = rs1.getString("NrMaszyny");
			String Opis = rs1.getString("Opis");
			String typ = "";
			//sprawdz czy 560-710;
			if((Opis.contains("MN")||Opis.contains("SC"))&&(Opis.contains("560")||Opis.contains("630")||Opis.contains("710"))){
				typ = "560-710";
			}
			else if(Opis.contains("CONTUR")){
				typ = "CONTUR";
			}
			else if(((Opis.contains("800")&&!Opis.contains("8000")) || Opis.contains("930"))&&Opis.contains("MN")){
				typ = "800";
			}
			else if(Opis.contains("CONTUR")){
				typ = "CONTUR";
			}
			else if((!Opis.contains("MN")&&!Opis.contains("SC"))&&(Opis.contains("560")||Opis.contains("630")||Opis.contains("710"))){
				typ = "KONW";
			}
			else if(Opis.contains("FCT")||Opis.contains("FTM")){
				typ = "FCT";
			}
			else if(Opis.contains("1150")||Opis.contains("1100") ||Opis.contains("1350") ||Opis.contains("1550")){
				typ = "1350";
			}
			if(!typ.equals("")){
				UpdateProject.setString(1, typ);
				UpdateProject.setString(2, numer);
				UpdateProject.executeUpdate();
			}
		}
		rs1.close();
		st1.close();	
		UpdateProject.close();
		connection.close();
	}
	
	//je¿eli po wszystkich analizach dot. projektów, jakiœ projekt nie zosta³ zanalizowany / zupdate'owany to znaczy, ze zostal zamkniety
	//metoda checkProjectSchedule oznacza wszystkie niezupdate'owane projekty jako zakonczone
	public static void checkProjectSchedule() throws SQLException{
		myConn = DBConnection.dbConnector();
		Calendar today = Calendar.getInstance();
		SimpleDateFormat data = new SimpleDateFormat("yyyy-MM-dd");
		String sql = "update calendar set Zakonczone = 1 where DataModyfikacji <> '"+data.format(today.getTime())+"'";
		Statement a = myConn.createStatement();
		a.executeUpdate(sql);
		a.close();
		myConn.close();
	}
	 
	//metoda tworz¹ca jeden rz¹d w dokumencie
	private static void createARow(String [] dane, PdfPTable t) {
		
		boolean red = false;
		try {
            Date wysylka = new SimpleDateFormat("yyyy-MM-dd").parse(dane[4]);
            Date produkcja = new SimpleDateFormat("yyyy-MM-dd").parse(dane[3]);
            if (produkcja.compareTo(wysylka) > 0) red = true;
        } catch (ParseException e) {
        	System.out.println(dane[0]+" Problem z formatem daty");
        }
		
		for(int i = 0; i< dane.length; i++) {
			PdfPCell cell1 = new PdfPCell(new Phrase(dane[i], ffont));
			cell1.setMinimumHeight(15);
			cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			if(red) cell1.setBackgroundColor(BaseColor.RED);
			t.addCell(cell1);
		}
		
	}

	//stworzenie naglowka dokumentu
	private static void createAHeader(PdfPTable t){
		
		String [] nagl = new String[] {"Numer projektu", "Nazwa", "Klient", "Data produkcji czêœci", "Data koñca monta¿u"};
		
		for(int i = 0; i< nagl.length; i++) {
			PdfPCell cell1 = new PdfPCell(new Phrase(nagl[i], ffont));
			cell1.setMinimumHeight(30);
			cell1.setBackgroundColor(BaseColor.ORANGE);
			cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
			t.addCell(cell1);
		}
	}
	
	//metoda tworzaca dokument - harmonogram wszystkich projektow
	public static void createPDFAll() throws SQLException{
		System.out.println("Tworzenie harmonogramu wszystkich projektów");
		Connection myConn = DBConnection.dbConnector();
		Document ps = new Document();
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		try {
			String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
			String name = "Harmonogram wszystkich zamowien.pdf";
			File f = new File(path+name);
			if(f.exists() && !f.isDirectory())
				name = godz.format(date.getTime())+" "+name;
			PdfWriter writer = PdfWriter.getInstance(ps, new FileOutputStream(path+name));
			ps.open();
			writer.setPageEvent(new PDF_MyFooter());
			PdfPTable table = new PdfPTable(5);
			float widths[] = new float[] { 1, 3, 2, 1, 1};
			createAHeader(table);
			Statement pobierzProjectSchedule = myConn.createStatement();
			String sql = "Select nrMaszyny, opis, klient, dataprodukcji, dataKoniecMontazu from calendar where zakonczone = 0 order by dataKoniecMontazu, nrMaszyny";
			ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql);
			
			while(ProjectSchedule.next()){
				String calyNumer = ProjectSchedule.getString("nrMaszyny");
				String [] p = calyNumer.split("/");
				String ProjectGroup = p[0];
				String ProjectNumber = p[1];
				String DeliveryDate = ProjectSchedule.getString("dataKoniecMontazu");
				String ProductionDate = ProjectSchedule.getString("dataProdukcji");
				String ProjectName = ProjectSchedule.getString("Opis");
				String klient = ProjectSchedule.getString("klient");
				
				if((DeliveryDate!=null && ProductionDate != null)||DeliveryDate!="" && ProductionDate != "") {
					String numerProjektu = ProjectNumber;
					if(!ProjectGroup.equals("0")) 
						numerProjektu = ProjectGroup + "/" + numerProjektu;
					createARow(new String [] {numerProjektu, ProjectName, klient, ProductionDate, DeliveryDate}, table);
				}
			}
			pobierzProjectSchedule.close();
			ProjectSchedule.close();
			table.setWidthPercentage(100);
			table.setWidths(widths);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setHeaderRows(1);
			ps.add(table);
			ps.add(new Phrase(" "));
			ps.close();
			myConn.close();
			System.out.println("Koniec harmonogramu wszystkich projektów");
			
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//metoda tworzaca dokument - wszystkie projekty MASZYN 2/ i 6/ i 0/
	public static void createPDFMachines() throws SQLException{
		System.out.println("Tworzenie harmonogramu maszyn");
		Document ps = new Document();
		Connection myConn = DBConnection.dbConnector();
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
		String name = "Harmonogram maszyn.pdf";
		File f = new File(path+name);
		if(f.exists() && !f.isDirectory())
			name = godz.format(date.getTime())+" "+name;
		PdfWriter writer;
		try {
			writer = PdfWriter.getInstance(ps, new FileOutputStream(path+name));
			writer.setPageEvent(new PDF_MyFooter());
		} catch (FileNotFoundException | DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ps.open();
		PdfPTable table = new PdfPTable(6);
		float widths[] = new float[] { 10, 30, 15, 10, 10, 10};
		createAHeader(table);
		
		//poszerzenie listy o jedna kolumne - data wysylki
		PdfPCell cell4 = new PdfPCell(new Phrase("Data wysy³ki", ffont));
		//PdfPCell cell4 = new PdfPCell(new Phrase("Kwota", ffont));
		cell4.setMinimumHeight(30);
		cell4.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell4.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell4.setBackgroundColor(BaseColor.ORANGE);
		table.addCell(cell4);
		
		Statement pobierzProjectSchedule = myConn.createStatement();
		String sql = "Select nrMaszyny, opis, klient, dataprodukcji, dataKontrakt, dataKoniecMontazu, komentarz from calendar where Zakonczone = 0 and Wyslano <> 1 order by dataKoniecMontazu, nrMaszyny";
		ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql);
		
		while(ProjectSchedule.next()){
			String calyNumer = ProjectSchedule.getString("nrMaszyny");
			String [] p = calyNumer.split("/");
			String ProjectGroup = p[0];
			String ProjectNumber = p[1];
			
			String DeliveryDate = ProjectSchedule.getString("dataKoniecMontazu");
			String ProductionDate = ProjectSchedule.getString("dataProdukcji");
			String Kontrakt = ProjectSchedule.getString("dataKontrakt");
			String ProjectName = ProjectSchedule.getString("Opis");
			String klient = ProjectSchedule.getString("klient");
			if((ProjectGroup.equals("2") || ProjectGroup.equals("6")||ProjectGroup.equals("0"))&&(ProjectNumber.length()==6||ProjectNumber.equals("17052020")))
				if((DeliveryDate!=null && ProductionDate != null)||DeliveryDate!="" && ProductionDate != "") {
					String numerProjektu = ProjectNumber;
					if(!ProjectGroup.equals("0")) 
						numerProjektu = ProjectGroup + "/" + numerProjektu;
					createARow(new String [] {numerProjektu, ProjectName, klient, ProductionDate, DeliveryDate, Kontrakt}, table);
				}
		}
		pobierzProjectSchedule.close();
		ProjectSchedule.close();
		table.setWidthPercentage(100);
		try {
			table.setWidths(widths);
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		table.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.setHeaderRows(1);
		try {
			ps.add(table);
			ps.add(new Phrase(" "));
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ps.close();
		myConn.close();
		//Desktop.getDesktop().open(new File("projectSchedule.pdf"));
		System.out.println("Koniec harmonogramu maszyn");
	}
	
	//metoda znajdujaca zmiane numeru -> podpinanie projektu pod projekt itd
	public static void findChangeOfNumber() throws SQLException{
		Connection connection = DBConnection.dbConnector();
		String sql1 = "select NrMaszyny, Opis from calendar";
		Statement st1 = connection.createStatement();
		ResultSet rs1 = st1.executeQuery(sql1);
		while(rs1.next()){
			String nrMaszyny = rs1.getString(1);
			String opis = rs1.getString(2);
			String[] a = nrMaszyny.split("/");
			//sprawdzenie czy maszyna a[] wchodzi w sklad innego projektu
			String sql2 = "Select AFDELING, AFDELINGSEQ from BESTELLING where leverancier  ='"+a[0]+"' and ordernummer = '"+a[1]+"' and afdeling <>'7' and afdeling <> '0'";
			Statement st2 = connection.createStatement();
			ResultSet rs2 = st2.executeQuery(sql2);
			while(rs2.next()){
				
				boolean czyZwykla = false;
				String grupa = rs2.getString(1);
				String numer = rs2.getString(2);
				String projektNadrzedny = grupa+"/"+numer;
				//sprawdz status nadrzednego
				String status = "";
				String sql8 = "Select statuscode from BESTELLING where leverancier  ='"+grupa+"' and ordernummer = '"+numer+"'";
				Statement st6 = connection.createStatement();
				ResultSet rs6 = st2.executeQuery(sql8);
				while(rs6.next()){
					status = rs6.getString(1);
				}
				rs6.close();
				st6.close();
				
				String nazwaNadrzednego = "";
				String sql9 = "select artikelomschrijving from bestellingdetail where leverancier  ='"+grupa+"' and ordernummer = '"+numer+"'";
				Statement st7 = connection.createStatement();
				ResultSet rs7 = st2.executeQuery(sql9);
				while(rs7.next()){
					nazwaNadrzednego = rs7.getString(1);
				}
				rs7.close();
				st7.close();
				
				//update nazwy projektu z EX 
				if(status==null) status = "";
				String sql3 = "Update calendar set Komentarz = 'Zmiana nr seryjnego na "+projektNadrzedny+"', Zakonczone = 2 where NrMaszyny = '"+nrMaszyny+"'";
				String sql4 = "Update calendar set Komentarz = '"+nrMaszyny+"' where NrMaszyny = '"+projektNadrzedny+"'";
				String sql5 = "Update calendar set opis = 'EX "+nrMaszyny+ " "+nazwaNadrzednego+"' where nrMaszyny ='"+projektNadrzedny+"'";
				
				if(a[1].equals(numer) || a[1].startsWith(numer)){
					czyZwykla=true;
					
				}
				
				if(!a[0].equals("14") && !czyZwykla && !status.contentEquals("H")){
					Statement st3 = connection.createStatement();
					st3.executeUpdate(sql3);
					st3.close();
					Statement st4 = connection.createStatement();
					st4.executeUpdate(sql4);
					st4.close();
					if(!opis.startsWith("EX") && !a[1].startsWith(numer)){
						Statement st5 = connection.createStatement();
						st5.executeUpdate(sql5);
						st5.close();
					}
				}
			}
			rs2.close();
			st2.close();
		}
		rs1.close();
		st1.close();
	}
}
